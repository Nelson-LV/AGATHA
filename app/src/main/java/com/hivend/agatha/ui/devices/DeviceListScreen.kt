package com.hivend.agatha.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.domain.model.TelemetryError
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.LocalOnline
import com.hivend.agatha.ui.components.SensorSections
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.components.formatDateTime
import com.hivend.agatha.ui.components.messageRes
import com.hivend.agatha.ui.components.relativeTimeAgo
import com.hivend.agatha.ui.theme.AgathaTheme
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Pestaña Dispositivos: los dispositivos reales de la plataforma, cada uno con su batería y su
 * último dato, y las secciones de sensores con las gráficas de las últimas 24 h. Se actualiza
 * a mano deslizando hacia abajo o con el botón de actualizar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onViewHistory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Sin red del celular o la API no respondió por falta de conexión.
    val offline = !LocalOnline.current || state.error == TelemetryError.NO_CONNECTION

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        AgathaHeader()
        ConnectivityBar(
            connected = !offline,
            message = when {
                offline -> stringResource(R.string.devices_offline)
                state.updatedAt != null -> stringResource(R.string.devices_updated_at, formatDateTime(state.updatedAt!!))
                else -> stringResource(R.string.telemetry_loading)
            },
        )

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.devices_title),
                            color = AgathaTheme.colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.devices.isNotEmpty()) {
                            StatusChip(
                                text = pluralStringResource(R.plurals.devices_count, state.devices.size, state.devices.size),
                                containerColor = AgathaTheme.colors.surfaceVariant,
                                contentColor = AgathaTheme.colors.textSecondary,
                            )
                        }
                        IconButton(onClick = viewModel::refresh, enabled = !state.refreshing) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.devices_refresh), tint = AgathaTheme.colors.brand)
                        }
                    }
                }
                when {
                    state.loading -> item { LoadingRow() }
                    state.devices.isEmpty() && state.error != null -> item {
                        MessageWithRetry(stringResource(state.error!!.messageRes()), onRetry = viewModel::refresh)
                    }
                    state.devices.isEmpty() -> item {
                        Text(stringResource(R.string.devices_empty), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> items(state.devices, key = { it.id }) { device ->
                        DeviceWithSensors(
                            device = device,
                            refreshKey = state.refreshTick,
                            onViewHistory = { onViewHistory(device.nodeCode) },
                        )
                    }
                }
            }
        }
    }
}

/** Ficha del dispositivo con sus secciones de sensores plegadas debajo. */
@Composable
private fun DeviceWithSensors(device: TelemetryDevice, refreshKey: Int, onViewHistory: () -> Unit) {
    var showSensors by rememberSaveable(device.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RealDeviceCard(device = device, onViewHistory = onViewHistory)
        Text(
            stringResource(if (showSensors) R.string.devices_sensors_hide else R.string.devices_sensors_show),
            color = AgathaTheme.colors.brand,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clickable { showSensors = !showSensors }
                .padding(horizontal = 4.dp, vertical = 4.dp),
        )
        if (showSensors) SensorSections(nodeId = device.nodeCode, refreshKey = refreshKey)
    }
}

/**
 * Ficha de un dispositivo real: código, PK, conexión (según la hora del último dato), batería
 * informativa y último dato. Sin nivel de alerta ni tipo de sensor: la API no los entrega.
 */
@Composable
private fun RealDeviceCard(device: TelemetryDevice, onViewHistory: () -> Unit) {
    val connected = device.isConnected(Instant.now())
    val connectionColor = if (connected) AgathaTheme.colors.positive else AgathaTheme.colors.nodeNotice
    val empty = stringResource(R.string.common_empty_value)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).background(connectionColor, CircleShape))
                Text(stringResource(R.string.node_name, device.nodeCode), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)
            }
            StatusChip(
                text = stringResource(if (connected) R.string.legend_connected else R.string.notice_no_communication),
                containerColor = connectionColor.copy(alpha = 0.14f),
                contentColor = connectionColor,
            )
        }
        Text(device.pk ?: device.name, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            InfoStat(
                label = stringResource(R.string.map_node_battery),
                value = device.lastBatteryLevel?.let { stringResource(R.string.common_percent, it.roundToInt()) } ?: empty,
            )
            InfoStat(
                label = stringResource(R.string.map_node_last_communication),
                value = device.lastConnection?.let {
                    stringResource(
                        R.string.common_dot_separated,
                        relativeTimeAgo(Duration.between(it, Instant.now()).toMinutes().toInt()),
                        formatDateTime(it),
                    )
                } ?: empty,
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            onClick = onViewHistory,
            colors = ButtonDefaults.buttonColors(containerColor = AgathaTheme.colors.brand),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.map_view_full_history))
        }
    }
}

@Composable
private fun InfoStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LoadingRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = AgathaTheme.colors.brand)
        Text(stringResource(R.string.telemetry_loading), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MessageWithRetry(message: String, onRetry: () -> Unit) {
    Column {
        Text(message, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.sync_retry)) }
    }
}
