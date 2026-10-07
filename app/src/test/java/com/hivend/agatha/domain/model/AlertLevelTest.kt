package com.hivend.agatha.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertLevelTest {

    @Test
    fun onlyOrangeAndRedRequireAttention() {
        val attention = AlertLevel.entries.filter { it.requiresAttention }
        assertEquals(listOf(AlertLevel.ORANGE, AlertLevel.RED), attention)
    }
}
