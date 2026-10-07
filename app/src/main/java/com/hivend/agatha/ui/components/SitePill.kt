package com.hivend.agatha.ui.components

import com.hivend.agatha.ui.theme.AgathaTheme
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
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R

/** Recordatorio persistente del sitio/tramo que el usuario de campo está atendiendo. */
@Composable
fun SitePill(site: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.site_pill, site),
        color = AgathaTheme.colors.textOnMuted,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, AgathaTheme.colors.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
