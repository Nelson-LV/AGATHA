package com.hivend.agatha.ui.inspection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.hivend.agatha.core.util.PreparedPhoto
import com.hivend.agatha.core.util.createEvidenceUri
import com.hivend.agatha.domain.model.PhotoEvidence
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.InspectionResult
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.domain.model.TextLimits
import com.hivend.agatha.ui.components.BackTopBar
import com.hivend.agatha.ui.components.ClassificationChip
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.ManagementStatusChip
import com.hivend.agatha.ui.components.formatDateTime
import com.hivend.agatha.ui.components.label
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.theme.AgathaTheme

/**
 * Reporte de inspección (HE-05, HU-5.1 a 5.4). Adapta a una sola pantalla los diálogos de la
 * web (docs/referencias-web/02 a 04): resultado RN-16, clasificación Confirmada / Falsa alerta
 * con la lista de etiquetas de esa clasificación, observación con contador, mantenimiento y
 * evidencias fotográficas.
 * Funciona sin conexión: el guardado solo escribe en el repositorio local.
 */
@Composable
fun InspectionFormScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InspectionFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaved(viewModel.alertId)
    }

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background).imePadding()) {
        BackTopBar(title = stringResource(R.string.inspection_top_bar_title), onBack = onBack)
        ConnectivityBar(connected = false, message = stringResource(R.string.connectivity_offline_saved_locally))

        val alert = uiState.alert ?: return@Column

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { ReportHeader(alert) }

            item {
                FormCard {
                    SectionTitle(stringResource(R.string.inspection_result_title))
                    RadioOptions(
                        options = InspectionResult.entries,
                        label = { it.labelRes() },
                        selected = draft.result,
                        onSelected = viewModel::onResultSelected,
                    )
                    if (draft.result == InspectionResult.OTHER) {
                        OtherDetailField(
                            value = draft.resultOtherDetail,
                            onValueChange = viewModel::onResultOtherDetailChange,
                            showError = uiState.resultDetailMissing,
                        )
                    }
                }
            }

            item {
                ClassificationSection(
                    uiState = uiState,
                    onClassificationSelected = viewModel::onClassificationSelected,
                    onTagSelected = viewModel::onTagSelected,
                    onTagOtherDetailChange = viewModel::onTagOtherDetailChange,
                )
            }

            item {
                FormCard {
                    if (alert.observations.isNotBlank()) {
                        PreviousObservation(alert.observations)
                    }
                    CountedTextField(
                        title = stringResource(R.string.inspection_observations_title),
                        value = draft.observations,
                        onValueChange = viewModel::onObservationsChange,
                        maxLength = TextLimits.OBSERVATIONS,
                        placeholder = stringResource(R.string.inspection_observations_placeholder),
                        minLines = 3,
                    )
                }
            }

            item {
                MaintenanceSection(
                    done = draft.maintenanceDone,
                    types = draft.maintenanceTypes,
                    otherDetail = draft.maintenanceOtherDetail,
                    description = draft.maintenanceDescription,
                    showOtherError = draft.maintenanceDone &&
                        MaintenanceType.OTHER in draft.maintenanceTypes && draft.maintenanceOtherDetail.isBlank(),
                    onToggle = viewModel::onMaintenanceToggled,
                    onTypeToggled = viewModel::onMaintenanceTypeToggled,
                    onOtherDetailChange = viewModel::onMaintenanceOtherDetailChange,
                    onDescriptionChange = viewModel::onMaintenanceDescriptionChange,
                )
            }

            item {
                PhotosSection(
                    photos = draft.photos,
                    rejected = draft.rejectedPhotos,
                    remaining = uiState.remainingPhotos,
                    onPhotosPicked = viewModel::onPhotosPicked,
                    onRemove = viewModel::onPhotoRemoved,
                    onDismissRejected = viewModel::onRejectedPhotoDismissed,
                    onDescriptionChange = viewModel::onPhotoDescriptionChange,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!uiState.canSave) {
                        Text(
                            stringResource(R.string.inspection_save_hint),
                            color = AgathaTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.common_cancel))
                        }
                        Button(
                            onClick = viewModel::saveInspection,
                            enabled = uiState.canSave && !uiState.saving,
                            colors = ButtonDefaults.buttonColors(containerColor = AgathaTheme.colors.brand),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (!uiState.saving) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                            Text(stringResource(if (uiState.saving) R.string.common_saving else R.string.inspection_save))
                        }
                    }
                }
            }
        }
    }
}

/** Encabezado como el de la web: dispositivo, PK, municipio y la alerta con su nivel y estado. */
@Composable
private fun ReportHeader(alert: Alert) {
    FormCard {
        Text(stringResource(R.string.inspection_title), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.inspection_device_subtitle, alert.sensorId, alert.pk, alert.municipality),
            color = AgathaTheme.colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.inspection_alert_line, stringResource(alert.level.labelRes()), alert.id, stringResource(alert.event.labelRes())),
                color = AgathaTheme.colors.textPrimary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            ManagementStatusChip(alert.managementStatus)
        }
    }
}

