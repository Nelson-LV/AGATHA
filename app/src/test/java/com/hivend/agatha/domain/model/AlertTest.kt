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
        deviceId = 1,
        pk = "PK1+000",
        event = AlertEvent.LEAK,
        level = levels.last(),
        maxLevel = levels.max(),
        levelTimeline = levels.mapIndexed { i, level -> LevelChange(level, start.plusSeconds(60L * i)) },
        startedAt = start,
    )

    @Test
    fun sensorIdIsTheMeasuringPointName() {
        assertEquals("MP-1", alert(AlertLevel.RED).sensorId)
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
    fun falseAlarmDoesNotInventAnEndTime() {
        // Si la alerta termina lo informa el backend; la app no inventa el fin.
        assertNull(alert(AlertLevel.RED).classify(AlertClassification.FALSE_ALARM, later, app).endedAt)
    }

    @Test
    fun theBackendStateDecidesWhetherTheAlertIsOngoing() {
        val finished = alert(AlertLevel.RED).copy(server = AlertServerState(state = AlertState.FINISHED))
        val reactivated = alert(AlertLevel.RED).copy(endedAt = later, server = AlertServerState(state = AlertState.ACTIVE))

        assertEquals(false, finished.isOngoing)
        assertEquals(true, reactivated.isOngoing)
    }

    @Test
    fun theBackendActionsDecideTheButtons() {
        val server = AlertServerState(
            managementStatus = ManagementStatus.UNCLASSIFIED,
            availableActions = setOf(ManagementAction.CONFIRM, ManagementAction.VIEW),
        )

        assertEquals(setOf(ManagementAction.CONFIRM, ManagementAction.VIEW), alert(AlertLevel.RED).copy(server = server).availableActions)
    }

    @Test
    fun aLocalClassificationNotYetSyncedOverridesTheBackendActions() {
        val server = AlertServerState(
            managementStatus = ManagementStatus.UNCLASSIFIED,
            availableActions = setOf(ManagementAction.CONFIRM, ManagementAction.FALSE_ALERT, ManagementAction.VIEW),
            blinking = true,
        )
        val classified = alert(AlertLevel.RED).copy(server = server).classify(AlertClassification.CONFIRMED, later, app)

        assertEquals(ManagementStatus.CLASSIFIED_WITHOUT_TAG, classified.managementStatus)
        assertEquals(setOf(ManagementAction.ADD_LABEL, ManagementAction.VIEW), classified.availableActions)
        assertEquals(false, classified.isBlinking)
    }

    @Test
    fun theBackendStatusWinsWhenItIsAhead() {
        // La etiqueta llegó con un valor que la app no conoce: el backend dice que ya tiene etiqueta.
        val server = AlertServerState(managementStatus = ManagementStatus.CLASSIFIED_WITH_TAG, availableActions = setOf(ManagementAction.VIEW))
        val alert = alert(AlertLevel.RED).classify(AlertClassification.CONFIRMED, later, app).copy(server = server)

        assertEquals(ManagementStatus.CLASSIFIED_WITH_TAG, alert.managementStatus)
        assertEquals(setOf(ManagementAction.VIEW), alert.availableActions)
    }

    @Test
    fun blinkingFollowsTheBackendAndFallsBackToRedUnclassifiedOngoing() {
        assertEquals(true, alert(AlertLevel.RED).isBlinking)
        assertEquals(false, alert(AlertLevel.ORANGE).isBlinking)
        assertEquals(false, alert(AlertLevel.RED).copy(server = AlertServerState(blinking = false)).isBlinking)
        assertEquals(true, alert(AlertLevel.ORANGE).copy(server = AlertServerState(blinking = true)).isBlinking)
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
        val falseAlarm = alert(AlertLevel.ORANGE).copy(id = "false", endedAt = later)
            .classify(AlertClassification.FALSE_ALARM, later, app)

        val ids = listOf(old, falseAlarm, confirmedOngoing, recent).forAlertList().map { it.id }

        assertEquals(listOf("recent", "old", "confirmed"), ids)
    }

    @Test(expected = IllegalStateException::class)
    fun unclassifiedAlertCannotBeTagged() {
        alert(AlertLevel.RED).addTag(TagRecord(AlertTag.GROUND_MOVEMENT, null, later, app))
    }
}
