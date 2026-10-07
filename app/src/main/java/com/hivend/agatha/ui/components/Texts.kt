package com.hivend.agatha.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.LocalizedText

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

/** Duración corta sin "hace": "5 min", "2 h". Para columnas de telemetría. */
@Composable
@ReadOnlyComposable
fun shortDuration(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    else -> stringResource(R.string.duration_hours, minutes / 60)
}
