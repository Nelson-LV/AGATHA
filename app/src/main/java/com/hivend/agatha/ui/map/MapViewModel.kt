package com.hivend.agatha.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.domain.repository.ConnectivityMonitor
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.repository.AlertRepository
import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.domain.model.TelemetryResult
import com.hivend.agatha.domain.repository.SensorNodeRepository
import com.hivend.agatha.domain.repository.TelemetryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    /** true cuando ya llegó la lista de dispositivos de la plataforma. */
    val telemetryLoaded: Boolean = false,
)

/**
 * El nodo con el estado real de la plataforma: comunicación, batería y última lectura de su
 * dispositivo, y el color del nivel actual de su alerta en curso más alta (RN-03). Una alerta
 * conserva su color aunque el nodo no comunique; sin alerta, el nodo es Normal si comunica y
 * aviso gris "Sin comunicación" (RN-02) si no tiene dispositivo o su último dato es viejo.
 */
internal fun SensorNode.withPlatformState(
    device: TelemetryDevice?,
    ongoingLevel: AlertLevel?,
    now: Instant,
): SensorNode {
    val status = when (ongoingLevel) {
        AlertLevel.RED -> SensorNodeStatus.RED
        AlertLevel.ORANGE -> SensorNodeStatus.ORANGE
        AlertLevel.YELLOW -> SensorNodeStatus.YELLOW
        AlertLevel.GREEN, null ->
            if (device?.isConnected(now) == true) SensorNodeStatus.NORMAL else SensorNodeStatus.NO_COMMUNICATION
    }
    return copy(
        nodeStatus = status,
        batteryLevel = device?.lastBatteryLevel?.roundToInt(),
        lastReportTimestamp = device?.lastConnection,
    )
}

/** Nivel actual más alto entre las alertas en curso de cada dispositivo ("MP-11556"). */
internal fun List<Alert>.ongoingLevelBySensor(): Map<String, AlertLevel> =
    filter { it.isOngoing }
        .groupBy { it.sensorId }
        .mapValues { (_, alerts) -> alerts.maxOf { it.level } }

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
    private val telemetryRepository: TelemetryRepository,
    private val alertRepository: AlertRepository,
    connectivityMonitor: ConnectivityMonitor,
    @param:ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private val selectedNodeId = MutableStateFlow<String?>(null)
    private val statusFilter = MutableStateFlow<SensorNodeStatus?>(null)
    private val routeData = MutableStateFlow(RouteData())
    private val nodesLoaded = MutableStateFlow(false)

    /** Dispositivos reales de la plataforma; null mientras no haya llegado ninguna respuesta. */
    private val telemetryDevices = MutableStateFlow<List<TelemetryDevice>?>(null)

    /**
     * Los puntos y su ubicación salen del catálogo de nodos; su estado sale de la plataforma: la
     * comunicación, la batería y la última lectura, de los dispositivos (como en Dispositivos), y
     * el color, de las alertas en curso. Antes el mapa mostraba los valores de ejemplo y daba
     * "En línea" y Normal a un nodo que llevaba horas sin enviar datos.
     */
    private val sensorNodes = combine(
        sensorNodeRepository.observeSensorNodes()
            .onEach { nodesLoaded.value = true }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                nodesLoaded.value = true
                emit(emptyList())
            },
        telemetryDevices,
        alertRepository.observeAlerts().catch { exception ->
            if (exception is CancellationException) throw exception
            emit(emptyList())
        },
    ) { nodes, devices, alerts ->
        val now = Instant.now()
        val levels = alerts.ongoingLevelBySensor()
        nodes.map { node ->
            node.withPlatformState(
                device = devices?.firstOrNull { it.matchesNode(node.id) },
                ongoingLevel = levels[node.id],
                now = now,
            )
        }
    }

    private val connected = connectivityMonitor.isOnline
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

    val uiState: StateFlow<MapState> = combine(mapContent, connected, telemetryDevices) { content, isConnected, devices ->
        content.copy(isOffline = !isConnected, telemetryLoaded = devices != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapState())

    init {
        loadRouteData()
    }

    /**
     * Pide de nuevo los dispositivos y las alertas a la plataforma; la pantalla lo llama cada vez
     * que se abre. Si falla (sin red), se conservan los últimos datos recibidos.
     */
    fun refreshPlatformState() {
        viewModelScope.launch {
            val result = telemetryRepository.devices(refresh = true)
            if (result is TelemetryResult.Success) telemetryDevices.value = result.data
        }
        viewModelScope.launch { alertRepository.refreshAlerts() }
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
