package com.hivend.agatha.ui.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.domain.repository.SensorNodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MapCoordinate(
    val latitude: Double,
    val longitude: Double,
)

data class MapState(
    val isLoading: Boolean = true,
    val isRouteLoading: Boolean = true,
    val isOffline: Boolean = true,
    val nodes: List<SensorNode> = emptyList(),
    val visibleNodes: List<SensorNode> = emptyList(),
    val route: List<MapCoordinate> = emptyList(),
    val selectedNode: SensorNode? = null,
    val statusFilter: SensorNodeStatus? = null,
    val routeError: Boolean = false,
)

private data class RouteData(
    val coordinates: List<MapCoordinate> = emptyList(),
    val referencePoints: List<ReferencePoint> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)

private data class ReferencePoint(
    val chainageMeters: Long?,
    val coordinate: MapCoordinate,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    sensorNodeRepository: SensorNodeRepository,
    @param:ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private val selectedNodeId = MutableStateFlow<String?>(null)
    private val statusFilter = MutableStateFlow<SensorNodeStatus?>(null)
    private val routeData = MutableStateFlow(RouteData())
    private val nodesLoaded = MutableStateFlow(false)

    private val sensorNodes = sensorNodeRepository.observeSensorNodes()
        .onEach { nodesLoaded.value = true }
        .catch { exception ->
            if (exception is CancellationException) throw exception
            nodesLoaded.value = true
            emit(emptyList())
        }

    private val connected = observeConnectivity()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val mapContent = combine(
        sensorNodes,
        nodesLoaded,
        selectedNodeId,
        statusFilter,
        routeData,
    ) { nodes, isLoaded, selectedId, selectedStatus, route ->
        val positionedNodes = attachCoordinates(nodes, route)
        val visibleNodes = positionedNodes.filter { node ->
            selectedStatus == null || node.nodeStatus == selectedStatus
        }
        val selectedNode = selectedId?.let { id ->
            visibleNodes.firstOrNull { it.id == id }
        }
        MapState(
            isLoading = !isLoaded,
            isRouteLoading = route.isLoading,
            nodes = positionedNodes,
            visibleNodes = visibleNodes,
            route = route.coordinates,
            selectedNode = selectedNode,
            statusFilter = selectedStatus,
            routeError = route.hasError,
        )
    }

    val uiState: StateFlow<MapState> = combine(mapContent, connected) { content, isConnected ->
        content.copy(isOffline = !isConnected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapState())

    init {
        loadRouteData()
    }

    fun selectNode(nodeId: String) {
        selectedNodeId.value = nodeId
    }

    fun clearSelection() {
        selectedNodeId.value = null
    }

    fun setStatusFilter(status: SensorNodeStatus?) {
        statusFilter.value = status
        selectedNodeId.value = null
    }

    private fun loadRouteData() {
        viewModelScope.launch {
            val loadedData = withContext(Dispatchers.IO) {
                runCatching {
                    val coordinates = applicationContext.assets.open(ROUTE_ASSET)
                        .bufferedReader()
                        .use(::parseRouteCoordinates)
                    val referencePoints = runCatching {
                        applicationContext.assets.open(REFERENCE_POINTS_ASSET)
                            .bufferedReader()
                            .use(::parseReferencePoints)
                    }.getOrDefault(emptyList())
                    RouteData(
                        coordinates = coordinates,
                        referencePoints = referencePoints,
                        isLoading = false,
                    )
                }.getOrElse {
                    RouteData(isLoading = false, hasError = true)
                }
            }
            routeData.value = loadedData
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeConnectivity(): Flow<Boolean> = callbackFlow {
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

    private fun attachCoordinates(nodes: List<SensorNode>, route: RouteData): List<SensorNode> =
        nodes.map { node ->
            val normalizedNode = node.copy(pk = normalizePk(node.pk))
            if (normalizedNode.latitude != null && normalizedNode.longitude != null) {
                normalizedNode
            } else {
                val chainage = parseChainage(normalizedNode.pk)
                val reference = chainage?.let { value ->
                    route.referencePoints
                        .filter { it.chainageMeters != null }
                        .minByOrNull { point -> kotlin.math.abs(point.chainageMeters!! - value) }
                }
                val fallback = if (reference == null && route.coordinates.isNotEmpty()) {
                    val routeIndex = (normalizedNode.normalizedX.coerceIn(0f, 1f) * (route.coordinates.size - 1))
                        .toInt()
                    route.coordinates[routeIndex]
                } else {
                    null
                }
                val coordinate = reference?.coordinate ?: fallback
                if (coordinate == null) normalizedNode else normalizedNode.copy(
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude,
                )
            }
        }

    private fun parseRouteCoordinates(reader: java.io.BufferedReader): List<MapCoordinate> =
        reader.lineSequence().mapNotNull { line ->
            val value = line.trim()
            if (value.isEmpty() || value.startsWith("#")) return@mapNotNull null
            val parts = value.split(',')
            if (parts.size != 2) return@mapNotNull null
            val latitude = parts[0].trim().toDoubleOrNull() ?: return@mapNotNull null
            val longitude = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return@mapNotNull null
            MapCoordinate(latitude, longitude)
        }.toList()

    private fun parseReferencePoints(reader: java.io.BufferedReader): List<ReferencePoint> =
        reader.lineSequence().mapNotNull { line ->
            val value = line.trim()
            if (value.isEmpty() || value.startsWith("#")) return@mapNotNull null
            val parts = value.split(',', limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val latitude = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
            val longitude = parts[2].trim().toDoubleOrNull() ?: return@mapNotNull null
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return@mapNotNull null
            ReferencePoint(
                chainageMeters = parseChainage(parts[0]),
                coordinate = MapCoordinate(latitude, longitude),
            )
        }.toList()

    private fun normalizePk(value: String): String =
        value.trim().replace(Regex("^PK\\s*", RegexOption.IGNORE_CASE), "")

    private companion object {
        const val ROUTE_ASSET = "coordenadas.txt"
        const val REFERENCE_POINTS_ASSET = "puntos_0g.txt"
        val CHAINAGE_PATTERN = Regex("(?:PK\\s*)?(\\d+)\\s*(?:\\+\\s*(\\d+))?", RegexOption.IGNORE_CASE)

        fun parseChainage(value: String): Long? {
            val match = CHAINAGE_PATTERN.find(value) ?: return null
            val kilometers = match.groupValues[1].toLongOrNull() ?: return null
            val meters = match.groupValues[2].toLongOrNull() ?: 0L
            return kilometers * 1_000L + meters
        }
    }
}
