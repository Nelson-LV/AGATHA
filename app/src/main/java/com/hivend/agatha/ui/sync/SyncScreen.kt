package com.hivend.agatha.ui.sync

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.SyncConflict
import com.hivend.agatha.domain.model.PendingRecord
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.components.relativeTimeAgo
import com.hivend.agatha.ui.components.resolve
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.hivend.agatha.R

/**
 * Sincronización offline-first (HE-07, RNF-MOV-02/03/04). Corresponde al nodo 34:398 de
 * Figma. Es una de las 4 pantallas raíz con barra inferior — ver
 * [com.hivend.agatha.core.navigation.AgathaDestination.BottomTab.Sync].
 */
@Composable
fun SyncScreen(
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        AgathaHeader()
        ConnectivityBar(
            connected = status?.connected ?: false,
            message = if (status?.connected == true) {
                stringResource(R.string.connectivity_connected_synced, relativeTimeAgo(status?.lastSuccessfulSyncMinutesAgo ?: 0))
            } else {
                val pendingCount = status?.pending?.size ?: 0
                pluralStringResource(R.plurals.connectivity_offline_pending, pendingCount, pendingCount)
            },
        )

        val current = status
        if (current != null) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                item { SitePill(site = stringResource(R.string.common_default_site)) }

                if (!current.connected) {
                    item { OfflineBanner() }
                }

                if (current.pending.isNotEmpty()) {
                    item { PendingSyncCard(pending = current.pending, onRetry = viewModel::retry) }
                }

                current.conflict?.let { conflict ->
                    item { ConflictCard(conflict = conflict, onUnderstood = { viewModel.confirmConflict(conflict.alertId) }) }
                }

                item {
                    SyncFooter(
                        lastSync = relativeTimeAgo(current.lastSuccessfulSyncMinutesAgo),
                        onSyncNow = viewModel::syncNow,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.offlineBannerContainer, RoundedCornerShape(12.dp))
            .border(1.3.dp, AgathaTheme.colors.offlineBannerBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AgathaTheme.colors.stateInspection, modifier = Modifier.size(22.dp))
        Column {
            Text(stringResource(R.string.sync_offline_title), color = AgathaTheme.colors.stateInspection, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.sync_offline_body),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PendingSyncCard(pending: List<PendingRecord>, onRetry: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.sync_pending_title, pending.size), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
        pending.forEach { record ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.title.resolve(), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.labelLarge)
                    Text(record.detail.resolve(), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(text = stringResource(R.string.sync_pending_chip), containerColor = AgathaTheme.colors.stateInspectionContainer, contentColor = AgathaTheme.colors.stateInspection)
                Text(
                    stringResource(R.string.sync_retry),
                    color = AgathaTheme.colors.brand,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clickable { onRetry(record.id) }
                        .padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ConflictCard(conflict: SyncConflict, onUnderstood: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.conflictContainer, RoundedCornerShape(14.dp))
            .border(1.3.dp, AgathaTheme.colors.conflictBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = AgathaTheme.colors.conflict, modifier = Modifier.size(18.dp))
            Text(conflict.conflictTitle.resolve(), color = AgathaTheme.colors.conflict, style = MaterialTheme.typography.titleSmall)
        }
        Text(conflict.description.resolve(), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ConflictVersion(stringResource(R.string.sync_conflict_local), conflict.localChange.resolve(), Modifier.weight(1f))
            ConflictVersion(stringResource(R.string.sync_conflict_server), conflict.serverChange.resolve(), Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = onUnderstood,
            border = BorderStroke(1.2.dp, AgathaTheme.colors.conflictBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgathaTheme.colors.conflict),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sync_understood))
        }
    }
}

@Composable
private fun ConflictVersion(label: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AgathaTheme.colors.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelSmall)
        Text(valor, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SyncFooter(lastSync: String, onSyncNow: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSyncNow,
            colors = ButtonDefaults.buttonColors(containerColor = AgathaTheme.colors.brand),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.sync_now), modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            stringResource(R.string.sync_last_success, lastSync),
            color = AgathaTheme.colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
