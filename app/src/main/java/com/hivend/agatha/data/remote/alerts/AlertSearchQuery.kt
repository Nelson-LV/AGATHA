package com.hivend.agatha.data.remote.alerts

import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.ManagementStatus
import java.time.format.DateTimeFormatter

/**
 * Los filtros como query params de `GET /api/alerts` y `GET /api/alerts/export.xlsx`; null = el
 * param no se manda. Las multiselecciones van separadas por comas y en el orden de las opciones,
 * así la misma selección arma siempre la misma URL.
 */
internal data class AlertSearchQuery(
    val device: String? = null,
    val from: String? = null,
    val to: String? = null,
    val maxLevel: String? = null,
    val event: String? = null,
    val status: String? = null,
)

internal fun AlertSearchFilters.toQuery(): AlertSearchQuery = AlertSearchQuery(
    device = deviceId?.toString(),
    from = from?.format(DateTimeFormatter.ISO_LOCAL_DATE),
    to = to?.format(DateTimeFormatter.ISO_LOCAL_DATE),
    maxLevel = maxLevels.sorted().mapNotNull { it.toApiLevel() }.joinedOrNull(),
    event = events.sorted().mapNotNull { it.toApiEvent() }.joinedOrNull(),
    status = statuses.sorted().map { it.toApiStatus() }.joinedOrNull(),
)

internal fun AlertLevel.toApiLevel(): String? = when (this) {
    AlertLevel.YELLOW -> "AMARILLO"
    AlertLevel.ORANGE -> "NARANJA"
    AlertLevel.RED -> "ROJO"
    AlertLevel.GREEN -> null
}

/** Fuga y movimiento a la vez no es un valor del filtro: se busca con los dos. */
internal fun AlertEvent.toApiEvent(): String? = when (this) {
    AlertEvent.LEAK -> "FUGA"
    AlertEvent.MOVEMENT -> "MOVIMIENTO"
    AlertEvent.LEAK_AND_MOVEMENT -> null
}

internal fun ManagementStatus.toApiStatus(): String = when (this) {
    ManagementStatus.UNCLASSIFIED -> "SIN_CLASIFICAR"
    ManagementStatus.CLASSIFIED_WITHOUT_TAG -> "CLASIFICADA_SIN_ETIQUETA"
    ManagementStatus.CLASSIFIED_WITH_TAG -> "CLASIFICADA_CON_ETIQUETA"
}

private fun List<String>.joinedOrNull(): String? = takeIf { it.isNotEmpty() }?.joinToString(",")

private val FILE_NAME = Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""", RegexOption.IGNORE_CASE)

/** El nombre de `Content-Disposition: attachment; filename="alertas_20261117_1530.xlsx"`, o null. */
internal fun fileNameFromContentDisposition(header: String?): String? =
    header?.let { FILE_NAME.find(it)?.groupValues?.get(1)?.trim() }
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.takeIf { it.isNotBlank() }
