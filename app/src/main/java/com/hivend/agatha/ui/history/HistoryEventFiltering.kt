package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.Alert
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
    site: HistorySite?,
    sensorId: String?,
    filters: HistoryEventFilters,
    zone: ZoneId = COLOMBIA,
): List<HistoryEvent> {
    val siteSensorIds = HistorySiteCatalog.sensorIds(site)

    return events.asSequence()
        .filter { event -> site == null || event.nodeId in siteSensorIds }
        .filter { event -> sensorId == null || event.nodeId == sensorId }
        .filter { event -> filters.eventType == null || event.type == filters.eventType }
        .filter { event -> filters.matchesDate(event.timestamp, zone) }
        .sortedByDescending(HistoryEvent::timestamp)
        .toList()
}

/** Como `from`/`to` de `GET /api/alerts` (la búsqueda de la web): cuenta el inicio de la alerta. */
internal fun filterHistoryAlerts(
    alerts: List<Alert>,
    filters: HistoryEventFilters,
    zone: ZoneId = COLOMBIA,
): List<Alert> = alerts
    .filter { alert -> filters.matchesDate(alert.startedAt, zone) }
    .sortedByDescending(Alert::startedAt)

private fun HistoryEventFilters.matchesDate(instant: Instant, zone: ZoneId): Boolean {
    val startInclusive = dateFrom?.atStartOfDay(zone)?.toInstant()
    val endExclusive = dateThrough?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
    return (startInclusive == null || !instant.isBefore(startInclusive)) &&
        (endExclusive == null || instant.isBefore(endExclusive))
}
