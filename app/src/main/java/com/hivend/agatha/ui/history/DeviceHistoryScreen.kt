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
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.EventType
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
    val events by viewModel.events.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(NeutralBackground)) {
        AgathaHeader()
        ConnectivityBar(connected = false, message = "Sin conexión · mostrando datos locales")

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { SitePill(site = "Güepsa – San José de Pare") }
            item {
                Text(
                    "Historial de: Sensor ${viewModel.deviceId}",
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
                    events.forEachIndexed { index, event ->
                        HistoryRow(event)
                        if (index != events.lastIndex) HorizontalDivider(color = NeutralBorder)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(event: HistoryEvent) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(event.type.emoji(), style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text("${event.time}  ${event.type.label()}", color = event.type.color(), style = MaterialTheme.typography.labelSmall)
            Text(event.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(event.detail, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            event.statusLabel?.let {
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

private fun EventType.emoji(): String = when (this) {
    EventType.ALERT -> "🔴"
    EventType.INSPECTION -> "🔧"
    EventType.CLASSIFICATION -> "🏷"
    EventType.EVIDENCE -> "📷"
    EventType.OBSERVATION -> "📝"
    EventType.MAINTENANCE -> "✅"
}

private fun EventType.label(): String = when (this) {
    EventType.ALERT -> "ALERTA"
    EventType.INSPECTION -> "INSPECCIÓN"
    EventType.CLASSIFICATION -> "CLASIFICACIÓN"
    EventType.EVIDENCE -> "EVIDENCIA"
    EventType.OBSERVATION -> "OBSERVACIÓN"
    EventType.MAINTENANCE -> "MANTENIMIENTO"
}

private fun EventType.color(): androidx.compose.ui.graphics.Color = when (this) {
    EventType.ALERT -> AlertRed
    EventType.INSPECTION -> AgathaBlue
    EventType.CLASSIFICATION -> StateInspectionText
    EventType.EVIDENCE -> AgathaBlue
    EventType.OBSERVATION -> TextSecondary
    EventType.MAINTENANCE -> AlertGreen
}
