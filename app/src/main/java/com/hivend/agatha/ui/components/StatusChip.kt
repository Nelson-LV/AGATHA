package com.hivend.agatha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hivend.agatha.domain.model.AlertStatus
import com.hivend.agatha.ui.theme.StateClassifiedContainer
import com.hivend.agatha.ui.theme.StateClassifiedText
import com.hivend.agatha.ui.theme.StateClosedContainer
import com.hivend.agatha.ui.theme.StateClosedText
import com.hivend.agatha.ui.theme.StateInspectionContainer
import com.hivend.agatha.ui.theme.StateInspectionText
import com.hivend.agatha.ui.theme.StateReceivedContainer
import com.hivend.agatha.ui.theme.StateReceivedText

/** Pastilla de texto genérica (contador, etiqueta de historial, tag "Pendiente", etc.). */
@Composable
fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = contentColor,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(containerColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private fun AlertStatus.label() = when (this) {
    AlertStatus.GENERATED -> "Generada"
    AlertStatus.RECEIVED -> "Recibida"
    AlertStatus.IN_INSPECTION -> "En inspección"
    AlertStatus.CLASSIFIED -> "Clasificada"
    AlertStatus.CLOSED -> "Cerrada"
}

private fun AlertStatus.statusColors(): Pair<Color, Color> = when (this) {
    AlertStatus.GENERATED -> StateInspectionContainer to StateInspectionText
    AlertStatus.RECEIVED -> StateReceivedContainer to StateReceivedText
    AlertStatus.IN_INSPECTION -> StateInspectionContainer to StateInspectionText
    AlertStatus.CLASSIFIED -> StateClassifiedContainer to StateClassifiedText
    AlertStatus.CLOSED -> StateClosedContainer to StateClosedText
}

/** Chip de estado de alerta (Recibida / En inspección / Clasificada / Cerrada). */
@Composable
fun AlertStatusChip(status: AlertStatus, modifier: Modifier = Modifier) {
    val (container, content) = status.statusColors()
    StatusChip(text = status.label(), containerColor = container, contentColor = content, modifier = modifier)
}
