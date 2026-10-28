package com.hivend.agatha.ui.alertdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.ui.components.formatDuration
import com.hivend.agatha.ui.components.formatTime
import com.hivend.agatha.ui.components.levelColor
import com.hivend.agatha.ui.components.shortLabelRes
import com.hivend.agatha.ui.theme.AgathaDataTextStyle
import com.hivend.agatha.ui.theme.AgathaTheme
import java.time.Duration
import java.time.Instant

/** Un nodo de la línea: el nivel que tomó la alerta (o el cierre en verde) y cuándo. */
private data class TimelineNode(
    val level: AlertLevel,
    val at: Instant,
    val indicatorValue: Double?,
    val isCurrent: Boolean,
)

private val NODE_WIDTH = 88.dp
private val LINE_THICKNESS = 3.dp
private val DOT_SIZE = 16.dp
private val CURRENT_DOT_SIZE = 28.dp

/**
 * Línea de tiempo de evolución de la alerta (RN-05), como la sección "Línea de tiempo de
 * evolución del evento" de la web: un nodo por nivel con su hora y el valor del indicador, unidos
 * por una línea que toma el color del nivel de cada tramo. El nodo actual, si la alerta sigue en
 * curso, se resalta con el ícono de alerta; si ya terminó, se cierra con un nodo "Normal".
 * Va dentro de la tarjeta de información de la alerta.
 */
@Composable
internal fun LevelTimeline(alert: Alert, modifier: Modifier = Modifier) {
    if (alert.levelTimeline.isEmpty()) return
    val nodes = alert.levelTimeline.mapIndexed { index, change ->
        TimelineNode(
            level = change.level,
            at = change.at,
            indicatorValue = change.indicatorValue,
            isCurrent = alert.isOngoing && index == alert.levelTimeline.lastIndex,
        )
    } + listOfNotNull(
        alert.endedAt?.let { TimelineNode(AlertLevel.GREEN, it, indicatorValue = null, isCurrent = false) },
    )
    val accumulated = Duration.between(alert.startedAt, alert.endedAt ?: Instant.now()).coerceAtLeast(Duration.ZERO)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.alert_detail_timeline),
                color = AgathaTheme.colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.alert_detail_timeline_duration, formatDuration(accumulated)),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        val scroll = rememberScrollState()
        LaunchedEffect(nodes.size, scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Pocos nodos se reparten a lo ancho; muchos (reactivaciones) hacen scroll horizontal.
            val nodeWidth = maxOf(NODE_WIDTH, maxWidth / nodes.size)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            nodes.forEachIndexed { index, node ->
                TimelineNodeColumn(
                    node = node,
                    width = nodeWidth,
                    // El tramo antes del nodo tiene el color del nivel anterior; el de después, el del propio nodo.
                    lineBefore = nodes.getOrNull(index - 1)?.level?.levelColor(),
                    lineAfter = if (index < nodes.lastIndex) node.level.levelColor() else null,
                )
            }
            }
        }
    }
}

@Composable
private fun TimelineNodeColumn(node: TimelineNode, width: Dp, lineBefore: Color?, lineAfter: Color?) {
    val color = node.level.levelColor()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(width)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(CURRENT_DOT_SIZE)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f).height(LINE_THICKNESS).background(lineBefore ?: Color.Transparent))
                Box(Modifier.weight(1f).height(LINE_THICKNESS).background(lineAfter ?: Color.Transparent))
            }
            if (node.isCurrent) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(CURRENT_DOT_SIZE)
                        .background(color, CircleShape)
                        .border(3.dp, color.copy(alpha = 0.3f), CircleShape),
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = AgathaTheme.colors.surface, modifier = Modifier.size(14.dp))
                }
            } else {
                Box(
                    Modifier
                        .size(DOT_SIZE)
                        .background(color, CircleShape)
                        .border(3.dp, AgathaTheme.colors.surface, CircleShape),
                )
            }
        }
        Text(
            stringResource(node.level.shortLabelRes()) +
                if (node.isCurrent) " " + stringResource(R.string.alert_detail_timeline_current) else "",
            color = color,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(formatTime(node.at), color = AgathaTheme.colors.textPrimary, style = AgathaDataTextStyle, textAlign = TextAlign.Center)
        node.indicatorValue?.let {
            Text(
                stringResource(R.string.alert_detail_timeline_value, it),
                color = AgathaTheme.colors.textSecondary,
                style = AgathaDataTextStyle,
                textAlign = TextAlign.Center,
            )
        }
    }
}
