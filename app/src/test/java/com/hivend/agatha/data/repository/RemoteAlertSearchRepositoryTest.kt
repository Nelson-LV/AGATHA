package com.hivend.agatha.data.repository

import com.hivend.agatha.data.remote.alerts.AlertDto
import com.hivend.agatha.data.remote.alerts.AlertPageDto
import com.hivend.agatha.data.remote.alerts.AlertPendingDto
import com.hivend.agatha.data.remote.alerts.AlertsApi
import com.hivend.agatha.data.remote.alerts.ClassificationRequestDto
import com.hivend.agatha.data.remote.alerts.DeviceRefDto
import com.hivend.agatha.data.remote.alerts.LabelRequestDto
import com.hivend.agatha.data.remote.alerts.ObservationRequestDto
import com.hivend.agatha.domain.model.AlertExportFile
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.AlertSearchPage
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteAlertSearchRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeApi : AlertsApi {
        var page: AlertPageDto = AlertPageDto()
        var searchError: Exception? = null
        var exportResponse: () -> Response<ResponseBody> = { error("No export configured") }
        val searches = mutableListOf<List<String?>>()
        val exports = mutableListOf<List<String?>>()

        override suspend fun alerts(
            device: String?,
            page: Int?,
            from: String?,
            to: String?,
            maxLevel: String?,
            event: String?,
            status: String?,
        ): AlertPageDto {
            searches += listOf(device, page?.toString(), from, to, maxLevel, event, status)
            searchError?.let { throw it }
            return this.page
        }

        override suspend fun export(
            device: String?,
            from: String?,
            to: String?,
            maxLevel: String?,
            event: String?,
            status: String?,
        ): Response<ResponseBody> {
            exports += listOf(device, from, to, maxLevel, event, status)
            return exportResponse()
        }

        override suspend fun pending(limit: Int?): AlertPendingDto = error("Not used")
        override suspend fun alert(id: String): AlertDto = error("Not used")
        override suspend fun active(): com.hivend.agatha.data.remote.alerts.ActiveAlertsDto = error("Not used")
        override suspend fun deviceAlerts(pointId: Int, from: String?, to: String?): com.hivend.agatha.data.remote.alerts.DeviceAlertsDto =
            error("Not used")
        override suspend fun classify(id: String, body: ClassificationRequestDto): Response<ResponseBody> = error("Not used")
        override suspend fun label(id: String, body: LabelRequestDto): Response<ResponseBody> = error("Not used")
        override suspend fun observe(id: String, body: ObservationRequestDto): Response<ResponseBody> = error("Not used")
    }

    private fun dto(id: Int, event: String = "FUGA") = AlertDto(
        id = id,
        code = "ALR-%06d".format(id),
        device = DeviceRefDto(12, "PK 45+300"),
        event = event,
        startedAt = "2026-11-14T08:00:00-05:00",
        inProgress = true,
        currentLevel = "ROJO",
        maxLevel = "ROJO",
    )

    private val filters = AlertSearchFilters(deviceId = 12, maxLevels = setOf(AlertLevel.RED))

    @Test
    fun `search sends the filters and maps the page`() = runBlocking {
        val api = FakeApi().apply {
            page = AlertPageDto(total = 37, page = 2, pageSize = 10, items = listOf(dto(123), dto(124, event = "OTRO")))
        }

        val result = RemoteAlertSearchRepository(api, json).search(filters, page = 2)

        assertEquals(listOf(listOf("12", "2", null, null, "ROJO", null, null)), api.searches)
        val page = (result as AlertResult.Success<AlertSearchPage>).value
        assertEquals(37, page.total)
        assertEquals(2, page.page)
        // Un evento que la app no conoce se muestra genérico en vez de perder la alerta (guía §10).
        assertEquals(listOf("ALR-000123", "ALR-000124"), page.alerts.map { it.id })
        assertEquals(com.hivend.agatha.domain.model.AlertEvent.UNKNOWN, page.alerts[1].event)
    }

    @Test
    fun `search failures become alert failures`() = runBlocking {
        val api = FakeApi()
        val repository = RemoteAlertSearchRepository(api, json)

        api.searchError = IOException("offline")
        assertEquals(AlertResult.Failure(AlertFailure.NoConnection), repository.search(filters, 1))

        api.searchError = HttpException(
            Response.error<Any>(
                400,
                """{"code":"INVALID_INPUT","message":"Fecha mal formada"}""".toResponseBody("application/json".toMediaType()),
            ),
        )
        assertEquals(
            AlertResult.Failure(AlertFailure.Rejected("INVALID_INPUT", "Fecha mal formada")),
            repository.search(filters, 1),
        )

        api.searchError = HttpException(Response.error<Any>(500, "{}".toResponseBody()))
        assertEquals(AlertResult.Failure(AlertFailure.Server), repository.search(filters, 1))
    }

    @Test
    fun `export returns the bytes with the name the server gave`() = runBlocking {
        val bytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        val api = FakeApi().apply {
            exportResponse = {
                Response.success(
                    bytes.toResponseBody(XLSX.toMediaType()),
                    headersOf("Content-Disposition", """attachment; filename="alertas_20261117_1530.xlsx""""),
                )
            }
        }

        val result = RemoteAlertSearchRepository(api, json).export(filters)

        assertEquals(listOf(listOf("12", null, null, "ROJO", null, null)), api.exports)
        val file = (result as AlertResult.Success<AlertExportFile>).value
        assertEquals("alertas_20261117_1530.xlsx", file.fileName)
        assertArrayEquals(bytes, file.bytes)
    }

    @Test
    fun `export without the header still gets an xlsx name`() = runBlocking {
        val api = FakeApi().apply { exportResponse = { Response.success(byteArrayOf(1).toResponseBody()) } }

        val file = (RemoteAlertSearchRepository(api, json).export(filters) as AlertResult.Success).value

        assertTrue(file.fileName.startsWith("alertas_") && file.fileName.endsWith(".xlsx"))
    }

    @Test
    fun `export over the limit shows the message of the server`() = runBlocking {
        val message = "La búsqueda tiene más de 10.000 alertas. Ajuste los filtros para exportar."
        val api = FakeApi().apply {
            exportResponse = {
                Response.error(
                    400,
                    """{"code":"INVALID_INPUT","message":"$message"}""".toResponseBody("application/json".toMediaType()),
                )
            }
        }

        assertEquals(
            AlertResult.Failure(AlertFailure.Rejected("INVALID_INPUT", message)),
            RemoteAlertSearchRepository(api, json).export(filters),
        )
    }

    private companion object {
        const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}
