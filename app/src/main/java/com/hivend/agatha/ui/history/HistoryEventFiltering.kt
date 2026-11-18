package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.AlertSearchFilters

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
