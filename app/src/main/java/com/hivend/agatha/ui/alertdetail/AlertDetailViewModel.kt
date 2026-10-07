package com.hivend.agatha.ui.alertdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertStatus
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.repository.AlertRepository
import com.hivend.agatha.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlertDetailUiState(
    val alert: Alert? = null,
    val recentHistory: List<HistoryEvent> = emptyList(),
    /** Texto que se ve en el campo de observaciones (borrador o lo ya guardado). */
    val observations: String = "",
    /** true cuando hay cambios en el campo que aún no se guardaron. */
    val observationsDirty: Boolean = false,
)

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alertRepository: AlertRepository,
    historyRepository: HistoryRepository,
) : ViewModel() {

    val alertId: String =
        checkNotNull(savedStateHandle[AgathaDestination.AlertDetail.ARG_ALERT_ID])

    /** null = sin edición en curso; el campo muestra lo guardado en la alerta. */
    private val observationsDraft = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AlertDetailUiState> = combine(
        alertRepository.observeAlert(alertId),
        historyRepository.observeHistory(alertId),
        observationsDraft,
    ) { alert, history, draft ->
        val saved = alert?.observations.orEmpty()
        AlertDetailUiState(
            alert = alert,
            recentHistory = history.take(4),
            observations = draft ?: saved,
            observationsDirty = draft != null && draft != saved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertDetailUiState())

    fun advanceStatus(newStatus: AlertStatus) {
        viewModelScope.launch { alertRepository.updateStatus(alertId, newStatus) }
    }

    fun onObservationsChange(text: String) {
        observationsDraft.value = text
    }

    fun saveObservations() {
        val draft = observationsDraft.value ?: return
        viewModelScope.launch {
            alertRepository.saveObservations(alertId, draft)
            observationsDraft.value = null
        }
    }
}
