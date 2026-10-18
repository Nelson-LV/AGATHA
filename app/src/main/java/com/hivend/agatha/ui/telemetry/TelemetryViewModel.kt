package com.hivend.agatha.ui.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.DeviceTelemetry
import com.hivend.agatha.domain.model.TelemetryError
import com.hivend.agatha.domain.model.TelemetryResult
import com.hivend.agatha.domain.repository.TelemetryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TelemetryUiState {
    data object Loading : TelemetryUiState
    data class Loaded(val telemetry: DeviceTelemetry) : TelemetryUiState
    data class Failed(val error: TelemetryError) : TelemetryUiState
}

/**
 * Últimas 24 h de telemetría de un nodo, para las gráficas de "Ver sensores" (Dispositivos) y
 * "Ver gráfica" (detalle de alerta, HU-4.2). Se pide solo al abrir la gráfica y solo con
 * conexión; no se guarda en el celular. Una instancia por nodo: la pantalla la crea con
 * `hiltViewModel(key = …)`.
 */
@HiltViewModel
class TelemetryViewModel @Inject constructor(
    private val repository: TelemetryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TelemetryUiState>(TelemetryUiState.Loading)
    val state: StateFlow<TelemetryUiState> = _state.asStateFlow()

    private var request: Pair<String, Set<String>>? = null
    private var job: Job? = null

    private var refreshKey = 0

    /**
     * Pide las variables [keys] del nodo [nodeId]. Repetir la misma petición no la vuelve a
     * enviar, salvo que [refreshKey] cambie (actualización manual de la pantalla).
     */
    fun load(nodeId: String, keys: Set<String>, refreshKey: Int = 0) {
        val next = nodeId to keys
        if (next == request && refreshKey == this.refreshKey) return
        request = next
        this.refreshKey = refreshKey
        fetch()
    }

    fun retry() = fetch()

    private fun fetch() {
        val (nodeId, keys) = request ?: return
        job?.cancel()
        _state.value = TelemetryUiState.Loading
        job = viewModelScope.launch {
            _state.value = when (val result = repository.recentTelemetry(nodeId, keys)) {
                is TelemetryResult.Success -> TelemetryUiState.Loaded(result.data)
                is TelemetryResult.Failure -> TelemetryUiState.Failed(result.error)
            }
        }
    }
}
