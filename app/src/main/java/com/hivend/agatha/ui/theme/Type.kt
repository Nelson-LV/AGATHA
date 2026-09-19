package com.hivend.agatha.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale calibrated against the Figma prototype's measured text sizes (e.g. the
 * "AGATHA" wordmark at 17px bold, alert headlines at 14.5px bold, body rows at 10.5px).
 *
 * The prototype specifies Inter; until that variable font is added under res/font (a
 * one-line change: swap [FontFamily.Default] for `FontFamily(Font(R.font.inter_variable))`),
 * [FontFamily.Default] renders with the same geometric-grotesque proportions on every
 * Android version we target and keeps the initial APK smaller.
 */
private val AgathaFontFamily = FontFamily.Default

val AgathaTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
        lineHeight = 19.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = AgathaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 8.5.sp,
        lineHeight = 11.sp,
        letterSpacing = 0.3.sp,
    ),
)
