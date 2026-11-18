package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.ui.components.COLOMBIA
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    sensorId: String?,
    filters: HistoryEventFilters,
    zone: ZoneId = COLOMBIA,
): List<HistoryEvent> = events.asSequence()
    .filter { event -> sensorId == null || event.nodeId == sensorId }
    .filter { event -> filters.eventType == null || event.type == filters.eventType }
    .filter { event -> filters.matchesDate(event.timestamp, zone) }
    .sortedByDescending(HistoryEvent::timestamp)
    .toList()

/**
 * Los filtros de la lista de alertas como los pide la API. [sensorId] "MP-11556" es el
 * `measuring_point_id` 11556; un id sin número no filtra por dispositivo.
 */
internal fun HistoryFilterSelection.toAlertSearchFilters(): AlertSearchFilters = AlertSearchFilters(
    deviceId = sensorId?.let(::measuringPointId),
    from = dateFrom,
    to = dateThrough,
    maxLevels = maxLevels,
    events = alertEvents,
    statuses = statuses,
)

internal fun measuringPointId(sensorId: String): Int? = sensorId.substringAfterLast('-').toIntOrNull()

private fun HistoryEventFilters.matchesDate(instant: Instant, zone: ZoneId): Boolean {
    val startInclusive = dateFrom?.atStartOfDay(zone)?.toInstant()
    val endExclusive = dateThrough?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
    return (startInclusive == null || !instant.isBefore(startInclusive)) &&
        (endExclusive == null || instant.isBefore(endExclusive))
}
