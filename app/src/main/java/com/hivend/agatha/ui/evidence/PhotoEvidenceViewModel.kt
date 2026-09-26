package com.hivend.agatha.ui.evidence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.PhotoEvidence
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

data class PhotoEvidenceUiState(
    val alert: Alert? = null,
    val evidence: List<PhotoEvidence> = emptyList(),
    val saving: Boolean = false,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class PhotoEvidenceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alertRepository: AlertRepository,
) : ViewModel() {

    val alertId: String =
        checkNotNull(savedStateHandle[AgathaDestination.PhotoEvidence.ARG_ALERT_ID])

    private val formState = MutableStateFlow(PhotoEvidenceUiState())
    val uiState: StateFlow<PhotoEvidenceUiState> = combine(
        formState,
        alertRepository.observeAlert(alertId),
    ) { form, alert -> form.copy(alert = alert) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoEvidenceUiState())

    fun onPhotoAdded(uri: String) {
        formState.update { it.copy(evidence = it.evidence + PhotoEvidence(uri, "")) }
    }

    fun onDescriptionChanged(uri: String, description: String) {
        formState.update { state ->
            state.copy(
                evidence = state.evidence.map {
                    if (it.uri == uri) it.copy(description = description) else it
                }
            )
        }
    }

    fun saveEvidence() {
        if (uiState.value.saving) return
        viewModelScope.launch {
            formState.update { it.copy(saving = true) }
            alertRepository.registerEvidence(alertId, uiState.value.evidence)
            formState.update { it.copy(saving = false, savedSuccessfully = true) }
        }
    }
}
