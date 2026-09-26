package com.hivend.agatha.ui.inspection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.EventCategory
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.InspectionResult
import com.hivend.agatha.domain.repository.AlertRepository
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
    val alert: Alert? = null,
    val result: InspectionResult? = null,
    val category: EventCategory? = EventCategory.GROUND_MOVEMENT,
    val observations: String = "",
    val saving: Boolean = false,
    val savedSuccessfully: Boolean = false,
) {
    val canSave: Boolean get() = result != null && category != null
}

@HiltViewModel
class InspectionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alertRepository: AlertRepository,
) : ViewModel() {

    val alertId: String =
        checkNotNull(savedStateHandle[AgathaDestination.InspectionForm.ARG_ALERT_ID])

    private val formState = MutableStateFlow(InspectionFormUiState())
    val uiState: StateFlow<InspectionFormUiState> = combine(
        formState,
        alertRepository.observeAlert(alertId),
    ) { form, alert -> form.copy(alert = alert) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InspectionFormUiState())

    fun onResultSelected(result: InspectionResult) {
        formState.update { it.copy(result = result) }
    }

    fun onCategorySelected(category: EventCategory) {
        formState.update { it.copy(category = category) }
    }

    fun onObservationsChange(text: String) {
        formState.update { it.copy(observations = text) }
    }

    fun saveInspection() {
        val state = uiState.value
        if (!state.canSave || state.saving) return

        viewModelScope.launch {
            formState.update { it.copy(saving = true) }
            alertRepository.registerInspection(
                Inspection(
                    alertId = alertId,
                    result = state.result!!,
                    category = state.category!!,
                    observations = state.observations,
                    recordedOffline = true,
                )
            )
            formState.update { it.copy(saving = false, savedSuccessfully = true) }
        }
    }
}
