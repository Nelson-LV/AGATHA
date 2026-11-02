package com.hivend.agatha.ui.history

import com.hivend.agatha.core.navigation.DEFAULT_HISTORY_DEVICE_ID
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryFilterSelection(
    val siteId: String?,
    val sensorId: String?,
)

/** Keeps the last history filters while the user switches between bottom tabs. */
@Singleton
class HistorySelectionStore @Inject constructor() {
    private val mutableSelection = MutableStateFlow(
        HistoryFilterSelection(
            siteId = HistorySiteCatalog.siteForSensor(DEFAULT_HISTORY_DEVICE_ID)?.id
                ?: HistorySiteCatalog.GUEPSA.id,
            sensorId = DEFAULT_HISTORY_DEVICE_ID,
        ),
    )

    val selection: StateFlow<HistoryFilterSelection> = mutableSelection.asStateFlow()

    private val lastSyncedAtBySensor = MutableStateFlow<Map<String, Instant>>(emptyMap())

    fun lastSyncedAt(sensorId: String?): Instant? = sensorId?.let { lastSyncedAtBySensor.value[it] }

    fun recordSync(sensorId: String, at: Instant) {
        lastSyncedAtBySensor.value = lastSyncedAtBySensor.value + (sensorId to at)
    }

    fun select(siteId: String?, sensorId: String?) {
        mutableSelection.value = HistoryFilterSelection(siteId, sensorId)
    }
}
