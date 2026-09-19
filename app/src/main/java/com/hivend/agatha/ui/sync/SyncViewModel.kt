package com.hivend.agatha.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.EstadoSincronizacion
import com.hivend.agatha.domain.repository.SincronizacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val sincronizacionRepository: SincronizacionRepository,
) : ViewModel() {

    val estado: StateFlow<EstadoSincronizacion?> = sincronizacionRepository.observarEstado()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun sincronizarAhora() {
        viewModelScope.launch { sincronizacionRepository.sincronizarAhora() }
    }

    fun reintentar(registroId: String) {
        viewModelScope.launch { sincronizacionRepository.reintentar(registroId) }
    }

    fun confirmarConflicto(alertaId: String) {
        viewModelScope.launch { sincronizacionRepository.confirmarConflicto(alertaId) }
    }
}
