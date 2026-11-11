package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import java.time.Instant
import java.time.LocalDate

/** Sin tipo elegido la lista son las alertas ([alerts]); con tipo, los registros de ese tipo ([events]). */
data class HistoryState(
    val alerts: List<Alert> = emptyList(),
    val events: List<HistoryEvent> = emptyList(),
    val sites: List<HistorySite> = emptyList(),
    val selectedSite: HistorySite? = null,
    val sensors: List<HistorySensorOption> = emptyList(),
    val selectedSensor: HistorySensorOption? = null,
    val selectedEventType: EventType? = null,
    val dateFrom: LocalDate? = null,
    val dateThrough: LocalDate? = null,
    val hasAdditionalFilters: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isConnected: Boolean = false,
    val lastSyncedAt: Instant? = null,
    val failure: AlertFailure? = null,
    val listScrollIndex: Int = 0,
    val listScrollOffset: Int = 0,
) {
    val shownCount: Int get() = if (selectedEventType == null) alerts.size else events.size
}
