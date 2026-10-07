package com.hivend.agatha.ui.alertdetail

import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector
import com.hivend.agatha.ui.theme.AgathaColors
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.BatteryChargingFull
import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertStatus
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.ui.components.BackTopBar
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.AlertStatusChip
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R
import androidx.annotation.StringRes
import com.hivend.agatha.ui.components.labelRes

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

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        BackTopBar(title = stringResource(R.string.alert_detail_title), onBack = onBack)
        ConnectivityBar(connected = true, message = stringResource(R.string.connectivity_connected_recent))

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
                    onAdvance = { viewModel.advanceStatus(it) },
                    onRegisterInspection = { onRegisterInspection(alert.id) },
                )
            }
            item {
                FiltersRow(
                    status = alert.status,
                    onViewHistory = { onViewHistory(alert.sensorId) },
                )
            }
            item { AccelerationGraphCard() }
            item { RecentHistoryCard(events = uiState.recentHistory) }
            item { SensorAccordionList() }
        }
    }
}

@Composable
private fun AlertCard(alert: Alert, onAdvance: (AlertStatus) -> Unit, onRegisterInspection: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.criticalSurface, RoundedCornerShape(14.dp))
            .border(1.5.dp, AgathaTheme.colors.criticalBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            text = stringResource(R.string.alert_detail_header, stringResource(alert.level.labelRes()), alert.sensorId, alert.pk),
            color = AgathaTheme.colors.critical,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(alert.description, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)

        PrimaryActions(status = alert.status, onAdvance = onAdvance)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(AgathaTheme.colors.brandContainer, RoundedCornerShape(10.dp))
                .border(1.3.dp, AgathaTheme.colors.brandContainer, RoundedCornerShape(10.dp))
                .padding(10.dp),
        ) {
            Icon(Icons.Filled.Build, contentDescription = null, tint = AgathaTheme.colors.brand, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.alert_detail_scheduled_maintenance),
                color = AgathaTheme.colors.brand,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }

        TelemetryRow(alert)

        Text(stringResource(R.string.alert_detail_next_step), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AgathaTheme.colors.border, RoundedCornerShape(8.dp))
                .clickable(onClick = onRegisterInspection)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(alert.nextStep, color = AgathaTheme.colors.brand, style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = AgathaTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun PrimaryActions(status: AlertStatus, onAdvance: (AlertStatus) -> Unit) {
    when (status) {
        AlertStatus.GENERATED, AlertStatus.RECEIVED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onAdvance(AlertStatus.IN_INSPECTION) },
                colors = ButtonDefaults.buttonColors(containerColor = AgathaTheme.colors.positive),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.alert_detail_start_inspection), modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedButton(
                onClick = { onAdvance(AlertStatus.CLOSED) },
                border = BorderStroke(1.3.dp, AgathaTheme.colors.critical),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AgathaTheme.colors.critical),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.alert_detail_mark_false), modifier = Modifier.padding(start = 4.dp))
            }
        }
        AlertStatus.IN_INSPECTION -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onAdvance(AlertStatus.CLASSIFIED) },
                colors = ButtonDefaults.buttonColors(containerColor = AgathaTheme.colors.positive),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.alert_detail_update_status), modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedButton(
                onClick = { onAdvance(AlertStatus.CLOSED) },
                border = BorderStroke(1.3.dp, AgathaTheme.colors.critical),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AgathaTheme.colors.critical),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.alert_detail_close_alert), modifier = Modifier.padding(start = 4.dp))
            }
        }
        AlertStatus.CLASSIFIED, AlertStatus.CLOSED -> StatusChip(
            text = stringResource(
                if (status == AlertStatus.CLOSED) R.string.alert_detail_no_pending_actions else R.string.alert_detail_pending_operator_close,
            ),
            containerColor = AgathaTheme.colors.surfaceVariant,
            contentColor = AgathaTheme.colors.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
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
            TelemetryStat(stringResource(R.string.telemetry_acceleration), alert.telemetry.acceleration)
            TelemetryStat(stringResource(R.string.telemetry_time_ago), alert.telemetry.relativeTime)
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
private fun TelemetryStat(label: String, value: String) {
    Column {
        Text(label, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun FiltersRow(status: AlertStatus, onViewHistory: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        StatusChip(
            text = stringResource(R.string.alert_detail_status, stringResource(status.labelRes())),
            containerColor = AgathaTheme.colors.surfaceVariant,
            contentColor = AgathaTheme.colors.textOnMuted,
        )
        Text(
            stringResource(R.string.alert_detail_view_status_history),
            color = AgathaTheme.colors.brand,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onViewHistory),
        )
    }
}

@Composable
private fun AccelerationGraphCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(stringResource(R.string.alert_detail_acceleration_chart), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelLarge)
        val colors = AgathaTheme.colors
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(colors.mapCanvas, RoundedCornerShape(10.dp)),
        ) {
            val points = listOf(
                Offset(size.width * 0.06f, size.height * 0.82f) to colors.nodeNormal,
                Offset(size.width * 0.5f, size.height * 0.45f) to colors.nodeAlert,
                Offset(size.width * 0.92f, size.height * 0.14f) to colors.nodeCritical,
            )
            drawLine(
                color = colors.brand,
                start = points[0].first,
                end = points[1].first,
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            drawLine(
                color = colors.brand,
                start = points[1].first,
                end = points[2].first,
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            points.forEach { (offset, color) ->
                drawCircle(color = color, radius = 7f, center = offset)
                drawCircle(color = colors.nodeOutline, radius = 7f, center = offset, style = Stroke(width = 2f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LegendDot(stringResource(R.string.legend_normal), AgathaTheme.colors.nodeNormal)
            LegendDot(stringResource(R.string.legend_alert), AgathaTheme.colors.nodeAlert)
            LegendDot(stringResource(R.string.legend_critical), AgathaTheme.colors.nodeCritical)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(label, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelSmall)
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
                Text("${event.time} · ${event.title}", color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.labelLarge)
                Text(event.detail, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                event.statusLabel?.let {
                    StatusChip(
                        text = it,
                        containerColor = AgathaTheme.colors.surfaceVariant,
                        contentColor = AgathaTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

private data class SensorInfo(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val detailRes: Int,
    val color: AgathaColors.() -> Color,
)

private val sensors = listOf(
    SensorInfo(Icons.Filled.Sensors, R.string.sensor_motion_title, R.string.sensor_motion_detail) { critical },
    SensorInfo(Icons.Filled.GraphicEq, R.string.sensor_acoustic_title, R.string.sensor_acoustic_detail) { warning },
    SensorInfo(Icons.Filled.Thermostat, R.string.sensor_environment_title, R.string.sensor_environment_detail) { brand },
    SensorInfo(Icons.Filled.BatteryChargingFull, R.string.sensor_power_title, R.string.sensor_power_detail) { positive },
)

@Composable
private fun SensorAccordionList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sensors.forEach { sensor -> AccordionRow(sensor) }
    }
}

@Composable
private fun AccordionRow(sensor: SensorInfo) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(sensor.icon, contentDescription = null, tint = sensor.color(AgathaTheme.colors), modifier = Modifier.size(20.dp))
                Text(stringResource(sensor.titleRes), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
            }
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = AgathaTheme.colors.textSecondary)
        }
        if (expanded) {
            Text(
                stringResource(sensor.detailRes),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
