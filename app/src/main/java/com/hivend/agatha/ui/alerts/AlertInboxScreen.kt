package com.hivend.agatha.ui.alerts

import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.ui.components.levelColor
import com.hivend.agatha.ui.components.agathaCard
import androidx.compose.runtime.ReadOnlyComposable
import com.hivend.agatha.ui.theme.AgathaTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Duration
import java.time.Instant
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.LocalOnline
import com.hivend.agatha.ui.components.ManagementStatusChip
import com.hivend.agatha.ui.components.NotificationPermissionNotice
import com.hivend.agatha.ui.components.relativeTimeAgo
import com.hivend.agatha.ui.components.resolve
import com.hivend.agatha.ui.components.toUiMessage
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.components.formatDateTime
import com.hivend.agatha.ui.components.LevelChip
import com.hivend.agatha.ui.components.labelRes
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val alerts = state.alerts
    val failure = state.failure
    val lastSyncedAt = state.lastSyncedAt

    // Se descarga al abrir la pantalla y cada vez que se vuelve a ella (p. ej. desde un detalle).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        val online = LocalOnline.current
        AgathaHeader()
        ConnectivityBar(
            connected = online && failure == null,
            message = when {
                !online || failure != null -> stringResource(R.string.connectivity_offline_local_data)
                lastSyncedAt == null -> stringResource(R.string.connectivity_syncing)
                else -> stringResource(
                    R.string.connectivity_connected_synced,
                    relativeTimeAgo(Duration.between(lastSyncedAt, Instant.now()).toMinutes().toInt()),
                )
            },
        )
        if (state.refreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AgathaTheme.colors.brand)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { NotificationPermissionNotice() }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.inbox_my_alerts), style = MaterialTheme.typography.titleLarge, color = AgathaTheme.colors.textPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusChip(
                                text = alerts.count { it.isOngoing }.let { pluralStringResource(R.plurals.inbox_active_count, it, it) },
                                containerColor = AgathaTheme.colors.noticeContainer,
                                contentColor = AgathaTheme.colors.textOnMuted,
                            )
                            IconButton(onClick = viewModel::refresh, enabled = !state.refreshing) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.alert_list_refresh),
                                    tint = AgathaTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                    if (alerts.isEmpty() && failure != null) {
                        Text(
                            failure.toUiMessage().resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgathaTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.common_retry)) }
                    } else if (alerts.isEmpty() && lastSyncedAt != null) {
                        Text(
                            stringResource(R.string.inbox_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgathaTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            items(alerts, key = { it.id }) { alert ->
                AlertCard(alert = alert, onClick = { onAlertClick(alert.id) })
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
private fun AlertCard(alert: Alert, onClick: () -> Unit) {
    // Una alerta roja sin clasificar se tiñe, como la tarjeta "Nueva" de la web: es lo primero que debe verse.
    val highlighted = alert.level == AlertLevel.RED && alert.managementStatus == ManagementStatus.UNCLASSIFIED
    // Como la tarjeta de alerta de la web: franja de nivel, evento, inicio y "Disp. · PK · Municipio".
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .agathaCard(
                stripeColor = alert.level.levelColor(),
                containerColor = if (highlighted) AgathaTheme.colors.criticalSurface else AgathaTheme.colors.surface,
                borderColor = if (highlighted) AgathaTheme.colors.criticalBorder else AgathaTheme.colors.cardBorder,
            )
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LevelChip(alert.level)
            Text(
                stringResource(alert.event.labelRes()),
                style = MaterialTheme.typography.titleSmall,
                color = AgathaTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(alert.id, style = MaterialTheme.typography.labelMedium, color = AgathaTheme.colors.textSecondary)
        }
        Text(
            stringResource(R.string.alert_list_location, alert.sensorId, alert.pk),
            style = MaterialTheme.typography.bodyMedium,
            color = AgathaTheme.colors.textSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                formatDateTime(alert.startedAt),
                style = MaterialTheme.typography.bodySmall,
                color = AgathaTheme.colors.textTertiary,
                modifier = Modifier.weight(1f),
            )
            ManagementStatusChip(status = alert.managementStatus)
        }
    }
}

@Composable
private fun QuickNavButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .agathaCard()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).background(AgathaTheme.colors.brandContainer, CircleShape),
        ) {
            Icon(icon, contentDescription = null, tint = AgathaTheme.colors.onBrandContainer, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = AgathaTheme.colors.textPrimary, textAlign = TextAlign.Center)
    }
}
