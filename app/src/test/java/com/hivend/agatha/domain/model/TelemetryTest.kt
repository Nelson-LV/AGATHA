package com.hivend.agatha.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryTest {

    private val start = Instant.parse("2026-10-17T00:00:00Z")

    private fun series(vararg minutes: Long) = TelemetrySeries(
        key = "temperatura",
        name = "Temperatura",
        unit = "°C",
        source = TelemetrySource.MEASUREMENT,
        points = minutes.map { TelemetryPoint(start.plusSeconds(it * 60), it.toDouble()) },
    )

    @Test
    fun `regular readings form one segment`() {
        assertEquals(listOf(4), series(0, 15, 30, 45).segments().map { it.size })
    }

    @Test
    fun `a gap longer than the usual interval splits the line instead of filling it`() {
        // Lecturas cada 15 min y un hueco de 2 h entre las 00:30 y las 02:30.
        val segments = series(0, 15, 30, 150, 165, 180).segments()
        assertEquals(listOf(3, 3), segments.map { it.size })
    }

    @Test
    fun `unsorted points are ordered and a lone point is its own segment`() {
        val segments = series(300, 0, 15, 30).segments()
        assertEquals(listOf(listOf(0.0, 15.0, 30.0), listOf(300.0)), segments.map { s -> s.map { it.value } })
    }

    @Test
    fun `empty series has no segments`() {
        assertTrue(series().segments().isEmpty())
    }

    @Test
    fun `device name matches the node code as a whole word`() {
        val device = TelemetryDevice(11556, "Sensor MP-11556 PK34-470", null, null)
        assertTrue(device.matchesNode("MP-11556"))
        assertTrue(device.matchesNode("mp-11556"))
        assertFalse(device.matchesNode("MP-1155"))
        assertFalse(device.matchesNode("MP-1156"))
    }

    @Test
    fun `device named only by number matches the node code through its id`() {
        val device = TelemetryDevice(11556, "Sensor 11556", null, null)
        assertTrue(device.matchesNode("MP-11556"))
        assertFalse(device.matchesNode("MP-1189"))
    }

    @Test
    fun `sections request each variable once and keep pitch and roll together`() {
        assertEquals(listOf("pitch", "roll"), SensorSection.MOTION.charts[1])
        assertTrue(SensorSection.POWER.keys.isEmpty())
        val all = SensorSection.entries.flatMap { it.charts.flatten() }
        assertEquals(all.size, all.toSet().size)
    }
}
