package com.hivend.agatha.ui.map

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertServerState
import com.hivend.agatha.domain.model.ClassificationRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapInspectionAlertTest {

    private fun alert(
        id: String,
        deviceId: Int = 11556,
        startedAt: String,
        classification: AlertClassification? = null,
    ) = Alert(
        id = id,
        deviceId = deviceId,
        pk = "34+470",
        event = AlertEvent.LEAK,
        level = AlertLevel.ORANGE,
        maxLevel = AlertLevel.ORANGE,
        startedAt = Instant.parse(startedAt),
        classification = classification?.let { ClassificationRecord(it, classifiedAt = null, origin = null) },
    )

    @Test
    fun `opens the most recent unclassified alert of the device`() {
        val alerts = listOf(
            alert("ALR-000001", startedAt = "2026-11-10T10:00:00Z"),
            alert("ALR-000003", startedAt = "2026-11-12T10:00:00Z"),
            alert("ALR-000002", startedAt = "2026-11-11T10:00:00Z"),
        )

        assertEquals("ALR-000003", alerts.latestUnclassifiedFor("MP-11556")?.id)
    }

    @Test
    fun `skips classified alerts and alerts of other devices`() {
        val alerts = listOf(
            alert("ALR-000001", startedAt = "2026-11-10T10:00:00Z"),
            alert("ALR-000002", startedAt = "2026-11-12T10:00:00Z", classification = AlertClassification.CONFIRMED),
            alert("ALR-000003", deviceId = 20001, startedAt = "2026-11-13T10:00:00Z"),
        )

        assertEquals("ALR-000001", alerts.latestUnclassifiedFor("MP-11556")?.id)
    }

    @Test
    fun `a device without unclassified alerts has nothing to inspect`() {
        val alerts = listOf(
            alert("ALR-000001", startedAt = "2026-11-10T10:00:00Z", classification = AlertClassification.FALSE_ALARM),
        )

        assertNull(alerts.latestUnclassifiedFor("MP-11556"))
        assertNull(emptyList<Alert>().latestUnclassifiedFor("MP-11556"))
    }

    @Test
    fun `highlights the devices whose alert the backend marks as blinking`() {
        val alerts = listOf(
            alert("ALR-000001", startedAt = "2026-11-10T10:00:00Z").copy(server = AlertServerState(blinking = true)),
            alert("ALR-000002", deviceId = 20001, startedAt = "2026-11-10T10:00:00Z").copy(server = AlertServerState(blinking = false)),
        )

        assertEquals(setOf("MP-11556"), alerts.blinkingSensorIds())
    }
}
