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
import com.hivend.agatha.domain.model.EstadoAlerta
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

private fun EstadoAlerta.etiqueta() = when (this) {
    EstadoAlerta.GENERADA -> "Generada"
    EstadoAlerta.RECIBIDA -> "Recibida"
    EstadoAlerta.EN_INSPECCION -> "En inspección"
    EstadoAlerta.CLASIFICADA -> "Clasificada"
    EstadoAlerta.CERRADA -> "Cerrada"
}

private fun EstadoAlerta.colores(): Pair<Color, Color> = when (this) {
    EstadoAlerta.GENERADA -> StateInspectionContainer to StateInspectionText
    EstadoAlerta.RECIBIDA -> StateReceivedContainer to StateReceivedText
    EstadoAlerta.EN_INSPECCION -> StateInspectionContainer to StateInspectionText
    EstadoAlerta.CLASIFICADA -> StateClassifiedContainer to StateClassifiedText
    EstadoAlerta.CERRADA -> StateClosedContainer to StateClosedText
}

/** Chip de estado de alerta (Recibida / En inspección / Clasificada / Cerrada). */
@Composable
fun EstadoAlertaChip(estado: EstadoAlerta, modifier: Modifier = Modifier) {
    val (container, content) = estado.colores()
    StatusChip(text = estado.etiqueta(), containerColor = container, contentColor = content, modifier = modifier)
}
