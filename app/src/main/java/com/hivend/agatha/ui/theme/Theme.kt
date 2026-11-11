package com.hivend.agatha.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Tema de AGATHA: [AgathaColors] (roles propios: severidad, estado, conectividad, mapa)
 * más el [ColorScheme] de Material 3 derivado de la misma paleta, para que los componentes
 * de Material (barra inferior, campos, botones) y los nuestros nunca se contradigan.
 *
 * Dynamic color (Material You, per-device wallpaper theming) is intentionally NOT offered
 * here: the red/amber/green alert semantics (RNF3.2-equivalent for mobile — high legibility
 * for field operation) must stay consistent across every device, independent of the user's
 * wallpaper. See docs/ARQUITECTURA_Y_DISENO.md § "Por qué no usamos Dynamic Color".
 */
@Composable
fun AgathaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAgathaColors else LightAgathaColors

    CompositionLocalProvider(LocalAgathaColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(darkTheme),
            typography = AgathaTypography,
            shapes = AgathaShapes,
            content = content,
        )
    }
}

/** Acceso a los roles de color de AGATHA desde cualquier composable: `AgathaTheme.colors.critical`. */
object AgathaTheme {
    val colors: AgathaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAgathaColors.current
}

private fun AgathaColors.toMaterialColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brand,
        onPrimary = onBrand,
        primaryContainer = brandContainer,
        onPrimaryContainer = onBrandContainer,
        secondary = textSecondary,
        onSecondary = surface,
        error = critical,
        onError = onCritical,
        errorContainer = criticalSurface,
        onErrorContainer = critical,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant,
        outline = border,
        outlineVariant = divider,
    )
}
