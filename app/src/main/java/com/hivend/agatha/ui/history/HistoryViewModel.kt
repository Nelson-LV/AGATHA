package com.hivend.agatha.ui.history

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.core.navigation.DEFAULT_HISTORY_DEVICE_ID
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.repository.HistoryRepository
import com.hivend.agatha.domain.repository.SensorNodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    historyRepository: HistoryRepository,
    sensorNodeRepository: SensorNodeRepository,
    @param:ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    val deviceId: String = savedStateHandle[BottomTab.History.ARG_DEVICE_ID] ?: DEFAULT_HISTORY_DEVICE_ID

    private val selectedNodeId = kotlinx.coroutines.flow.MutableStateFlow<String?>(
        savedStateHandle[BottomTab.History.ARG_DEVICE_ID],
    )
    private val selectedEventType = kotlinx.coroutines.flow.MutableStateFlow<EventType?>(null)
    private val dateFrom = kotlinx.coroutines.flow.MutableStateFlow<LocalDate?>(null)
    private val dateThrough = kotlinx.coroutines.flow.MutableStateFlow<LocalDate?>(null)
    private val nodeDataLoaded = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val historyLoaded = kotlinx.coroutines.flow.MutableStateFlow(false)

    private val nodes = sensorNodeRepository.observeSensorNodes()
        .onEach { nodeDataLoaded.value = true }
        .catch { exception ->
            if (exception is CancellationException) throw exception
            nodeDataLoaded.value = true
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val sourceEvents = combine(nodes, selectedNodeId) { allNodes, nodeId ->
        nodeId?.let(::listOf) ?: allNodes.map(SensorNode::id)
    }.flatMapLatest { nodeIds ->
        val historyFlows = nodeIds.distinct().map(historyRepository::observeHistoryForNode)
        if (historyFlows.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(historyFlows) { batches -> batches.flatMap { it } }
        }
    }.onEach { historyLoaded.value = true }
        .catch { exception ->
            if (exception is CancellationException) throw exception
            historyLoaded.value = true
            emit(emptyList())
        }

    private val selectedFilters = combine(
        selectedNodeId,
        selectedEventType,
        dateFrom,
        dateThrough,
    ) { nodeId, eventType, from, through ->
        SelectedHistoryFilters(nodeId, eventType, from, through)
    }
    private val nodesAndLoading = combine(nodes, nodeDataLoaded) { allNodes, loaded ->
        HistoryNodeSnapshot(allNodes, loaded)
    }
    private val eventsAndLoading = combine(sourceEvents, historyLoaded) { events, loaded ->
        HistoryEventSnapshot(events, loaded)
    }
    private val connected = observeConnectivity()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<HistoryState> = combine(
        eventsAndLoading,
        selectedFilters,
        nodesAndLoading,
        connected,
    ) { eventSnapshot, filters, nodeSnapshot, isConnected ->
        val now = Instant.now()
        val oldestAllowed = now.minus(HISTORY_WINDOW)
        val zone = ZoneId.systemDefault()
        val startInclusive = filters.dateFrom?.atStartOfDay(zone)?.toInstant()
        val endExclusive = filters.dateThrough?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
        val visibleEvents = eventSnapshot.events
            .asSequence()
            .filter { event -> !event.timestamp.isBefore(oldestAllowed) && !event.timestamp.isAfter(now) }
            .filter { event -> filters.eventType == null || event.type == filters.eventType }
            .filter { event -> startInclusive == null || !event.timestamp.isBefore(startInclusive) }
            .filter { event -> endExclusive == null || event.timestamp.isBefore(endExclusive) }
            .sortedByDescending(HistoryEvent::timestamp)
            .toList()

        HistoryState(
            events = visibleEvents,
            nodes = nodeSnapshot.nodes,
            selectedNodeId = filters.nodeId,
            selectedNode = filters.nodeId?.let { id -> nodeSnapshot.nodes.firstOrNull { it.id == id } },
            selectedEventType = filters.eventType,
            dateFrom = filters.dateFrom,
            dateThrough = filters.dateThrough,
            isLoading = !nodeSnapshot.isLoaded || !eventSnapshot.isLoaded,
            isOffline = !isConnected,
            lastDataTimestamp = eventSnapshot.events.maxOfOrNull(HistoryEvent::timestamp),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    val events: StateFlow<List<HistoryEvent>> = uiState
        .map { state -> state.events }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectNode(nodeId: String?) {
        selectedNodeId.value = nodeId
    }

    fun setEventTypeFilter(eventType: EventType?) {
        selectedEventType.value = eventType
    }

    fun setDateRange(from: LocalDate?, through: LocalDate?) {
        val today = LocalDate.now()
        val oldestAllowed = today.minusDays(HISTORY_WINDOW.toDays())
        var start = from?.coerceIn(oldestAllowed, today)
        var end = through?.coerceIn(oldestAllowed, today)
        if (start != null && end != null && start.isAfter(end)) {
            val earlier = end
            end = start
            start = earlier
        }
        dateFrom.value = start
        dateThrough.value = end
    }

    fun clearFilters() {
        selectedNodeId.value = null
        selectedEventType.value = null
        dateFrom.value = null
        dateThrough.value = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeConnectivity(): Flow<Boolean> = kotlinx.coroutines.flow.callbackFlow {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

        fun publishConnectivity() {
            val network: Network? = connectivityManager.activeNetwork
            val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            trySend(hasInternet && isValidated)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publishConnectivity()
            override fun onLost(network: Network) = publishConnectivity()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) =
                publishConnectivity()
        }

        publishConnectivity()
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private data class SelectedHistoryFilters(
        val nodeId: String?,
        val eventType: EventType?,
        val dateFrom: LocalDate?,
        val dateThrough: LocalDate?,
    )

    private data class HistoryNodeSnapshot(val nodes: List<SensorNode>, val isLoaded: Boolean)

    private data class HistoryEventSnapshot(val events: List<HistoryEvent>, val isLoaded: Boolean)

    private companion object {
        val HISTORY_WINDOW: Duration = Duration.ofDays(30)
    }
}
