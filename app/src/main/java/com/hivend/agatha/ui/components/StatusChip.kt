package com.hivend.agatha.ui.components

import androidx.compose.runtime.ReadOnlyComposable
import com.hivend.agatha.ui.theme.AgathaTheme
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
import androidx.compose.ui.res.stringResource

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

@Composable
@ReadOnlyComposable
private fun AlertStatus.statusColors(): Pair<Color, Color> = when (this) {
    AlertStatus.GENERATED -> AgathaTheme.colors.stateInspectionContainer to AgathaTheme.colors.stateInspection
    AlertStatus.RECEIVED -> AgathaTheme.colors.stateReceivedContainer to AgathaTheme.colors.stateReceived
    AlertStatus.IN_INSPECTION -> AgathaTheme.colors.stateInspectionContainer to AgathaTheme.colors.stateInspection
    AlertStatus.CLASSIFIED -> AgathaTheme.colors.stateClassifiedContainer to AgathaTheme.colors.stateClassified
    AlertStatus.CLOSED -> AgathaTheme.colors.stateClosedContainer to AgathaTheme.colors.stateClosed
}

/** Chip de estado de alerta (Recibida / En inspección / Clasificada / Cerrada). */
@Composable
fun AlertStatusChip(status: AlertStatus, modifier: Modifier = Modifier) {
    val (container, content) = status.statusColors()
    StatusChip(text = stringResource(status.labelRes()), containerColor = container, contentColor = content, modifier = modifier)
}
