package com.hivend.agatha.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.model.Origin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Formateo de textos que llegan del dominio sin idioma: LocalizedText y tiempos relativos
 * expresados en minutos. Se resuelven al dibujar para seguir el idioma del dispositivo.
 */

/** Texto listo para mostrar en el idioma actual. */
@Composable
@ReadOnlyComposable
fun LocalizedText.resolve(): String = when (this) {
    is LocalizedText.Plain -> value
    is LocalizedText.Resource -> stringResource(id, *args.toTypedArray())
}

/** "justo ahora", "hace 5 min", "hace 2 h". */
@Composable
@ReadOnlyComposable
fun relativeTimeAgo(minutes: Int): String = when {
    minutes <= 0 -> stringResource(R.string.time_just_now)
    minutes < 60 -> stringResource(R.string.time_minutes_ago, minutes)
    else -> stringResource(R.string.time_hours_ago, minutes / 60)
}

/** Hora de Colombia (UTC-5), la que usa la web en todas sus fechas (Backlog 1.3 § 2). */
private val COLOMBIA: ZoneId = ZoneId.of("America/Bogota")

/** Fecha-hora con el formato de la web: dd/mm/aaaa HH:mm (24 h), hora de Colombia. */
@Composable
fun formatDateTime(instant: Instant): String {
    val pattern = stringResource(R.string.common_date_time_pattern)
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofPattern(pattern, locale).withZone(COLOMBIA).format(instant)
}

/** "Web" o "App móvil · CEL-01" (RN-10). */
@Composable
@ReadOnlyComposable
fun Origin.label(): String = when (this) {
    Origin.Web -> stringResource(labelRes())
    is Origin.MobileApp -> stringResource(labelRes(), deviceId)
}

/** Duración corta sin "hace": "5 min", "2 h". Para columnas de telemetría. */
@Composable
@ReadOnlyComposable
fun shortDuration(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    else -> stringResource(R.string.duration_hours, minutes / 60)
}
