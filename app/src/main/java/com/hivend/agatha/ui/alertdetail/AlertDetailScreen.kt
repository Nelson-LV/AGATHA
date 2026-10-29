package com.hivend.agatha.ui.alertdetail

import com.hivend.agatha.ui.theme.AgathaDataTextStyle
import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.hivend.agatha.domain.model.SensorSection
import com.hivend.agatha.ui.components.TelemetryChart
import com.hivend.agatha.ui.components.TelemetryStateContent
import com.hivend.agatha.ui.telemetry.TelemetryViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.ui.components.BackTopBar
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.LocalOnline
import com.hivend.agatha.ui.components.ClassificationChip
import com.hivend.agatha.ui.components.ManagementStatusChip
import com.hivend.agatha.ui.components.LevelChip
import com.hivend.agatha.ui.components.levelColor
import com.hivend.agatha.ui.components.shortLabelRes
import com.hivend.agatha.ui.components.formatDateTime
import com.hivend.agatha.ui.components.label
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.R
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.resolve
import com.hivend.agatha.ui.components.shortDuration

/**
 * Detalle de alerta (HE-04). Corresponde a los nodos 1:4 ("En inspección") y 27:2
 * ("Recibida") de Figma: es la MISMA pantalla en ambos casos — sus acciones principales
 * cambian según [Alerta.estado] en lugar de duplicar la vista, evitando la pantalla-por-estado
 * que el prototipo dibuja como dos frames separados. Ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Una pantalla, un estado — no una pantalla por variante".
 */
@Composable
fun AlertDetailScreen(
    onBack: () -> Unit,
    onRegisterInspection: (String) -> Unit,
    onViewHistory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlertDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val alert = uiState.alert
    val scope = rememberCoroutineScope()
    /** Clasificación elegida que espera confirmación: no se puede cambiar después (RN-06). */
    var pendingChoice by remember { mutableStateOf<AlertClassification?>(null) }

    pendingChoice?.let { choice ->
        ClassifyDialog(
            classification = choice,
            onConfirm = {
                pendingChoice = null
                scope.launch {
                    viewModel.classify(choice)
                    onRegisterInspection(viewModel.alertId)
                }
            },
            onDismiss = { pendingChoice = null },
        )
    }

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background).imePadding()) {
        BackTopBar(title = stringResource(R.string.alert_detail_title), onBack = onBack)
        val online = LocalOnline.current
        ConnectivityBar(
            connected = online,
            message = stringResource(if (online) R.string.connectivity_online else R.string.connectivity_offline_local_data),
        )

        if (alert == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.alert_detail_not_found), color = AgathaTheme.colors.textSecondary)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SitePill(site = alert.site) }
            item {
                AlertCard(
                    alert = alert,
                    onClassify = { pendingChoice = it },
                    onNewReport = { onRegisterInspection(alert.id) },
                )
            }
            item {
                ClassificationCard(
                    alert = alert,
                    pendingSync = uiState.classificationPendingSync,
                    onViewHistory = { onViewHistory(alert.sensorId) },
                )
            }
            item { LevelTimelineCard(alert) }
            item { ReportsCard(uiState.reports) }
            item {
                ObservationsCard(
                    text = uiState.observations,
                    dirty = uiState.observationsDirty,
                    hasSaved = alert.observations.isNotBlank(),
                    onTextChange = viewModel::onObservationsChange,
                    onSave = viewModel::saveObservations,
                )
            }
            item { IndicatorChartCard(sensorId = alert.sensorId) }
            item { RecentHistoryCard(events = uiState.recentHistory) }
        }
    }
}

/**
 * Encabezado y datos de la alerta como en la web (referencia 02): "chip nivel + Alerta ALR-xxxx +
 * chip estado" y la grilla Dispositivo, PK, Municipio, Evento, Inicio, Fin, Indicador y nivel
 * máximo (HU-4.2, RN-04, RN-05). Sin clasificar, la acción es "Falsa alerta" / "Confirmar
 * alerta" (HU-5.2); ya clasificada, un nuevo reporte de inspección.
 */
