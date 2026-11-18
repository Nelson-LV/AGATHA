package com.hivend.agatha.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
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
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.shortLabelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Filtros del Historial de alertas: fechas (selector de rango) y nivel máximo, evento y estado con
 * selección múltiple, como en la web. Son chips de ancho natural que bajan de línea si no caben,
 * así que nunca hay scroll horizontal ni texto cortado.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryEventFilterControls(
    dateFrom: LocalDate?,
    dateThrough: LocalDate?,
    maxLevels: Set<AlertLevel>,
    alertEvents: Set<AlertEvent>,
    statuses: Set<ManagementStatus>,
    hasAdditionalFilters: Boolean,
    onDateRangeSelected: (LocalDate?, LocalDate?) -> Unit,
    onToggleMaxLevel: (AlertLevel) -> Unit,
    onToggleAlertEvent: (AlertEvent) -> Unit,
    onToggleStatus: (ManagementStatus) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = dateFrom != null || dateThrough != null,
            onClick = { showDatePicker = true },
            label = { Text(dateRangeLabel(dateFrom, dateThrough), maxLines = 1) },
            trailingIcon = { DropdownArrow() },
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        MultiSelectFilter(
            title = stringResource(R.string.history_filter_max_level),
            options = AlertSearchFilters.LEVEL_OPTIONS,
            selected = maxLevels,
            optionLabel = { stringResource(it.shortLabelRes()) },
            onToggle = onToggleMaxLevel,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        MultiSelectFilter(
            title = stringResource(R.string.history_filter_event),
            options = AlertSearchFilters.EVENT_OPTIONS,
            selected = alertEvents,
            optionLabel = { stringResource(it.labelRes()) },
            onToggle = onToggleAlertEvent,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        MultiSelectFilter(
            title = stringResource(R.string.history_filter_status),
            options = ManagementStatus.entries,
            selected = statuses,
            optionLabel = { stringResource(it.labelRes()) },
            onToggle = onToggleStatus,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        if (hasAdditionalFilters) {
            IconButton(onClick = onClearFilters, modifier = Modifier.align(Alignment.CenterVertically)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.history_clear_filters))
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

/**
 * Chip con menú de casillas: cada opción se marca o desmarca sin cerrar el menú, como la
 * multiselección de la web. El chip dice la opción elegida o cuántas hay.
 */
@Composable
private fun <T> MultiSelectFilter(
    title: String,
    options: List<T>,
    selected: Set<T>,
    optionLabel: @Composable (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected.size) {
        0 -> title
        1 -> optionLabel(selected.first())
        else -> stringResource(R.string.history_filter_count, title, selected.size)
    }

    Box(modifier = modifier) {
        FilterChip(
            selected = selected.isNotEmpty(),
            onClick = { expanded = true },
            label = { Text(label, maxLines = 1) },
            trailingIcon = { DropdownArrow() },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val checked = option in selected
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onToggle(option) },
                    leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
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
