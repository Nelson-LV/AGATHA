package com.hivend.agatha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hivend.agatha.ui.theme.NeutralBorder
import com.hivend.agatha.ui.theme.NeutralSurfaceVariant
import com.hivend.agatha.ui.theme.TextOnMuted

/** Recordatorio persistente del sitio/tramo que el usuario de campo está atendiendo. */
@Composable
fun SitePill(sitio: String, modifier: Modifier = Modifier) {
    Text(
        text = "Sitio: $sitio",
        color = TextOnMuted,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(NeutralSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, NeutralBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
