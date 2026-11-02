package com.hivend.agatha.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.repository.ConnectivityMonitor
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.core.navigation.DEFAULT_HISTORY_DEVICE_ID
import com.hivend.agatha.core.navigation.HISTORY_LAST_SELECTED_DEVICE_ID
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.repository.HistoryRepository
import com.hivend.agatha.domain.repository.SensorNodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
    sensorNodeRepository: SensorNodeRepository,
    connectivityMonitor: ConnectivityMonitor,
    private val selectionStore: HistorySelectionStore,
) : ViewModel() {

    private data class SyncState(
        val refreshing: Boolean = true,
        val lastSyncedAt: java.time.Instant? = null,
        val failure: AlertFailure? = null,
    )

    private data class SelectionSnapshot(
        val site: HistorySite?,
        val sensors: List<HistorySensorOption>,
        val selectedSensor: HistorySensorOption?,
    )

    private data class HistorySnapshot(
        val events: List<HistoryEvent>,
        val loaded: Boolean,
    )

    private var refreshJob: Job? = null
    private var refreshingSensorId: String? = null

    private val nodes = sensorNodeRepository.observeSensorNodes()
        .catch { exception ->
            if (exception is CancellationException) throw exception
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val connectivity = connectivityMonitor.isOnline
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val sync = MutableStateFlow(SyncState())
    private val historyLoaded = MutableStateFlow(false)
    private val selectedEventType = MutableStateFlow<EventType?>(null)
    private val dateFrom = MutableStateFlow<LocalDate?>(null)
    private val dateThrough = MutableStateFlow<LocalDate?>(null)
    private val eventFilters = combine(selectedEventType, dateFrom, dateThrough) { type, from, through ->
        HistoryEventFilters(type, from, through)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryEventFilters())

    private val selection = combine(nodes, selectionStore.selection) { allNodes, stored ->
        resolveSelection(stored, allNodes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        resolveSelection(selectionStore.selection.value, emptyList()),
    )

    private val selectedSensorId = selection
        .map { snapshot -> snapshot.selectedSensor?.id }
        .distinctUntilChanged()

    private val sourceEvents = selectedSensorId.flatMapLatest { sensorId ->
        if (sensorId == null) {
            flowOf(emptyList())
        } else {
            historyRepository.observeHistory(sensorId)
                .map { history -> history.toHistoryEvents().forSensor(sensorId) }
        }
    }.onEach { historyLoaded.value = true }
        .catch { exception ->
            if (exception is CancellationException) throw exception
            historyLoaded.value = true
            emit(emptyList())
        }

    private val historySnapshot = combine(sourceEvents, historyLoaded) { events, loaded ->
        HistorySnapshot(events.sortedByDescending(HistoryEvent::timestamp), loaded)
    }

    val uiState: StateFlow<HistoryState> = combine(
        selection,
        historySnapshot,
        eventFilters,
        sync,
        connectivity,
    ) { selected, history, filters, syncState, connected ->
        HistoryState(
            events = filterHistoryEvents(
                events = history.events,
                site = selected.site,
                sensorId = selected.selectedSensor?.id,
                filters = filters,
            ),
            sites = HistorySiteCatalog.sites,
            selectedSite = selected.site,
            sensors = selected.sensors,
            selectedSensor = selected.selectedSensor,
            selectedEventType = filters.eventType,
            dateFrom = filters.dateFrom,
            dateThrough = filters.dateThrough,
            hasAdditionalFilters = filters.isActive,
            isLoading = !history.loaded || (syncState.refreshing && history.events.isEmpty()),
            isRefreshing = syncState.refreshing,
            isConnected = connected,
            lastSyncedAt = syncState.lastSyncedAt,
            failure = syncState.failure,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    init {
        restoreSelectionFromNavigation()
        viewModelScope.launch {
            combine(selectedSensorId, connectivity) { sensorId, connected -> sensorId to connected }
                .distinctUntilChanged()
                .collect { (sensorId, connected) ->
                    if (sensorId == null) {
                        sync.update { it.copy(refreshing = false, lastSyncedAt = null, failure = null) }
                    } else if (connected) {
                        refreshSensor(sensorId)
                    } else {
                        refreshJob?.cancel()
                        refreshingSensorId = null
                        sync.update {
                            it.copy(
                                refreshing = false,
                                lastSyncedAt = selectionStore.lastSyncedAt(sensorId),
                                failure = AlertFailure.NoConnection,
                            )
                        }
                    }
                }
        }
    }

    fun selectSite(siteId: String) {
        val site = HistorySiteCatalog.site(siteId) ?: return
        val options = HistorySiteCatalog.sensorOptions(site, nodes.value)
        val currentSensorId = selectionStore.selection.value.sensorId
        val nextSensorId = currentSensorId?.takeIf { current -> options.any { it.id == current } }
            ?: options.firstOrNull()?.id
        saveSelection(site.id, nextSensorId)
    }

    fun selectSensor(sensorId: String) {
        val current = selection.value
        if (current.sensors.none { it.id == sensorId }) return
        saveSelection(current.site?.id, sensorId)
    }

    fun setEventTypeFilter(eventType: EventType?) {
        selectedEventType.value = eventType
    }

    fun setDateRange(from: LocalDate?, through: LocalDate?) {
        val today = LocalDate.now()
        val earliestAllowed = today.minusDays(HISTORY_DATE_WINDOW_DAYS)
        var start = from?.coerceIn(earliestAllowed, today)
        var end = through?.coerceIn(earliestAllowed, today)
        if (start != null && end != null && start.isAfter(end)) {
            val earlier = end
            end = start
            start = earlier
        }
        dateFrom.value = start
        dateThrough.value = end
    }

    /** Clears the added event/date filters while preserving the current site and sensor. */
    fun clearFilters() {
        selectedEventType.value = null
        dateFrom.value = null
        dateThrough.value = null
    }

    fun refresh() {
        val sensorId = selection.value.selectedSensor?.id ?: return
        refreshSensor(sensorId, force = true)
    }

    private fun saveSelection(siteId: String?, sensorId: String?) {
        selectionStore.select(siteId, sensorId)
        savedStateHandle[SAVED_SITE_ID] = siteId
        savedStateHandle[SAVED_SENSOR_ID] = sensorId
    }

    private fun restoreSelectionFromNavigation() {
        val routeDeviceId = savedStateHandle.get<String>(BottomTab.History.ARG_DEVICE_ID)
        val savedSiteId = savedStateHandle.get<String>(SAVED_SITE_ID)
        val savedSensorId = savedStateHandle.get<String>(SAVED_SENSOR_ID)
        when {
            !routeDeviceId.isNullOrBlank() && routeDeviceId != HISTORY_LAST_SELECTED_DEVICE_ID -> {
                selectionStore.select(HistorySiteCatalog.siteForSensor(routeDeviceId)?.id, routeDeviceId)
            }
            savedSiteId != null || savedSensorId != null -> {
                val sensor = savedSensorId ?: DEFAULT_HISTORY_DEVICE_ID
                val siteId = savedSiteId ?: HistorySiteCatalog.siteForSensor(sensor)?.id
                selectionStore.select(siteId, sensor)
            }
        }
    }

    private fun resolveSelection(
        stored: HistoryFilterSelection,
        allNodes: List<SensorNode>,
    ): SelectionSnapshot {
        val site = HistorySiteCatalog.site(stored.siteId)
            ?: HistorySiteCatalog.siteForSensor(stored.sensorId)
        val options = if (site == null) {
            HistorySiteCatalog.sensorOptionsForUnknownSite(stored.sensorId, allNodes)
        } else {
            HistorySiteCatalog.sensorOptions(site, allNodes)
        }
        val selectedSensor = options.firstOrNull { it.id == stored.sensorId }
            ?: if (site == null) options.firstOrNull() else options.firstOrNull()
        return SelectionSnapshot(site, options, selectedSensor)
    }

    private fun refreshSensor(sensorId: String, force: Boolean = false) {
        if (!force && refreshingSensorId == sensorId && sync.value.refreshing) return
        refreshJob?.cancel()
        refreshingSensorId = sensorId
        sync.update {
            it.copy(
                refreshing = true,
                lastSyncedAt = selectionStore.lastSyncedAt(sensorId),
                failure = null,
            )
        }
        refreshJob = viewModelScope.launch {
            val result = historyRepository.refresh(sensorId)
            if (refreshingSensorId != sensorId) return@launch
            refreshingSensorId = null
            if (result is AlertResult.Success) selectionStore.recordSync(sensorId, java.time.Instant.now())
            sync.update { previous ->
                when (result) {
                    is AlertResult.Success -> SyncState(
                        refreshing = false,
                        lastSyncedAt = selectionStore.lastSyncedAt(sensorId),
                        failure = null,
                    )
                    is AlertResult.Failure -> previous.copy(refreshing = false, failure = result.failure)
                }
            }
        }
    }

    private companion object {
        const val SAVED_SITE_ID = "history.selectedSiteId"
        const val SAVED_SENSOR_ID = "history.selectedSensorId"
    }
}
