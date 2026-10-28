package com.hivend.agatha.data.repository

import com.hivend.agatha.data.remote.alerts.AlertDto
import com.hivend.agatha.data.remote.alerts.AlertPageDto
import com.hivend.agatha.data.remote.alerts.AlertPendingDto
import com.hivend.agatha.data.remote.alerts.AlertsApi
import com.hivend.agatha.data.remote.alerts.ClassificationRequestDto
import com.hivend.agatha.data.remote.alerts.DeviceRefDto
import com.hivend.agatha.data.remote.alerts.LabelRequestDto
import com.hivend.agatha.data.remote.alerts.ObservationRequestDto
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ClassifyOutcome
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.domain.model.Origin
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteAlertRepositoryTest {

    private fun body(text: String = "{}") = text.toResponseBody("application/json".toMediaType())

    private class FakeApi : AlertsApi {
        val pending = mutableListOf<AlertDto>()
        val recent = mutableListOf<AlertDto>()
        val details = mutableMapOf<String, AlertDto>()
        var offline = false
        var classifyResponse: () -> Response<ResponseBody> = { Response.success(201, "{}".toResponseBody()) }
        var labelResponse: () -> Response<ResponseBody> = { Response.success(201, "{}".toResponseBody()) }
        val classifyCalls = mutableListOf<ClassificationRequestDto>()
        val labelCalls = mutableListOf<LabelRequestDto>()
        val observationCalls = mutableListOf<ObservationRequestDto>()

        private fun checkNetwork() {
            if (offline) throw IOException("no network")
        }

        override suspend fun alerts(device: String?, page: Int?): AlertPageDto {
            checkNetwork()
            val items = if (device == null) recent else recent.filter { it.device.id.toString() == device }
            return AlertPageDto(total = items.size, page = page ?: 1, pageSize = 10, items = items)
        }

        override suspend fun pending(): AlertPendingDto {
            checkNetwork()
            return AlertPendingDto(pending.size, pending)
        }

        override suspend fun alert(id: String): AlertDto {
            checkNetwork()
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

    private fun repository(api: FakeApi) = RemoteAlertRepository(api, json)

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
            levels = listOf(com.hivend.agatha.data.remote.alerts.LevelDto(1, "NARANJA", "2026-10-20T10:01:00-05:00")),
        )
        val repo = repository(api)
        repo.refreshAlerts()
        repo.loadAlert("ALR-000001")

        repo.refreshAlerts()

        assertEquals(1, repo.observeAlert("ALR-000001").first()!!.levelTimeline.size)
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

        api.offline = false
        api.details["ALR-000001"] = dto(1, classification = "CONFIRMADA")
        api.recent[0] = dto(1, classification = "CONFIRMADA")
        repo.refreshAlerts()

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
        api.classifyResponse = { Response.success(200, body("""{"status":"ALREADY_RECEIVED"}""")) }
        val retry = repo.classifyAlert("ALR-000001", AlertClassification.CONFIRMED, at.plusSeconds(60), phone)

        assertEquals(AlertResult.Success(ClassifyOutcome.Saved), retry)
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
                    """{"status":"SAVED_AS_CONFLICT_OBSERVATION",
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

        assertEquals("MOVIMIENTO_TIERRA", api.labelCalls.single().label)
        assertEquals("report-1", api.labelCalls.single().clientRecordId)
        assertEquals("Erosión en el talud", api.observationCalls.single().text)
        assertTrue(repo.observePendingInspectionIds().first().isEmpty())
        assertEquals(1, repo.observeInspections("ALR-000001").first().size)
    }

    @Test
    fun `a tag sent before the classification arrives stays pending and goes out later`() = runBlocking {
        val api = FakeApi()
        api.recent += dto(1, classification = "CONFIRMADA")
        val repo = repository(api)
        repo.refreshAlerts()
        api.labelResponse = { Response.error(409, body("""{"code":"CONFLICT","message":"La alerta aún no está clasificada"}""")) }

        repo.registerInspection(report(AlertTag.GROUND_MOVEMENT, "Nota"))

        assertEquals(setOf("report-1"), repo.observePendingInspectionIds().first())
        assertTrue(api.observationCalls.isEmpty())

        api.labelResponse = { Response.success(201, body()) }
        repo.refreshAlerts()

        assertTrue(repo.observePendingInspectionIds().first().isEmpty())
        assertEquals(2, api.labelCalls.size)
        assertEquals(1, api.observationCalls.size)
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
}
