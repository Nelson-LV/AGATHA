package com.hivend.agatha.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Expone la bandeja de alertas como [StateFlow] (patrón Observer): la pantalla vuelve a
 * dibujarse sola cuando el repositorio emite un cambio de estado, sin que la UI tenga que
 * pedir datos de forma imperativa. Solo muestra alertas (amarillo, naranja y rojo): el verde
 * indica normalidad y no es una alerta (ver [com.hivend.agatha.domain.model.AlertLevel.isAlert]).
 */
@HiltViewModel
class AlertInboxViewModel @Inject constructor(
    alertRepository: AlertRepository,
) : ViewModel() {

    val alerts: StateFlow<List<Alert>> = alertRepository.observeAlerts()
        .map { list -> list.filter { it.level.isAlert } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
