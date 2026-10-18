package com.hivend.agatha.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.DeviceInfoCard
import com.hivend.agatha.ui.components.SensorSections
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.theme.AgathaTheme

/**
 * Pestaña Dispositivos: la misma ficha de nodo que muestra el mapa, pero como lista de todos
 * los nodos del tramo (incluidos los verdes, que no aparecen en Alertas), cada una con las
 * secciones de sus sensores.
 */
@Composable
fun DeviceListScreen(
    onViewHistory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceListViewModel = hiltViewModel(),
) {
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        AgathaHeader()
        ConnectivityBar(connected = true, message = stringResource(R.string.connectivity_connected_recent))

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { SitePill(site = nodes.firstOrNull()?.site ?: stringResource(R.string.common_empty_value)) }
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.devices_title), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                    StatusChip(
                        text = pluralStringResource(R.plurals.devices_count, nodes.size, nodes.size),
                        containerColor = AgathaTheme.colors.surfaceVariant,
                        contentColor = AgathaTheme.colors.textSecondary,
                    )
                }
            }
            items(nodes, key = { it.id }) { node ->
                DeviceWithSensors(node = node, onViewHistory = { onViewHistory(node.id) })
            }
        }
    }
}

/**
 * Ficha del nodo con sus secciones de sensores plegadas debajo, con las gráficas de las
 * últimas 24 h que llegan de la API de tablero (antes estaban en el detalle de la alerta).
 */
@Composable
private fun DeviceWithSensors(node: SensorNode, onViewHistory: () -> Unit) {
    var showSensors by rememberSaveable(node.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DeviceInfoCard(node = node, onViewHistory = onViewHistory)
        Text(
            stringResource(if (showSensors) R.string.devices_sensors_hide else R.string.devices_sensors_show),
            color = AgathaTheme.colors.brand,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clickable { showSensors = !showSensors }
                .padding(horizontal = 4.dp, vertical = 4.dp),
        )
        if (showSensors) SensorSections(nodeId = node.id)
    }
}
