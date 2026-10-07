package com.hivend.agatha.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Escala tipográfica de AGATHA, pensada para lectura en campo (sol directo, guantes,
 * vehículo en movimiento). El prototipo de Figma medía el texto de cuerpo en 10,5 px y las
 * etiquetas en 8,5 px, por debajo del mínimo legible de Material 3; esta escala parte de
 * 14 sp para el cuerpo y nunca baja de 12 sp, y respeta el tamaño de fuente del sistema.
 *
 * Familia: la del sistema (Roboto en Android), que es legible, cubre tildes y eñes, y no
 * depende de descargas — la app opera sin conexión. Si se adopta una fuente de marca
 * (p. ej. Inter), basta con cambiar [AgathaFontFamily] por `FontFamily(Font(R.font.…))`.
 *
 * Jerarquía: los títulos usan SemiBold/Bold con tracking levemente negativo para que se
 * lean como bloque; las etiquetas (chips, encabezados de sección en mayúsculas) usan
 * tracking positivo para separarse del cuerpo sin necesitar emojis ni color extra.
 */
private val AgathaFontFamily = FontFamily.Default

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
)
