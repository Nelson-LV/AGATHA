package com.hivend.agatha.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertTest {

    private val start = Instant.parse("2026-10-10T12:00:00Z")
    private val later = Instant.parse("2026-10-10T13:00:00Z")
    private val app = Origin.MobileApp("CEL-01")

    private fun alert(vararg levels: AlertLevel) = Alert(
        id = "ALR-0001",
        pointNumber = 1,
        sensorId = "MP-1",
        pk = "PK1+000",
        site = "Site",
        municipality = "Town",
        event = AlertEvent.LEAK,
        description = LocalizedText.Plain(""),
        level = levels.last(),
        levelTimeline = levels.mapIndexed { i, level -> LevelChange(level, start.plusSeconds(60L * i)) },
        startedAt = start,
        telemetry = AlertTelemetry(90, AccelerationLevel.LOW, 80, isRealData = true),
    )

    @Test
    fun maxLevelIsTheHighestInTheTimeline() {
        assertEquals(AlertLevel.RED, alert(AlertLevel.YELLOW, AlertLevel.RED, AlertLevel.ORANGE).maxLevel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun timelineCannotHoldGreen() {
        alert(AlertLevel.ORANGE, AlertLevel.GREEN)
    }

    @Test
    fun managementStatusAdvancesWithClassificationAndTag() {
        val unclassified = alert(AlertLevel.ORANGE)
        assertEquals(ManagementStatus.UNCLASSIFIED, unclassified.managementStatus)

        val classified = unclassified.classify(AlertClassification.CONFIRMED, later, app)
        assertEquals(ManagementStatus.CLASSIFIED_WITHOUT_TAG, classified.managementStatus)

        val tagged = classified.addTag(TagRecord(AlertTag.GROUND_MOVEMENT, null, later, app))
        assertEquals(ManagementStatus.CLASSIFIED_WITH_TAG, tagged.managementStatus)
    }

    @Test
    fun confirmingKeepsTheAlertOngoing() {
        assertNull(alert(AlertLevel.RED).classify(AlertClassification.CONFIRMED, later, app).endedAt)
    }

    @Test
    fun falseAlarmEndsAnOngoingAlert() {
        val ended = alert(AlertLevel.RED).classify(AlertClassification.FALSE_ALARM, later, app)
        assertEquals(later, ended.endedAt)
    }

    @Test(expected = IllegalStateException::class)
    fun classificationCannotBeChanged() {
        alert(AlertLevel.RED)
            .classify(AlertClassification.CONFIRMED, later, app)
            .classify(AlertClassification.FALSE_ALARM, later, Origin.Web)
    }

    @Test
    fun alertListShowsUnclassifiedFirstThenOngoingNewestFirst() {
        val old = alert(AlertLevel.RED).copy(id = "old", startedAt = start.minusSeconds(3600))
        val recent = alert(AlertLevel.YELLOW).copy(id = "recent", startedAt = start)
        val confirmedOngoing = alert(AlertLevel.ORANGE).copy(id = "confirmed", startedAt = later)
            .classify(AlertClassification.CONFIRMED, later, app)
        val falseAlarm = alert(AlertLevel.ORANGE).copy(id = "false").classify(AlertClassification.FALSE_ALARM, later, app)

        val ids = listOf(old, falseAlarm, confirmedOngoing, recent).forAlertList().map { it.id }

        assertEquals(listOf("recent", "old", "confirmed"), ids)
    }

    @Test(expected = IllegalStateException::class)
    fun unclassifiedAlertCannotBeTagged() {
        alert(AlertLevel.RED).addTag(TagRecord(AlertTag.GROUND_MOVEMENT, null, later, app))
    }
}
