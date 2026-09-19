package com.hivend.agatha.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.Alerta
import com.hivend.agatha.domain.repository.AlertaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Respalda la pantalla de "notificación push" simulada (nodo 34:2 de Figma). Muestra la
 * alerta más severa vigente, tal como haría un push real de FCM (HE-04, RNF-MOV-06) antes
 * de que el pipeline de notificaciones esté conectado a Firebase — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Notificaciones push: simulación vs. FCM real".
 */
@HiltViewModel
class NotificationPreviewViewModel @Inject constructor(
    alertaRepository: AlertaRepository,
) : ViewModel() {

    val alertaDestacada: StateFlow<Alerta?> = alertaRepository.observarAlertas()
        .map { alertas -> alertas.minByOrNull { it.nivel.severidad() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private fun com.hivend.agatha.domain.model.NivelAlerta.severidad(): Int = when (this) {
    com.hivend.agatha.domain.model.NivelAlerta.ROJO -> 0
    com.hivend.agatha.domain.model.NivelAlerta.AMARILLO -> 1
    com.hivend.agatha.domain.model.NivelAlerta.VERDE -> 2
}
