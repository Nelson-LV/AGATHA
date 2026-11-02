package com.hivend.agatha.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.ui.components.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedEventType == null,
                onClick = { onSelectEventType(null) },
                label = { Text(stringResource(R.string.history_filter_all_types)) },
            )
            FilterChip(
                selected = selectedEventType == EventType.ALERT,
                onClick = { onSelectEventType(EventType.ALERT) },
                label = { Text(stringResource(R.string.history_filter_alerts)) },
            )
            listOf(
                EventType.INSPECTION,
                EventType.EVIDENCE,
                EventType.MAINTENANCE,
                EventType.OBSERVATION,
                EventType.CLASSIFICATION,
            ).forEach { eventType ->
                FilterChip(
                    selected = selectedEventType == eventType,
                    onClick = { onSelectEventType(eventType) },
                    label = {
                        Text(
                            stringResource(
                                if (eventType == EventType.OBSERVATION) {
                                    R.string.history_filter_observations
                                } else {
                                    eventType.labelRes()
                                },
                            ),
                        )
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { datePickerTarget = DatePickerTarget.FROM },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = dateFrom?.let { stringResource(R.string.history_filter_from, formatDate(it, locale)) }
                        ?: stringResource(R.string.history_filter_choose_from),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = { datePickerTarget = DatePickerTarget.THROUGH },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = dateThrough?.let { stringResource(R.string.history_filter_through, formatDate(it, locale)) }
                        ?: stringResource(R.string.history_filter_choose_through),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClearFilters, enabled = hasAdditionalFilters) {
                Text(stringResource(R.string.history_clear_filters), maxLines = 1)
            }
        }
    }

    when (datePickerTarget) {
        DatePickerTarget.FROM -> HistoryDatePickerDialog(
            initialDate = dateFrom,
            onDateSelected = { onDateRangeSelected(it, dateThrough) },
            onDismiss = { datePickerTarget = null },
        )
        DatePickerTarget.THROUGH -> HistoryDatePickerDialog(
            initialDate = dateThrough,
            onDateSelected = { onDateRangeSelected(dateFrom, it) },
            onDismiss = { datePickerTarget = null },
        )
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    val earliestDate = today.minusDays(HISTORY_DATE_WINDOW_DAYS)
    val selectedDate = initialDate?.coerceIn(earliestDate, today)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = earliestDate.year..today.year,
        selectableDates = HistorySelectableDates(earliestDate, today),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
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
        DatePicker(state = pickerState, showModeToggle = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class HistorySelectableDates(
    private val earliestDate: LocalDate,
    private val latestDate: LocalDate,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return date in earliestDate..latestDate
    }

    override fun isSelectableYear(year: Int): Boolean = year in earliestDate.year..latestDate.year
}

private fun formatDate(date: LocalDate, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale).format(date)

private enum class DatePickerTarget {
    FROM,
    THROUGH,
}
