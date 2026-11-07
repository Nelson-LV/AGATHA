package com.hivend.agatha.ui.map

import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.domain.model.TelemetryDevice
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapTelemetryTest {

    private val now = Instant.parse("2026-11-07T21:00:00Z")
    private val node = SensorNode(
        id = "MP-11556",
        pk = "34+470",
        municipality = "San José de Pare",
        nodeStatus = SensorNodeStatus.NORMAL,
        batteryLevel = 100,
        lastReportTimestamp = now,
    )

    private fun device(minutesAgo: Long, battery: Double? = 63.6) = TelemetryDevice(
        id = 11556,
        name = "Sensor MP-11556 PK34-470",
        lastConnection = now.minus(Duration.ofMinutes(minutesAgo)),
        lastBatteryLevel = battery,
    )

    @Test
    fun `a device without recent data is shown without communication`() {
        val result = node.withTelemetry(device(minutesAgo = 120), now)

        assertEquals(SensorNodeStatus.NO_COMMUNICATION, result.nodeStatus)
        assertEquals(now.minus(Duration.ofMinutes(120)), result.lastReportTimestamp)
        assertEquals(64, result.batteryLevel)
    }

    @Test
    fun `a connected device keeps the node level`() {
        val result = node.copy(nodeStatus = SensorNodeStatus.ORANGE).withTelemetry(device(minutesAgo = 10), now)

        assertEquals(SensorNodeStatus.ORANGE, result.nodeStatus)
    }

    @Test
    fun `a connected device is no longer a gray notice`() {
        val result = node.copy(nodeStatus = SensorNodeStatus.NO_COMMUNICATION).withTelemetry(device(minutesAgo = 10), now)

        assertEquals(SensorNodeStatus.NORMAL, result.nodeStatus)
    }

    @Test
    fun `a node without a platform device drops the sample values`() {
        val result = node.withTelemetry(device = null, now = now)

        assertEquals(SensorNodeStatus.NO_COMMUNICATION, result.nodeStatus)
        assertNull(result.batteryLevel)
        assertNull(result.lastReportTimestamp)
    }
}
