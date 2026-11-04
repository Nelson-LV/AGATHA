package com.hivend.agatha.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushMessageDeduplicatorTest {

    @Test
    fun `a new message is remembered`() {
        assertEquals(listOf("a", "b"), rememberMessageId(listOf("a"), "b"))
    }

    @Test
    fun `a message delivered twice is rejected`() {
        assertNull(rememberMessageId(listOf("a", "b"), "a"))
    }

    @Test
    fun `only the most recent messages are kept`() {
        assertEquals(listOf("b", "c"), rememberMessageId(listOf("a", "b"), "c", max = 2))
    }
}
