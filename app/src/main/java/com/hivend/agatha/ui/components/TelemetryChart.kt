package com.hivend.agatha.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.DeviceTelemetry
import com.hivend.agatha.domain.model.TelemetrySeries
import com.hivend.agatha.ui.telemetry.TelemetryUiState
import com.hivend.agatha.ui.theme.AgathaTheme
import java.text.NumberFormat
import java.time.Instant
import kotlin.math.abs

/** Rango vertical de una gráfica, con un margen para que la línea no toque los bordes. */
internal data class ChartRange(val min: Double, val max: Double) {
    val span: Double get() = max - min

    companion object {
        /** null si ninguna serie tiene puntos. Un valor constante se centra con ±10 % (o ±1). */
        fun of(series: List<TelemetrySeries>): ChartRange? {
            val values = series.flatMap { s -> s.points.map { it.value } }
            if (values.isEmpty()) return null
            val low = values.min()
            val high = values.max()
            if (low == high) {
                val pad = if (low == 0.0) 1.0 else abs(low) * 0.1
                return ChartRange(low - pad, high + pad)
            }
            val pad = (high - low) * 0.08
            return ChartRange(low - pad, high + pad)
        }
    }
}

/**
 * Gráfica de líneas de una o dos variables en la ventana [from]–[to], con el estilo de la web
 * (cuadrícula punteada, serie azul). Los huecos sin datos quedan en blanco: cada tramo de
 * [TelemetrySeries.segments] se dibuja por separado y un punto aislado como un círculo.
 */
@Composable
fun TelemetryChart(
    series: List<TelemetrySeries>,
    from: Instant,
    to: Instant,
    modifier: Modifier = Modifier,
    title: String = seriesTitle(series),
) {
    val colors = AgathaTheme.colors
    val palette = listOf(colors.chartLine, colors.chartLineSecondary)
    val range = ChartRange.of(series)
    val unit = series.firstOrNull()?.unit?.takeUnless { it.isBlank() || it == "-" }
    val locale = LocalConfiguration.current.locales[0]
    val numbers = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = if (range != null && range.span < 1.0) 3 else 2
    }
    val valueWithUnit = stringResource(R.string.telemetry_value_unit)
    fun format(value: Double): String =
        unit?.let { u -> valueWithUnit.format(numbers.format(value), u) } ?: numbers.format(value)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, MaterialTheme.shapes.medium)
            .padding(12.dp),
    ) {
        Text(title, color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
        if (series.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                series.forEachIndexed { i, s -> ChartLegend(s.label(), palette[i % palette.size]) }
            }
        }
        if (range == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) {
                Text(stringResource(R.string.telemetry_no_data), color = colors.textTertiary, style = MaterialTheme.typography.bodySmall)
            }
            return@Column
        }
        val values = series.flatMap { s -> s.points.map { it.value } }
        val low = format(values.min())
        val high = format(values.max())
        Text(
            stringResource(R.string.telemetry_y_range, low, high),
            color = colors.textSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        val description = stringResource(R.string.telemetry_chart_description, title, low, high)
        val segmentsBySeries = series.map { it.segments() }
        val window = (to.toEpochMilli() - from.toEpochMilli()).coerceAtLeast(1).toFloat()
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .semantics { contentDescription = description },
        ) {
            val gridEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            for (i in 0..4) {
                val y = size.height * i / 4f
                drawLine(colors.chartGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = gridEffect)
            }
            for (i in 0..6) {
                val x = size.width * i / 6f
                drawLine(colors.chartGrid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx(), pathEffect = gridEffect)
            }
            fun position(at: Instant, value: Double) = Offset(
                x = ((at.toEpochMilli() - from.toEpochMilli()) / window).coerceIn(0f, 1f) * size.width,
                y = (size.height * (1 - (value - range.min) / range.span)).toFloat(),
            )
            segmentsBySeries.forEachIndexed { i, segments ->
                val color = palette[i % palette.size]
                segments.forEach { segment ->
                    if (segment.size == 1) {
                        drawCircle(color, radius = 2.5.dp.toPx(), center = position(segment[0].at, segment[0].value))
                    } else {
                        val path = Path()
                        segment.forEachIndexed { j, point ->
                            val p = position(point.at, point.value)
                            if (j == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                        }
                        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(formatDateTime(from), color = colors.textTertiary, style = MaterialTheme.typography.labelSmall)
            Text(formatDateTime(to), color = colors.textTertiary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

/** "Pitch / Roll": los nombres de las variables que comparten la gráfica. */
@Composable
private fun seriesTitle(series: List<TelemetrySeries>): String {
    val labels = series.map { it.label() }
    val separator = stringResource(R.string.telemetry_series_separator)
    return labels.reduceOrNull { acc, label -> separator.format(acc, label) }.orEmpty()
}

/** Nombre traducido de la variable; si la app no conoce la clave, el que da el backend. */
@Composable
fun TelemetrySeries.label(): String = telemetryVariableLabelRes(key)?.let { stringResource(it) } ?: name

/**
 * Cargando / error / contenido de una petición de telemetría. Los errores explican que las
 * gráficas solo se ven con conexión y ofrecen reintentar.
 */
@Composable
fun TelemetryStateContent(
    state: TelemetryUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (DeviceTelemetry) -> Unit,
) {
    when (state) {
        TelemetryUiState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = AgathaTheme.colors.brand)
            Text(stringResource(R.string.telemetry_loading), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        is TelemetryUiState.Failed -> Column(modifier = modifier.fillMaxWidth()) {
            Text(stringResource(state.error.messageRes()), color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.sync_retry)) }
        }
        is TelemetryUiState.Loaded -> content(state.telemetry)
    }
}
