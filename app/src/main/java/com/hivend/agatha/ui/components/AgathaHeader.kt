package com.hivend.agatha.ui.components

import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R

/**
 * Encabezado oscuro de marca de las pantallas raíz, como la barra superior de la web. Llega hasta
 * la barra de estado: Scaffold no reserva ese espacio (ver AgathaApp).
 */
@Composable
fun AgathaHeader(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.headerBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        AgathaLogoMark()
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                stringResource(R.string.app_name),
                color = AgathaTheme.colors.onHeader,
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 3.sp),
            )
            Text(
                stringResource(R.string.header_tagline),
                color = AgathaTheme.colors.onHeaderMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
            .background(AgathaTheme.colors.headerBackground)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = AgathaTheme.colors.onHeader)
        }
        Text(
            title,
            color = AgathaTheme.colors.onHeader,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
