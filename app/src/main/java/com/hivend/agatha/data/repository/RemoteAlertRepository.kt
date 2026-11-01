package com.hivend.agatha.data.repository

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
import com.hivend.agatha.domain.model.ClassifyOutcome
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.repository.AlertRepository
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Alertas leídas y enviadas a la API de gestión de alertas (la de la web). Mantiene una caché
 * en memoria que las pantallas observan; la lista, el detalle y el historial de dispositivo la
 * van llenando. Es @Singleton para que todas las pantallas vean el mismo estado.
 *
 * Offline: cada envío lleva un `client_record_id` (el id del reporte, o uno por clasificación) y,
 * si falla por red, queda en una cola **en memoria** que se reenvía con el mismo id en la
 * siguiente actualización, así que reintentar no duplica. La cola no sobrevive a que Android
 * cierre el proceso: la persistencia (Room, WorkManager) es del sprint de sincronización (HE-07).
 */
@Singleton
class RemoteAlertRepository @Inject constructor(
    private val api: AlertsApi,
    private val json: Json,
) : AlertRepository {

    private class PendingClassification(
        val classification: AlertClassification,
        val at: Instant,
        val origin: Origin,
        val clientRecordId: String,
    )

    private class PendingInspection(
        val inspection: Inspection,
        val tagDone: Boolean,
        val observationDone: Boolean,
    )

    private val alerts = MutableStateFlow<List<Alert>>(emptyList())
    private val inspections = MutableStateFlow<List<Inspection>>(emptyList())
    private val pendingClassifications = MutableStateFlow<Map<String, PendingClassification>>(emptyMap())
    private val pendingInspections = MutableStateFlow<Map<String, PendingInspection>>(emptyMap())

    /** Ids que trajo la última lista, para retirar de la caché las que ya no aplican. */
    private var lastListIds: Set<String> = emptySet()

    /** Evita que dos envíos pendientes se crucen y que una observación se envíe dos veces. */
    private val sendLock = Mutex()

    override fun observeAlerts(): StateFlow<List<Alert>> = alerts

    override fun observeAlert(alertId: String): Flow<Alert?> =
        alerts.map { list -> list.find { it.id == alertId } }

    // ---- Lectura -------------------------------------------------------------------------

    override suspend fun refreshAlerts(): AlertResult<Unit> {
        flushPending()
        return fetch {
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
    }

    override suspend fun loadAlert(alertId: String): AlertResult<Unit> = fetch {
        val alert = api.alert(alertId).toDomain() ?: throw SerializationException("Unsupported alert $alertId")
        alerts.update { current -> replace(current, overlayPending(alert)) }
    }

    override suspend fun refreshDevice(deviceId: Int): AlertResult<Unit> = fetch {
        val rows = mutableListOf<Alert>()
        var seen = 0
        for (page in 1..DEVICE_PAGES) {
            val result = api.alerts(device = deviceId.toString(), page = page)
            rows += result.items.mapNotNull(AlertDto::toDomain)
            seen += result.items.size
            if (result.items.size < result.pageSize || seen >= result.total) break
        }
        upsertRows(rows)
    }

    /** Las filas de la lista no traen el detalle: se conserva el que ya se tenía de cada alerta. */
    private fun upsertRows(rows: List<Alert>) = alerts.update { current ->
        val byId = current.associateBy { it.id }
        rows.fold(current) { acc, row -> replace(acc, overlayPending(mergeRow(byId[row.id], row))) }
    }

    private fun mergeRow(old: Alert?, row: Alert): Alert {
        if (old == null) return row
        val record = row.classification
        val oldRecord = old.classification
        val mergedRecord = if (record != null && oldRecord != null && record.classification == oldRecord.classification) {
            record.copy(
                classifiedAt = oldRecord.classifiedAt,
                origin = oldRecord.origin,
                tag = record.tag?.let { tag ->
                    val oldTag = oldRecord.tag?.takeIf { it.tag == tag.tag }
                    if (oldTag == null) tag else tag.copy(taggedAt = oldTag.taggedAt, origin = oldTag.origin)
                },
            )
        } else {
            record
        }
        return row.copy(
            levelTimeline = old.levelTimeline,
            observations = old.observations,
            indicatorValue = old.indicatorValue,
            classification = mergedRecord,
        )
    }

    private fun replace(list: List<Alert>, alert: Alert): List<Alert> =
        (list.filterNot { it.id == alert.id } + alert).sortedByDescending { it.startedAt }

    /** Lo que este celular guardó y aún no llegó al servidor se sigue viendo sobre lo descargado. */
    private fun overlayPending(alert: Alert): Alert {
        var result = alert
        pendingClassifications.value[alert.id]?.let { p ->
            if (result.classification == null) result = result.classify(p.classification, p.at, p.origin)
        }
        pendingInspections.value.values
            .filter { it.inspection.alertId == alert.id && !it.tagDone }
            .forEach { p -> result = runCatching { result.applyInspection(p.inspection) }.getOrDefault(result) }
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
        // Un reintento de la misma clasificación reutiliza su client_record_id.
        val pending = pendingClassifications.value[alertId]?.takeIf { it.classification == classification }
            ?: PendingClassification(classification, at, origin, UUID.randomUUID().toString())
        return sendLock.withLock { sendClassification(alertId, pending) }
    }

    private suspend fun sendClassification(alertId: String, pending: PendingClassification): AlertResult<ClassifyOutcome> {
        val body = ClassificationRequestDto(
            classification = pending.classification.toApi(),
            observation = null,
            origin = ORIGIN_MOBILE_APP,
            deviceIdentifier = pending.origin.deviceIdOrNull().orEmpty(),
            clientRecordId = pending.clientRecordId,
            recordedAt = pending.at.toString(),
        )
        return when (val posted = post { api.classify(alertId, body) }) {
            is Posted.Ok -> {
                pendingClassifications.update { it - alertId }
                loadAlert(alertId)
                AlertResult.Success(ClassifyOutcome.Saved)
            }
            is Posted.SavedAsConflict -> {
                pendingClassifications.update { it - alertId }
                val loaded = loadAlert(alertId)
                val existing = posted.body.existing?.toDomain()
                    ?: alerts.value.find { it.id == alertId }?.classification
                if (existing == null || loaded is AlertResult.Failure) AlertResult.Success(ClassifyOutcome.Saved)
                else AlertResult.Success(ClassifyOutcome.SavedAsConflict(existing))
            }
            Posted.Transient -> {
                queueClassification(alertId, pending)
                AlertResult.Success(ClassifyOutcome.QueuedOffline)
            }
            else -> {
                pendingClassifications.update { it - alertId }
                AlertResult.Failure(posted.toFailure())
            }
        }
    }

    /** Sin red: la clasificación se muestra ya en el celular y se reenviará con el mismo id. */
    private fun queueClassification(alertId: String, pending: PendingClassification) {
        pendingClassifications.update { it + (alertId to pending) }
        alerts.update { list -> list.map { if (it.id == alertId) overlayPending(it) else it } }
    }

    override fun observeClassificationPendingSync(alertId: String): Flow<Boolean> =
        pendingClassifications.map { alertId in it }

    // ---- Reportes de inspección ----------------------------------------------------------

    override suspend fun registerInspection(inspection: Inspection) {
        inspections.update { it + inspection }
        val alertId = inspection.alertId ?: return
        val pending = PendingInspection(
            inspection = inspection,
            tagDone = inspection.tag == null,
            observationDone = inspection.observations.isNullOrBlank(),
        )
        if (pending.tagDone && pending.observationDone) return
        pendingInspections.update { it + (inspection.id to pending) }
        alerts.update { list -> list.map { if (it.id == alertId) overlayPending(it) else it } }
        sendLock.withLock { sendInspection(inspection.id) }
    }

    /** Envía lo que falta de un reporte: primero la etiqueta, luego la observación. */
    private suspend fun sendInspection(inspectionId: String) {
        var pending = pendingInspections.value[inspectionId] ?: return
        val inspection = pending.inspection
        val alertId = inspection.alertId ?: return
        val deviceId = inspection.origin.deviceIdOrNull()

        if (!pending.tagDone) {
            val tag = checkNotNull(inspection.tag)
            val body = LabelRequestDto(
                label = tag.toApi(),
                otherText = inspection.tagOtherDetail.takeIf { tag.requiresDetail },
                observation = null,
                origin = ORIGIN_MOBILE_APP,
                deviceIdentifier = deviceId.orEmpty(),
                clientRecordId = inspection.id,
                recordedAt = inspection.recordedAt.toString(),
            )
            when (val posted = post { api.label(alertId, body) }) {
                is Posted.Ok, is Posted.SavedAsConflict -> pending = PendingInspection(inspection, true, pending.observationDone)
                Posted.Transient -> return
                // 409 CONFLICT: la clasificación de este celular todavía no llegó; se reintenta después.
                is Posted.Rejected -> if (posted.httpCode == HTTP_CONFLICT) return else pending = PendingInspection(inspection, true, pending.observationDone)
                Posted.NotFound -> pending = PendingInspection(inspection, true, pending.observationDone)
            }
            pendingInspections.update { it + (inspectionId to pending) }
        }
        if (!pending.observationDone) {
            val text = checkNotNull(inspection.observations)
            when (val posted = post { api.observe(alertId, ObservationRequestDto(text, ORIGIN_MOBILE_APP, deviceId)) }) {
                is Posted.Ok, is Posted.SavedAsConflict, is Posted.Rejected, Posted.NotFound ->
                    pending = PendingInspection(inspection, pending.tagDone, true)
                Posted.Transient -> {
                    pendingInspections.update { it + (inspectionId to pending) }
                    return
                }
            }
        }
        pendingInspections.update { it - inspectionId }
        loadAlert(alertId)
    }

    /** Reenvía lo pendiente: primero las clasificaciones, que las etiquetas necesitan. */
    private suspend fun flushPending() = sendLock.withLock {
        pendingClassifications.value.forEach { (alertId, pending) -> sendClassification(alertId, pending) }
        pendingInspections.value.keys.forEach { sendInspection(it) }
    }

    override fun observeInspections(alertId: String): Flow<List<Inspection>> =
        inspections.map { list -> list.filter { it.alertId == alertId }.sortedByDescending { it.recordedAt } }

    override fun observeDeviceInspections(deviceId: String): Flow<List<Inspection>> =
        inspections.map { list -> list.filter { it.deviceId == deviceId }.sortedByDescending { it.recordedAt } }

    override fun observePendingInspectionIds(): Flow<Set<String>> =
        pendingInspections.map { it.keys }

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
        const val DEVICE_PAGES = 5
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_TIMEOUT = 408
        const val HTTP_TOO_MANY = 429
        const val OUTCOME_SAVED_AS_CONFLICT = "SAVED_AS_CONFLICT_OBSERVATION"
    }
}
