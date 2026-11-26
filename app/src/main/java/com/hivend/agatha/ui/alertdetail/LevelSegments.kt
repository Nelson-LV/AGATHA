package com.hivend.agatha.ui.alertdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertMessage
import com.hivend.agatha.domain.model.LevelSegment
import com.hivend.agatha.domain.model.MessageKind
import com.hivend.agatha.ui.components.agathaCard
import com.hivend.agatha.ui.components.formatDateTime
import com.hivend.agatha.ui.components.formatTime
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.levelColor
import com.hivend.agatha.ui.components.shortLabelRes
import com.hivend.agatha.ui.theme.AgathaTheme

/** Niveles del filtro por color (guía §6.1); null = Todos. */
internal val SEGMENT_FILTER_OPTIONS: List<AlertLevel?> = listOf(null, AlertLevel.YELLOW, AlertLevel.ORANGE, AlertLevel.RED)

/**
 * Encabezado de la sección de mensajes: título y chips de filtro por color. El filtro es del
 * cliente: no pide nada al servidor.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LevelSegmentsHeader(selected: AlertLevel?, onSelect: (AlertLevel?) -> Unit, empty: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.alert_detail_messages_title), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
        if (empty) {
            Text(stringResource(R.string.alert_detail_messages_empty), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SEGMENT_FILTER_OPTIONS.forEach { level ->
                FilterChip(
                    selected = selected == level,
                    onClick = { onSelect(level) },
                    label = { Text(stringResource(level?.shortLabelRes() ?: R.string.alert_detail_messages_filter_all)) },
                    leadingIcon = level?.let { { Box(Modifier.size(10.dp).background(it.levelColor(), CircleShape)) } },
                )
            }
        }
    }
}

/**
 * Un tramo de color (guía §6.1): cabecera con el color, el rango "inicio – fin" ("ahora" si sigue
 * vigente) y cómo empezó (abrió, subió, bajó, se reactivó); debajo sus mensajes en orden. Si
 * [focusMessageId] es de este tramo, o el tramo es el vigente y [focusCurrent] es true, se trae
 * a la vista al aparecer (apertura desde un push).
 */
@Composable
internal fun LevelSegmentBlock(
    segment: LevelSegment,
    focusMessageId: Long? = null,
    focusCurrent: Boolean = false,
    onFocused: () -> Unit = {},
) {
    val color = segment.level?.levelColor() ?: AgathaTheme.colors.textTertiary
    val segmentRequester = remember { BringIntoViewRequester() }
    val focusSegment = focusCurrent && focusMessageId == null && segment.isCurrent
    if (focusSegment) {
        LaunchedEffect(Unit) {
            segmentRequester.bringIntoView()
            onFocused()
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(segmentRequester)
            .agathaCard(stripeColor = color)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        SegmentHeader(segment, color)
        segment.messages.forEach { message ->
            MessageRow(message, color, focused = message.id == focusMessageId, onFocused = onFocused)
        }
    }
}

@Composable
private fun SegmentHeader(segment: LevelSegment, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Text(
            stringResource(segment.level?.shortLabelRes() ?: R.string.alert_detail_segment_unknown_level),
            color = color,
            style = MaterialTheme.typography.titleSmall,
        )
        segment.openedBy.labelRes()?.let {
            Text(stringResource(it), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
    Text(
        stringResource(
            R.string.alert_detail_segment_range,
            formatDateTime(segment.startedAt),
            segment.endedAt?.let { formatDateTime(it) } ?: stringResource(R.string.alert_detail_segment_now),
        ),
        color = AgathaTheme.colors.textSecondary,
        style = MaterialTheme.typography.bodySmall,
    )
}

/**
 * Un mensaje: hora, título en negrita y cuerpo sin modificar (respeta los saltos de línea). El
 * estilo depende del tipo (guía §6.2): aviso destacado, resumen atenuado y plegable, fin como
 * separador de cierre y gestión con ícono de check.
 */
@Composable
private fun MessageRow(message: AlertMessage, segmentColor: Color, focused: Boolean, onFocused: () -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    if (focused) {
        LaunchedEffect(Unit) {
            requester.bringIntoView()
            onFocused()
        }
    }
    val modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requester)
    when (message.kind) {
        MessageKind.END -> EndMessage(message, modifier)
        MessageKind.SUMMARY -> SummaryMessage(message, modifier)
        else -> {
            val notification = message.kind == MessageKind.NOTIFICATION
            val background = if (notification) segmentColor.copy(alpha = 0.10f) else AgathaTheme.colors.surfaceVariant
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier.background(background, RoundedCornerShape(10.dp)).padding(10.dp),
            ) {
                when (message.kind) {
                    MessageKind.NOTIFICATION ->
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = segmentColor, modifier = Modifier.size(18.dp))
                    MessageKind.MANAGEMENT ->
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AgathaTheme.colors.brand, modifier = Modifier.size(18.dp))
                    else -> Unit
                }
                MessageTexts(message, dimmed = false)
            }
        }
    }
}

@Composable
private fun MessageTexts(message: AlertMessage, dimmed: Boolean, showBody: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        message.at?.let {
            Text(formatTime(it), color = AgathaTheme.colors.textTertiary, style = MaterialTheme.typography.labelSmall)
        }
        if (message.title.isNotBlank()) {
            Text(
                message.title,
                fontWeight = FontWeight.Bold,
                color = if (dimmed) AgathaTheme.colors.textSecondary else AgathaTheme.colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (showBody && message.body.isNotBlank()) {
            Text(
                message.body,
                color = if (dimmed) AgathaTheme.colors.textTertiary else AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Resumen periódico: atenuado y plegado; se despliega al tocarlo. */
@Composable
private fun SummaryMessage(message: AlertMessage, modifier: Modifier) {
    var expanded by rememberSaveable(message.id) { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.clickable { expanded = !expanded }.padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(Modifier.weight(1f)) { MessageTexts(message, dimmed = true, showBody = expanded) }
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = stringResource(if (expanded) R.string.alert_detail_message_collapse else R.string.alert_detail_message_expand),
            tint = AgathaTheme.colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Fin de la alerta: separador de cierre con el mensaje del backend. */
@Composable
private fun EndMessage(message: AlertMessage, modifier: Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.padding(top = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AgathaTheme.colors.divider))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Flag, contentDescription = null, tint = AgathaTheme.colors.positive, modifier = Modifier.size(18.dp))
            MessageTexts(message, dimmed = false)
        }
    }
}
