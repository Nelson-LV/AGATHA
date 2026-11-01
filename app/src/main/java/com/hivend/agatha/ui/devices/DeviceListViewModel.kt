package com.hivend.agatha.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.domain.model.TelemetryError
import com.hivend.agatha.domain.model.TelemetryResult
import com.hivend.agatha.domain.repository.TelemetryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceListUiState(
    val devices: List<TelemetryDevice> = emptyList(),
    /** Primera carga, sin datos todavía. */
    val loading: Boolean = true,
    /** Recarga pedida por el técnico (deslizar hacia abajo o el botón de actualizar). */
    val refreshing: Boolean = false,
    /** Error de la última carga; con dispositivos ya cargados se siguen mostrando. */
    val error: TelemetryError? = null,
    /** Hora de la última respuesta correcta del servidor. */
    val updatedAt: Instant? = null,
    /** Sube en cada actualización manual para que las gráficas abiertas se pidan de nuevo. */
    val refreshTick: Int = 0,
)

/**
 * Pestaña Dispositivos con los dispositivos reales de la plataforma (`/api/dashboard/devices`):
 * desde el 17/10/2026 ya no muestra los nodos de ejemplo. Solo con conexión; el técnico puede
 * actualizar a mano la lista, la batería y las gráficas abiertas.
 */
@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val repository: TelemetryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    init {
        load(refresh = false)
    }

    fun refresh() {
        if (_uiState.value.refreshing) return
        _uiState.update { it.copy(refreshing = true) }
        load(refresh = true)
    }

    /**
     * Al volver a la pestaña la lista se pide de nuevo si tiene más de [STALE_AFTER]: la última
     * conexión y la batería que muestra cada tarjeta salen de ella, mientras que las gráficas se
     * piden cada vez. Sin esto la tarjeta se quedaba en la hora de la primera carga.
     */
    fun onResume() {
        val state = _uiState.value
        if (state.loading || state.refreshing) return
        val updatedAt = state.updatedAt
        if (updatedAt == null || Duration.between(updatedAt, Instant.now()) > STALE_AFTER) refresh()
    }

    private fun load(refresh: Boolean) {
        viewModelScope.launch {
            val result = repository.devices(refresh)
            _uiState.update { current ->
                // Las gráficas se recargan después de la lista, para que Energía use la batería nueva.
                val state = if (refresh) current.copy(refreshTick = current.refreshTick + 1) else current
                when (result) {
                    is TelemetryResult.Success -> state.copy(
                        devices = result.data,
                        loading = false,
                        refreshing = false,
                        error = null,
                        updatedAt = Instant.now(),
                    )
                    is TelemetryResult.Failure -> state.copy(loading = false, refreshing = false, error = result.error)
                }
            }
        }
    }

    private companion object {
        val STALE_AFTER: Duration = Duration.ofSeconds(60)
    }
}
