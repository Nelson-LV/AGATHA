package com.hivend.agatha.push

import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertPushParserTest {

    private val fullPayload = mapOf(
        "alertId" to "ALR-0012",
        "level" to "RED",
        "kind" to "LEVEL_UP",
        "sensorId" to "MP-11556",
        "pk" to "12+300",
        "event" to "LEAK",
        "startedAt" to "2026-10-18T14:30:00-05:00",
        "classification" to "",
    )

    @Test
    fun `maps every field of an alert push`() {
        val push = AlertPushParser.parse(fullPayload)

        assertEquals(
            AlertPush(
                alertId = "ALR-0012",
                level = AlertLevel.RED,
                kind = AlertPushKind.LEVEL_UP,
                sensorId = "MP-11556",
                pk = "12+300",
                event = AlertEvent.LEAK,
                startedAt = Instant.parse("2026-10-18T19:30:00Z"),
            ),
            push,
        )
    }

    @Test
    fun `accepts yellow and orange and tolerates lowercase and extra fields`() {
        val yellow = AlertPushParser.parse(mapOf("alertId" to "ALR-1", "level" to "yellow", "foo" to "bar"))
        val orange = AlertPushParser.parse(mapOf("alertId" to "ALR-2", "level" to "Orange"))

        assertEquals(AlertLevel.YELLOW, yellow?.level)
        assertEquals(AlertLevel.ORANGE, orange?.level)
    }

    @Test
    fun `missing optional fields default to a new alert without details`() {
        val push = AlertPushParser.parse(mapOf("alertId" to "ALR-3", "level" to "RED", "startedAt" to "ayer"))

        assertEquals(AlertPush(alertId = "ALR-3", level = AlertLevel.RED, kind = AlertPushKind.NEW), push)
    }

    @Test
    fun `gray notices and the normal level never notify`() {
        assertNull(AlertPushParser.parse(mapOf("alertId" to "ALR-4", "level" to "GRAY")))
        assertNull(AlertPushParser.parse(mapOf("alertId" to "ALR-4", "level" to "NO_COMMUNICATION")))
        assertNull(AlertPushParser.parse(mapOf("alertId" to "ALR-4", "level" to "GREEN")))
        assertNull(AlertPushParser.parse(mapOf("alertId" to "ALR-4")))
    }

    @Test
    fun `alerts classified as false alarm never notify`() {
        assertNull(AlertPushParser.parse(fullPayload + ("classification" to "FALSE_ALARM")))
    }

    @Test
    fun `a message without alert id is dropped`() {
        assertNull(AlertPushParser.parse(mapOf("level" to "RED")))
        assertNull(AlertPushParser.parse(mapOf("alertId" to "  ", "level" to "RED")))
    }
}
