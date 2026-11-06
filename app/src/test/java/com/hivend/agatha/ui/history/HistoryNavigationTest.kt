package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryNavigationTest {

    @Test
    fun rowsWithAnAlertAssociationPassThatAlertIdToNavigation() {
        val alertId = "ALR-000042"
        val eventTypes = listOf(
            EventType.ALERT,
            EventType.INSPECTION,
            EventType.EVIDENCE,
            EventType.MAINTENANCE,
            EventType.OBSERVATION,
            EventType.CLASSIFICATION,
        )

        eventTypes.forEach { type ->
            assertEquals(alertId, event(type, alertId).alertIdForNavigation())
        }
    }

    @Test
    fun rowsWithoutAnAlertAssociationDoNotEnableNavigation() {
        assertNull(event(EventType.INSPECTION, alertId = null).alertIdForNavigation())
        assertNull(event(EventType.OBSERVATION, alertId = " ").alertIdForNavigation())
    }

    private fun event(type: EventType, alertId: String?) = HistoryEvent(
        id = "event:$type",
        nodeId = "MP-11556",
        timestamp = Instant.parse("2026-10-30T15:00:00Z"),
        type = type,
        description = "",
        alertId = alertId,
    )
}
