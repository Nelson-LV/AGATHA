package com.hivend.agatha.data.sync

import com.hivend.agatha.data.remote.ApiError
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushTokenRetryDelayTest {

    @Test
    fun `the wait doubles from 30 seconds and stops growing at one hour`() {
        val minutes = (0..8).map { pushTokenRetryDelay(ApiError.NoConnection, it)?.seconds }
        assertEquals(listOf<Long?>(30, 60, 120, 240, 480, 960, 1920, 3600, 3600), minutes)
        assertEquals(Duration.ofHours(1), pushTokenRetryDelay(ApiError.NoConnection, 1000))
    }

    @Test
    fun `network failures, timeouts, 429 and 5xx are retried`() {
        val retryable = listOf(ApiError.NoConnection, http(408), http(429), http(500), http(503))
        retryable.forEach { assertEquals(Duration.ofSeconds(30), pushTokenRetryDelay(it, 0)) }
    }

    @Test
    fun `any other 4xx and an invalid response are discarded`() {
        val final = listOf(http(400), http(404), http(405), http(413), ApiError.InvalidResponse)
        final.forEach { assertNull(pushTokenRetryDelay(it, 0)) }
    }

    @Test
    fun `Retry-After wins over a shorter backoff, even past one hour`() {
        assertEquals(Duration.ofMinutes(5), pushTokenRetryDelay(http(429, Duration.ofMinutes(5)), 0))
        assertEquals(Duration.ofHours(3), pushTokenRetryDelay(http(429, Duration.ofHours(3)), 8))
    }

    @Test
    fun `a Retry-After shorter than the backoff does not shorten the wait`() {
        assertEquals(Duration.ofMinutes(4), pushTokenRetryDelay(http(429, Duration.ZERO), 3))
    }

    private fun http(code: Int, retryAfter: Duration? = null) = ApiError.Http(code, null, retryAfter)
}
