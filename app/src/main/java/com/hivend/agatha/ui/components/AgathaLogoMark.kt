package com.hivend.agatha.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.ui.theme.AgathaTheme

/** El ícono de la app en miniatura, con las mismas capas del launcher, para los encabezados. */
@Composable
fun AgathaLogoMark(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(40.dp).clip(RoundedCornerShape(11.dp))) {
        // Decorativo: el nombre AGATHA va siempre al lado en el encabezado.
        Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

@Preview
@Composable
private fun AgathaLogoMarkPreview() {
    AgathaTheme {
        Box(Modifier.background(AgathaTheme.colors.headerBackground).padding(16.dp)) {
            AgathaLogoMark()
        }
    }
}
