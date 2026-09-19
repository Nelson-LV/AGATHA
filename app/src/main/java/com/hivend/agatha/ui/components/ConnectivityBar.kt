package com.hivend.agatha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hivend.agatha.ui.theme.AgathaTheme
import com.hivend.agatha.ui.theme.ConnectedContainer
import com.hivend.agatha.ui.theme.ConnectedText
import com.hivend.agatha.ui.theme.OfflineContainer
import com.hivend.agatha.ui.theme.OfflineText

/**
 * Franja de conectividad presente en todas las pantallas raíz (HE-07): comunica en todo
 * momento si la app está trabajando en línea o localmente sin conexión.
 */
@Composable
fun ConnectivityBar(
    conectado: Boolean,
    mensaje: String,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (conectado) ConnectedContainer else OfflineContainer
    val contentColor = if (conectado) ConnectedText else OfflineText

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Dot(color = contentColor)
        Text(
            text = mensaje,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, CircleShape),
    )
}

@Preview
@Composable
private fun ConnectivityBarPreview() {
    AgathaTheme {
        Column {
            ConnectivityBar(conectado = true, mensaje = "Conectado · Sincronizado hace 2 min")
            ConnectivityBar(conectado = false, mensaje = "Sin conexión · 3 pendientes por sincronizar")
        }
    }
}
