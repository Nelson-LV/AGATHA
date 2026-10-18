package com.hivend.agatha.data.repository

import com.hivend.agatha.data.remote.dashboard.DashboardApi
import com.hivend.agatha.data.remote.dashboard.DeviceDto
import com.hivend.agatha.data.remote.dashboard.SeriesResponseDto
import com.hivend.agatha.data.remote.dashboard.VariableDto
import com.hivend.agatha.domain.model.TelemetryError
import com.hivend.agatha.domain.model.TelemetryResult
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteTelemetryRepositoryTest {

    private class FakeApi(var failWith: Exception? = null) : DashboardApi {
        var devicesCalls = 0
        var lastSeries: List<Any?>? = null

        override suspend fun devices(): List<DeviceDto> {
            failWith?.let { throw it }
            devicesCalls++
            return listOf(DeviceDto(11556, "Sensor MP-11556 PK34-470", "2026-10-17T21:00:42-05:00", 100.0))
        }

        override suspend fun variables(): List<VariableDto> = emptyList()

        override suspend fun series(deviceId: Int, startDate: String, endDate: String, variables: String?): SeriesResponseDto {
            lastSeries = listOf(deviceId, startDate, endDate, variables)
            return SeriesResponseDto(deviceId, startDate, endDate)
        }
    }

    private val now = Instant.parse("2026-10-18T02:00:30.500Z")

    private fun repository(api: FakeApi) = RemoteTelemetryRepository(api).also { it.now = { now } }

    @Test
    fun `asks the API for the last 24 h ending a minute ago, never in the future`() = runBlocking {
        val api = FakeApi()
        val result = repository(api).recentTelemetry("MP-11556", linkedSetOf("indicador"))
        assertTrue(result is TelemetryResult.Success)
        assertEquals(listOf(11556, "2026-10-17T01:59:30Z", "2026-10-18T01:59:30Z", "indicador"), api.lastSeries)
    }

    @Test
    fun `no keys requests every variable`() = runBlocking {
        val api = FakeApi()
        repository(api).recentTelemetry("MP-11556", emptySet())
        assertEquals(null, api.lastSeries!![3])
    }

    @Test
    fun `node without a platform device is reported as not found`() = runBlocking {
        val result = repository(FakeApi()).findDevice("MP-1189")
        assertEquals(TelemetryResult.Failure(TelemetryError.DEVICE_NOT_FOUND), result)
    }

    @Test
    fun `network failure is reported as no connection`() = runBlocking {
        val result = repository(FakeApi(failWith = IOException("offline"))).recentTelemetry("MP-11556", emptySet())
        assertEquals(TelemetryResult.Failure(TelemetryError.NO_CONNECTION), result)
    }

    @Test
    fun `device list is cached between requests`() = runBlocking {
        val api = FakeApi()
        val repository = repository(api)
        repository.findDevice("MP-11556")
        repository.recentTelemetry("MP-11556", emptySet())
        assertEquals(1, api.devicesCalls)
    }
}
