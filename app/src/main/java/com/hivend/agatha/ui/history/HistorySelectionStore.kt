package com.hivend.agatha.ui.history

import com.hivend.agatha.core.navigation.DEFAULT_HISTORY_DEVICE_ID
import com.hivend.agatha.domain.model.EventType
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryFilterSelection(
    val siteId: String?,
    val sensorId: String?,
    val eventType: EventType? = null,
    val dateFrom: LocalDate? = null,
    val dateThrough: LocalDate? = null,
    val listScrollIndex: Int = 0,
    val listScrollOffset: Int = 0,
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
        mutableSelection.value = mutableSelection.value.let { current ->
            val changedSensor = current.sensorId != sensorId
            current.copy(
                siteId = siteId,
                sensorId = sensorId,
                listScrollIndex = if (changedSensor) 0 else current.listScrollIndex,
                listScrollOffset = if (changedSensor) 0 else current.listScrollOffset,
            )
        }
    }

    fun updateFilters(eventType: EventType?, dateFrom: LocalDate?, dateThrough: LocalDate?) {
        mutableSelection.value = mutableSelection.value.let { current ->
            val changed = current.eventType != eventType || current.dateFrom != dateFrom || current.dateThrough != dateThrough
            current.copy(
                eventType = eventType,
                dateFrom = dateFrom,
                dateThrough = dateThrough,
                listScrollIndex = if (changed) 0 else current.listScrollIndex,
                listScrollOffset = if (changed) 0 else current.listScrollOffset,
            )
        }
    }

    fun updateListScrollPosition(index: Int, offset: Int) {
        mutableSelection.value = mutableSelection.value.copy(
            listScrollIndex = index.coerceAtLeast(0),
            listScrollOffset = offset.coerceAtLeast(0),
        )
    }
}
