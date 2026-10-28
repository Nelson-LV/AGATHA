package com.hivend.agatha.ui.alertdetail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.R
import com.hivend.agatha.core.device.DeviceOriginProvider
import com.hivend.agatha.core.navigation.AgathaDestination
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.ClassifyOutcome
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.TextLimits
import com.hivend.agatha.domain.repository.AlertRepository
import com.hivend.agatha.domain.repository.HistoryRepository
import com.hivend.agatha.ui.components.UiMessage
import com.hivend.agatha.ui.components.toUiMessage
import com.hivend.agatha.ui.history.toEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertDetailUiState(
    val alert: Alert? = null,
    /** Descargando el detalle de la alerta. */
    val loading: Boolean = true,
    /** Falla de la última descarga; con la alerta en caché se sigue mostrando lo que había. */
    val loadFailure: AlertFailure? = null,
    val recentHistory: List<HistoryEvent> = emptyList(),
    /** Reportes de inspección de esta alerta, el más reciente primero (HU-4.2). */
    val reports: List<Inspection> = emptyList(),
    /** Reportes cuya etiqueta u observación aún no llegó a la API. */
    val pendingReportIds: Set<String> = emptySet(),
    /** La clasificación se hizo en este celular y aún no se envía a la web. */
    val classificationPendingSync: Boolean = false,
    /** Texto de la observación que se está escribiendo. */
    val observationDraft: String = "",
    val savingObservation: Boolean = false,
    val classifying: Boolean = false,
) {
    val canAddObservation: Boolean get() = observationDraft.isNotBlank() && !savingObservation
}

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alertRepository: AlertRepository,
    private val historyRepository: HistoryRepository,
    private val originProvider: DeviceOriginProvider,
) : ViewModel() {

    val alertId: String =
        checkNotNull(savedStateHandle[AgathaDestination.AlertDetail.ARG_ALERT_ID])

    private data class Local(
        val loading: Boolean = true,
        val loadFailure: AlertFailure? = null,
        val observationDraft: String = "",
        val savingObservation: Boolean = false,
        val classifying: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)

    /** Avisos de una sola vez (errores de envío, conflictos) que la pantalla muestra en un snackbar. */
    val messages: SharedFlow<UiMessage> = _messages

    /** El historial es del dispositivo, no de la alerta (ids ALR-xxxx ≠ id del sensor). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val deviceHistory = alertRepository.observeAlert(alertId)
        .map { it?.sensorId }
        .distinctUntilChanged()
        .flatMapLatest { sensorId ->
            if (sensorId == null) flowOf(emptyList()) else historyRepository.observeHistory(sensorId).map { it.toEvents() }
        }

    val uiState: StateFlow<AlertDetailUiState> = combine(
        alertRepository.observeAlert(alertId),
        deviceHistory,
        alertRepository.observeInspections(alertId),
        alertRepository.observePendingInspectionIds(),
        alertRepository.observeClassificationPendingSync(alertId),
    ) { alert, history, reports, pendingIds, pendingSync ->
        AlertDetailUiState(
            alert = alert,
            recentHistory = history.take(4),
            reports = reports,
            pendingReportIds = pendingIds,
            classificationPendingSync = pendingSync,
        )
    }.combine(local) { state, local ->
        state.copy(
            loading = local.loading,
            loadFailure = local.loadFailure,
            observationDraft = local.observationDraft,
            savingObservation = local.savingObservation,
            classifying = local.classifying,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertDetailUiState())

    init {
        load()
    }

    /** Descarga el detalle (niveles, clasificación, observaciones) y el historial del dispositivo. */
    fun load() {
        local.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = alertRepository.loadAlert(alertId)
            local.update { it.copy(loading = false, loadFailure = (result as? AlertResult.Failure)?.failure) }
            alertRepository.observeAlert(alertId).first()?.let { historyRepository.refresh(it.sensorId) }
        }
    }

    fun onObservationChange(text: String) {
        local.update { it.copy(observationDraft = text.take(TextLimits.OBSERVATIONS)) }
    }

    /** Agrega la observación escrita (no se edita después). Si falla, el texto se conserva. */
    fun addObservation() {
        val draft = local.value.observationDraft.trim()
        if (draft.isEmpty() || local.value.savingObservation) return
        local.update { it.copy(savingObservation = true) }
        viewModelScope.launch {
            val result = alertRepository.addObservation(alertId, draft, originProvider.current())
            local.update {
                it.copy(
                    savingObservation = false,
                    observationDraft = if (result is AlertResult.Success) "" else it.observationDraft,
                )
            }
            if (result is AlertResult.Failure) _messages.tryEmit(result.failure.toUiMessage())
        }
    }

    /**
     * Primer envío (HU-5.2): guarda la clasificación con fecha-hora y origen del celular. Cuando
     * queda guardada (o en cola por falta de red) llama a [onSaved], que abre el reporte de
     * inspección con las etiquetas de esa clasificación; si la alerta ya estaba clasificada
     * desde otro cliente, avisa y no navega.
     */
    fun classify(classification: AlertClassification, onSaved: () -> Unit) {
        if (local.value.classifying) return
        local.update { it.copy(classifying = true) }
        viewModelScope.launch {
            val result = alertRepository.classifyAlert(alertId, classification, Instant.now(), originProvider.current())
            local.update { it.copy(classifying = false) }
            when (result) {
                is AlertResult.Failure -> _messages.tryEmit(result.failure.toUiMessage())
                is AlertResult.Success -> when (val outcome = result.value) {
                    ClassifyOutcome.Saved -> onSaved()
                    ClassifyOutcome.QueuedOffline -> {
                        _messages.tryEmit(UiMessage.Res(R.string.alert_classify_saved_offline))
                        onSaved()
                    }
                    is ClassifyOutcome.SavedAsConflict -> _messages.tryEmit(UiMessage.Res(outcome.existing.conflictMessage()))
                }
            }
        }
    }

    @StringRes
    private fun ClassificationRecord.conflictMessage(): Int = when (classification) {
        AlertClassification.CONFIRMED -> R.string.alert_classify_conflict_confirmed
        AlertClassification.FALSE_ALARM -> R.string.alert_classify_conflict_false_alarm
    }
}
