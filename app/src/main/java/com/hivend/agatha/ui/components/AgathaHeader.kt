package com.hivend.agatha.ui.components

import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R

/** Encabezado de marca mostrado en las 4 pantallas raíz (Alertas, Mapa, Historial, Sync). */
@Composable
fun AgathaHeader(
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        AgathaLogoMark()
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                stringResource(R.string.app_name),
                color = AgathaTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
            )
            Text(
                stringResource(R.string.header_tagline),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        trailing?.invoke(this)
    }
}

/** Barra superior de las pantallas apiladas (Detalle, Formulario, Evidencia) con botón de regreso. */
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = AgathaTheme.colors.textPrimary)
        }
        Text(
            title,
            color = AgathaTheme.colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
