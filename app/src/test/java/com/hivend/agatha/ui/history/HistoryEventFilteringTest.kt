package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryEventFilteringTest {

    @Test
    fun alertsFilterByStartDateLikeTheWebSearch() {
        val alert = Alert(
            id = "ALR-000006",
            deviceId = 11556,
            pk = "PK34-470",
            event = AlertEvent.LEAK,
            level = AlertLevel.ORANGE,
            maxLevel = AlertLevel.ORANGE,
            startedAt = Instant.parse("2026-09-18T19:00:00Z"),
            endedAt = Instant.parse("2026-09-19T20:00:00Z"),
        )
        val startDay = LocalDate.parse("2026-09-18")
        val nextDay = LocalDate.parse("2026-09-19")

        assertEquals(
            listOf(alert),
            filterHistoryAlerts(listOf(alert), HistoryEventFilters(dateFrom = startDay, dateThrough = startDay), ZoneOffset.UTC),
        )
        // Seguía activa ese día, pero la búsqueda de la web cuenta solo el inicio de la alerta.
        assertEquals(
            emptyList<Alert>(),
            filterHistoryAlerts(listOf(alert), HistoryEventFilters(dateFrom = nextDay, dateThrough = nextDay), ZoneOffset.UTC),
        )
    }

    @Test
    fun combinesSiteSensorTypeAndDateFilters() {
        val date = LocalDate.parse("2026-10-10")
        val events = listOf(
            event("target", "MP-11556", EventType.INSPECTION, "2026-10-10T12:00:00Z"),
            event("wrong-type", "MP-11556", EventType.ALERT, "2026-10-10T13:00:00Z"),
            event("wrong-sensor", "MP-1189", EventType.INSPECTION, "2026-10-10T14:00:00Z"),
            event("wrong-site", "MP-99999", EventType.INSPECTION, "2026-10-10T15:00:00Z"),
            event("before-range", "MP-11556", EventType.INSPECTION, "2026-10-09T23:59:59Z"),
        )

        val filtered = filterHistoryEvents(
            events = events,
            site = HistorySiteCatalog.GUEPSA,
            sensorId = "MP-11556",
            filters = HistoryEventFilters(
                eventType = EventType.INSPECTION,
                dateFrom = date,
                dateThrough = date,
            ),
            zone = ZoneOffset.UTC,
        )

        assertEquals(listOf("target"), filtered.map(HistoryEvent::id))
    }

    @Test
    fun dateRangeIncludesBothSelectedLocalDatesAndExcludesNextMidnight() {
        val day = LocalDate.parse("2026-10-10")
        val events = listOf(
            event("start", "MP-11556", EventType.ALERT, "2026-10-10T00:00:00Z"),
            event("last-instant", "MP-11556", EventType.INSPECTION, "2026-10-10T23:59:59.999999999Z"),
            event("end-exclusive", "MP-11556", EventType.ALERT, "2026-10-11T00:00:00Z"),
        )

        val filtered = filterHistoryEvents(
            events = events,
            site = null,
            sensorId = "MP-11556",
            filters = HistoryEventFilters(dateFrom = day, dateThrough = day),
            zone = ZoneOffset.UTC,
        )

        assertEquals(listOf("last-instant", "start"), filtered.map(HistoryEvent::id))
    }

    @Test
    fun siteFilterOnlyAllowsSensorsInTheSelectedSite() {
        val events = listOf(
            event("mapped", "MP-11556", EventType.ALERT, "2026-10-10T00:00:00Z"),
            event("unmapped", "MP-99999", EventType.ALERT, "2026-10-10T00:00:00Z"),
        )

        val filtered = filterHistoryEvents(
            events = events,
            site = HistorySiteCatalog.GUEPSA,
            sensorId = null,
            filters = HistoryEventFilters(),
            zone = ZoneOffset.UTC,
        )

        assertEquals(listOf("mapped"), filtered.map(HistoryEvent::id))
    }

    @Test
    fun eachNewEventTypeCanBeSelectedIndividually() {
        val eventTypes = listOf(
            EventType.INSPECTION,
            EventType.EVIDENCE,
            EventType.MAINTENANCE,
            EventType.OBSERVATION,
            EventType.CLASSIFICATION,
        )
        val events = eventTypes.mapIndexed { index, type ->
            event("event-$index", "MP-11556", type, "2026-10-10T12:00:00Z")
        }

        eventTypes.forEach { type ->
            val filtered = filterHistoryEvents(
                events = events,
                site = HistorySiteCatalog.GUEPSA,
                sensorId = "MP-11556",
                filters = HistoryEventFilters(eventType = type),
                zone = ZoneOffset.UTC,
            )

            assertEquals(listOf(type), filtered.map(HistoryEvent::type))
        }
    }

    @Test
    fun unmatchedFiltersReturnAnEmptyList() {
        val events = listOf(event("alert", "MP-11556", EventType.ALERT, "2026-10-10T12:00:00Z"))

        val filtered = filterHistoryEvents(
            events = events,
            site = HistorySiteCatalog.GUEPSA,
            sensorId = "MP-11556",
            filters = HistoryEventFilters(
                eventType = EventType.INSPECTION,
                dateFrom = LocalDate.parse("2026-10-11"),
            ),
            zone = ZoneOffset.UTC,
        )

        assertEquals(emptyList<HistoryEvent>(), filtered)
    }

    private fun event(id: String, sensorId: String, type: EventType, at: String) = HistoryEvent(
        id = id,
        nodeId = sensorId,
        timestamp = Instant.parse(at),
        type = type,
        description = id,
    )
}
