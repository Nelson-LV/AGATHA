package com.hivend.agatha.ui.alerts

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertStatus
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.AlertStatusChip
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.theme.AlertGreen
import com.hivend.agatha.ui.theme.AlertRed
import com.hivend.agatha.ui.theme.NeutralBackground
import com.hivend.agatha.ui.theme.NeutralBorder
import com.hivend.agatha.ui.theme.NeutralSurface
import com.hivend.agatha.ui.theme.NeutralSurfaceVariant
import com.hivend.agatha.ui.theme.StateClosedContainer
import com.hivend.agatha.ui.theme.StateClosedText
import com.hivend.agatha.ui.theme.StateInspectionText
import com.hivend.agatha.ui.theme.TextPrimary
import com.hivend.agatha.ui.theme.TextSecondary
import com.hivend.agatha.ui.theme.TextTertiary

/**
 * Bandeja de alertas (HE-04): pantalla raíz de la app y punto de entrada a la atención de
 * cada alerta en campo. Corresponde al nodo 2:2 del prototipo Figma.
 */
@Composable
fun AlertInboxScreen(
    onAlertClick: (String) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSync: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlertInboxViewModel = hiltViewModel(),
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(NeutralBackground)) {
        AgathaHeader {
            StatusChip(text = "CONECTADO", containerColor = StateClosedContainer, contentColor = StateClosedText)
        }
        ConnectivityBar(connected = true, message = "Conectado · Sincronizado hace 2 min")

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { SitePill(site = alerts.firstOrNull()?.site ?: "—") }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeutralSurface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Mis alertas", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        StatusChip(
                            text = "${alerts.count { it.status != AlertStatus.CLOSED }} activas",
                            containerColor = NeutralSurfaceVariant,
                            contentColor = TextSecondary,
                        )
                    }
                    alerts.forEachIndexed { index, alert ->
                        AlertRow(
                            alert = alert,
                            showDivider = index != alerts.lastIndex,
                            onClick = { onAlertClick(alert.id) },
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickNavButton("Mapa de nodos", Icons.Filled.Map, Modifier.weight(1f), onNavigateToMap)
                    QuickNavButton("Historial", Icons.AutoMirrored.Filled.Assignment, Modifier.weight(1f), onNavigateToHistory)
                    QuickNavButton("Sincronización", Icons.Filled.Sync, Modifier.weight(1f), onNavigateToSync)
                }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: Alert, showDivider: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .background(alert.level.color(), CircleShape),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text("${alert.point} · Sensor ${alert.sensorId}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Text(alert.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(alert.relativeTime, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            AlertStatusChip(status = alert.status)
        }
        if (showDivider) {
            HorizontalDivider(color = NeutralBorder)
        }
    }
}

@Composable
private fun QuickNavButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(NeutralSurface, RoundedCornerShape(12.dp))
            .border(1.dp, NeutralBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary, textAlign = TextAlign.Center)
    }
}

private fun AlertLevel.color(): Color = when (this) {
    AlertLevel.RED -> AlertRed
    AlertLevel.YELLOW -> StateInspectionText
    AlertLevel.GREEN -> AlertGreen
}
