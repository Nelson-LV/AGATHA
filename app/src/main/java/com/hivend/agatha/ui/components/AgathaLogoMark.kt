package com.hivend.agatha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hivend.agatha.ui.theme.AgathaTheme

/** El glifo de tres barras del wordmark AGATHA, usado en el encabezado de cada pantalla. */
@Composable
fun AgathaLogoMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(AgathaTheme.colors.brand, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.width(4.dp).height(9.dp).background(AgathaTheme.colors.onBrand, RoundedCornerShape(1.dp)))
            Box(Modifier.padding(start = 2.dp).width(4.dp).height(16.dp).background(AgathaTheme.colors.onBrand, RoundedCornerShape(1.dp)))
            Box(Modifier.padding(start = 2.dp).width(4.dp).height(12.dp).background(AgathaTheme.colors.onBrand, RoundedCornerShape(1.dp)))
        }
    }
}

@Preview
@Composable
private fun AgathaLogoMarkPreview() {
    AgathaTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            AgathaLogoMark()
        }
    }
}
