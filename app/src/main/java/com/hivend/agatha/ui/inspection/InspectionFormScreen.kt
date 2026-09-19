package com.hivend.agatha.ui.inspection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.CategoriaEvento
import com.hivend.agatha.domain.model.ResultadoInspeccion
import com.hivend.agatha.ui.components.BackTopBar
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.SitePill
import com.hivend.agatha.ui.theme.AgathaBlue
import com.hivend.agatha.ui.theme.AlertGreen
import com.hivend.agatha.ui.theme.NeutralBackground
import com.hivend.agatha.ui.theme.NeutralBorder
import com.hivend.agatha.ui.theme.NeutralSurface
import com.hivend.agatha.ui.theme.NeutralSurfaceVariant
import com.hivend.agatha.ui.theme.TextPrimary
import com.hivend.agatha.ui.theme.TextSecondary

/**
 * Formulario de inspección (HE-05, HU-5.1/5.2/5.3). Corresponde al nodo 34:134 de Figma.
 * Funciona íntegramente sin conexión: el banner superior lo deja explícito y el guardado
 * solo escribe en el repositorio local (ver InMemoryAlertaRepository — Sprint 3 lo respalda
 * con Room + WorkManager, sin cambiar esta pantalla).
 */
@Composable
fun InspectionFormScreen(
    onBack: () -> Unit,
    onGuardado: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InspectionFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.guardadoConExito) {
        if (uiState.guardadoConExito) onGuardado(viewModel.alertaId)
    }

    Column(modifier = modifier.fillMaxSize().background(NeutralBackground)) {
        BackTopBar(title = "Registrar Inspección", onBack = onBack)
        ConnectivityBar(conectado = false, mensaje = "Sin conexión · se guarda localmente")

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { SitePill(sitio = uiState.alerta?.sitio ?: "—") }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeutralSurface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Column {
                        Text("Registrar inspección", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sensor ${uiState.alerta?.sensorId ?: viewModel.alertaId} · ${uiState.alerta?.pk ?: ""}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    RadioSection(
                        title = "Resultado de la inspección",
                        subtitle = "Selecciona el estado encontrado en el dispositivo o sensor.",
                        options = ResultadoInspeccion.entries,
                        optionLabel = { it.etiqueta },
                        selected = uiState.resultado,
                        onSelected = viewModel::onResultadoSeleccionado,
                    )

                    HorizontalDivider(color = NeutralBorder)

                    RadioSection(
                        title = "Clasificación del evento",
                        subtitle = "¿Qué originó realmente la alerta?",
                        options = CategoriaEvento.entries,
                        optionLabel = { it.etiqueta },
                        selected = uiState.categoria,
                        onSelected = viewModel::onCategoriaSeleccionada,
                    )

                    HorizontalDivider(color = NeutralBorder)

                    Column {
                        Text("Observaciones de campo", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = uiState.observaciones,
                            onValueChange = viewModel::onObservacionesChange,
                            placeholder = { Text("Escribe aquí situaciones no contempladas en las categorías (se guarda incluso sin conexión)…") },
                            modifier = Modifier.fillMaxWidth().height(90.dp).padding(top = 6.dp),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = viewModel::guardarInspeccion,
                            enabled = uiState.puedeGuardar && !uiState.guardando,
                            colors = ButtonDefaults.buttonColors(containerColor = AlertGreen),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (uiState.guardando) "Guardando…" else "✓ Guardar inspección")
                        }
                        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> RadioSection(
    title: String,
    subtitle: String,
    options: List<T>,
    optionLabel: (T) -> String,
    selected: T?,
    onSelected: (T) -> Unit,
) {
    Column {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) AgathaBlue.copy(alpha = 0.08f) else NeutralSurfaceVariant,
                            RoundedCornerShape(10.dp),
                        )
                        .border(1.dp, if (isSelected) AgathaBlue else NeutralBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelected(option) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelected(option) },
                        colors = RadioButtonDefaults.colors(selectedColor = AgathaBlue),
                    )
                    Text(optionLabel(option), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
