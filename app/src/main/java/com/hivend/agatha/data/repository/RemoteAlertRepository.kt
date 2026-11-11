package com.hivend.agatha.data.repository

import com.hivend.agatha.data.local.alert.AlertActionEntity
import com.hivend.agatha.data.local.alert.AlertOutbox
import com.hivend.agatha.data.remote.ApiError
import com.hivend.agatha.data.remote.ApiErrorBody
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.remote.alerts.ActionBodyDto
import com.hivend.agatha.data.remote.alerts.AlertDto
import com.hivend.agatha.data.remote.alerts.AlertsApi
import com.hivend.agatha.data.remote.alerts.ClassificationRequestDto
import com.hivend.agatha.data.remote.alerts.LabelRequestDto
import com.hivend.agatha.data.remote.alerts.ORIGIN_MOBILE_APP
import com.hivend.agatha.data.remote.alerts.ObservationRequestDto
import com.hivend.agatha.data.remote.alerts.toApi
import com.hivend.agatha.data.remote.alerts.toDomain
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.ClassifyOutcome
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.domain.model.TagRecord
import com.hivend.agatha.domain.repository.AlertRepository
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Alertas leídas y enviadas a la API de gestión de alertas (la de la web). Mantiene una caché
 * en memoria con lo que respondió el servidor; la lista, el detalle y el historial de dispositivo
 * la van llenando. Es @Singleton para que todas las pantallas vean el mismo estado.
 *
 * Offline (HU-7.2, RN-17): lo que no llega al servidor se guarda en la cola de salida de Room
 * ([AlertOutbox]) con su `client_record_id`, se ve en la pantalla de Sincronización y lo envía
 * WorkManager al recuperar la red, aunque la app se haya cerrado. Mientras tanto las pantallas
 * lo ven aplicado sobre la caché. La clasificación se intenta enviar de inmediato porque el
 * detalle necesita saber si la web ya la había clasificado; la etiqueta y la observación de un
 * reporte de inspección van siempre por la cola.
 */
