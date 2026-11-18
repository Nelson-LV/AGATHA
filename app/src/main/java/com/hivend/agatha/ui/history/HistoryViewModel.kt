package com.hivend.agatha.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.core.navigation.HISTORY_LAST_SELECTED_DEVICE_ID
import com.hivend.agatha.data.export.ExportFileStore
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.domain.model.TelemetryResult
import com.hivend.agatha.domain.repository.AlertSearchRepository
import com.hivend.agatha.domain.repository.ConnectivityMonitor
import com.hivend.agatha.domain.repository.HistoryRepository
import com.hivend.agatha.domain.repository.SensorNodeRepository
import com.hivend.agatha.domain.repository.TelemetryRepository
import com.hivend.agatha.ui.components.COLOMBIA
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Historial de alertas (HE-09): las alertas de la plataforma con los filtros de la web
 * (dispositivo, fechas, nivel máximo, evento y estado), buscadas en el servidor de a 10 por
 * página y exportables a Excel con los mismos filtros. Sin conexión muestra las alertas que ya
 * tiene el celular, filtradas igual.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    historyRepository: HistoryRepository,
    private val searchRepository: AlertSearchRepository,
    private val telemetryRepository: TelemetryRepository,
    sensorNodeRepository: SensorNodeRepository,
    connectivityMonitor: ConnectivityMonitor,
    private val selectionStore: HistorySelectionStore,
    private val exportFiles: ExportFileStore,
) : ViewModel() {

    /** La búsqueda de alertas en el servidor para [filters], con las páginas ya cargadas. */
    private data class SearchState(
        val filters: AlertSearchFilters? = null,
        val alerts: List<Alert> = emptyList(),
        val total: Int = 0,
        val loadedPage: Int = 0,
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val failure: AlertFailure? = null,
        val searchedAt: Instant? = null,
    )

    private var searchJob: Job? = null
    private var lastSearchAt: Instant? = null

    private val nodes = sensorNodeRepository.observeSensorNodes()
        .catch { exception ->
            if (exception is CancellationException) throw exception
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Dispositivos de la plataforma (`GET /api/dashboard/devices`): las opciones del filtro. */
    private val platformDevices = MutableStateFlow<List<TelemetryDevice>>(emptyList())

    private val connectivity = connectivityMonitor.isOnline
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val search = MutableStateFlow(SearchState())
    private val export = MutableStateFlow<HistoryExportState>(HistoryExportState.Idle)
    private val selection = selectionStore.selection

    private val deviceOptions = combine(platformDevices, nodes, selection) { devices, allNodes, stored ->
        historyDeviceOptions(devices, allNodes, stored.sensorId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lo que ya tiene el celular, para la lista sin conexión. */
    private val cachedAlerts = historyRepository.observeCachedAlerts()
        .catch { exception ->
            if (exception is CancellationException) throw exception
            emit(emptyList())
        }

    private val base = combine(selection, deviceOptions, connectivity) { stored, options, connected ->
        HistoryState(
            devices = options,
            selectedDevice = stored.sensorId?.let { id -> options.firstOrNull { it.id == id } },
            dateFrom = stored.dateFrom,
            dateThrough = stored.dateThrough,
            maxLevels = stored.maxLevels,
            alertEvents = stored.alertEvents,
            statuses = stored.statuses,
            hasAdditionalFilters = stored.toAlertSearchFilters().hasRefinements,
            isConnected = connected,
            listScrollIndex = stored.listScrollIndex,
            listScrollOffset = stored.listScrollOffset,
        )
    }

    val uiState: StateFlow<HistoryState> = combine(base, search, cachedAlerts, export) { state, searchState, cached, exportState ->
        if (!state.isConnected) {
            val filters = selection.value.toAlertSearchFilters()
            val local = cached.filter { filters.matches(it, COLOMBIA) }.sortedByDescending(Alert::startedAt)
            state.copy(
                alerts = local,
                totalAlerts = local.size,
                showingLocalAlerts = true,
                isLoading = false,
                isRefreshing = false,
                lastSyncedAt = lastSearchAt,
                failure = AlertFailure.NoConnection,
                export = exportState,
            )
        } else {
            state.copy(
                alerts = searchState.alerts,
                totalAlerts = searchState.total,
                isLoadingMore = searchState.loadingMore,
                isLoading = searchState.loading && searchState.alerts.isEmpty(),
                isRefreshing = searchState.loading,
                lastSyncedAt = searchState.searchedAt,
                failure = searchState.failure,
                export = exportState,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    init {
        restoreSelectionFromNavigation()
        loadDevices()
        viewModelScope.launch {
            combine(selection, connectivity) { stored, connected -> stored.toAlertSearchFilters() to connected }
                .distinctUntilChanged()
                .collect { (filters, connected) ->
                    if (connected) {
                        startSearch(filters)
                        if (platformDevices.value.isEmpty()) loadDevices()
                    } else {
                        searchJob?.cancel()
                        search.update { it.copy(loading = false, loadingMore = false) }
                    }
                }
        }
    }

    /** Elige un dispositivo, o todos con null. */
    fun selectDevice(sensorId: String?) {
        selectionStore.select(sensorId)
        savedStateHandle[SAVED_SENSOR_ID] = sensorId ?: ALL_DEVICES
    }

    fun setDateRange(from: LocalDate?, through: LocalDate?) {
        val today = LocalDate.now()
        var start = from?.coerceAtMost(today)
        var end = through?.coerceAtMost(today)
        if (start != null && end != null && start.isAfter(end)) {
            val earlier = end
            end = start
            start = earlier
        }
        selectionStore.updateDates(start, end)
    }

    fun toggleMaxLevel(level: AlertLevel) {
        val stored = selection.value
        selectionStore.updateAlertFilters(stored.maxLevels.toggle(level), stored.alertEvents, stored.statuses)
    }

    fun toggleAlertEvent(event: AlertEvent) {
        val stored = selection.value
        selectionStore.updateAlertFilters(stored.maxLevels, stored.alertEvents.toggle(event), stored.statuses)
    }

    fun toggleStatus(status: ManagementStatus) {
        val stored = selection.value
        selectionStore.updateAlertFilters(stored.maxLevels, stored.alertEvents, stored.statuses.toggle(status))
    }

    /** Quita los filtros agregados y conserva el dispositivo elegido. */
    fun clearFilters() {
        selectionStore.updateDates(dateFrom = null, dateThrough = null)
        selectionStore.updateAlertFilters(emptySet(), emptySet(), emptySet())
    }

    fun updateListScrollPosition(index: Int, offset: Int) {
        selectionStore.updateListScrollPosition(index, offset)
    }

    fun refresh() {
        if (!connectivity.value) return
        startSearch(selection.value.toAlertSearchFilters())
        loadDevices(refresh = true)
    }

    /** La lista llegó al final de lo cargado: pide la página siguiente si hay más. */
    fun loadMore() {
        val current = search.value
        val filters = current.filters ?: return
        if (!connectivity.value || current.loading || current.loadingMore || current.failure != null) return
        if (current.alerts.size >= current.total) return
        search.update { it.copy(loadingMore = true) }
        searchJob = viewModelScope.launch {
            val result = searchRepository.search(filters, current.loadedPage + 1)
            search.update { previous ->
                if (previous.filters != filters) return@update previous
                when (result) {
                    is AlertResult.Success -> previous.copy(
                        alerts = (previous.alerts + result.value.alerts).distinctBy(Alert::id),
                        total = result.value.total,
                        loadedPage = result.value.page,
                        loadingMore = false,
                    )
                    is AlertResult.Failure -> previous.copy(loadingMore = false, failure = result.failure)
                }
            }
        }
    }

    /** Exporta a Excel lo que se está viendo: mismos filtros, todas las páginas. */
    fun export() {
        if (!uiState.value.canExport) return
        val filters = selection.value.toAlertSearchFilters()
        export.value = HistoryExportState.Running
        viewModelScope.launch {
            export.value = when (val result = searchRepository.export(filters)) {
                is AlertResult.Success -> try {
                    HistoryExportState.Ready(exportFiles.save(result.value))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: java.io.IOException) {
                    HistoryExportState.Failed(AlertFailure.Server)
                }
                is AlertResult.Failure -> HistoryExportState.Failed(result.failure)
            }
        }
    }

    /** La pantalla ya abrió el diálogo de compartir, o el técnico cerró el aviso de error. */
    fun onExportHandled() {
        export.value = HistoryExportState.Idle
    }

    private fun startSearch(filters: AlertSearchFilters) {
        searchJob?.cancel()
        search.value = SearchState(filters = filters, loading = true, searchedAt = search.value.searchedAt)
        searchJob = viewModelScope.launch {
            val result = searchRepository.search(filters, page = 1)
            if (result is AlertResult.Success) lastSearchAt = Instant.now()
            search.update { previous ->
                if (previous.filters != filters) return@update previous
                when (result) {
                    is AlertResult.Success -> previous.copy(
                        alerts = result.value.alerts,
                        total = result.value.total,
                        loadedPage = result.value.page,
                        loading = false,
                        searchedAt = Instant.now(),
                    )
                    is AlertResult.Failure -> previous.copy(loading = false, failure = result.failure)
                }
            }
        }
    }

    private fun loadDevices(refresh: Boolean = false) {
        viewModelScope.launch {
            val result = telemetryRepository.devices(refresh)
            if (result is TelemetryResult.Success) platformDevices.value = result.data
        }
    }

    private fun restoreSelectionFromNavigation() {
        val routeDeviceId = savedStateHandle.get<String>(BottomTab.History.ARG_DEVICE_ID)
        val savedSensorId = savedStateHandle.get<String>(SAVED_SENSOR_ID)
        when {
            !routeDeviceId.isNullOrBlank() && routeDeviceId != HISTORY_LAST_SELECTED_DEVICE_ID ->
                selectionStore.select(routeDeviceId)
            savedSensorId != null -> selectionStore.select(savedSensorId.takeUnless { it == ALL_DEVICES })
        }
    }

    private companion object {
        const val SAVED_SENSOR_ID = "history.selectedSensorId"
        const val ALL_DEVICES = "_all"
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

/**
 * Opciones del filtro Dispositivo: los de la plataforma (`id` de la API como "MP-<id>", igual que
 * [Alert.sensorId]) o, si todavía no llegaron, los nodos conocidos. El elegido siempre aparece
 * aunque no esté en ninguna de las dos listas, p. ej. al llegar desde el detalle de una alerta.
 */
internal fun historyDeviceOptions(
    devices: List<TelemetryDevice>,
    nodes: List<SensorNode>,
    selectedId: String?,
): List<HistoryDeviceOption> {
    val known = if (devices.isNotEmpty()) {
        devices.map { HistoryDeviceOption(id = "MP-${it.id}", label = it.name.ifBlank { "MP-${it.id}" }) }
    } else {
        nodes.map { HistoryDeviceOption(id = it.id, label = it.name.ifBlank { it.id }) }
    }
    val options = known.distinctBy(HistoryDeviceOption::id).sortedBy(HistoryDeviceOption::label)
    return if (selectedId == null || options.any { it.id == selectedId }) {
        options
    } else {
        options + HistoryDeviceOption(selectedId, nodes.firstOrNull { it.id == selectedId }?.name?.ifBlank { null } ?: selectedId)
    }
}
