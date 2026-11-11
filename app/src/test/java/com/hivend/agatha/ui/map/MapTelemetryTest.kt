package com.hivend.agatha.ui.map

import com.hivend.agatha.domain.model.AlertLevel
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
    fun `a device without recent data and no alert is shown without communication`() {
        val result = node.withPlatformState(device(minutesAgo = 120), ongoingLevel = null, now = now)

        assertEquals(SensorNodeStatus.NO_COMMUNICATION, result.nodeStatus)
        assertEquals(now.minus(Duration.ofMinutes(120)), result.lastReportTimestamp)
        assertEquals(64, result.batteryLevel)
    }

    @Test
    fun `a connected device without an ongoing alert is normal`() {
        val result = node.copy(nodeStatus = SensorNodeStatus.RED).withPlatformState(device(minutesAgo = 10), null, now)

        assertEquals(SensorNodeStatus.NORMAL, result.nodeStatus)
    }

    @Test
    fun `an ongoing alert colors the node even without communication`() {
        val result = node.withPlatformState(device(minutesAgo = 120), AlertLevel.ORANGE, now)

        assertEquals(SensorNodeStatus.ORANGE, result.nodeStatus)
    }

    @Test
    fun `a node without a platform device drops the sample values`() {
        val result = node.withPlatformState(device = null, ongoingLevel = null, now = now)

        assertEquals(SensorNodeStatus.NO_COMMUNICATION, result.nodeStatus)
        assertNull(result.batteryLevel)
        assertNull(result.lastReportTimestamp)
    }
}