/**
 * HU-5.2. Sin clasificar: par de botones "Falsa alerta" / "Confirmar alerta" (opcional) y,
 * elegida una, la lista de causas de esa clasificación. Ya clasificada: solo lectura, y si
 * falta la etiqueta se puede agregar. Con etiqueta: solo lectura.
 */
@Composable
private fun ClassificationSection(
    uiState: InspectionFormUiState,
    onClassificationSelected: (AlertClassification) -> Unit,
    onTagSelected: (AlertTag) -> Unit,
    onTagOtherDetailChange: (String) -> Unit,
) {
    val alert = uiState.alert ?: return
    val saved = alert.classification
    FormCard {
        SectionTitle(stringResource(R.string.inspection_classification_title))
        if (uiState.canClassify) {
            Banner(
                icon = Icons.Filled.Info,
                text = stringResource(R.string.inspection_classification_info),
                container = AgathaTheme.colors.brandContainer,
                content = AgathaTheme.colors.onBrandContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ClassificationButton(
                    text = stringResource(R.string.inspection_false_alarm),
                    selected = uiState.draft.classification == AlertClassification.FALSE_ALARM,
                    color = AgathaTheme.colors.classificationFalseAlarm,
                    onClick = { onClassificationSelected(AlertClassification.FALSE_ALARM) },
                    modifier = Modifier.weight(1f),
                )
                ClassificationButton(
                    text = stringResource(R.string.inspection_confirm_alert),
                    selected = uiState.draft.classification == AlertClassification.CONFIRMED,
                    color = AgathaTheme.colors.critical,
                    onClick = { onClassificationSelected(AlertClassification.CONFIRMED) },
                    modifier = Modifier.weight(1f),
                )
            }
        } else if (saved != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClassificationChip(saved.classification)
                Text(
                    stringResource(R.string.common_dot_separated, formatDateTime(saved.classifiedAt), saved.origin.label()),
                    color = AgathaTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                stringResource(R.string.inspection_classification_read_only),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            saved.tag?.let { tagRecord ->
                Text(
                    stringResource(
                        R.string.inspection_saved_tag,
                        tagRecord.otherDetail ?: stringResource(tagRecord.tag.labelRes()),
                    ),
                    color = AgathaTheme.colors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        val classification = uiState.effectiveClassification
        if (uiState.canTag && classification != null) {
            SectionTitle(
                stringResource(
                    if (classification == AlertClassification.CONFIRMED) R.string.inspection_tag_title_confirmed
                    else R.string.inspection_tag_title_false_alarm,
                ),
            )
            RadioOptions(
                options = AlertTag.forClassification(classification),
                label = { it.labelRes() },
                selected = uiState.draft.tag,
                onSelected = onTagSelected,
            )
            if (uiState.draft.tag?.requiresDetail == true) {
                OtherDetailField(
                    value = uiState.draft.tagOtherDetail,
                    onValueChange = onTagOtherDetailChange,
                    showError = uiState.tagDetailMissing,
                )
            }
        }
    }
}

@Composable
private fun ClassificationButton(text: String, selected: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier) {
    if (selected) {
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = color), modifier = modifier) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 2.dp))
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            border = BorderStroke(1.3.dp, color),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            modifier = modifier,
        ) { Text(text) }
    }
}

@Composable
private fun PreviousObservation(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text(stringResource(R.string.inspection_previous_observation), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        Text(text, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

/** HU-5.4: interruptor "Se realizó mantenimiento", tipos (varios) y descripción opcional. */
@Composable
private fun MaintenanceSection(
    done: Boolean,
    types: Set<MaintenanceType>,
    otherDetail: String,
    description: String,
    showOtherError: Boolean,
    onToggle: (Boolean) -> Unit,
    onTypeToggled: (MaintenanceType) -> Unit,
    onOtherDetailChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    FormCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.inspection_maintenance_toggle),
                color = AgathaTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = done,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = AgathaTheme.colors.brand),
            )
        }
        if (done) {
            Text(stringResource(R.string.inspection_maintenance_types), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            MaintenanceType.entries.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onTypeToggled(type) },
                ) {
                    Checkbox(
                        checked = type in types,
                        onCheckedChange = { onTypeToggled(type) },
                        colors = CheckboxDefaults.colors(checkedColor = AgathaTheme.colors.brand),
                    )
                    Text(stringResource(type.labelRes()), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (MaintenanceType.OTHER in types) {
                OtherDetailField(value = otherDetail, onValueChange = onOtherDetailChange, showError = showOtherError)
            }
            CountedTextField(
                title = stringResource(R.string.inspection_maintenance_description),
                value = description,
                onValueChange = onDescriptionChange,
                maxLength = TextLimits.OBSERVATIONS,
                placeholder = stringResource(R.string.inspection_maintenance_description_placeholder),
                minLines = 2,
            )
        }
    }
}

