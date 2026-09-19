package com.hivend.agatha.ui.inspection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination
import com.hivend.agatha.domain.model.Alerta
import com.hivend.agatha.domain.model.CategoriaEvento
import com.hivend.agatha.domain.model.Inspeccion
import com.hivend.agatha.domain.model.ResultadoInspeccion
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

data class InspectionFormUiState(
    val alerta: Alerta? = null,
    val resultado: ResultadoInspeccion? = null,
    val categoria: CategoriaEvento? = CategoriaEvento.MOVIMIENTO_TIERRA,
    val observaciones: String = "",
    val guardando: Boolean = false,
    val guardadoConExito: Boolean = false,
) {
    val puedeGuardar: Boolean get() = resultado != null && categoria != null
}

@HiltViewModel
class InspectionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alertaRepository: AlertaRepository,
) : ViewModel() {

    val alertaId: String =
        checkNotNull(savedStateHandle[AgathaDestination.InspectionForm.ARG_ALERT_ID])

    private val formState = MutableStateFlow(InspectionFormUiState())
    val uiState: StateFlow<InspectionFormUiState> = combine(
        formState,
        alertaRepository.observarAlerta(alertaId),
    ) { form, alerta -> form.copy(alerta = alerta) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InspectionFormUiState())

    fun onResultadoSeleccionado(resultado: ResultadoInspeccion) {
        formState.update { it.copy(resultado = resultado) }
    }

    fun onCategoriaSeleccionada(categoria: CategoriaEvento) {
        formState.update { it.copy(categoria = categoria) }
    }

    fun onObservacionesChange(texto: String) {
        formState.update { it.copy(observaciones = texto) }
    }

    fun guardarInspeccion() {
        val estado = uiState.value
        if (!estado.puedeGuardar || estado.guardando) return

        viewModelScope.launch {
            formState.update { it.copy(guardando = true) }
            alertaRepository.registrarInspeccion(
                Inspeccion(
                    alertaId = alertaId,
                    resultado = estado.resultado!!,
                    categoria = estado.categoria!!,
                    observaciones = estado.observaciones,
                    registradaSinConexion = true,
                )
            )
            formState.update { it.copy(guardando = false, guardadoConExito = true) }
        }
    }
}
