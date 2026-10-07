package com.hivend.agatha.ui.inspection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import com.hivend.agatha.ui.theme.AgathaTheme
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
import com.hivend.agatha.domain.model.EventCategory
import com.hivend.agatha.domain.model.InspectionResult
import com.hivend.agatha.ui.components.BackTopBar
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.SitePill
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R
import com.hivend.agatha.ui.components.labelRes

/**
 * Formulario de inspección (HE-05, HU-5.1/5.2/5.3). Corresponde al nodo 34:134 de Figma.
 * Funciona íntegramente sin conexión: el banner superior lo deja explícito y el guardado
 * solo escribe en el repositorio local (ver InMemoryAlertaRepository — Sprint 3 lo respalda
 * con Room + WorkManager, sin cambiar esta pantalla).
 */
@Composable
fun InspectionFormScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InspectionFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaved(viewModel.alertId)
    }

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        BackTopBar(title = stringResource(R.string.inspection_top_bar_title), onBack = onBack)
        ConnectivityBar(connected = false, message = stringResource(R.string.connectivity_offline_saved_locally))

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { SitePill(site = uiState.alert?.site ?: stringResource(R.string.common_empty_value)) }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Column {
                        Text(stringResource(R.string.inspection_title), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.inspection_sensor_subtitle, uiState.alert?.sensorId ?: viewModel.alertId, uiState.alert?.pk ?: ""),
                            color = AgathaTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    RadioSection(
                        title = stringResource(R.string.inspection_result_title),
                        subtitle = stringResource(R.string.inspection_result_subtitle),
                        options = InspectionResult.entries,
                        optionLabel = { it.labelRes() },
                        selected = uiState.result,
                        onSelected = viewModel::onResultSelected,
                    )

                    HorizontalDivider(color = AgathaTheme.colors.border)

                    RadioSection(
                        title = stringResource(R.string.inspection_category_title),
                        subtitle = stringResource(R.string.inspection_category_subtitle),
                        options = EventCategory.entries,
                        optionLabel = { it.labelRes() },
                        selected = uiState.category,
                        onSelected = viewModel::onCategorySelected,
                    )

                    HorizontalDivider(color = AgathaTheme.colors.border)

                    Column {
                        Text(stringResource(R.string.inspection_observations_title), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = uiState.observations,
                            onValueChange = viewModel::onObservationsChange,
                            placeholder = { Text(stringResource(R.string.inspection_observations_placeholder)) },
                            modifier = Modifier.fillMaxWidth().height(90.dp).padding(top = 6.dp),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = viewModel::saveInspection,
                            enabled = uiState.canSave && !uiState.saving,
                            colors = ButtonDefaults.buttonColors(containerColor = AgathaTheme.colors.positive),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (!uiState.saving) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                            Text(stringResource(if (uiState.saving) R.string.common_saving else R.string.inspection_save))
                        }
                        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.common_cancel))
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
    optionLabel: (T) -> Int,
    selected: T?,
    onSelected: (T) -> Unit,
) {
    Column {
        Text(title, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) AgathaTheme.colors.brand.copy(alpha = 0.08f) else AgathaTheme.colors.surfaceVariant,
                            RoundedCornerShape(10.dp),
                        )
                        .border(1.dp, if (isSelected) AgathaTheme.colors.brand else AgathaTheme.colors.border, RoundedCornerShape(10.dp))
                        .clickable { onSelected(option) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelected(option) },
                        colors = RadioButtonDefaults.colors(selectedColor = AgathaTheme.colors.brand),
                    )
                    Text(stringResource(optionLabel(option)), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