/**
 * Evidencias fotográficas del reporte (HE-06, opcional), como en la web: contador n/5, cámara
 * o galería, descripción opcional por foto (máx. 200) y error por archivo rechazado. La cámara
 * del sistema y el Photo Picker no requieren pedir permisos en tiempo de ejecución.
 */
@Composable
private fun PhotosSection(
    photos: List<PhotoDraft>,
    rejected: List<PreparedPhoto.Rejected>,
    remaining: Int,
    onPhotosPicked: (List<Uri>) -> Unit,
    onRemove: (String) -> Unit,
    onDismissRejected: (PreparedPhoto.Rejected) -> Unit,
    onDescriptionChange: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) onPhotosPicked(listOf(uri))
    }
    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(PhotoEvidence.MAX_PER_REPORT),
    ) { uris -> if (uris.isNotEmpty()) onPhotosPicked(uris) }

    FormCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            SectionTitle(stringResource(R.string.evidence_title))
            Box(Modifier.weight(1f))
            Text(
                stringResource(R.string.common_char_counter, photos.size, PhotoEvidence.MAX_PER_REPORT),
                color = AgathaTheme.colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(stringResource(R.string.evidence_formats_hint), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)

        if (remaining > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val uri = createEvidenceUri(context)
                        pendingCameraUri = uri
                        takePhoto.launch(uri)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                    Text(stringResource(R.string.evidence_take_photo))
                }
                OutlinedButton(
                    onClick = { pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                    Text(stringResource(R.string.evidence_pick_gallery))
                }
            }
        } else {
            Banner(
                icon = Icons.Filled.Info,
                text = stringResource(R.string.evidence_limit_reached),
                container = AgathaTheme.colors.surfaceVariant,
                content = AgathaTheme.colors.textSecondary,
            )
        }

        photos.forEach { photo ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.description.ifBlank { null },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AgathaTheme.colors.surfaceVariant),
                )
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = photo.description,
                        onValueChange = { onDescriptionChange(photo.uri, it) },
                        placeholder = { Text(stringResource(R.string.evidence_add_description), style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.common_char_counter, photo.description.length, PhotoEvidence.DESCRIPTION_MAX_LENGTH),
                        color = AgathaTheme.colors.textTertiary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
                IconButton(onClick = { onRemove(photo.uri) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.evidence_remove), tint = AgathaTheme.colors.textSecondary)
                }
            }
        }

        rejected.forEach { file ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AgathaTheme.colors.criticalSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, AgathaTheme.colors.criticalBorder, RoundedCornerShape(10.dp))
                    .padding(start = 12.dp),
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = AgathaTheme.colors.critical, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.fileName, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(
                            when (file.reason) {
                                PreparedPhoto.Reason.FORMAT_NOT_ALLOWED -> R.string.evidence_format_not_allowed
                                PreparedPhoto.Reason.UNREADABLE -> R.string.evidence_unreadable
                            },
                        ),
                        color = AgathaTheme.colors.critical,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = { onDismissRejected(file) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.evidence_remove), tint = AgathaTheme.colors.textSecondary)
                }
            }
        }
    }
}

/** "¿Cuál? (obligatorio, 1 a 100 caracteres)" con contador y el error de la web. */
@Composable
private fun OtherDetailField(value: String, onValueChange: (String) -> Unit, showError: Boolean) {
    CountedTextField(
        title = stringResource(R.string.inspection_other_detail_title),
        value = value,
        onValueChange = onValueChange,
        maxLength = TextLimits.OTHER_DETAIL,
        placeholder = stringResource(R.string.inspection_other_detail_placeholder),
        isError = showError,
        errorText = stringResource(R.string.inspection_other_detail_error),
    )
}

@Composable
private fun CountedTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    placeholder: String,
    minLines: Int = 1,
    isError: Boolean = false,
    errorText: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(title, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.common_char_counter, value.length, maxLength),
                color = AgathaTheme.colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            minLines = minLines,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(errorBorderColor = AgathaTheme.colors.critical),
            modifier = Modifier.fillMaxWidth(),
        )
        if (isError && errorText != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = AgathaTheme.colors.critical, modifier = Modifier.size(14.dp))
                Text(errorText, color = AgathaTheme.colors.critical, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun <T> RadioOptions(
    options: List<T>,
    label: (T) -> Int,
    selected: T?,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) AgathaTheme.colors.brand.copy(alpha = 0.08f) else AgathaTheme.colors.surfaceVariant,
                        RoundedCornerShape(10.dp),
                    )
                    .border(1.dp, if (isSelected) AgathaTheme.colors.brand else AgathaTheme.colors.border, RoundedCornerShape(10.dp))
                    .clickable { onSelected(option) }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelected(option) },
                    colors = RadioButtonDefaults.colors(selectedColor = AgathaTheme.colors.brand),
                )
                Text(stringResource(label(option)), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Banner(icon: ImageVector, text: String, container: Color, content: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().background(container, RoundedCornerShape(10.dp)).padding(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, color = content, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) { content() }
}
