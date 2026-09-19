package com.hivend.agatha.ui.evidence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination
import com.hivend.agatha.domain.model.Alerta
import com.hivend.agatha.domain.model.EvidenciaFoto
import com.hivend.agatha.domain.repository.AlertaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoEvidenceUiState(
    val alerta: Alerta? = null,
    val evidencias: List<EvidenciaFoto> = emptyList(),
    val guardando: Boolean = false,
    val guardadoConExito: Boolean = false,
)

@HiltViewModel
class PhotoEvidenceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alertaRepository: AlertaRepository,
) : ViewModel() {

    val alertaId: String =
        checkNotNull(savedStateHandle[AgathaDestination.PhotoEvidence.ARG_ALERT_ID])

    private val formState = MutableStateFlow(PhotoEvidenceUiState())
    val uiState: StateFlow<PhotoEvidenceUiState> = combine(
        formState,
        alertaRepository.observarAlerta(alertaId),
    ) { form, alerta -> form.copy(alerta = alerta) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoEvidenceUiState())

    fun onFotoAgregada(uri: String) {
        formState.update { it.copy(evidencias = it.evidencias + EvidenciaFoto(uri, "")) }
    }

    fun onDescripcionCambiada(uri: String, descripcion: String) {
        formState.update { estado ->
            estado.copy(
                evidencias = estado.evidencias.map {
                    if (it.uri == uri) it.copy(descripcion = descripcion) else it
                }
            )
        }
    }

    fun guardarEvidencias() {
        if (uiState.value.guardando) return
        viewModelScope.launch {
            formState.update { it.copy(guardando = true) }
            alertaRepository.registrarEvidencias(alertaId, uiState.value.evidencias)
            formState.update { it.copy(guardando = false, guardadoConExito = true) }
        }
    }
}
