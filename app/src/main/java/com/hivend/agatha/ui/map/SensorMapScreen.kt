package com.hivend.agatha.ui.map

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.hivend.agatha.domain.model.NivelAlerta
import com.hivend.agatha.domain.model.NodoSensor
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.theme.AgathaBlue
import com.hivend.agatha.ui.theme.MapCanvasBackground
import com.hivend.agatha.ui.theme.NeutralBackground
import com.hivend.agatha.ui.theme.NeutralSurface
import com.hivend.agatha.ui.theme.NeutralSurfaceVariant
import com.hivend.agatha.ui.theme.NodeAlert
import com.hivend.agatha.ui.theme.NodeCritical
import com.hivend.agatha.ui.theme.NodeNormal
import com.hivend.agatha.ui.theme.StateClosedContainer
import com.hivend.agatha.ui.theme.StateClosedText
import com.hivend.agatha.ui.theme.TextPrimary
import com.hivend.agatha.ui.theme.TextSecondary

/**
 * Mapa de nodos (HE-08). Corresponde al nodo 34:530 de Figma. El lienzo es un
 * [Canvas] estático con las posiciones relativas de [NodoSensor] — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Mapa de nodos: mock-up vs. SDK real" para el plan de
 * reemplazo por `com.google.maps.android:maps-compose` cuando existan coordenadas reales.
 */
@Composable
fun SensorMapScreen(
    onVerHistorial: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(NeutralBackground)) {
        AgathaHeader()
        ConnectivityBar(conectado = true, mensaje = "Conectado · Sincronizado hace 2 min")

        Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SitePill(sitio = uiState.nodos.firstOrNull()?.sitio ?: "—")

            Box(modifier = Modifier.weight(1f)) {
                MapCanvas(
                    nodos = uiState.nodos,
                    seleccionado = uiState.nodoSeleccionado,
                    onNodoClick = viewModel::seleccionarNodo,
                    modifier = Modifier.fillMaxSize(),
                )
                ZoomControls(modifier = Modifier.padding(10.dp))
                Legend(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
            }

            uiState.nodoSeleccionado?.let { nodo ->
                NodeInfoCard(nodo = nodo, onVerHistorial = { onVerHistorial(nodo.id) })
            }
        }
    }
}

@Composable
private fun MapCanvas(
    nodos: List<NodoSensor>,
    seleccionado: NodoSensor?,
    onNodoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Un Canvas de solo-dibujo no puede recibir toques por posición sin más trabajo; los
    // nodos son además clicables por su propia burbuja posicionada con offset, así que el
    // Canvas de abajo únicamente pinta el fondo y el resto se compone con Box + offset.
    Box(modifier = modifier.background(MapCanvasBackground, RoundedCornerShape(10.dp))) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            nodos.forEach { nodo ->
                val isSelected = nodo.id == seleccionado?.id
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (nodo.xNormalizado * widthPx).toInt(),
                                (nodo.yNormalizado * heightPx).toInt(),
                            )
                        }
                        .size(if (isSelected) 22.dp else 14.dp)
                        .background(nodo.estado.color(), CircleShape)
                        .then(if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                        .clickable { onNodoClick(nodo.id) },
                )
            }
        }
    }
}

@Composable
private fun ZoomControls(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(NeutralSurface, RoundedCornerShape(6.dp)),
    ) {
        listOf("+", "–", "◎").forEach {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Text(it, color = TextSecondary, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        LegendDot("Conectado", NodeNormal)
        LegendDot("Alerta", NodeAlert)
        LegendDot("Crítico", NodeCritical)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NodeInfoCard(nodo: NodoSensor, onVerHistorial: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralSurface, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(nodo.nombre, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            StatusChip(text = "Conectado", containerColor = StateClosedContainer, contentColor = StateClosedText)
        }
        Text("${nodo.pk} · ${nodo.sitio}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            InfoStat("TIPO", nodo.tipoSensor)
            InfoStat("BATERÍA", "${nodo.bateriaPorcentaje}%")
            InfoStat("ÚLT. COMUNICACIÓN", nodo.ultimaComunicacion)
        }
        Button(
            onClick = onVerHistorial,
            colors = ButtonDefaults.buttonColors(containerColor = AgathaBlue),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ver historial completo →")
        }
    }
}

@Composable
private fun InfoStat(label: String, value: String) {
    Column {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

private fun NivelAlerta.color(): Color = when (this) {
    NivelAlerta.ROJO -> NodeCritical
    NivelAlerta.AMARILLO -> NodeAlert
    NivelAlerta.VERDE -> NodeNormal
}
