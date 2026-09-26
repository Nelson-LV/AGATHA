package com.hivend.agatha.ui.alertdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertStatus
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.ui.components.BackTopBar
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.AlertStatusChip
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.theme.AgathaBlue
import com.hivend.agatha.ui.theme.AgathaBlueContainer
import com.hivend.agatha.ui.theme.AlertGreen
import com.hivend.agatha.ui.theme.AlertRed
import com.hivend.agatha.ui.theme.AlertRedBorder
import com.hivend.agatha.ui.theme.AlertRedSurface
import com.hivend.agatha.ui.theme.NeutralBackground
import com.hivend.agatha.ui.theme.NeutralBorder
import com.hivend.agatha.ui.theme.NeutralSurface
import com.hivend.agatha.ui.theme.NeutralSurfaceVariant
import com.hivend.agatha.ui.theme.MapCanvasBackground
import com.hivend.agatha.ui.theme.NodeAlert
import com.hivend.agatha.ui.theme.NodeCritical
import com.hivend.agatha.ui.theme.NodeNormal
import com.hivend.agatha.ui.theme.StateClosedText
import com.hivend.agatha.ui.theme.StateInspectionText
import com.hivend.agatha.ui.theme.TextOnMuted
import com.hivend.agatha.ui.theme.TextPrimary
import com.hivend.agatha.ui.theme.TextSecondary
import com.hivend.agatha.ui.theme.TextTertiary

/**
 * Detalle de alerta (HE-04). Corresponde a los nodos 1:4 ("En inspección") y 27:2
 * ("Recibida") de Figma: es la MISMA pantalla en ambos casos — sus acciones principales
 * cambian según [Alerta.estado] en lugar de duplicar la vista, evitando la pantalla-por-estado
 * que el prototipo dibuja como dos frames separados. Ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Una pantalla, un estado — no una pantalla por variante".
 */
@Composable
fun AlertDetailScreen(
    onBack: () -> Unit,
    onRegisterInspection: (String) -> Unit,
    onViewHistory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlertDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val alert = uiState.alert

    Column(modifier = modifier.fillMaxSize().background(NeutralBackground)) {
        BackTopBar(title = "Detalle de Alerta", onBack = onBack)
        ConnectivityBar(connected = true, message = "Conectado · Sincronizado hace 2 min")

        if (alert == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Alerta no encontrada", color = TextSecondary)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SitePill(site = alert.site) }
            item {
                AlertCard(
                    alert = alert,
                    onAdvance = { viewModel.advanceStatus(it) },
                    onRegisterInspection = { onRegisterInspection(alert.id) },
                )
            }
            item {
                FiltersRow(
                    status = alert.status,
                    onViewHistory = { onViewHistory(alert.sensorId) },
                )
            }
            item { AccelerationGraphCard() }
            item { RecentHistoryCard(events = uiState.recentHistory) }
            item { SensorAccordionList() }
        }
    }
}

