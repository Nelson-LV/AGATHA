package com.hivend.agatha.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Material 3 [androidx.compose.material3.ColorScheme]s for AGATHA.
 *
 * Dynamic color (Material You, per-device wallpaper theming) is intentionally NOT offered
 * here: the red/amber/green alert semantics (RNF3.2-equivalent for mobile — high legibility
 * for field operation) must stay consistent across every device, independent of the user's
 * wallpaper. See docs/ARQUITECTURA_Y_DISENO.md § "Por qué no usamos Dynamic Color".
 */
private val AgathaLightColorScheme = lightColorScheme(
    primary = AgathaBlue,
    onPrimary = Color.White,
    primaryContainer = AgathaBlueContainer,
    onPrimaryContainer = AgathaBlue,

    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedSurface,
    onErrorContainer = AlertRed,

    background = NeutralBackground,
    onBackground = TextPrimary,

    surface = NeutralSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = NeutralBorder,
    outlineVariant = NeutralDivider,
)

private val AgathaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7EA8F5),
    onPrimary = Color(0xFF0B2A6B),
    primaryContainer = Color(0xFF17325C),
    onPrimaryContainer = Color(0xFFD6E4FF),

    error = Color(0xFFF08787),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF4A1414),
    onErrorContainer = Color(0xFFFFDAD8),

    background = Color(0xFF121316),
    onBackground = Color(0xFFE4E5E8),

    surface = Color(0xFF1B1C1F),
    onSurface = Color(0xFFE4E5E8),
    surfaceVariant = Color(0xFF232428),
    onSurfaceVariant = Color(0xFFA6ABB2),

    outline = Color(0xFF3A3B40),
    outlineVariant = Color(0xFF2A2B2F),
)

@Composable
fun AgathaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AgathaDarkColorScheme else AgathaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgathaTypography,
        shapes = AgathaShapes,
        content = content,
    )
}
