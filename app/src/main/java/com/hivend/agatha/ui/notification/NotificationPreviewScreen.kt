package com.hivend.agatha.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.ui.theme.AlertRed
import com.hivend.agatha.ui.theme.TextPrimary
import com.hivend.agatha.ui.theme.TextSecondary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Simula la notificación push de una alerta roja recién generada (HE-04, HU-4.1/4.2),
 * corresponde al nodo 34:2 de Figma. Es el punto de entrada de la app: tocar la tarjeta
 * abre directamente [com.hivend.agatha.ui.alertdetail.AlertDetailScreen], igual que un push
 * real de Firebase Cloud Messaging haría con un `PendingIntent` (RNF-MOV-06).
 */
@Composable
fun NotificationPreviewScreen(
    onAbrirDetalle: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationPreviewViewModel = hiltViewModel(),
) {
    val alerta by viewModel.alertaDestacada.collectAsStateWithLifecycle()
    val ahora = remember { LocalDateTime.now() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12141A))
            .padding(top = 90.dp, start = 24.dp, end = 24.dp),
    ) {
        Text(
            ahora.format(DateTimeFormatter.ofPattern("H:mm")),
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
        )
        Text(
            ahora.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
        )

        alerta?.let { destacada ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .clickable { onAbrirDetalle(destacada.id) }
                    .padding(14.dp),
            ) {
                Box(Modifier.padding(top = 4.dp).size(10.dp).background(AlertRed, CircleShape))
                Column {
                    Text("AGATHA · ahora", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Alerta roja — Sensor ${destacada.sensorId}",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        "${destacada.pk} · ${destacada.sitio} · ${destacada.descripcion.substringAfter("— ").ifBlank { destacada.descripcion }}. Toca para ver el detalle.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
