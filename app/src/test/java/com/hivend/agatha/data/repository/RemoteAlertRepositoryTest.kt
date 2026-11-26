package com.hivend.agatha.data.repository

import com.hivend.agatha.data.local.alert.AlertActionEntity
import com.hivend.agatha.data.local.alert.AlertOutbox
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.remote.alerts.ActiveAlertsDto
import com.hivend.agatha.data.remote.alerts.ActivePointDto
import com.hivend.agatha.data.remote.alerts.AlertDto
import com.hivend.agatha.data.remote.alerts.DeviceAlertsDto
import com.hivend.agatha.data.remote.alerts.AlertPageDto
import com.hivend.agatha.data.remote.alerts.AlertPendingDto
import com.hivend.agatha.data.remote.alerts.AlertsApi
import com.hivend.agatha.data.remote.alerts.ClassificationDetailDto
import com.hivend.agatha.data.remote.alerts.ClassificationRequestDto
import com.hivend.agatha.data.remote.alerts.DeviceRefDto
import com.hivend.agatha.data.remote.alerts.LabelRequestDto
import com.hivend.agatha.data.remote.alerts.ObservationDto
import com.hivend.agatha.data.remote.alerts.ObservationRequestDto
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertHistoryPhase
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ClassifyOutcome
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.MaintenanceRecord
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.ui.history.toHistoryEvents
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteAlertRepositoryTest {

    private fun body(text: String = "{}") = text.toResponseBody("application/json".toMediaType())

    private class FakeApi : AlertsApi {
        val pending = mutableListOf<AlertDto>()
        val recent = mutableListOf<AlertDto>()
        val details = mutableMapOf<String, AlertDto>()
        var offline = false
        var alertError: HttpException? = null
        var classifyResponse: () -> Response<ResponseBody> = { Response.success(201, "{}".toResponseBody()) }
        var labelResponse: () -> Response<ResponseBody> = { Response.success(201, "{}".toResponseBody()) }
        val classifyCalls = mutableListOf<ClassificationRequestDto>()
        val labelCalls = mutableListOf<LabelRequestDto>()
        val observationCalls = mutableListOf<ObservationRequestDto>()

        private fun checkNetwork() {
            if (offline) throw IOException("no network")
        }

        override suspend fun alerts(
            device: String?,
            page: Int?,
            from: String?,
            to: String?,
            maxLevel: String?,
            event: String?,
            status: String?,
        ): AlertPageDto {
            checkNetwork()
            pagedCalls++
            val items = if (device == null) recent else recent.filter { it.device?.id.toString() == device }
            return AlertPageDto(total = items.size, page = page ?: 1, pageSize = 10, items = items)
        }

        override suspend fun export(
            device: String?,
            from: String?,
            to: String?,
            maxLevel: String?,
            event: String?,
            status: String?,
        ): Response<ResponseBody> = error("Not used by these tests")

        val pendingLimits = mutableListOf<Int?>()
        var pendingTotal: Int? = null

        override suspend fun pending(limit: Int?): AlertPendingDto {
            checkNetwork()
            pendingLimits += limit
            return AlertPendingDto(pendingTotal ?: pending.size, pending.take(limit ?: 5))
        }

        var pagedCalls = 0
        var detailCalls = 0
        val deviceCalls = mutableListOf<Pair<Int, String?>>()
        var deviceEndpointMissing = false
        val active = mutableListOf<AlertDto>()
        var activeFails = false

        override suspend fun active(): ActiveAlertsDto {
            checkNetwork()
            if (activeFails) throw HttpException(Response.error<Any>(500, "{}".toResponseBody()))
            return ActiveAlertsDto(listOf(ActivePointDto(11556, "Sensor PK34-470", "ROJO", false, active)))
        }

        override suspend fun deviceAlerts(pointId: Int, from: String?, to: String?): DeviceAlertsDto {
            checkNetwork()
            if (deviceEndpointMissing) throw HttpException(Response.error<Any>(404, "{}".toResponseBody()))
            deviceCalls += pointId to from
            // La cabecera reducida de §5.5: sin `device`, `current_level` ni `in_progress`.
            val rows = recent.filter { it.device?.id == pointId }
                .map { it.copy(device = null, currentLevel = null, inProgress = false, state = if (it.inProgress) "ACTIVA" else "FINALIZADA") }
            return DeviceAlertsDto(pointId, "Sensor PK34-470", rows)
        }

        override suspend fun alert(id: String): AlertDto {
            checkNetwork()
            detailCalls++
            alertError?.let { throw it }
            return details[id] ?: (pending + recent).first { it.code == id }
        }

        override suspend fun classify(id: String, body: ClassificationRequestDto): Response<ResponseBody> {
            checkNetwork()
            classifyCalls += body
            return classifyResponse()
        }

        override suspend fun label(id: String, body: LabelRequestDto): Response<ResponseBody> {
            checkNetwork()
            labelCalls += body
            return labelResponse()
        }

        override suspend fun observe(id: String, body: ObservationRequestDto): Response<ResponseBody> {
            checkNetwork()
            observationCalls += body
            return Response.success(201, "{}".toResponseBody())
        }
    }

    /** La cola de Room en memoria; [drain] hace lo que el SyncWorker hace al tener red. */
    private class FakeOutbox : AlertOutbox {
        private val entries = MutableStateFlow<List<Pair<AlertActionEntity, Boolean>>>(emptyList())

        val all: List<AlertActionEntity> get() = entries.value.map { it.first }
        private val reports = MutableStateFlow<List<Inspection>>(emptyList())

        override fun observeReports(): Flow<List<Inspection>> = reports.map { list -> list.sortedByDescending { it.recordedAt } }

        override suspend fun saveReport(report: Inspection) = reports.update { it + report }

        override fun observeUnsynced(): Flow<List<AlertActionEntity>> =
            entries.map { list -> list.filterNot { it.second }.map { it.first } }

        override suspend fun get(id: String): AlertActionEntity? = all.find { it.id == id }

        override suspend fun enqueue(actions: List<AlertActionEntity>) {
            entries.update { it + actions.map { action -> action to false } }
        }

        val conflicts = MutableStateFlow<List<com.hivend.agatha.domain.model.ActionConflict>>(emptyList())

        override suspend fun saveConflict(conflict: com.hivend.agatha.domain.model.ActionConflict) =
            conflicts.update { it + conflict }

        override fun observeConflicts(): Flow<List<com.hivend.agatha.domain.model.ActionConflict>> = conflicts

        override suspend fun acknowledgeConflicts(alertId: String) =
            conflicts.update { list -> list.filterNot { it.alertId == alertId } }

        suspend fun drain(repo: RemoteAlertRepository) {
            entries.value.filterNot { it.second }.forEach { (action, _) ->
                if (repo.sendQueued(action.id) is ApiResult.Success) {
                    entries.update { list -> list.map { if (it.first.id == action.id) it.first to true else it } }
                }
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val phone = Origin.MobileApp("CEL-TEST01")
    private val at = Instant.parse("2026-10-20T15:00:00Z")

    private fun dto(id: Int, level: String = "NARANJA", classification: String? = null, inProgress: Boolean = true) = AlertDto(
        id = id,
        code = "ALR-%06d".format(id),
        device = DeviceRefDto(11556, "Sensor MP-11556 PK34-470"),
        event = "FUGA",
        startedAt = "2026-10-20T10:%02d:00-05:00".format(id),
        endedAt = if (inProgress) null else "2026-10-20T12:00:00-05:00",
        inProgress = inProgress,
        currentLevel = if (inProgress) level else null,
        maxLevel = level,
        classification = classification,
    )

    private val outbox = FakeOutbox()

    private fun repository(api: FakeApi) = RemoteAlertRepository(api, json, outbox)

    @Test
    fun `the list merges unclassified and recent alerts without duplicates, newest first`() = runBlocking {
        val api = FakeApi()
        api.pending += listOf(dto(1, inProgress = false), dto(2))
        api.recent += listOf(dto(3), dto(2))
        val repo = repository(api)

        assertTrue(repo.refreshAlerts() is AlertResult.Success)

        assertEquals(listOf("ALR-000003", "ALR-000002", "ALR-000001"), repo.observeAlerts().first().map { it.id })
    }

    @Test
    fun `the list asks for up to 50 pending alerts and exposes the server total`() = runBlocking {
        val api = FakeApi()
        api.pending += (1..8).map { dto(it) }
        api.pendingTotal = 60
        val repo = repository(api)

        repo.refreshAlerts()

        assertEquals(listOf<Int?>(50), api.pendingLimits)
        assertEquals(8, repo.observeAlerts().first().size)
        assertEquals(60, repo.observeUnclassifiedTotal().first())
    }

    @Test
    fun `a failed refresh keeps what was already downloaded`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)
        repo.refreshAlerts()

        api.offline = true
        val result = repo.refreshAlerts()

        assertEquals(AlertResult.Failure(AlertFailure.NoConnection), result)
        assertEquals(1, repo.observeAlerts().first().size)
    }

    @Test
    fun `refreshing the list keeps the timeline loaded from the detail`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        api.details["ALR-000001"] = dto(1).copy(
            levels = listOf(
                com.hivend.agatha.data.remote.alerts.LevelDto(seq = 1, openedBy = "OPEN", level = "NARANJA", startedAt = "2026-10-20T10:01:00-05:00"),
            ),
        )
        val repo = repository(api)
        repo.refreshAlerts()
        repo.loadAlert("ALR-000001")

        repo.refreshAlerts()

        assertEquals(1, repo.observeAlert("ALR-000001").first()!!.levelTimeline.size)
        assertEquals(1, repo.observeAlert("ALR-000001").first()!!.levelSegments.size)
    }

    @Test
    fun `an alert id the api considers invalid counts as not found`() = runBlocking {
        val api = FakeApi()
        api.alertError = HttpException(
            Response.error<AlertDto>(400, body("""{"code":"INVALID_INPUT","message":"el id no es válido"}""")),
        )

        val result = repository(api).loadAlert("ALR-PRUEBA-000001")

        assertEquals(AlertResult.Failure(AlertFailure.NotFound), result)
    }

    @Test
    fun `classifying sends the phone as origin with a record id and refreshes the alert`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)
        repo.refreshAlerts()
        api.details["ALR-000001"] = dto(1, classification = "CONFIRMADA")

        val result = repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at, phone)

        assertEquals(AlertResult.Success(ClassifyOutcome.Saved), result)
        val sent = api.classifyCalls.single()
        assertEquals("CONFIRMADA", sent.classification)
        assertEquals("APP_MOVIL", sent.origin)
        assertEquals("CEL-TEST01", sent.deviceIdentifier)
        assertEquals(at.toString(), sent.recordedAt)
        assertEquals(AlertClassification.CONFIRMED, repo.observeAlert("ALR-000001").first()!!.classification!!.classification)
        assertFalse(repo.observeClassificationPendingSync("ALR-000001").first())
    }

    @Test
    fun `classification immediately appears in the reactive history and survives stale list rows`() = runBlocking {
        val api = FakeApi().apply {
            recent += dto(1)
            // The POST succeeds, but the following detail GET and device list still omit the result.
            details["ALR-000001"] = dto(1)
        }
        val repo = repository(api)
        repo.refreshAlerts()
        val historyRepository = AlertHistoryRepository(repo)

        val result = repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at, phone)

        assertEquals(AlertResult.Success(ClassifyOutcome.Saved), result)
        val reflectedEvents = historyRepository.observeHistory("MP-11556").first().toHistoryEvents()
        val generated = reflectedEvents.single { it.alertPhase == AlertHistoryPhase.GENERATED }
        val classificationEvent = reflectedEvents.single { it.type == EventType.CLASSIFICATION }
        assertEquals(ManagementStatus.CLASSIFIED_WITHOUT_TAG, generated.alertManagementStatus)
        assertEquals(AlertClassification.CONFIRMED, generated.alertClassification)
        assertEquals(at, classificationEvent.timestamp)
        assertEquals("ALR-000001", classificationEvent.alertId)

        assertTrue(repo.refreshDevice(11556) is AlertResult.Success)
        val afterStaleRefresh = historyRepository.observeHistory("MP-11556").first().alerts.single()
        assertEquals(ManagementStatus.CLASSIFIED_WITHOUT_TAG, afterStaleRefresh.managementStatus)
        assertEquals(at, afterStaleRefresh.classification?.classifiedAt)
        assertEquals(phone, afterStaleRefresh.classification?.origin)
    }

    @Test
    fun `refreshing a device brings the observations and classification details without opening the alert`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA", inProgress = false)
        api.details["ALR-000001"] = dto(1, classification = "CONFIRMADA", inProgress = false).copy(
            classificationDetail = ClassificationDetailDto(
                value = "CONFIRMADA",
                at = "2026-10-20T13:00:00-05:00",
                origin = "WEB",
            ),
            observations = listOf(
                ObservationDto(id = 9, text = "Revisado", recordedAt = "2026-10-20T13:05:00-05:00"),
            ),
        )
        val repo = repository(api)

        assertTrue(repo.refreshDevice(11556) is AlertResult.Success)

        val events = AlertHistoryRepository(repo).observeHistory("MP-11556").first().toHistoryEvents()
        assertEquals(1, events.count { it.type == EventType.CLASSIFICATION })
        assertEquals("Revisado", events.single { it.type == EventType.OBSERVATION }.description)
    }

    @Test
    fun `a failed detail is reported but the alert stays listed`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        api.alertError = HttpException(Response.error<Any>(500, "{}".toResponseBody()))
        val repo = repository(api)

        assertEquals(AlertResult.Failure(AlertFailure.Server), repo.refreshDevice(11556))
        assertEquals(listOf("ALR-000001"), repo.observeAlerts().first().map { it.id })
    }

    @Test
    fun `without a connection the classification shows locally and retries with the same record id`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)
        repo.refreshAlerts()

        api.offline = true
        val result = repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at, phone)

        assertEquals(AlertResult.Success(ClassifyOutcome.QueuedOffline), result)
        assertTrue(repo.observeClassificationPendingSync("ALR-000001").first())
        assertEquals(ManagementStatus.CLASSIFIED_WITHOUT_TAG, repo.observeAlert("ALR-000001").first()!!.managementStatus)
        // Queda en la cola de Room, que es lo que muestra la pantalla de Sincronización.
        assertEquals(SyncRecordType.CLASSIFICATION, outbox.all.single().type)

        api.offline = false
        api.details["ALR-000001"] = dto(1, classification = "CONFIRMADA")
        api.recent[0] = dto(1, classification = "CONFIRMADA")
        outbox.drain(repo)

        assertFalse(repo.observeClassificationPendingSync("ALR-000001").first())
        // Sin red no llegó nada al servidor; al volver la conexión se envía una sola vez.
        assertEquals(1, api.classifyCalls.size)
        assertEquals("CEL-TEST01", api.classifyCalls.single().deviceIdentifier)
        assertEquals(ManagementStatus.CLASSIFIED_WITHOUT_TAG, repo.observeAlert("ALR-000001").first()!!.managementStatus)
    }

    @Test
    fun `the retry reuses the record id so the server can drop duplicates`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)
        repo.refreshAlerts()
        // Primer intento: el servidor recibe el envío pero la respuesta se pierde (error 503).
        api.classifyResponse = { Response.error(503, body()) }

        repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at, phone)
        // Tocar de nuevo "Clasificar" no manda otra: la que está en la cola es la que sale.
        val again = repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at.plusSeconds(60), phone)
        api.classifyResponse = { Response.success(200, body("""{"outcome":"ALREADY_RECEIVED"}""")) }
        outbox.drain(repo)

        assertEquals(AlertResult.Success(ClassifyOutcome.QueuedOffline), again)
        assertFalse(repo.observeClassificationPendingSync("ALR-000001").first())
        assertEquals(2, api.classifyCalls.size)
        assertEquals(api.classifyCalls[0].clientRecordId, api.classifyCalls[1].clientRecordId)
    }

    @Test
    fun `an alert already classified elsewhere is reported as a conflict`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)
        repo.refreshAlerts()
        api.classifyResponse = {
            Response.error(
                409,
                body(
                    """{"code":"CONFLICT","outcome":"SAVED_AS_CONFLICT_OBSERVATION",
                        "existing":{"classification":"FALSA_ALERTA","classified_at":"2026-10-20T10:00:00-05:00","origin":"WEB"}}""",
                ),
            )
        }

        val result = repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at, phone)

        val outcome = (result as AlertResult.Success).value as ClassifyOutcome.SavedAsConflict
        assertEquals(AlertClassification.FALSE_ALARM, outcome.existing.classification)
        assertEquals(Origin.Web, outcome.existing.origin)
        assertFalse(repo.observeClassificationPendingSync("ALR-000001").first())
    }

    @Test
    fun `a rejected classification shows the server message and is not retried`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)
        repo.refreshAlerts()
        api.classifyResponse = { Response.error(400, body("""{"code":"INVALID_INPUT","message":"Dato inválido"}""")) }

        val result = repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at, phone)

        assertEquals(AlertResult.Failure(AlertFailure.Rejected("INVALID_INPUT", "Dato inválido")), result)
        assertFalse(repo.observeClassificationPendingSync("ALR-000001").first())
    }

    private fun report(tag: AlertTag?, observations: String?) = Inspection(
        id = "report-1", deviceId = "MP-11556", alertId = "ALR-000001",
        tag = tag, observations = observations, recordedAt = at, origin = phone,
    )

    @Test
    fun `an inspection report sends its tag and its observation`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA")
        val repo = repository(api)
        repo.refreshAlerts()

        repo.registerInspection(report(AlertTag.GROUND_MOVEMENT, "Erosión en el talud"))
        assertEquals(setOf("report-1"), repo.observePendingInspectionIds().first())
        outbox.drain(repo)

        assertEquals("MOVIMIENTO_TIERRA", api.labelCalls.single().label)
        assertEquals("report-1", api.labelCalls.single().clientRecordId)
        assertEquals("Erosión en el talud", api.observationCalls.single().text)
        assertTrue(repo.observePendingInspectionIds().first().isEmpty())
        assertEquals(1, repo.observeInspections("ALR-000001").first().size)
    }

    @Test
    fun `a queued observation is sent with its record id so a retry does not duplicate it`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA")
        val repo = repository(api)
        repo.refreshAlerts()

        repo.registerInspection(report(null, "Cuadrilla en el sitio"))
        val queued = outbox.all.single { it.type == SyncRecordType.OBSERVATION }
        repo.sendQueued(queued.id)
        repo.sendQueued(queued.id)

        assertEquals(2, api.observationCalls.size)
        api.observationCalls.forEach { sent ->
            assertEquals(queued.id, sent.clientRecordId)
            assertEquals(at.toString(), sent.recordedAt)
            assertEquals("CEL-TEST01", sent.deviceIdentifier)
            assertEquals("APP_MOVIL", sent.origin)
        }
    }

    @Test
    fun `an online observation carries a record id, the time and the phone`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)

        repo.addObservation("ALR-000001", "Texto", phone)

        val sent = api.observationCalls.single()
        assertEquals("CEL-TEST01", sent.deviceIdentifier)
        assertTrue(runCatching { java.util.UUID.fromString(sent.clientRecordId) }.isSuccess)
        assertTrue(runCatching { java.time.OffsetDateTime.parse(sent.recordedAt) }.isSuccess)
    }

    @Test
    fun `a tag sent before the classification arrives stays pending and goes out later`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA")
        val repo = repository(api)
        repo.refreshAlerts()
        api.labelResponse = { Response.error(409, body("""{"code":"CONFLICT","message":"La alerta aún no está clasificada"}""")) }

        repo.registerInspection(report(AlertTag.GROUND_MOVEMENT, null))
        outbox.drain(repo)

        // El rechazo no se descarta: sigue en la cola para mostrarlo en Error y reintentarlo.
        assertEquals(setOf("report-1"), repo.observePendingInspectionIds().first())

        api.labelResponse = { Response.success(201, body()) }
        outbox.drain(repo)

        assertTrue(repo.observePendingInspectionIds().first().isEmpty())
        assertEquals(2, api.labelCalls.size)
    }

    @Test
    fun `an offline report keeps its tag and observation queued and shows the tag right away`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA")
        val repo = repository(api)
        repo.refreshAlerts()
        api.offline = true

        repo.registerInspection(report(AlertTag.GROUND_MOVEMENT, "Erosión en el talud"))
        outbox.drain(repo)

        assertEquals(listOf(SyncRecordType.INSPECTION_REPORT, SyncRecordType.OBSERVATION), outbox.all.map { it.type })
        assertEquals(ManagementStatus.CLASSIFIED_WITH_TAG, repo.observeAlert("ALR-000001").first()!!.managementStatus)
        assertTrue(api.observationCalls.isEmpty())

        api.offline = false
        outbox.drain(repo)

        assertEquals("Erosión en el talud", api.observationCalls.single().text)
        assertTrue(repo.observePendingInspectionIds().first().isEmpty())
    }

    @Test
    fun `maintenance stays on the phone because the API has no endpoint for it`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA")
        val repo = repository(api)
        repo.refreshAlerts()

        repo.registerInspection(
            report(null, null).copy(maintenance = MaintenanceRecord(setOf(MaintenanceType.CLEANING), description = "Limpieza")),
        )
        outbox.drain(repo)

        assertTrue(outbox.all.isEmpty())
        assertTrue(api.labelCalls.isEmpty() && api.observationCalls.isEmpty())
        assertEquals("Limpieza", repo.observeInspections("ALR-000001").first().single().maintenance!!.description)
    }

    @Test
    fun `an observation reports a network failure so the text can be kept`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)
        api.offline = true

        val result = repo.addObservation("ALR-000001", "Texto", phone)

        assertEquals(AlertResult.Failure(AlertFailure.NoConnection), result)
    }

    @Test
    fun `a queued classification that conflicts leaves the queue and is reported with what stands`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        val repo = repository(api)
        repo.refreshAlerts()
        api.offline = true
        repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at, phone)
        api.offline = false
        api.classifyResponse = {
            Response.error(
                409,
                body(
                    """{"code":"CONFLICT","outcome":"SAVED_AS_CONFLICT_OBSERVATION",
                        "existing":{"classification":"FALSA_ALERTA","classified_at":"2026-10-20T10:00:00-05:00","origin":"WEB"}}""",
                ),
            )
        }

        outbox.drain(repo)

        assertFalse(repo.observeClassificationPendingSync("ALR-000001").first())
        val conflict = repo.observeConflicts("ALR-000001").first().single()
        assertEquals(SyncRecordType.CLASSIFICATION, conflict.type)
        assertEquals(AlertClassification.CONFIRMED, conflict.localClassification)
        assertEquals(AlertClassification.FALSE_ALARM, conflict.existing!!.classification)
        assertEquals(Origin.Web, conflict.existing!!.origin)

        repo.acknowledgeConflicts("ALR-000001")
        assertTrue(repo.observeConflicts("ALR-000001").first().isEmpty())
    }

    @Test
    fun `a queued tag that conflicts is reported with the tag that stands`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA")
        val repo = repository(api)
        repo.refreshAlerts()
        api.labelResponse = {
            Response.error(
                409,
                body(
                    """{"code":"CONFLICT","outcome":"SAVED_AS_CONFLICT_OBSERVATION",
                        "existing":{"classification":"CONFIRMADA","label":"MAQUINARIA_AJENA","origin":"WEB"}}""",
                ),
            )
        }

        repo.registerInspection(report(AlertTag.GROUND_MOVEMENT, null))
        outbox.drain(repo)

        assertTrue(repo.observePendingInspectionIds().first().isEmpty())
        val conflict = repo.observeConflicts("ALR-000001").first().single()
        assertEquals(AlertTag.GROUND_MOVEMENT, conflict.localTag)
        assertEquals(AlertTag.THIRD_PARTY_MACHINERY, conflict.existing!!.tag!!.tag)
    }

    @Test
    fun `a tag rejected because the alert is not classified yet is not a conflict`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA")
        val repo = repository(api)
        repo.refreshAlerts()
        api.labelResponse = { Response.error(409, body("""{"code":"CONFLICT","message":"La alerta aún no está clasificada"}""")) }

        repo.registerInspection(report(AlertTag.GROUND_MOVEMENT, null))
        outbox.drain(repo)

        assertEquals(setOf("report-1"), repo.observePendingInspectionIds().first())
        assertTrue(repo.observeConflicts("ALR-000001").first().isEmpty())
    }

    @Test
    fun `a device history comes in one call and each alert detail is completed only once`() = runBlocking {
        val api = FakeApi()
        api.recent += listOf(dto(1), dto(2, inProgress = false))
        api.recent[0] = api.recent[0].copy(
            levels = listOf(
                com.hivend.agatha.data.remote.alerts.LevelDto(seq = 1, openedBy = "OPEN", level = "AMARILLO", startedAt = "2026-10-20T10:01:00-05:00", endedAt = "2026-10-20T11:00:00-05:00"),
                com.hivend.agatha.data.remote.alerts.LevelDto(seq = 2, openedBy = "UP", level = "NARANJA", startedAt = "2026-10-20T11:00:00-05:00"),
            ),
        )
        val repo = repository(api)

        assertTrue(repo.refreshDevice(11556) is AlertResult.Success)
        assertEquals(listOf(11556 to "2026-01-01"), api.deviceCalls)
        assertEquals(0, api.pagedCalls)
        assertEquals(2, api.detailCalls)

        assertTrue(repo.refreshDevice(11556) is AlertResult.Success)
        assertEquals(2, api.detailCalls)
        val ongoing = repo.observeAlert("ALR-000001").first()!!
        assertTrue(ongoing.isOngoing)
        assertEquals(com.hivend.agatha.domain.model.AlertLevel.ORANGE, ongoing.level)
        assertEquals(2, ongoing.levelSegments.size)
    }

    @Test
    fun `without the device endpoint the history falls back to paging the alerts`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        api.deviceEndpointMissing = true
        val repo = repository(api)

        assertTrue(repo.refreshDevice(11556) is AlertResult.Success)

        assertEquals(1, api.pagedCalls)
        assertEquals(listOf("ALR-000001"), repo.observeAlerts().first().map { it.id })
    }

    @Test
    fun `the list adds every active alert even beyond the first page`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        api.active += dto(40, classification = "CONFIRMADA")
        val repo = repository(api)

        repo.refreshAlerts()

        assertEquals(setOf("ALR-000001", "ALR-000040"), repo.observeAlerts().first().map { it.id }.toSet())
    }

    @Test
    fun `a failing active endpoint does not break the list`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1)
        api.activeFails = true
        val repo = repository(api)

        assertTrue(repo.refreshAlerts() is AlertResult.Success)
        assertEquals(listOf("ALR-000001"), repo.observeAlerts().first().map { it.id })
    }
}
