package com.hivend.agatha.ui.alerts

import androidx.compose.runtime.ReadOnlyComposable
import com.hivend.agatha.ui.theme.AgathaTheme
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
import com.hivend.agatha.ui.components.relativeTimeAgo
import com.hivend.agatha.ui.components.resolve
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.hivend.agatha.R

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

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        AgathaHeader {
            StatusChip(text = stringResource(R.string.inbox_connected_chip), containerColor = AgathaTheme.colors.stateClosedContainer, contentColor = AgathaTheme.colors.stateClosed)
        }
        ConnectivityBar(connected = true, message = stringResource(R.string.connectivity_connected_recent))

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { SitePill(site = alerts.firstOrNull()?.site ?: stringResource(R.string.common_empty_value)) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.inbox_my_alerts), style = MaterialTheme.typography.titleMedium, color = AgathaTheme.colors.textPrimary)
                        StatusChip(
                            text = alerts.count { it.status != AlertStatus.CLOSED }.let { pluralStringResource(R.plurals.inbox_active_count, it, it) },
                            containerColor = AgathaTheme.colors.surfaceVariant,
                            contentColor = AgathaTheme.colors.textSecondary,
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
                    QuickNavButton(stringResource(R.string.inbox_nav_map), Icons.Filled.Map, Modifier.weight(1f), onNavigateToMap)
                    QuickNavButton(stringResource(R.string.inbox_nav_history), Icons.AutoMirrored.Filled.Assignment, Modifier.weight(1f), onNavigateToHistory)
                    QuickNavButton(stringResource(R.string.inbox_nav_sync), Icons.Filled.Sync, Modifier.weight(1f), onNavigateToSync)
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
                Text(
                    stringResource(R.string.inbox_alert_title, stringResource(R.string.alert_point, alert.pointNumber), alert.sensorId),
                    style = MaterialTheme.typography.labelLarge,
                    color = AgathaTheme.colors.textPrimary,
                )
                Text(alert.description.resolve(), style = MaterialTheme.typography.bodyMedium, color = AgathaTheme.colors.textSecondary)
                Text(relativeTimeAgo(alert.minutesAgo), style = MaterialTheme.typography.bodySmall, color = AgathaTheme.colors.textTertiary)
            }
            AlertStatusChip(status = alert.status)
        }
        if (showDivider) {
            HorizontalDivider(color = AgathaTheme.colors.border)
        }
    }
}

@Composable
private fun QuickNavButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(AgathaTheme.colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, AgathaTheme.colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AgathaTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AgathaTheme.colors.textPrimary, textAlign = TextAlign.Center)
    }
}

@Composable
@ReadOnlyComposable
private fun AlertLevel.color(): Color = when (this) {
    AlertLevel.RED -> AgathaTheme.colors.critical
    AlertLevel.YELLOW -> AgathaTheme.colors.warning
    AlertLevel.GREEN -> AgathaTheme.colors.positive
}
