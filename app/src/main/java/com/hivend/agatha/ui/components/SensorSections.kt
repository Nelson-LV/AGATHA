package com.hivend.agatha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.ui.theme.AgathaColors
import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.SensorSection
import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.ui.telemetry.TelemetryUiState
import com.hivend.agatha.ui.telemetry.TelemetryViewModel
import kotlin.math.roundToInt

private val SensorSection.icon: ImageVector
    get() = when (this) {
        SensorSection.MOTION -> Icons.Filled.Sensors
        SensorSection.LEAK -> Icons.Filled.GraphicEq
        SensorSection.ENVIRONMENT -> Icons.Filled.Thermostat
        SensorSection.POWER -> Icons.Filled.BatteryChargingFull
    }

private fun SensorSection.color(colors: AgathaColors): Color = when (this) {
    SensorSection.MOTION -> colors.critical
    SensorSection.LEAK -> colors.warning
    SensorSection.ENVIRONMENT -> colors.brand
    SensorSection.POWER -> colors.positive
}

/** Todas las variables que muestran las secciones: se piden juntas, en una sola llamada. */
private val allSectionKeys: Set<String> = SensorSection.entries.flatMap { it.keys }.toSet()

/**
 * Secciones de sensores de un dispositivo (movimiento, fuga, ambiente, energía), en la pestaña
 * Dispositivos desde el 14/10/2026. Al mostrarse piden las últimas 24 h del nodo [nodeId] a la
 * API de tablero (solo con conexión) y cada sección dibuja sus gráficas; Energía muestra la
 * última batería y el último dato recibido. Cada cambio de [refreshKey] las vuelve a pedir.
 */
@Composable
fun SensorSections(nodeId: String, modifier: Modifier = Modifier, refreshKey: Int = 0) {
    val viewModel: TelemetryViewModel = hiltViewModel(key = "sensors-$nodeId")
    LaunchedEffect(nodeId, refreshKey) { viewModel.load(nodeId, allSectionKeys, refreshKey) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Text(
            stringResource(R.string.telemetry_window_24h),
            color = AgathaTheme.colors.textTertiary,
            style = MaterialTheme.typography.labelSmall,
        )
        SensorSection.entries.forEach { section ->
            SensorRow(section = section, state = state, onRetry = viewModel::retry)
        }
    }
}

@Composable
private fun SensorRow(section: SensorSection, state: TelemetryUiState, onRetry: () -> Unit) {
    var expanded by rememberSaveable(section) { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Icon(section.icon, contentDescription = null, tint = section.color(AgathaTheme.colors), modifier = Modifier.size(20.dp))
            Text(
                stringResource(section.titleRes()),
                color = AgathaTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = AgathaTheme.colors.textSecondary,
            )
        }
        if (expanded) {
            Text(
                stringResource(section.detailRes()),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            TelemetryStateContent(state = state, onRetry = onRetry) { telemetry ->
                if (section == SensorSection.POWER) {
                    PowerSummary(telemetry.device)
                } else {
                    section.charts.forEach { keys ->
                        val series = keys.mapNotNull { telemetry.series(it) }
                        if (series.isNotEmpty()) TelemetryChart(series = series, from = telemetry.from, to = telemetry.to)
                    }
                }
            }
        }
    }
}

/** Batería informativa (sin color, Backlog 1.3) y hora del último dato recibido. */
@Composable
private fun PowerSummary(device: TelemetryDevice) {
    val empty = stringResource(R.string.common_empty_value)
    val battery = device.lastBatteryLevel?.let { stringResource(R.string.common_percent, it.roundToInt()) } ?: empty
    val lastData = device.lastConnection?.let { formatDateTime(it) } ?: empty
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.telemetry_battery_last, battery), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.telemetry_last_data, lastData), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
