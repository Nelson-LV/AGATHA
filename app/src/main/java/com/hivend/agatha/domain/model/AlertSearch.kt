package com.hivend.agatha.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Filtros del Historial de alertas, los mismos de la búsqueda de la web (`GET /api/alerts`) y de
 * su exportación a Excel. Un conjunto vacío no filtra; dentro de un filtro los valores se suman
 * (OR) y entre filtros se cruzan (AND).
 *
 * - [deviceId]: `measuring_point_id` del dispositivo; null = todos.
 * - [from]/[to]: fecha de **inicio** de la alerta en hora de Colombia, ambas inclusivas.
 * - [maxLevels]: nivel **máximo** alcanzado, no el actual.
 */
data class AlertSearchFilters(
    val deviceId: Int? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val maxLevels: Set<AlertLevel> = emptySet(),
    val events: Set<AlertEvent> = emptySet(),
    val statuses: Set<ManagementStatus> = emptySet(),
) {
    /** true si hay algún filtro además del dispositivo. */
    val hasRefinements: Boolean
        get() = from != null || to != null || maxLevels.isNotEmpty() || events.isNotEmpty() || statuses.isNotEmpty()

    /**
     * Lo mismo que aplica el servidor, para filtrar sin conexión las alertas que ya tiene el
     * celular. Una alerta de fuga y movimiento cuenta para los dos eventos.
     */
    fun matches(alert: Alert, zone: ZoneId): Boolean {
        if (deviceId != null && alert.deviceId != deviceId) return false
        if (maxLevels.isNotEmpty() && alert.maxLevel !in maxLevels) return false
        if (events.isNotEmpty() && events.none { it in alert.event.parts() }) return false
        if (statuses.isNotEmpty() && alert.managementStatus !in statuses) return false
        return startsWithin(alert.startedAt, zone)
    }

    private fun startsWithin(instant: Instant, zone: ZoneId): Boolean {
        val startInclusive = from?.atStartOfDay(zone)?.toInstant()
        val endExclusive = to?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
        return (startInclusive == null || !instant.isBefore(startInclusive)) &&
            (endExclusive == null || instant.isBefore(endExclusive))
    }

    private fun AlertEvent.parts(): Set<AlertEvent> = when (this) {
        AlertEvent.LEAK_AND_MOVEMENT -> setOf(AlertEvent.LEAK, AlertEvent.MOVEMENT)
        else -> setOf(this)
    }

    companion object {
        /** Opciones del filtro de evento: la web solo filtra por fuga o movimiento. */
        val EVENT_OPTIONS = listOf(AlertEvent.LEAK, AlertEvent.MOVEMENT)

        /** Opciones del filtro de nivel máximo, de menor a mayor. */
        val LEVEL_OPTIONS = listOf(AlertLevel.YELLOW, AlertLevel.ORANGE, AlertLevel.RED)

        /** Más de esto no se exporta: el servidor responde 400 (guía del backend). */
        const val MAX_EXPORT = 10_000
    }
}

/** Una página de la búsqueda. [total] cuenta todas las alertas que cumplen los filtros. */
data class AlertSearchPage(
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val alerts: List<Alert>,
)

/** El Excel que armó el servidor con [fileName] como lo nombró en `Content-Disposition`. */
class AlertExportFile(
    val fileName: String,
    val bytes: ByteArray,
)
