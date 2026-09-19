package com.hivend.agatha.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.NodoSensor
import com.hivend.agatha.domain.repository.NodoSensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MapUiState(
    val nodos: List<NodoSensor> = emptyList(),
    val nodoSeleccionado: NodoSensor? = null,
)

/**
 * Mapa de nodos de sensores (HE-08). El nodo seleccionado por defecto es el primero de la
 * lista (igual que el prototipo, que abre con MP-1156 ya resaltado en la tarjeta inferior).
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    nodoSensorRepository: NodoSensorRepository,
) : ViewModel() {

    private val nodoSeleccionadoId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        nodoSensorRepository.observarNodos(),
        nodoSeleccionadoId,
    ) { nodos, seleccionadoId ->
        val seleccionado = nodos.find { it.id == seleccionadoId } ?: nodos.firstOrNull()
        MapUiState(nodos = nodos, nodoSeleccionado = seleccionado)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun seleccionarNodo(nodoId: String) {
        nodoSeleccionadoId.value = nodoId
    }
}
