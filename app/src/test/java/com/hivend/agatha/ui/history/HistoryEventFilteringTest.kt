package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryEventFilteringTest {

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
