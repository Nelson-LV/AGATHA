package com.hivend.agatha.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.repository.SensorNodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MapUiState(
    val nodes: List<SensorNode> = emptyList(),
    val selectedNode: SensorNode? = null,
)

/**
 * Mapa de nodos de sensores (HE-08). El nodo seleccionado por defecto es el primero de la
 * lista (igual que el prototipo, que abre con MP-1156 ya resaltado en la tarjeta inferior).
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    sensorNodeRepository: SensorNodeRepository,
) : ViewModel() {

    private val selectedNodeId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        sensorNodeRepository.observeNodes(),
        selectedNodeId,
    ) { nodes, seleccionadoId ->
        val selected = nodes.find { it.id == seleccionadoId } ?: nodes.firstOrNull()
        MapUiState(nodes = nodes, selectedNode = selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun selectNode(nodeId: String) {
        selectedNodeId.value = nodeId
    }
}
