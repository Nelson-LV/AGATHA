package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.HistoryEvent
import java.time.Instant

data class HistoryState(
    val events: List<HistoryEvent> = emptyList(),
    val sites: List<HistorySite> = emptyList(),
    val selectedSite: HistorySite? = null,
    val sensors: List<HistorySensorOption> = emptyList(),
    val selectedSensor: HistorySensorOption? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isConnected: Boolean = false,
    val lastSyncedAt: Instant? = null,
    val failure: AlertFailure? = null,
)
