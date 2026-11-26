package com.hivend.agatha.domain.model

import com.hivend.agatha.ui.alertdetail.detailFocusOf
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LevelSegmentFocusTest {

    private val start = Instant.parse("2026-11-20T13:00:00Z")

    private fun message(id: Long) = AlertMessage(id, start, MessageKind.UPDATE, null, null, "t", "b", null)

    private fun segment(seq: Int, ended: Boolean, vararg ids: Long) = LevelSegment(
        seq = seq,
        openedBy = SegmentOpening.UP,
        level = AlertLevel.YELLOW,
        startedAt = start.plusSeconds(3600L * seq),
        endedAt = if (ended) start.plusSeconds(3600L * (seq + 1)) else null,
        messages = ids.map(::message),
    )

    private val active = listOf(segment(1, true, 9001, 9007), segment(2, true, 9010), segment(3, false, 9020))

    @Test
    fun `a push message id points at its segment and message`() {
        assertEquals(FocusTarget(1, 9010), active.focusTarget(DetailFocus.Message(9010)))
    }

    @Test
    fun `an unknown message id or no message id goes to the current segment`() {
        assertEquals(FocusTarget(2, null), active.focusTarget(DetailFocus.Message(1)))
        assertEquals(FocusTarget(2, null), active.focusTarget(DetailFocus.CurrentSegment))
    }

    @Test
    fun `a finished alert goes to its last segment`() {
        val finished = listOf(segment(1, true, 1), segment(2, true, 2))

        assertEquals(FocusTarget(1, null), finished.focusTarget(DetailFocus.CurrentSegment))
    }

    @Test
    fun `there is nothing to focus until the segments arrive`() {
        assertNull(emptyList<LevelSegment>().focusTarget(DetailFocus.CurrentSegment))
    }

    @Test
    fun `the route argument becomes the focus`() {
        assertNull(detailFocusOf(null))
        assertNull(detailFocusOf(""))
        assertEquals(DetailFocus.Message(9010), detailFocusOf("9010"))
        assertEquals(DetailFocus.CurrentSegment, detailFocusOf("current"))
        assertEquals(DetailFocus.CurrentSegment, detailFocusOf("msg-1"))
    }
}
