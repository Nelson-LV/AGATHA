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
import com.hivend.agatha.domain.model.ConflictoSincronizacion
import com.hivend.agatha.domain.model.RegistroPendiente
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
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(NeutralBackground)) {
        AgathaHeader()
        ConnectivityBar(
            conectado = estado?.conectado ?: false,
            mensaje = if (estado?.conectado == true) {
                "Conectado · Sincronizado ${estado?.ultimaSincronizacionExitosa}"
            } else {
                "Sin conexión · ${estado?.pendientes?.size ?: 0} pendientes por sincronizar"
            },
        )

        val actual = estado
        if (actual != null) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                item { SitePill(sitio = "Güepsa – San José de Pare") }

                if (!actual.conectado) {
                    item { OfflineBanner() }
                }

                if (actual.pendientes.isNotEmpty()) {
                    item { PendingSyncCard(pendientes = actual.pendientes, onReintentar = viewModel::reintentar) }
                }

                actual.conflicto?.let { conflicto ->
                    item { ConflictCard(conflicto = conflicto, onEntendido = { viewModel.confirmarConflicto(conflicto.alertaId) }) }
                }

                item {
                    SyncFooter(
                        ultimaSincronizacion = actual.ultimaSincronizacionExitosa,
                        onSincronizarAhora = viewModel::sincronizarAhora,
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
private fun PendingSyncCard(pendientes: List<RegistroPendiente>, onReintentar: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralSurface, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text("Pendientes por sincronizar (${pendientes.size})", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
        pendientes.forEach { registro ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeutralSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(registro.titulo, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                    Text(registro.detalle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(text = "Pendiente", containerColor = StateInspectionContainer, contentColor = StateInspectionText)
                Text(
                    "↻ Reintentar",
                    color = AgathaBlue,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clickable { onReintentar(registro.id) }
                        .padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ConflictCard(conflicto: ConflictoSincronizacion, onEntendido: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(ConflictContainer, RoundedCornerShape(14.dp))
            .border(1.3.dp, ConflictBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text("⚠ ${conflicto.tituloConflicto}", color = ConflictText, style = MaterialTheme.typography.titleSmall)
        Text(conflicto.descripcion, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ConflictVersion("TU CAMBIO (LOCAL)", conflicto.cambioLocal, Modifier.weight(1f))
            ConflictVersion("CAMBIO DEL SERVIDOR", conflicto.cambioServidor, Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = onEntendido,
            border = BorderStroke(1.2.dp, ConflictBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ConflictText),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Entendido")
        }
    }
}

@Composable
private fun ConflictVersion(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(NeutralSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(etiqueta, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(valor, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SyncFooter(ultimaSincronizacion: String, onSincronizarAhora: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSincronizarAhora,
            colors = ButtonDefaults.buttonColors(containerColor = AgathaBlue),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("🔄 Sincronizar ahora")
        }
        Text(
            "Última sincronización correcta: $ultimaSincronizacion",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
