package com.hivend.agatha.ui.history

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
import com.hivend.agatha.domain.model.EventoHistorial
import com.hivend.agatha.domain.model.TipoEvento
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.theme.AgathaBlue
import com.hivend.agatha.ui.theme.AlertGreen
import com.hivend.agatha.ui.theme.AlertRed
import com.hivend.agatha.ui.theme.NeutralBackground
import com.hivend.agatha.ui.theme.NeutralBorder
import com.hivend.agatha.ui.theme.NeutralSurface
import com.hivend.agatha.ui.theme.NeutralSurfaceVariant
import com.hivend.agatha.ui.theme.StateInspectionText
import com.hivend.agatha.ui.theme.TextPrimary
import com.hivend.agatha.ui.theme.TextSecondary

/**
 * Historial trazable de un dispositivo (HU-8.3, RNF-MOV-07). Corresponde al nodo 34:662 de
 * Figma. Es una de las 4 pantallas raíz con barra inferior.
 */
@Composable
fun DeviceHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val eventos by viewModel.eventos.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(NeutralBackground)) {
        AgathaHeader()
        ConnectivityBar(conectado = false, mensaje = "Sin conexión · mostrando datos locales")

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { SitePill(sitio = "Güepsa – San José de Pare") }
            item {
                Text(
                    "Historial de: Sensor ${viewModel.dispositivoId}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeutralSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeutralSurface, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                ) {
                    eventos.forEachIndexed { index, evento ->
                        HistoryRow(evento)
                        if (index != eventos.lastIndex) HorizontalDivider(color = NeutralBorder)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(evento: EventoHistorial) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(evento.tipo.emoji(), style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text("${evento.hora}  ${evento.tipo.etiqueta()}", color = evento.tipo.color(), style = MaterialTheme.typography.labelSmall)
            Text(evento.titulo, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(evento.detalle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            evento.etiquetaEstado?.let {
                StatusChip(
                    text = it,
                    containerColor = NeutralSurfaceVariant,
                    contentColor = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun TipoEvento.emoji(): String = when (this) {
    TipoEvento.ALERTA -> "🔴"
    TipoEvento.INSPECCION -> "🔧"
    TipoEvento.CLASIFICACION -> "🏷"
    TipoEvento.EVIDENCIA -> "📷"
    TipoEvento.OBSERVACION -> "📝"
    TipoEvento.MANTENIMIENTO -> "✅"
}

private fun TipoEvento.etiqueta(): String = when (this) {
    TipoEvento.ALERTA -> "ALERTA"
    TipoEvento.INSPECCION -> "INSPECCIÓN"
    TipoEvento.CLASIFICACION -> "CLASIFICACIÓN"
    TipoEvento.EVIDENCIA -> "EVIDENCIA"
    TipoEvento.OBSERVACION -> "OBSERVACIÓN"
    TipoEvento.MANTENIMIENTO -> "MANTENIMIENTO"
}

private fun TipoEvento.color(): androidx.compose.ui.graphics.Color = when (this) {
    TipoEvento.ALERTA -> AlertRed
    TipoEvento.INSPECCION -> AgathaBlue
    TipoEvento.CLASIFICACION -> StateInspectionText
    TipoEvento.EVIDENCIA -> AgathaBlue
    TipoEvento.OBSERVACION -> TextSecondary
    TipoEvento.MANTENIMIENTO -> AlertGreen
}
