package com.hivend.agatha.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertLevelTest {

    @Test
    fun yellowOrangeAndRedAreAlerts() {
        val alerts = AlertLevel.entries.filter { it.isAlert }
        assertEquals(listOf(AlertLevel.YELLOW, AlertLevel.ORANGE, AlertLevel.RED), alerts)
    }

    @Test
    fun levelsAreOrderedBySeverity() {
        assertTrue(AlertLevel.GREEN < AlertLevel.YELLOW)
        assertTrue(AlertLevel.YELLOW < AlertLevel.ORANGE)
        assertTrue(AlertLevel.ORANGE < AlertLevel.RED)
    }
}
