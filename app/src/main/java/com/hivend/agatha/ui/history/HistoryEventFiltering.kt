package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.ui.components.COLOMBIA
import java.time.LocalDate
import java.time.ZoneId

internal const val HISTORY_DATE_WINDOW_DAYS = 30L

internal data class HistoryEventFilters(
    val eventType: EventType? = null,
    val dateFrom: LocalDate? = null,
    val dateThrough: LocalDate? = null,
) {
    val isActive: Boolean get() = eventType != null || dateFrom != null || dateThrough != null
}

/** Filters only the already-loaded sensor history; the inclusive end date is next day's midnight. */
internal fun filterHistoryEvents(
    events: List<HistoryEvent>,
    site: HistorySite?,
    sensorId: String?,
    filters: HistoryEventFilters,
    zone: ZoneId = COLOMBIA,
): List<HistoryEvent> {
    val siteSensorIds = HistorySiteCatalog.sensorIds(site)
    val startInclusive = filters.dateFrom?.atStartOfDay(zone)?.toInstant()
    val endExclusive = filters.dateThrough?.plusDays(1)?.atStartOfDay(zone)?.toInstant()

    return events.asSequence()
        .filter { event -> site == null || event.nodeId in siteSensorIds }
        .filter { event -> sensorId == null || event.nodeId == sensorId }
        .filter { event -> filters.eventType == null || event.type == filters.eventType }
        .filter { event -> startInclusive == null || !event.timestamp.isBefore(startInclusive) }
        .filter { event -> endExclusive == null || event.timestamp.isBefore(endExclusive) }
        .sortedByDescending(HistoryEvent::timestamp)
        .toList()
}
