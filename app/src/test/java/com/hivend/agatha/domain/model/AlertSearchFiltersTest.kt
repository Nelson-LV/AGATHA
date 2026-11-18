package com.hivend.agatha.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertSearchFiltersTest {

    private fun alert(
        deviceId: Int = 11556,
        event: AlertEvent = AlertEvent.LEAK,
        maxLevel: AlertLevel = AlertLevel.ORANGE,
        startedAt: String = "2026-09-18T19:00:00Z",
        classification: AlertClassification? = null,
    ) = Alert(
        id = "ALR-000006",
        deviceId = deviceId,
        pk = "PK34-470",
        event = event,
        level = AlertLevel.YELLOW,
        maxLevel = maxLevel,
        startedAt = Instant.parse(startedAt),
        classification = classification?.let { ClassificationRecord(it, classifiedAt = null, origin = null) },
    )

    @Test
    fun `no filter matches every alert`() {
        assertTrue(AlertSearchFilters().matches(alert(), ZoneOffset.UTC))
        assertFalse(AlertSearchFilters().hasRefinements)
    }

    @Test
    fun `dates count only the start of the alert like the web search`() {
        val startDay = LocalDate.parse("2026-09-18")
        val nextDay = LocalDate.parse("2026-09-19")
        val ongoingNextDay = alert()

        assertTrue(AlertSearchFilters(from = startDay, to = startDay).matches(ongoingNextDay, ZoneOffset.UTC))
        assertFalse(AlertSearchFilters(from = nextDay, to = nextDay).matches(ongoingNextDay, ZoneOffset.UTC))
    }

    @Test
    fun `dates are read in Colombia time`() {
        // 02:00 UTC del 19/09 son las 21:00 del 18/09 en Bogotá.
        val lateEvening = alert(startedAt = "2026-09-19T02:00:00Z")
        val day = LocalDate.parse("2026-09-18")

        assertTrue(AlertSearchFilters(from = day, to = day).matches(lateEvening, ZoneId.of("America/Bogota")))
    }

    @Test
    fun `level filter uses the maximum level reached`() {
        val reachedRed = alert(maxLevel = AlertLevel.RED)

        assertTrue(AlertSearchFilters(maxLevels = setOf(AlertLevel.RED)).matches(reachedRed, ZoneOffset.UTC))
        assertFalse(AlertSearchFilters(maxLevels = setOf(AlertLevel.YELLOW)).matches(reachedRed, ZoneOffset.UTC))
    }

    @Test
    fun `values of one filter add up and different filters combine`() {
        val filters = AlertSearchFilters(
            deviceId = 11556,
            maxLevels = setOf(AlertLevel.ORANGE, AlertLevel.RED),
            statuses = setOf(ManagementStatus.UNCLASSIFIED),
        )

        assertTrue(filters.matches(alert(maxLevel = AlertLevel.ORANGE), ZoneOffset.UTC))
        assertTrue(filters.matches(alert(maxLevel = AlertLevel.RED), ZoneOffset.UTC))
        assertFalse(filters.matches(alert(deviceId = 1189), ZoneOffset.UTC))
        assertFalse(filters.matches(alert(classification = AlertClassification.CONFIRMED), ZoneOffset.UTC))
        assertTrue(filters.hasRefinements)
    }

    @Test
    fun `a leak and movement alert counts for both events`() {
        val both = alert(event = AlertEvent.LEAK_AND_MOVEMENT)

        assertTrue(AlertSearchFilters(events = setOf(AlertEvent.MOVEMENT)).matches(both, ZoneOffset.UTC))
        assertTrue(AlertSearchFilters(events = setOf(AlertEvent.LEAK)).matches(both, ZoneOffset.UTC))
        assertFalse(AlertSearchFilters(events = setOf(AlertEvent.MOVEMENT)).matches(alert(), ZoneOffset.UTC))
    }
}
