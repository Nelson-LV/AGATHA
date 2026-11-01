package com.hivend.agatha.ui.history

import com.hivend.agatha.R
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertHistoryPhase
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.LevelChange
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.repository.DeviceHistory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryEventsTest {

    private val origin = Origin.Web

    @Test
    fun eachAlertGetsAStartRowAndAClosedAlertGetsAnEndRowInDescendingOrder() {
        val start = Instant.parse("2026-09-26T23:00:00Z")
        val end = Instant.parse("2026-09-27T06:00:00Z")
        val alert = alert(id = "ALR-000007", startedAt = start, endedAt = end)

        val events = DeviceHistory(alerts = listOf(alert)).toEvents()

        assertEquals(listOf("ALR-000007:ended", "ALR-000007:started"), events.map(HistoryEvent::id))
        assertEquals(listOf(end, start), events.map(HistoryEvent::timestamp))
        assertEquals(listOf(AlertHistoryPhase.ENDED, AlertHistoryPhase.GENERATED), events.map(HistoryEvent::alertPhase))
        assertEquals(listOf(EventType.ALERT, EventType.ALERT), events.map(HistoryEvent::type))
        assertEquals("MP-11556", events.first().nodeId)
    }

    @Test
    fun ongoingAlertHasNoEndRow() {
        val events = DeviceHistory(
            alerts = listOf(alert(id = "ALR-000008", startedAt = Instant.parse("2026-09-27T06:00:00Z"))),
        ).toEvents()

        assertEquals(1, events.size)
        assertEquals(AlertHistoryPhase.GENERATED, events.single().alertPhase)
    }

    @Test
    fun generatedRowUsesClassificationInsteadOfManagementStatus() {
        val classified = alert(
            id = "ALR-000009",
            startedAt = Instant.parse("2026-09-27T06:00:00Z"),
            classification = ClassificationRecord(
                classification = AlertClassification.FALSE_ALARM,
                classifiedAt = Instant.parse("2026-09-27T07:00:00Z"),
                origin = origin,
            ),
        )
        val unclassified = alert(
            id = "ALR-000010",
            startedAt = Instant.parse("2026-09-27T08:00:00Z"),
        )

        val generated = DeviceHistory(alerts = listOf(classified, unclassified)).toEvents()
            .filter { it.alertPhase == AlertHistoryPhase.GENERATED }
            .associateBy(HistoryEvent::alertId)

        assertEquals(AlertClassification.FALSE_ALARM, generated.getValue("ALR-000009").alertClassification)
        assertEquals(
            LocalizedText.Resource(R.string.classification_false_alarm),
            generated.getValue("ALR-000009").statusLabel,
        )
        assertNull(generated.getValue("ALR-000010").alertClassification)
        assertEquals(
            LocalizedText.Resource(R.string.management_status_unclassified),
            generated.getValue("ALR-000010").statusLabel,
        )
        assertEquals(2, generated.size)
    }

    private fun alert(
        id: String,
        startedAt: Instant,
        endedAt: Instant? = null,
        classification: ClassificationRecord? = null,
    ) = Alert(
        id = id,
        deviceId = 11556,
        pk = "PK34-470",
        event = AlertEvent.LEAK,
        level = AlertLevel.YELLOW,
        maxLevel = AlertLevel.RED,
        levelTimeline = listOf(LevelChange(AlertLevel.YELLOW, startedAt)),
        startedAt = startedAt,
        endedAt = endedAt,
        classification = classification,
    )
}
