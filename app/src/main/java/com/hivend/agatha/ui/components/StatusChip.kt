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
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.ManagementStatus
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
private fun ManagementStatus.statusColors(): Pair<Color, Color> = when (this) {
    ManagementStatus.UNCLASSIFIED -> AgathaTheme.colors.managementUnclassifiedContainer to AgathaTheme.colors.managementUnclassified
    ManagementStatus.CLASSIFIED_WITHOUT_TAG -> AgathaTheme.colors.managementUntaggedContainer to AgathaTheme.colors.managementUntagged
    ManagementStatus.CLASSIFIED_WITH_TAG -> AgathaTheme.colors.managementTaggedContainer to AgathaTheme.colors.managementTagged
}

/** Chip de estado de gestión de la web: Sin clasificar (gris), sin etiqueta (azul), con etiqueta (verde). */
@Composable
fun ManagementStatusChip(status: ManagementStatus, modifier: Modifier = Modifier) {
    val (container, content) = status.statusColors()
    StatusChip(text = stringResource(status.labelRes()), containerColor = container, contentColor = content, modifier = modifier)
}

@Composable
@ReadOnlyComposable
private fun AlertClassification.chipColors(): Pair<Color, Color> = when (this) {
    AlertClassification.CONFIRMED -> AgathaTheme.colors.classificationConfirmedContainer to AgathaTheme.colors.classificationConfirmed
    AlertClassification.FALSE_ALARM -> AgathaTheme.colors.classificationFalseAlarmContainer to AgathaTheme.colors.classificationFalseAlarm
}

/** Chip de clasificación de la web: Confirmada (rojo) o Falsa alerta (morado). */
@Composable
fun ClassificationChip(classification: AlertClassification, modifier: Modifier = Modifier) {
    val (container, content) = classification.chipColors()
    StatusChip(text = stringResource(classification.labelRes()), containerColor = container, contentColor = content, modifier = modifier)
}
