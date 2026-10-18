package com.hivend.agatha.data.remote.dashboard

import com.hivend.agatha.domain.model.TelemetrySource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardMappersTest {

    @Test
    fun `parses RFC 3339 in UTC and with the Colombia offset`() {
        assertEquals(Instant.parse("2026-10-18T02:00:42Z"), parseRfc3339("2026-10-17T21:00:42-05:00"))
        assertEquals(Instant.parse("2026-10-18T01:45:41Z"), parseRfc3339("2026-10-18T01:45:41Z"))
        assertNull(parseRfc3339(null))
        assertNull(parseRfc3339("ayer"))
    }

    @Test
    fun `device without last connection or battery keeps nulls`() {
        val device = DeviceDto(id = 7, name = "Sensor MP-7", lastConnection = null, lastBatteryLevel = null).toDomain()
        assertNull(device.lastConnection)
        assertNull(device.lastBatteryLevel)
    }

    @Test
    fun `points without a value are dropped, never turned into zero`() {
        val series = SeriesDto(
            key = "indicador",
            name = "Indicador I (24 h)",
            unit = "-",
            source = "evaluation",
            data = listOf(
                PointDto("2026-10-17T02:00:00Z", 0.07),
                PointDto("2026-10-17T03:00:00Z", null),
                PointDto("2026-10-17T04:00:00Z", 0.05),
            ),
        ).toDomain()
        assertEquals(TelemetrySource.EVALUATION, series.source)
        assertEquals(listOf(0.07, 0.05), series.points.map { it.value })
    }
}