@Composable
private fun AlertCard(alert: Alert, onClassify: (AlertClassification) -> Unit, onNewReport: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .border(1.5.dp, alert.level.levelColor().copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LevelChip(alert.level)
            Text(
                stringResource(R.string.alert_detail_alert_id, alert.id),
                color = AgathaTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            ManagementStatusChip(alert.managementStatus)
        }
        Text(alert.description.resolve(), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)

        DataGrid(
            listOf(
                stringResource(R.string.alert_detail_device) to alert.sensorId,
                stringResource(R.string.alert_detail_pk) to alert.pk,
                stringResource(R.string.alert_detail_municipality) to alert.municipality,
                stringResource(R.string.alert_detail_event) to stringResource(alert.event.labelRes()),
                stringResource(R.string.alert_detail_start) to formatDateTime(alert.startedAt),
                stringResource(R.string.alert_detail_end) to (alert.endedAt?.let { formatDateTime(it) } ?: ""),
                stringResource(R.string.alert_detail_indicator) to (
                    alert.indicator?.let { stringResource(R.string.alert_detail_indicator_value, it.value, it.threshold) }
                        ?: stringResource(R.string.notice_indicator_unavailable)
                    ),
                stringResource(R.string.alert_detail_max_level) to stringResource(alert.maxLevel.shortLabelRes()),
            ),
            ongoing = alert.isOngoing,
        )

        if (alert.classification == null) {
            Text(
                stringResource(R.string.inspection_classification_info),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ClassificationButton(
                    text = stringResource(R.string.inspection_false_alarm),
                    color = AgathaTheme.colors.classificationFalseAlarm,
                    onClick = { onClassify(AlertClassification.FALSE_ALARM) },
                    modifier = Modifier.weight(1f),
                )
                ClassificationButton(
                    text = stringResource(R.string.inspection_confirm_alert),
                    color = AgathaTheme.colors.critical,
                    onClick = { onClassify(AlertClassification.CONFIRMED) },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Button(
                onClick = onNewReport,
                colors = ButtonDefaults.buttonColors(containerColor = AgathaTheme.colors.brand),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.alert_detail_new_inspection_report), modifier = Modifier.padding(start = 4.dp))
            }
        }

        TelemetryRow(alert)
    }
}

@Composable
private fun ClassificationButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.3.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = modifier,
    ) { Text(text) }
}