@Composable
private fun AlertCard(alert: Alert, onAdvance: (AlertStatus) -> Unit, onRegisterInspection: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AlertRedSurface, RoundedCornerShape(14.dp))
            .border(1.5.dp, AlertRedBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            text = "${alert.level.label()} · Sensor ${alert.sensorId} · ${alert.pk}",
            color = AlertRed,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(alert.description, color = TextPrimary, style = MaterialTheme.typography.titleMedium)

        PrimaryActions(status = alert.status, onAdvance = onAdvance)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(AgathaBlueContainer, RoundedCornerShape(10.dp))
                .border(1.3.dp, AgathaBlueContainer, RoundedCornerShape(10.dp))
                .padding(10.dp),
        ) {
            Text("🔧", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Mantenimiento programado",
                color = AgathaBlue,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }

        TelemetryRow(alert)

        Text("📝  Siguiente paso", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeutralBorder, RoundedCornerShape(8.dp))
                .clickable(onClick = onRegisterInspection)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(alert.nextStep, color = AgathaBlue, style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun PrimaryActions(status: AlertStatus, onAdvance: (AlertStatus) -> Unit) {
    when (status) {
        AlertStatus.GENERATED, AlertStatus.RECEIVED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onAdvance(AlertStatus.IN_INSPECTION) },
                colors = ButtonDefaults.buttonColors(containerColor = AlertGreen),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Iniciar inspección", modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedButton(
                onClick = { onAdvance(AlertStatus.CLOSED) },
                border = BorderStroke(1.3.dp, AlertRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Marcar como falsa", modifier = Modifier.padding(start = 4.dp))
            }
        }
        AlertStatus.IN_INSPECTION -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onAdvance(AlertStatus.CLASSIFIED) },
                colors = ButtonDefaults.buttonColors(containerColor = AlertGreen),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Actualizar estado", modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedButton(
                onClick = { onAdvance(AlertStatus.CLOSED) },
                border = BorderStroke(1.3.dp, AlertRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Cerrar alerta", modifier = Modifier.padding(start = 4.dp))
            }
        }
        AlertStatus.CLASSIFIED, AlertStatus.CLOSED -> StatusChip(
            text = if (status == AlertStatus.CLOSED) "Sin acciones pendientes" else "Pendiente de cierre por operador",
            containerColor = NeutralSurfaceVariant,
            contentColor = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TelemetryRow(alert: Alert) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralSurfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, NeutralBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TelemetryStat("CONFIANZA", "${alert.telemetry.confidencePercentage}%")
            TelemetryStat("ACELERACIÓN", alert.telemetry.acceleration)
            TelemetryStat("HACE", alert.telemetry.relativeTime)
            TelemetryStat("BATERÍA", "${alert.telemetry.batteryPercentage}%")
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(6.dp).background(if (alert.telemetry.isRealData) AlertGreen else TextTertiary, CircleShape))
            Text(
                if (alert.telemetry.isRealData) "Dato real" else "Dato simulado",
                color = StateClosedText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TelemetryStat(label: String, value: String) {
    Column {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun FiltersRow(status: AlertStatus, onViewHistory: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralSurface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        StatusChip(
            text = "Estado: ${status.shortLabel()}",
            containerColor = NeutralSurfaceVariant,
            contentColor = TextOnMuted,
        )
        Text(
            "⬇  Ver historial de cambios de estado",
            color = AgathaBlue,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onViewHistory),
        )
    }
}

@Composable
private fun AccelerationGraphCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralSurface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text("🗺  GRÁFICA ACELERACIÓN 24H", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(MapCanvasBackground, RoundedCornerShape(10.dp)),
        ) {
            val points = listOf(
                Offset(size.width * 0.06f, size.height * 0.82f) to NodeNormal,
                Offset(size.width * 0.5f, size.height * 0.45f) to NodeAlert,
                Offset(size.width * 0.92f, size.height * 0.14f) to NodeCritical,
            )
            drawLine(
                color = AgathaBlue,
                start = points[0].first,
                end = points[1].first,
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            drawLine(
                color = AgathaBlue,
                start = points[1].first,
                end = points[2].first,
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            points.forEach { (offset, color) ->
                drawCircle(color = color, radius = 7f, center = offset)
                drawCircle(color = Color.White, radius = 7f, center = offset, style = Stroke(width = 2f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LegendDot("Normal", NodeNormal)
            LegendDot("Alerta", NodeAlert)
            LegendDot("Crítico", NodeCritical)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RecentHistoryCard(events: List<HistoryEvent>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralSurface, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text("📰  HISTORIAL DEL DISPOSITIVO", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        events.forEach { event ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("${event.time} · ${event.title}", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                Text(event.detail, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                event.statusLabel?.let {
                    StatusChip(
                        text = it,
                        containerColor = NeutralSurfaceVariant,
                        contentColor = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

private data class SensorInfo(val emoji: String, val title: String, val detail: String, val color: Color)

private val sensors = listOf(
    SensorInfo("📡", "Movimiento y orientación (detecta deslizamiento)", "Acelerómetro + giroscopio triaxial. Umbral de alerta: 3 cm de desplazamiento.", AlertRed),
    SensorInfo("🎵", "Acústica (detecta fuga de gas)", "Micrófono de contacto sobre la tubería. Detecta el patrón acústico de una fuga.", StateInspectionText),
    SensorInfo("🌡", "Condiciones ambientales", "Temperatura y humedad relativa alrededor del punto de monitoreo.", AgathaBlue),
    SensorInfo("🔋", "Energía del dispositivo", "Nivel de batería y estado del panel solar del nodo.", AlertGreen),
)

@Composable
private fun SensorAccordionList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sensors.forEach { sensor -> AccordionRow(sensor) }
    }
}

@Composable
private fun AccordionRow(sensor: SensorInfo) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralSurface, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sensor.emoji, color = sensor.color, style = MaterialTheme.typography.titleSmall)
                Text(sensor.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            }
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = TextSecondary)
        }
        if (expanded) {
            Text(
                sensor.detail,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun AlertLevel.label(): String = when (this) {
    AlertLevel.RED -> "ALERTA ROJA"
    AlertLevel.YELLOW -> "ALERTA AMARILLA"
    AlertLevel.GREEN -> "ALERTA VERDE"
}

private fun AlertStatus.shortLabel(): String = when (this) {
    AlertStatus.GENERATED -> "Generada"
    AlertStatus.RECEIVED -> "Recibida"
    AlertStatus.IN_INSPECTION -> "En inspección"
    AlertStatus.CLASSIFIED -> "Clasificada"
    AlertStatus.CLOSED -> "Cerrada"
}