@Singleton
class RemoteAlertRepository @Inject constructor(
    private val api: AlertsApi,
    private val json: Json,
    private val outbox: AlertOutbox,
) : AlertRepository {

    private val alerts = MutableStateFlow<List<Alert>>(emptyList())
    private val inspections = outbox.observeReports()
    private val unsynced = outbox.observeUnsynced()

    /** Ids que trajo la última lista, para retirar de la caché las que ya no aplican. */
    private var lastListIds: Set<String> = emptySet()

    override fun observeAlerts(): Flow<List<Alert>> =
        combine(alerts, unsynced) { list, pending -> list.map { overlayPending(it, pending) } }

    override fun observeAlert(alertId: String): Flow<Alert?> =
        observeAlerts().map { list -> list.find { it.id == alertId } }

    // ---- Lectura -------------------------------------------------------------------------

    override suspend fun refreshAlerts(): AlertResult<Unit> = fetch {
        val (pending, recent) = coroutineScope {
            val pending = async { api.pending().items }
            val recent = async { api.alerts(page = 1).items }
            pending.await() to recent.await()
        }
        val rows = (pending + recent).distinctBy { it.id }.mapNotNull { it.toDomain() }
        val ids = rows.map { it.id }.toSet()
        alerts.update { current -> current.filterNot { it.id in lastListIds && it.id !in ids } }
        lastListIds = ids
        upsertRows(rows)
    }

    override suspend fun loadAlert(alertId: String): AlertResult<Unit> {
        val result = fetch {
            val alert = api.alert(alertId).toDomain() ?: throw SerializationException("Unsupported alert $alertId")
            alerts.update { current ->
                val cached = current.firstOrNull { it.id == alert.id }
                replace(current, alert.copy(classification = mergeClassification(cached?.classification, alert.classification)))
            }
        }
        // Un id con formato que la API no acepta (400 INVALID_INPUT, p. ej. el ALR-PRUEBA-000001 de
        // los push de prueba) es, para quien abre el detalle, una alerta que no existe.
        val invalidId = result is AlertResult.Failure &&
            (result.failure as? AlertFailure.Rejected)?.code == API_CODE_INVALID_INPUT
        return if (invalidId) AlertResult.Failure(AlertFailure.NotFound) else result
    }

    /**
     * El listado no trae observaciones ni el detalle de clasificación y etiqueta: solo vienen en
     * `GET /api/alerts/{id}`. Primero se publican las filas y luego se completa cada alerta, así el
     * historial tiene esos datos sin que haga falta abrir cada alerta.
     */
    override suspend fun refreshDevice(deviceId: Int): AlertResult<Unit> {
        val rows = mutableListOf<Alert>()
        val listed = fetch {
            var seen = 0
            for (page in 1..DEVICE_PAGES) {
                val result = api.alerts(device = deviceId.toString(), page = page)
                rows += result.items.mapNotNull(AlertDto::toDomain)
                seen += result.items.size
                if (result.items.size < result.pageSize || seen >= result.total) break
            }
            upsertRows(rows)
        }
        if (listed is AlertResult.Failure) return listed
        val permits = Semaphore(DETAIL_CONCURRENCY)
        val details = coroutineScope {
            rows.map { row -> async { permits.withPermit { loadAlert(row.id) } } }.awaitAll()
        }
        return details.firstOrNull { it is AlertResult.Failure } ?: AlertResult.Success(Unit)
    }

    /** Las filas de la lista no traen el detalle: se conserva el que ya se tenía de cada alerta. */
    private fun upsertRows(rows: List<Alert>) = alerts.update { current ->
        val byId = current.associateBy { it.id }
        rows.fold(current) { acc, row -> replace(acc, mergeRow(byId[row.id], row)) }
    }

    private fun mergeRow(old: Alert?, row: Alert): Alert {
        if (old == null) return row
        return row.copy(
            levelTimeline = old.levelTimeline,
            observations = old.observations,
            indicatorValue = old.indicatorValue,
            classification = mergeClassification(old.classification, row.classification),
        )
    }

    /** List and detail responses can omit classification metadata already present in the cache. */
    private fun mergeClassification(cached: ClassificationRecord?, incoming: ClassificationRecord?): ClassificationRecord? =
        when {
            incoming == null -> cached
            cached == null || cached.classification != incoming.classification -> incoming
            else -> incoming.copy(
                classifiedAt = incoming.classifiedAt ?: cached.classifiedAt,
                origin = incoming.origin ?: cached.origin,
                tag = mergeTag(cached.tag, incoming.tag),
            )
        }

    private fun mergeTag(
        cached: TagRecord?,
        incoming: TagRecord?,
    ): TagRecord? = when {
        incoming == null -> cached
        cached == null || cached.tag != incoming.tag -> incoming
        else -> incoming.copy(
            taggedAt = incoming.taggedAt ?: cached.taggedAt,
            origin = incoming.origin ?: cached.origin,
        )
    }

    /** An accepted online action is visible immediately, even if the following GET is stale. */
    private fun applyClassificationLocally(action: AlertActionEntity) {
        val classification = action.classification ?: return
        alerts.update { current ->
            val cached = current.firstOrNull { it.id == action.alertId } ?: return@update current
            if (cached.classification != null) return@update current
            val updated = cached.classify(
                classification = classification,
                at = action.recordedAt,
                origin = Origin.MobileApp(action.deviceIdentifier),
            )
            replace(current, updated)
        }
    }

    private fun replace(list: List<Alert>, alert: Alert): List<Alert> =
        (list.filterNot { it.id == alert.id } + alert).sortedByDescending { it.startedAt }

    /** Lo que este celular guardó y aún no llegó al servidor se sigue viendo sobre lo descargado. */
    private fun overlayPending(alert: Alert, pending: List<AlertActionEntity>): Alert {
        var result = alert
        for (action in pending) {
            if (action.alertId != alert.id) continue
            val origin = Origin.MobileApp(action.deviceIdentifier)
            val classification = action.classification
            val tag = action.tag
            result = when {
                action.type == SyncRecordType.CLASSIFICATION && classification != null && result.classification == null ->
                    result.classify(classification, action.recordedAt, origin)
                action.type == SyncRecordType.INSPECTION_REPORT && tag != null ->
                    runCatching { result.addTag(TagRecord(tag, action.tagOtherDetail, action.recordedAt, origin)) }
                        .getOrDefault(result)
                else -> result
            }
        }
        return result
    }

    // ---- Observaciones -------------------------------------------------------------------

    override suspend fun addObservation(alertId: String, text: String, origin: Origin): AlertResult<Unit> {
        val posted = post { api.observe(alertId, ObservationRequestDto(text, ORIGIN_MOBILE_APP, origin.deviceIdOrNull())) }
        return when (posted) {
            is Posted.Ok, is Posted.SavedAsConflict -> {
                loadAlert(alertId)
                AlertResult.Success(Unit)
            }
            else -> AlertResult.Failure(posted.toFailure())
        }
    }

    // ---- Clasificación -------------------------------------------------------------------

    override suspend fun classifyAlert(
        alertId: String,
        classification: AlertClassification,
        at: Instant,
        origin: Origin,
    ): AlertResult<ClassifyOutcome> {
        // Ya hay una en la cola: la envía la sincronización, con su mismo client_record_id.
        if (unsynced.first().any { it.alertId == alertId && it.type == SyncRecordType.CLASSIFICATION }) {
            return AlertResult.Success(ClassifyOutcome.QueuedOffline)
        }
        val action = AlertActionEntity(
            id = UUID.randomUUID().toString(),
            alertId = alertId,
            type = SyncRecordType.CLASSIFICATION,
            classification = classification,
            deviceIdentifier = origin.deviceIdOrNull().orEmpty(),
            recordedAt = at,
        )
        return when (val posted = postAction(action)) {
            is Posted.Ok -> {
                applyClassificationLocally(action)
                loadAlert(alertId)
                AlertResult.Success(ClassifyOutcome.Saved)
            }
            is Posted.SavedAsConflict -> {
                val loaded = loadAlert(alertId)
                val existing = posted.body.existing?.toDomain()
                    ?: alerts.value.find { it.id == alertId }?.classification
                if (existing == null || loaded is AlertResult.Failure) AlertResult.Success(ClassifyOutcome.Saved)
                else AlertResult.Success(ClassifyOutcome.SavedAsConflict(existing))
            }
            Posted.Transient -> {
                // Sin red: la clasificación se muestra ya en el celular y la envía la cola.
                outbox.enqueue(listOf(action))
                AlertResult.Success(ClassifyOutcome.QueuedOffline)
            }
            else -> AlertResult.Failure(posted.toFailure())
        }
    }

    override fun observeClassificationPendingSync(alertId: String): Flow<Boolean> =
        unsynced.map { list -> list.any { it.alertId == alertId && it.type == SyncRecordType.CLASSIFICATION } }

    // ---- Reportes de inspección ----------------------------------------------------------

    override suspend fun registerInspection(inspection: Inspection) {
        outbox.saveReport(inspection)
        val alertId = inspection.alertId ?: return
        val deviceId = inspection.origin.deviceIdOrNull().orEmpty()
        val actions = buildList {
            inspection.tag?.let { tag ->
                // El id del reporte es el client_record_id de la etiqueta.
                add(
                    AlertActionEntity(
                        id = inspection.id,
                        alertId = alertId,
                        type = SyncRecordType.INSPECTION_REPORT,
                        inspectionId = inspection.id,
                        tag = tag,
                        tagOtherDetail = inspection.tagOtherDetail.takeIf { tag.requiresDetail },
                        deviceIdentifier = deviceId,
                        recordedAt = inspection.recordedAt,
                    ),
                )
            }
            inspection.observations?.takeIf { it.isNotBlank() }?.let { text ->
                add(
                    AlertActionEntity(
                        id = UUID.randomUUID().toString(),
                        alertId = alertId,
                        type = SyncRecordType.OBSERVATION,
                        inspectionId = inspection.id,
                        text = text,
                        deviceIdentifier = deviceId,
                        recordedAt = inspection.recordedAt,
                    ),
                )
            }
        }
        outbox.enqueue(actions)
    }

    override fun observeInspections(alertId: String): Flow<List<Inspection>> =
        inspections.map { list -> list.filter { it.alertId == alertId }.sortedByDescending { it.recordedAt } }

    override fun observeDeviceInspections(deviceId: String): Flow<List<Inspection>> =
        inspections.map { list -> list.filter { it.deviceId == deviceId }.sortedByDescending { it.recordedAt } }

    override fun observePendingInspectionIds(): Flow<Set<String>> =
        unsynced.map { list -> list.mapNotNull { it.inspectionId }.toSet() }

    // ---- Cola de salida ------------------------------------------------------------------

    /**
     * Envía un registro de la cola (lo llaman los emisores de `data/sync`). Un 409 de conflicto
     * (RN-18) cuenta como recibido: el servidor lo guardó como observación de conflicto. Si llega,
     * vuelve a pedir el detalle de la alerta para mostrar lo que quedó en el servidor.
     */
    suspend fun sendQueued(recordId: String): ApiResult<Unit> {
        val action = outbox.get(recordId) ?: return ApiResult.Success(Unit)
        return when (val posted = postAction(action)) {
            is Posted.Ok, is Posted.SavedAsConflict -> {
                loadAlert(action.alertId)
                ApiResult.Success(Unit)
            }
            Posted.Transient -> ApiResult.Failure(ApiError.NoConnection)
            Posted.NotFound -> ApiResult.Failure(ApiError.Http(HTTP_NOT_FOUND, null))
            is Posted.Rejected -> ApiResult.Failure(
                ApiError.Http(
                    posted.httpCode,
                    posted.body?.let { body -> body.code?.let { ApiErrorBody(it, body.message.orEmpty()) } },
                ),
            )
        }
    }

    private suspend fun postAction(action: AlertActionEntity): Posted = when (action.type) {
        SyncRecordType.CLASSIFICATION -> post {
            api.classify(
                action.alertId,
                ClassificationRequestDto(
                    classification = checkNotNull(action.classification).toApi(),
                    observation = null,
                    origin = ORIGIN_MOBILE_APP,
                    deviceIdentifier = action.deviceIdentifier,
                    clientRecordId = action.id,
                    recordedAt = action.recordedAt.toString(),
                ),
            )
        }
        SyncRecordType.INSPECTION_REPORT -> post {
            api.label(
                action.alertId,
                LabelRequestDto(
                    label = checkNotNull(action.tag).toApi(),
                    otherText = action.tagOtherDetail,
                    observation = null,
                    origin = ORIGIN_MOBILE_APP,
                    deviceIdentifier = action.deviceIdentifier,
                    clientRecordId = action.id,
                    recordedAt = action.recordedAt.toString(),
                ),
            )
        }
        SyncRecordType.OBSERVATION -> post {
            api.observe(
                action.alertId,
                ObservationRequestDto(checkNotNull(action.text), ORIGIN_MOBILE_APP, action.deviceIdentifier.ifEmpty { null }),
            )
        }
        SyncRecordType.PHOTO -> error("${action.type} is not an alert action")
    }

    // ---- HTTP ----------------------------------------------------------------------------

    private sealed interface Posted {
        data class Ok(val body: ActionBodyDto?) : Posted
        data class SavedAsConflict(val body: ActionBodyDto) : Posted
        data class Rejected(val httpCode: Int, val body: ActionBodyDto?) : Posted
        data object NotFound : Posted

        /** Sin red o error del servidor: vale la pena reintentar con el mismo registro. */
        data object Transient : Posted
    }

    private suspend fun post(call: suspend () -> Response<ResponseBody>): Posted = try {
        val response = call()
        if (response.isSuccessful) {
            Posted.Ok(decodeBody(response.body()?.string()))
        } else {
            val body = decodeBody(response.errorBody()?.string())
            val code = response.code()
            when {
                code == HTTP_CONFLICT && body?.outcome == OUTCOME_SAVED_AS_CONFLICT -> Posted.SavedAsConflict(body)
                code == HTTP_NOT_FOUND -> Posted.NotFound
                code >= 500 || code == HTTP_TIMEOUT || code == HTTP_TOO_MANY -> Posted.Transient
                else -> Posted.Rejected(code, body)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        Posted.Transient
    }

    private fun decodeBody(text: String?): ActionBodyDto? =
        text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString<ActionBodyDto>(it) }.getOrNull() }

    private fun Posted.toFailure(): AlertFailure = when (this) {
        Posted.Transient -> AlertFailure.NoConnection
        Posted.NotFound -> AlertFailure.NotFound
        is Posted.Rejected -> AlertFailure.Rejected(body?.code, body?.message)
        is Posted.Ok, is Posted.SavedAsConflict -> error("Not a failure")
    }

    private inline fun fetch(block: () -> Unit): AlertResult<Unit> = try {
        block()
        AlertResult.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        AlertResult.Failure(AlertFailure.NoConnection)
    } catch (e: HttpException) {
        val body = decodeBody(e.response()?.errorBody()?.string())
        AlertResult.Failure(
            when {
                e.code() == HTTP_NOT_FOUND -> AlertFailure.NotFound
                e.code() >= 500 -> AlertFailure.Server
                else -> AlertFailure.Rejected(body?.code, body?.message)
            },
        )
    } catch (_: SerializationException) {
        AlertResult.Failure(AlertFailure.Server)
    }

    private fun Origin.deviceIdOrNull(): String? = (this as? Origin.MobileApp)?.deviceId

    private companion object {
        const val DEVICE_PAGES = 20
        const val DETAIL_CONCURRENCY = 4
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_TIMEOUT = 408
        const val HTTP_TOO_MANY = 429
        const val OUTCOME_SAVED_AS_CONFLICT = "SAVED_AS_CONFLICT_OBSERVATION"
        const val API_CODE_INVALID_INPUT = "INVALID_INPUT"
    }
}
