package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.ManagementStatus
import java.io.File
import java.time.Instant
import java.time.LocalDate

/** Un dispositivo del filtro: [id] como lo nombra la app ("MP-11556") y su nombre en la plataforma. */
data class HistoryDeviceOption(
    val id: String,
    val label: String,
)

/** Exportación a Excel de la lista de alertas que se está viendo. */
sealed interface HistoryExportState {
    data object Idle : HistoryExportState
    data object Running : HistoryExportState

    /** El archivo ya está en el celular; la pantalla lo comparte una vez y avisa. */
    data class Ready(val file: File) : HistoryExportState
    data class Failed(val failure: AlertFailure) : HistoryExportState
}

/** Historial de alertas ([alerts]), buscado en el servidor con los filtros de la web y paginado. */
data class HistoryState(
    val alerts: List<Alert> = emptyList(),
    /** Alertas que cumplen los filtros en el servidor (o en el celular, sin conexión). */
    val totalAlerts: Int = 0,
    val isLoadingMore: Boolean = false,
    /** La lista sale de las alertas guardadas en el celular porque no hay conexión. */
    val showingLocalAlerts: Boolean = false,
    val devices: List<HistoryDeviceOption> = emptyList(),
    /** null = todos los dispositivos. */
    val selectedDevice: HistoryDeviceOption? = null,
    val dateFrom: LocalDate? = null,
    val dateThrough: LocalDate? = null,
    val maxLevels: Set<AlertLevel> = emptySet(),
    val alertEvents: Set<AlertEvent> = emptySet(),
    val statuses: Set<ManagementStatus> = emptySet(),
    val hasAdditionalFilters: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isConnected: Boolean = false,
    val lastSyncedAt: Instant? = null,
    val failure: AlertFailure? = null,
    val export: HistoryExportState = HistoryExportState.Idle,
    val listScrollIndex: Int = 0,
    val listScrollOffset: Int = 0,
) {
    val shownCount: Int get() = alerts.size

    val canLoadMore: Boolean get() = !showingLocalAlerts && alerts.size < totalAlerts

    /** Se exporta con conexión, si hay algo que exportar y no pasa del límite del servidor. */
    val canExport: Boolean
        get() = isConnected && !showingLocalAlerts && totalAlerts in 1..AlertSearchFilters.MAX_EXPORT &&
            export != HistoryExportState.Running

    val exceedsExportLimit: Boolean get() = totalAlerts > AlertSearchFilters.MAX_EXPORT
}
