package com.hivend.agatha.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hivend.agatha.R

/**
 * Escala tipográfica de AGATHA, pensada para lectura en campo (sol directo, guantes,
 * vehículo en movimiento). El prototipo de Figma medía el texto de cuerpo en 10,5 px y las
 * etiquetas en 8,5 px, por debajo del mínimo legible de Material 3; esta escala parte de
 * 14 sp para el cuerpo y nunca baja de 12 sp, y respeta el tamaño de fuente del sistema.
 *
 * Familias: las mismas del frontend web. Geist para toda la interfaz (`font-sans` en la
 * web) y JetBrains Mono para datos técnicos, horas y métricas (`font-data-mono`). Ambas son
 * OFL y van empaquetadas en `res/font` como fuentes variables, no como fuentes descargables:
 * la app opera sin conexión y no puede depender de Google Play Services para mostrar texto.
 * Licencias en docs/licencias-fuentes/.
 *
 * Jerarquía: los títulos usan SemiBold/Bold con tracking levemente negativo para que se
 * lean como bloque; las etiquetas (chips, encabezados de sección en mayúsculas) usan
 * tracking positivo para separarse del cuerpo sin necesitar emojis ni color extra.
 */
/** Una instancia por peso usado: la fuente variable se fija con el eje `wght`. */
@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int, vararg weights: FontWeight) = FontFamily(
    weights.map { weight ->
        Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)

private val AgathaFontFamily = variableFamily(
    R.font.geist,
    FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold,
)

/** Fuente de datos técnicos (valores de sensores, horas, identificadores), como en la web. */
val AgathaMonoFontFamily = variableFamily(R.font.jetbrains_mono, FontWeight.Normal, FontWeight.Medium)

/** Valor numérico destacado (métricas de telemetría): mono, tamaño de `titleSmall`. */
val AgathaDataTextStyle = TextStyle(
    fontFamily = AgathaMonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp,
)

val AgathaTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
).withFamily(AgathaFontFamily)

/** Los estilos que no se redefinen arriba (display*, headlineLarge…) también usan Geist. */
private fun Typography.withFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)