/** Confirma la clasificación antes de guardarla: es el primer envío y no se puede editar. */
@Composable
private fun ClassifyDialog(classification: AlertClassification, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alert_detail_classify_dialog_title, stringResource(classification.labelRes()))) },
        text = { Text(stringResource(R.string.alert_detail_classify_dialog_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.alert_detail_classify_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/** Grilla de dos columnas etiqueta/valor. Un "Fin" vacío se muestra como "En curso" en rojo. */
@Composable
private fun DataGrid(items: List<Pair<String, String>>, ongoing: Boolean) {
    val endLabel = stringResource(R.string.alert_detail_end)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = AgathaTheme.colors.textTertiary, style = MaterialTheme.typography.labelMedium)
                        if (label == endLabel && ongoing) {
                            Text(stringResource(R.string.alert_detail_ongoing), color = AgathaTheme.colors.critical, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(value, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/** Línea de tiempo de niveles (RN-05): cada cambio de nivel con su fecha-hora. */
@Composable
private fun LevelTimelineCard(alert: Alert) {
    SectionCard(title = stringResource(R.string.alert_detail_timeline)) {
        alert.levelTimeline.forEach { change ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LevelChip(change.level)
                Text(formatDateTime(change.at), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        alert.endedAt?.let { ended ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LevelChip(AlertLevel.GREEN)
                Text(
                    stringResource(R.string.alert_detail_timeline_end, formatDateTime(ended)),
                    color = AgathaTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Clasificación, estado de gestión y etiqueta, con fecha-hora y origen (RN-06, RN-10). */
@Composable
private fun ClassificationCard(alert: Alert, pendingSync: Boolean, onViewHistory: () -> Unit) {
    SectionCard(title = stringResource(R.string.alert_detail_classification)) {
        val record = alert.classification
        if (record == null) {
            ManagementStatusChip(alert.managementStatus)
            Text(stringResource(R.string.alert_detail_unclassified_hint), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ClassificationChip(record.classification)
                ManagementStatusChip(alert.managementStatus)
            }
            if (pendingSync) PendingSyncChip()
            Text(
                stringResource(R.string.common_dot_separated, formatDateTime(record.classifiedAt), record.origin.label()),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            val tag = record.tag
            Text(
                if (tag == null) stringResource(R.string.alert_detail_no_tag)
                else stringResource(R.string.inspection_saved_tag, tag.otherDetail ?: stringResource(tag.tag.labelRes())),
                color = AgathaTheme.colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            stringResource(R.string.alert_detail_view_device_history),
            color = AgathaTheme.colors.brand,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onViewHistory),
        )
    }
}

/**
 * Reportes de inspección de la alerta (HU-4.2): resultado, mantenimiento, observación y fotos,
 * con fecha-hora y origen. No se editan; los guardados en el celular esperan sincronizarse.
 */
@Composable
private fun ReportsCard(reports: List<Inspection>) {
    SectionCard(title = pluralStringResource(R.plurals.alert_detail_reports, reports.size, reports.size)) {
        if (reports.isEmpty()) {
            Text(stringResource(R.string.alert_detail_no_reports), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        reports.forEach { report ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.common_dot_separated, formatDateTime(report.recordedAt), report.origin.label()),
                        color = AgathaTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    PendingSyncChip()
                }
                report.tag?.let { tag ->
                    Text(
                        stringResource(R.string.inspection_saved_tag, report.tagOtherDetail ?: stringResource(tag.labelRes())),
                        color = AgathaTheme.colors.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                report.maintenance?.let { maintenance ->
                    val types = maintenance.types.sortedBy { it.ordinal }.map {
                        if (it == MaintenanceType.OTHER) maintenance.otherDetail.orEmpty() else stringResource(it.labelRes())
                    }
                    Text(
                        stringResource(R.string.alert_detail_report_maintenance, types.joinToString(", ")),
                        color = AgathaTheme.colors.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    maintenance.description?.let {
                        Text(it, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                report.observations?.let {
                    Text(it, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                if (report.photos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.photos.forEach { photo ->
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.description.ifBlank { null },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSyncChip() {
    StatusChip(
        text = stringResource(R.string.alert_detail_pending_sync),
        containerColor = AgathaTheme.colors.offlineContainer,
        contentColor = AgathaTheme.colors.offline,
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(title, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

/**
 * Observaciones de campo al mismo nivel que la información de la alerta: el técnico las
 * escribe sin tener que entrar a un reporte de inspección.
 */
@Composable
private fun ObservationsCard(
    text: String,
    dirty: Boolean,
    hasSaved: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.EditNote, contentDescription = null, tint = AgathaTheme.colors.brand, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.alert_detail_observations_title), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(stringResource(R.string.alert_detail_observations_placeholder)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (hasSaved && !dirty) {
                Text(stringResource(R.string.alert_detail_observations_saved), color = AgathaTheme.colors.positive, style = MaterialTheme.typography.bodySmall)
            } else {
                Box(Modifier)
            }
            OutlinedButton(onClick = onSave, enabled = dirty) {
                Text(stringResource(R.string.alert_detail_observations_save))
            }
        }
    }
}

@Composable
private fun TelemetryRow(alert: Alert) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, AgathaTheme.colors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TelemetryStat(stringResource(R.string.telemetry_confidence), stringResource(R.string.common_percent, alert.telemetry.confidencePercentage))
            TelemetryStat(stringResource(R.string.telemetry_acceleration), stringResource(alert.telemetry.acceleration.labelRes()), isData = false)
            TelemetryStat(stringResource(R.string.telemetry_time_ago), shortDuration(alert.minutesSinceStart()))
            TelemetryStat(stringResource(R.string.telemetry_battery), stringResource(R.string.common_percent, alert.telemetry.batteryPercentage))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(6.dp).background(if (alert.telemetry.isRealData) AgathaTheme.colors.positive else AgathaTheme.colors.textTertiary, CircleShape))
            Text(
                stringResource(if (alert.telemetry.isRealData) R.string.telemetry_real_data else R.string.telemetry_simulated_data),
                color = AgathaTheme.colors.stateClosed,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TelemetryStat(label: String, value: String, isData: Boolean = true) {
    Column {
        Text(label, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = AgathaTheme.colors.textPrimary, style = if (isData) AgathaDataTextStyle else MaterialTheme.typography.titleSmall)
    }
}


/**
 * "Ver gráfica" (HU-4.2): el indicador del dispositivo en las últimas 24 h, pedido a la API de
 * tablero solo cuando el técnico abre la gráfica y solo con conexión. El umbral no se dibuja:
 * el endpoint todavía no lo trae y no se inventa un valor.
 */
@Composable
private fun IndicatorChartCard(sensorId: String) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.alert_detail_indicator_chart),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { open = !open }) {
                Text(stringResource(if (open) R.string.alert_detail_hide_chart else R.string.alert_detail_view_chart))
            }
        }
        if (open) {
            val viewModel: TelemetryViewModel = hiltViewModel(key = "indicator-$sensorId")
            LaunchedEffect(sensorId) { viewModel.load(sensorId, setOf(SensorSection.INDICATOR_KEY)) }
            val state by viewModel.state.collectAsStateWithLifecycle()
            TelemetryStateContent(state = state, onRetry = viewModel::retry) { telemetry ->
                val series = listOfNotNull(telemetry.series(SensorSection.INDICATOR_KEY))
                TelemetryChart(series = series, from = telemetry.from, to = telemetry.to)
                Text(
                    stringResource(R.string.telemetry_threshold_pending),
                    color = AgathaTheme.colors.textTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun RecentHistoryCard(events: List<HistoryEvent>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(stringResource(R.string.alert_detail_device_history), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelLarge)
        events.forEach { event ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(
                    stringResource(R.string.common_dot_separated, event.time, event.title.resolve()),
                    color = AgathaTheme.colors.textPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(event.detail.resolve(), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                event.statusLabel?.let {
                    StatusChip(
                        text = it.resolve(),
                        containerColor = AgathaTheme.colors.surfaceVariant,
                        contentColor = AgathaTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
