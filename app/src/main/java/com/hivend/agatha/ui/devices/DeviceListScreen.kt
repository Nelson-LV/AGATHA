package com.hivend.agatha.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.R
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.DeviceInfoCard
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.theme.AgathaTheme

/**
 * Pestaña Dispositivos: la misma ficha de nodo que muestra el mapa, pero como lista de todos
 * los nodos del tramo (incluidos los verdes, que no aparecen en Alertas).
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
                DeviceInfoCard(node = node, onViewHistory = { onViewHistory(node.id) })
            }
        }
    }
}
