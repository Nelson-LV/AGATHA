package com.hivend.agatha.ui.alertdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination
import com.hivend.agatha.domain.model.Alerta
import com.hivend.agatha.domain.model.EstadoAlerta
import com.hivend.agatha.domain.model.EventoHistorial
import com.hivend.agatha.domain.repository.AlertaRepository
import com.hivend.agatha.domain.repository.HistorialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlertDetailUiState(
    val alerta: Alerta? = null,
    val historialReciente: List<EventoHistorial> = emptyList(),
)

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alertaRepository: AlertaRepository,
    historialRepository: HistorialRepository,
) : ViewModel() {

    val alertaId: String =
        checkNotNull(savedStateHandle[AgathaDestination.AlertDetail.ARG_ALERT_ID])

    val uiState: StateFlow<AlertDetailUiState> = combine(
        alertaRepository.observarAlerta(alertaId),
        historialRepository.observarHistorial(alertaId),
    ) { alerta, historial ->
        AlertDetailUiState(alerta = alerta, historialReciente = historial.take(4))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertDetailUiState())

    fun avanzarEstado(nuevoEstado: EstadoAlerta) {
        viewModelScope.launch { alertaRepository.actualizarEstado(alertaId, nuevoEstado) }
    }
}
