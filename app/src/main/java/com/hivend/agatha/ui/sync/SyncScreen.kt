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
                "Conectado · Sincronizado ${status?.lastSuccessfulSync}"
            } else {
                "Sin conexión · ${status?.pending?.size ?: 0} pendientes por sincronizar"
            },
        )

        val current = status
        if (current != null) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                item { SitePill(site = "Güepsa – San José de Pare") }

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
            Text("Trabajando sin conexión", color = StateInspectionText, style = MaterialTheme.typography.titleSmall)
            Text(
                "Tus registros (inspecciones, clasificaciones, observaciones y fotos) se guardan en este dispositivo y se sincronizarán automáticamente al recuperar señal.",
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
        Text("Pendientes por sincronizar (${pending.size})", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
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
                StatusChip(text = "Pendiente", containerColor = StateInspectionContainer, contentColor = StateInspectionText)
                Text(
                    "↻ Reintentar",
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
        Text("⚠ ${conflict.conflictTitle}", color = ConflictText, style = MaterialTheme.typography.titleSmall)
        Text(conflict.description, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ConflictVersion("TU CAMBIO (LOCAL)", conflict.localChange, Modifier.weight(1f))
            ConflictVersion("CAMBIO DEL SERVIDOR", conflict.serverChange, Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = onUnderstood,
            border = BorderStroke(1.2.dp, ConflictBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ConflictText),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Entendido")
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
            Text("🔄 Sincronizar ahora")
        }
        Text(
            "Última sincronización correcta: $lastSync",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
