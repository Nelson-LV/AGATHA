package com.hivend.agatha.ui.sync

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
import com.hivend.agatha.ui.theme.AgathaBlue
import com.hivend.agatha.ui.theme.ConflictBorder
import com.hivend.agatha.ui.theme.ConflictContainer
import com.hivend.agatha.ui.theme.ConflictText
import com.hivend.agatha.ui.theme.NeutralBackground
import com.hivend.agatha.ui.theme.NeutralSurface
import com.hivend.agatha.ui.theme.NeutralSurfaceVariant
import com.hivend.agatha.ui.theme.OfflineBannerBorder
import com.hivend.agatha.ui.theme.OfflineBannerContainer
import com.hivend.agatha.ui.theme.StateInspectionContainer
import com.hivend.agatha.ui.theme.StateInspectionText
import com.hivend.agatha.ui.theme.TextPrimary
import com.hivend.agatha.ui.theme.TextSecondary
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

    Column(modifier = modifier.fillMaxSize().background(NeutralBackground)) {
        AgathaHeader()
        ConnectivityBar(
            connected = status?.connected ?: false,
            message = if (status?.connected == true) {
                stringResource(R.string.connectivity_connected_synced, status?.lastSuccessfulSync.orEmpty())
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
                        lastSync = current.lastSuccessfulSync,
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
            .background(OfflineBannerContainer, RoundedCornerShape(12.dp))
            .border(1.3.dp, OfflineBannerBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text("📴", style = MaterialTheme.typography.titleMedium)
        Column {
            Text(stringResource(R.string.sync_offline_title), color = StateInspectionText, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.sync_offline_body),
                color = TextSecondary,
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
            .background(NeutralSurface, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.sync_pending_title, pending.size), color = TextPrimary, style = MaterialTheme.typography.titleSmall)
        pending.forEach { record ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeutralSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.title, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                    Text(record.detail, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(text = stringResource(R.string.sync_pending_chip), containerColor = StateInspectionContainer, contentColor = StateInspectionText)
                Text(
                    stringResource(R.string.sync_retry),
                    color = AgathaBlue,
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
            .background(ConflictContainer, RoundedCornerShape(14.dp))
            .border(1.3.dp, ConflictBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.sync_conflict_title, conflict.conflictTitle), color = ConflictText, style = MaterialTheme.typography.titleSmall)
        Text(conflict.description, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ConflictVersion(stringResource(R.string.sync_conflict_local), conflict.localChange, Modifier.weight(1f))
            ConflictVersion(stringResource(R.string.sync_conflict_server), conflict.serverChange, Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = onUnderstood,
            border = BorderStroke(1.2.dp, ConflictBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ConflictText),
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
            .background(NeutralSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(valor, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SyncFooter(lastSync: String, onSyncNow: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSyncNow,
            colors = ButtonDefaults.buttonColors(containerColor = AgathaBlue),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sync_now))
        }
        Text(
            stringResource(R.string.sync_last_success, lastSync),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
