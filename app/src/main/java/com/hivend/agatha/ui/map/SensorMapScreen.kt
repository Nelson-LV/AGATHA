package com.hivend.agatha.ui.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.DeviceInfoCard
import com.hivend.agatha.ui.components.nodeColor
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R

/**
 * Mapa de nodos (HE-08). Corresponde al nodo 34:530 de Figma. El lienzo es un
 * [Canvas] estático con las posiciones relativas de [NodoSensor] — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Mapa de nodos: mock-up vs. SDK real" para el plan de
 * reemplazo por `com.google.maps.android:maps-compose` cuando existan coordenadas reales.
 */
@Composable
fun SensorMapScreen(
    onViewHistory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        AgathaHeader()
        ConnectivityBar(connected = true, message = stringResource(R.string.connectivity_connected_recent))

        Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SitePill(site = uiState.nodes.firstOrNull()?.site ?: stringResource(R.string.common_empty_value))

            Box(modifier = Modifier.weight(1f)) {
                MapCanvas(
                    nodes = uiState.nodes,
                    selected = uiState.selectedNode,
                    onNodeClick = viewModel::selectNode,
                    modifier = Modifier.fillMaxSize(),
                )
                ZoomControls(modifier = Modifier.padding(10.dp))
                Legend(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
            }

            uiState.selectedNode?.let { node ->
                DeviceInfoCard(node = node, onViewHistory = { onViewHistory(node.id) })
            }
        }
    }
}

@Composable
private fun MapCanvas(
    nodes: List<SensorNode>,
    selected: SensorNode?,
    onNodeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Un Canvas de solo-dibujo no puede recibir toques por posición sin más trabajo; los
    // nodos son además clicables por su propia burbuja posicionada con offset, así que el
    // Canvas de abajo únicamente pinta el fondo y el resto se compone con Box + offset.
    Box(modifier = modifier.background(AgathaTheme.colors.mapCanvas, RoundedCornerShape(10.dp))) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            nodes.forEach { node ->
                val isSelected = node.id == selected?.id
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (node.normalizedX * widthPx).toInt(),
                                (node.normalizedY * heightPx).toInt(),
                            )
                        }
                        .size(if (isSelected) 22.dp else 14.dp)
                        .background(node.status.nodeColor(), CircleShape)
                        .then(if (isSelected) Modifier.border(2.dp, AgathaTheme.colors.nodeOutline, CircleShape) else Modifier)
                        .clickable { onNodeClick(node.id) },
                )
            }
        }
    }
}

@Composable
private fun ZoomControls(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AgathaTheme.colors.surface, RoundedCornerShape(6.dp)),
    ) {
        listOf(
            Icons.Filled.Add to R.string.map_zoom_in,
            Icons.Filled.Remove to R.string.map_zoom_out,
            Icons.Filled.MyLocation to R.string.map_recenter,
        ).forEach { (icon, descriptionRes) ->
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = stringResource(descriptionRes), tint = AgathaTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.background(AgathaTheme.colors.mapLabelContainer, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        LegendDot(stringResource(R.string.legend_connected), AgathaTheme.colors.nodeNormal)
        LegendDot(stringResource(R.string.legend_alert), AgathaTheme.colors.nodeAlert)
        LegendDot(stringResource(R.string.legend_critical), AgathaTheme.colors.nodeCritical)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(label, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

