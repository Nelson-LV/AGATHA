package com.hivend.agatha.ui.history

import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R
import com.hivend.agatha.ui.components.labelRes

/**
 * Historial trazable de un dispositivo (HU-8.3, RNF-MOV-07). Corresponde al nodo 34:662 de
 * Figma. Es una de las 4 pantallas raíz con barra inferior.
 */
@Composable
fun DeviceHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        AgathaHeader()
        ConnectivityBar(connected = false, message = stringResource(R.string.connectivity_offline_local_data))

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { SitePill(site = stringResource(R.string.common_default_site)) }
            item {
                Text(
                    stringResource(R.string.history_device, viewModel.deviceId),
                    color = AgathaTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                ) {
                    events.forEachIndexed { index, event ->
                        HistoryRow(event)
                        if (index != events.lastIndex) HorizontalDivider(color = AgathaTheme.colors.border)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(event: HistoryEvent) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(event.type.color().copy(alpha = 0.12f), CircleShape),
        ) {
            Icon(event.type.icon(), contentDescription = null, tint = event.type.color(), modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text("${event.time}  ${stringResource(event.type.labelRes())}", color = event.type.color(), style = MaterialTheme.typography.labelSmall)
            Text(event.title, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
            Text(event.detail, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            event.statusLabel?.let {
                StatusChip(
                    text = it,
                    containerColor = AgathaTheme.colors.surfaceVariant,
                    contentColor = AgathaTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun EventType.icon(): ImageVector = when (this) {
    EventType.ALERT -> Icons.Filled.Warning
    EventType.INSPECTION -> Icons.Filled.Build
    EventType.CLASSIFICATION -> Icons.AutoMirrored.Filled.Label
    EventType.EVIDENCE -> Icons.Filled.PhotoCamera
    EventType.OBSERVATION -> Icons.Filled.EditNote
    EventType.MAINTENANCE -> Icons.Filled.CheckCircle
}

@Composable
@ReadOnlyComposable
private fun EventType.color(): androidx.compose.ui.graphics.Color = when (this) {
    EventType.ALERT -> AgathaTheme.colors.critical
    EventType.INSPECTION -> AgathaTheme.colors.brand
    EventType.CLASSIFICATION -> AgathaTheme.colors.stateInspection
    EventType.EVIDENCE -> AgathaTheme.colors.brand
    EventType.OBSERVATION -> AgathaTheme.colors.textSecondary
    EventType.MAINTENANCE -> AgathaTheme.colors.positive
}
