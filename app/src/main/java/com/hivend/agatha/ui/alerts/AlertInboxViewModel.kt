package com.hivend.agatha.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.forAlertList
import com.hivend.agatha.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertInboxUiState(
    val alerts: List<Alert> = emptyList(),
    val refreshing: Boolean = false,
    /** Cuándo se descargó la lista por última vez; null = todavía no. */
    val lastSyncedAt: Instant? = null,
    /** Falla de la última actualización; las alertas ya descargadas se siguen mostrando. */
    val failure: AlertFailure? = null,
)

/**
 * Expone la bandeja de alertas como [StateFlow] (patrón Observer): la pantalla vuelve a
 * dibujarse sola cuando el repositorio emite un cambio de estado. Orden y filtro de HU-4.2: ver
 * [forAlertList]. La lista se descarga al abrir la pantalla y con [refresh].
 */
@HiltViewModel
class AlertInboxViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    private data class Sync(
        val refreshing: Boolean = false,
        val lastSyncedAt: Instant? = null,
        val failure: AlertFailure? = null,
    )

    private val sync = MutableStateFlow(Sync())

    val uiState: StateFlow<AlertInboxUiState> = combine(alertRepository.observeAlerts(), sync) { alerts, sync ->
        AlertInboxUiState(alerts.forAlertList(), sync.refreshing, sync.lastSyncedAt, sync.failure)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertInboxUiState())

    fun refresh() {
        if (sync.value.refreshing) return
        sync.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val result = alertRepository.refreshAlerts()
            sync.update {
                when (result) {
                    is AlertResult.Success -> Sync(refreshing = false, lastSyncedAt = Instant.now(), failure = null)
                    is AlertResult.Failure -> it.copy(refreshing = false, failure = result.failure)
                }
            }
        }
    }
}
