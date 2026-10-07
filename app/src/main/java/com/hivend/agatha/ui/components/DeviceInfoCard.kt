package com.hivend.agatha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.ui.theme.AgathaTheme

/**
 * Ficha de un nodo de sensor (estado, PK, tipo, batería y última comunicación). La comparten
 * el mapa (nodo seleccionado) y la pestaña Dispositivos (un elemento por nodo), para que
 * ambas pantallas muestren exactamente la misma información.
 */
@Composable
fun DeviceInfoCard(node: SensorNode, onViewHistory: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
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
                Box(Modifier.size(10.dp).background(node.status.nodeColor(), CircleShape))
                Text(stringResource(R.string.node_name, node.id), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)
            }
            StatusChip(
                text = stringResource(node.status.deviceStatusRes()),
                containerColor = node.status.nodeColor().copy(alpha = 0.14f),
                contentColor = node.status.nodeColor(),
            )
        }
        Text(stringResource(R.string.common_dot_separated, node.pk, node.site), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            InfoStat(stringResource(R.string.map_node_type), stringResource(node.sensorType.labelRes()), Modifier.weight(1f))
            InfoStat(stringResource(R.string.map_node_battery), stringResource(R.string.common_percent, node.batteryPercentage))
            InfoStat(stringResource(R.string.map_node_last_communication), relativeTimeAgo(node.lastCommunicationMinutesAgo))
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

/** Color del punto de un nodo en el mapa y en la lista de dispositivos. */
@Composable
@ReadOnlyComposable
fun AlertLevel.nodeColor(): Color = when (this) {
    AlertLevel.RED -> AgathaTheme.colors.nodeCritical
    AlertLevel.ORANGE -> AgathaTheme.colors.nodeAlert
    AlertLevel.GREEN -> AgathaTheme.colors.nodeNormal
}
