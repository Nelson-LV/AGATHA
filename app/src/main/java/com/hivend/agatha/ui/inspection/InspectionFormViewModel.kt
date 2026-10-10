package com.hivend.agatha.ui.inspection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.device.DeviceOriginProvider
import com.hivend.agatha.core.navigation.AgathaDestination
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.InspectionResult
import com.hivend.agatha.domain.model.MaintenanceRecord
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.domain.model.TextLimits
import com.hivend.agatha.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que el técnico va llenando en el reporte. Ver [InspectionFormUiState] para lo derivado. */
data class InspectionDraft(
    val result: InspectionResult? = null,
    val resultOtherDetail: String = "",
    val classification: AlertClassification? = null,
    val tag: AlertTag? = null,
    val tagOtherDetail: String = "",
    val observations: String = "",
    val maintenanceDone: Boolean = false,
    val maintenanceTypes: Set<MaintenanceType> = emptySet(),
    val maintenanceOtherDetail: String = "",
    val maintenanceDescription: String = "",
)

data class InspectionFormUiState(
    val alert: Alert? = null,
    val draft: InspectionDraft = InspectionDraft(),
    val saving: Boolean = false,
    val savedSuccessfully: Boolean = false,
) {
    /** HU-5.2: solo se clasifica si la alerta está Sin clasificar según lo último sincronizado. */
    val canClassify: Boolean get() = alert?.managementStatus == ManagementStatus.UNCLASSIFIED

    /** Clasificación con la que se etiqueta: la elegida ahora o la que ya tenía la alerta. */
    val effectiveClassification: AlertClassification?
        get() = if (canClassify) draft.classification else alert?.classification?.classification

    /** La etiqueta solo se puede elegir si la alerta aún no tiene una (RN-06). */
    val canTag: Boolean
        get() = effectiveClassification != null && alert?.classification?.tag == null

    val resultDetailMissing: Boolean
        get() = draft.result == InspectionResult.OTHER && draft.resultOtherDetail.isBlank()

    val tagDetailMissing: Boolean
        get() = canTag && draft.tag?.requiresDetail == true && draft.tagOtherDetail.isBlank()

    val maintenanceIncomplete: Boolean
        get() = draft.maintenanceDone && (
            draft.maintenanceTypes.isEmpty() ||
                (MaintenanceType.OTHER in draft.maintenanceTypes && draft.maintenanceOtherDetail.isBlank())
            )

    val canSave: Boolean
        get() = draft.result != null && !resultDetailMissing && !tagDetailMissing && !maintenanceIncomplete
}

/**
 * Reporte de inspección (HE-05): resultado RN-16, clasificación y etiqueta de la alerta
 * asociada (HU-5.2), observación (HU-5.3) y mantenimiento (HU-5.4). Funciona sin conexión:
 * guarda en el repositorio local con fecha-hora y origen del celular.
 */
@HiltViewModel
class InspectionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alertRepository: AlertRepository,
    private val originProvider: DeviceOriginProvider,
) : ViewModel() {

    val alertId: String =
        checkNotNull(savedStateHandle[AgathaDestination.InspectionForm.ARG_ALERT_ID])

    private val formState = MutableStateFlow(InspectionFormUiState())
    val uiState: StateFlow<InspectionFormUiState> = combine(
        formState,
        alertRepository.observeAlert(alertId),
    ) { form, alert -> form.copy(alert = alert) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InspectionFormUiState())

    private fun edit(block: (InspectionDraft) -> InspectionDraft) {
        formState.update { it.copy(draft = block(it.draft)) }
    }

    fun onResultSelected(result: InspectionResult) = edit { it.copy(result = result) }

    fun onResultOtherDetailChange(text: String) = edit { it.copy(resultOtherDetail = text.take(TextLimits.OTHER_DETAIL)) }

    /** Tocar la clasificación elegida la quita; cambiarla borra la etiqueta de la otra lista. */
    fun onClassificationSelected(classification: AlertClassification) = edit {
        if (it.classification == classification) it.copy(classification = null, tag = null, tagOtherDetail = "")
        else it.copy(classification = classification, tag = null, tagOtherDetail = "")
    }

    fun onTagSelected(tag: AlertTag) = edit {
        if (it.tag == tag) it.copy(tag = null, tagOtherDetail = "") else it.copy(tag = tag, tagOtherDetail = "")
    }

    fun onTagOtherDetailChange(text: String) = edit { it.copy(tagOtherDetail = text.take(TextLimits.OTHER_DETAIL)) }

    fun onObservationsChange(text: String) = edit { it.copy(observations = text.take(TextLimits.OBSERVATIONS)) }

    fun onMaintenanceToggled(done: Boolean) = edit { it.copy(maintenanceDone = done) }

    fun onMaintenanceTypeToggled(type: MaintenanceType) = edit {
        val types = if (type in it.maintenanceTypes) it.maintenanceTypes - type else it.maintenanceTypes + type
        it.copy(maintenanceTypes = types)
    }

    fun onMaintenanceOtherDetailChange(text: String) = edit { it.copy(maintenanceOtherDetail = text.take(TextLimits.OTHER_DETAIL)) }

    fun onMaintenanceDescriptionChange(text: String) = edit { it.copy(maintenanceDescription = text.take(TextLimits.OBSERVATIONS)) }

    fun saveInspection() {
        val state = uiState.value
        val alert = state.alert ?: return
        val result = state.draft.result ?: return
        if (!state.canSave || state.saving) return
        val draft = state.draft
        val tag = draft.tag.takeIf { state.canTag }

        viewModelScope.launch {
            formState.update { it.copy(saving = true) }
            alertRepository.registerInspection(
                Inspection(
                    id = UUID.randomUUID().toString(),
                    deviceId = alert.sensorId,
                    alertId = alert.id,
                    result = result,
                    resultOtherDetail = draft.resultOtherDetail.trim().takeIf { result == InspectionResult.OTHER },
                    classification = draft.classification.takeIf { state.canClassify },
                    tag = tag,
                    tagOtherDetail = draft.tagOtherDetail.trim().takeIf { tag?.requiresDetail == true },
                    observations = draft.observations.trim().ifBlank { null },
                    maintenance = if (draft.maintenanceDone) {
                        MaintenanceRecord(
                            types = draft.maintenanceTypes,
                            otherDetail = draft.maintenanceOtherDetail.trim()
                                .takeIf { MaintenanceType.OTHER in draft.maintenanceTypes },
                            description = draft.maintenanceDescription.trim().ifBlank { null },
                        )
                    } else {
                        null
                    },
                    recordedAt = Instant.now(),
                    origin = originProvider.current(),
                ),
            )
            formState.update { it.copy(saving = false, savedSuccessfully = true) }
        }
    }
}
