package com.hivend.agatha.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.ui.components.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Filtros en una sola línea: tipo (menú) y fechas (selector de rango). Son chips de ancho
 * natural que bajan de línea si no caben, así que nunca hay scroll horizontal ni texto cortado.
 * Sin tipo elegido la pantalla lista las alertas; por eso "Alertas" no es una opción del menú.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryEventFilterControls(
    selectedEventType: EventType?,
    dateFrom: LocalDate?,
    dateThrough: LocalDate?,
    hasAdditionalFilters: Boolean,
    onSelectEventType: (EventType?) -> Unit,
    onDateRangeSelected: (LocalDate?, LocalDate?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EventTypeFilter(
            selectedEventType = selectedEventType,
            onSelectEventType = onSelectEventType,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        // La X va pegada al chip de fechas para que nunca baje sola a otra línea.
        Row(
            modifier = Modifier.align(Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = dateFrom != null || dateThrough != null,
                onClick = { showDatePicker = true },
                label = { Text(dateRangeLabel(dateFrom, dateThrough), maxLines = 1) },
                trailingIcon = { DropdownArrow() },
            )
            if (hasAdditionalFilters) {
                IconButton(onClick = onClearFilters) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.history_clear_filters))
                }
            }
        }
    }

    if (showDatePicker) {
        HistoryDateRangeDialog(
            initialFrom = dateFrom,
            initialThrough = dateThrough,
            onRangeSelected = onDateRangeSelected,
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun EventTypeFilter(
    selectedEventType: EventType?,
    onSelectEventType: (EventType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = selectedEventType != null,
            onClick = { expanded = true },
            label = { Text(stringResource(eventTypeFilterLabelRes(selectedEventType)), maxLines = 1) },
            trailingIcon = { DropdownArrow() },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EVENT_TYPE_FILTER_OPTIONS.forEach { eventType ->
                DropdownMenuItem(
                    text = { Text(stringResource(eventTypeFilterLabelRes(eventType))) },
                    onClick = {
                        // Tocar el tipo elegido lo quita y vuelve a la lista de alertas.
                        onSelectEventType(eventType.takeUnless { it == selectedEventType })
                        expanded = false
                    },
                    trailingIcon = if (eventType == selectedEventType) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun DropdownArrow() {
    Icon(
        Icons.Filled.ArrowDropDown,
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
    )
}

@Composable
private fun dateRangeLabel(from: LocalDate?, through: LocalDate?): String {
    val formatter = DateTimeFormatter.ofPattern(
        stringResource(R.string.history_filter_short_date_pattern),
        LocalConfiguration.current.locales[0],
    )
    return when {
        from != null && through != null && from == through -> formatter.format(from)
        from != null && through != null ->
            stringResource(R.string.history_filter_date_range, formatter.format(from), formatter.format(through))
        from != null -> stringResource(R.string.history_filter_from, formatter.format(from))
        through != null -> stringResource(R.string.history_filter_through, formatter.format(through))
        else -> stringResource(R.string.history_filter_dates)
    }
}

@StringRes
private fun eventTypeFilterLabelRes(eventType: EventType?): Int = when (eventType) {
    null -> R.string.history_filter_type
    EventType.CLASSIFICATION -> R.string.history_filter_classifications
    EventType.OBSERVATION -> R.string.history_filter_observations
    EventType.MAINTENANCE -> R.string.history_filter_maintenance
    else -> eventType.labelRes()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDateRangeDialog(
    initialFrom: LocalDate?,
    initialThrough: LocalDate?,
    onRangeSelected: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFrom?.toUtcMillis(),
        // El selector no admite una fecha final sin inicial.
        initialSelectedEndDateMillis = initialThrough?.takeIf { initialFrom != null }?.toUtcMillis(),
        yearRange = DatePickerDefaults.YearRange.first..today.year,
        selectableDates = HistorySelectableDates(today),
    )
    val dateFormatter = remember { DatePickerDefaults.dateFormatter() }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedStartDateMillis != null,
                onClick = {
                    onRangeSelected(
                        pickerState.selectedStartDateMillis?.toUtcLocalDate(),
                        pickerState.selectedEndDateMillis?.toUtcLocalDate(),
                    )
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.history_date_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    ) {
        DateRangePicker(
            state = pickerState,
            showModeToggle = false,
            // Los márgenes por defecto están pensados para pantalla completa, no para un diálogo.
            title = {
                DateRangePickerDefaults.DateRangePickerTitle(
                    displayMode = pickerState.displayMode,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            headline = {
                // Con el tamaño por defecto el rango no cabe en una línea en pantallas angostas.
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    DateRangePickerDefaults.DateRangePickerHeadline(
                        selectedStartDateMillis = pickerState.selectedStartDateMillis,
                        selectedEndDateMillis = pickerState.selectedEndDateMillis,
                        displayMode = pickerState.displayMode,
                        dateFormatter = dateFormatter,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class HistorySelectableDates(private val latestDate: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.toUtcLocalDate() <= latestDate

    override fun isSelectableYear(year: Int): Boolean = year <= latestDate.year
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val EVENT_TYPE_FILTER_OPTIONS = listOf(
    EventType.CLASSIFICATION,
    EventType.OBSERVATION,
    EventType.MAINTENANCE,
)
