package com.hivend.agatha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hivend.agatha.ui.theme.AgathaTheme

private val CardShape = RoundedCornerShape(14.dp)
private val SeverityStripeWidth = 4.dp

/**
 * Superficie de tarjeta de toda la app, como las de la web: sombra suave, borde fino y esquinas
 * de 14 dp. Con [stripeColor] dibuja a la izquierda la franja de severidad de las tarjetas de
 * alerta; ponla antes de `clickable` y `padding` para que el ripple respete las esquinas.
 */
@Composable
fun Modifier.agathaCard(
    stripeColor: Color? = null,
    containerColor: Color = AgathaTheme.colors.surface,
    borderColor: Color = AgathaTheme.colors.cardBorder,
): Modifier {
    val shadowColor = AgathaTheme.colors.cardShadow
    return this
        .shadow(elevation = 3.dp, shape = CardShape, ambientColor = shadowColor, spotColor = shadowColor)
        .clip(CardShape)
        .background(containerColor)
        .border(1.dp, borderColor, CardShape)
        .then(
            if (stripeColor == null) {
                Modifier
            } else {
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(stripeColor, size = Size(SeverityStripeWidth.toPx(), size.height))
                }
            },
        )
}
