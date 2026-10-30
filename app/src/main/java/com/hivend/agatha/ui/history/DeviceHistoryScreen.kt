package com.hivend.agatha.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.resolve
import java.time.ZoneId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.text.NumberFormat
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DeviceHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }
    var selectedEvent by remember { mutableStateOf<HistoryEvent?>(null) }
    val eventSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AgathaHeader()
        Text(
            text = stringResource(R.string.tab_history),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (state.isOffline) {
            HistoryOfflineBanner(state.lastDataTimestamp)
        }
        HistoryFilters(
            nodes = state.nodes,
            selectedNodeId = state.selectedNodeId,
            selectedEventType = state.selectedEventType,
            dateFrom = state.dateFrom,
            dateThrough = state.dateThrough,
            onSelectNode = viewModel::selectNode,
            onSelectEventType = viewModel::setEventTypeFilter,
            onOpenFromPicker = { datePickerTarget = DatePickerTarget.FROM },
            onOpenThroughPicker = { datePickerTarget = DatePickerTarget.THROUGH },
            onClearFilters = viewModel::clearFilters,
        )

        when (datePickerTarget) {
            DatePickerTarget.FROM -> HistoryDatePickerDialog(
                initialDate = state.dateFrom,
                onDateSelected = { date -> viewModel.setDateRange(date, state.dateThrough) },
                onDismiss = { datePickerTarget = null },
            )
            DatePickerTarget.THROUGH -> HistoryDatePickerDialog(
                initialDate = state.dateThrough,
                onDateSelected = { date -> viewModel.setDateRange(state.dateFrom, date) },
                onDismiss = { datePickerTarget = null },
            )
            null -> Unit
        }

        when {
            state.isLoading && state.events.isEmpty() -> HistoryLoadingState(Modifier.weight(1f))
            state.events.isEmpty() -> HistoryEmptyState(Modifier.weight(1f))
            else -> {
                val nodesById = state.nodes.associateBy(SensorNode::id)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.events, key = HistoryEvent::id) { event ->
                        HistoryEventCard(event, nodesById[event.nodeId]) {
                            selectedEvent = event
                        }
                    }
                }
            }
        }

        selectedEvent?.let { event ->
            ModalBottomSheet(
                onDismissRequest = { selectedEvent = null },
                sheetState = eventSheetState,
            ) {
                HistoryEventDetails(event, state.nodes.firstOrNull { it.id == event.nodeId })
            }
        }
    }
}

@Composable
private fun HistoryOfflineBanner(lastDataTimestamp: Instant?) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val message = lastDataTimestamp?.let { timestamp ->
        stringResource(R.string.history_offline_data_as_of, formatTimestamp(timestamp, locale))
    } ?: stringResource(R.string.history_offline_no_timestamp)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun HistoryFilters(
    nodes: List<SensorNode>,
    selectedNodeId: String?,
    selectedEventType: EventType?,
    dateFrom: LocalDate?,
    dateThrough: LocalDate?,
    onSelectNode: (String?) -> Unit,
    onSelectEventType: (EventType?) -> Unit,
    onOpenFromPicker: () -> Unit,
    onOpenThroughPicker: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistoryNodeSelector(
                nodes = nodes,
                selectedNodeId = selectedNodeId,
                onSelectNode = onSelectNode,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClearFilters) {
                Text(stringResource(R.string.history_clear_filters), maxLines = 1)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedEventType == null,
                onClick = { onSelectEventType(null) },
                label = { Text(stringResource(R.string.history_filter_all_types)) },
            )
            listOf(
                EventType.ALERT,
                EventType.INSPECTION,
                EventType.MAINTENANCE,
                EventType.OBSERVATION,
                EventType.EVIDENCE,
            ).forEach { eventType ->
                FilterChip(
                    selected = selectedEventType == eventType,
                    onClick = { onSelectEventType(eventType) },
                    label = { Text(stringResource(eventType.labelRes())) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onOpenFromPicker, modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFrom?.let { date ->
                        stringResource(R.string.history_filter_from, formatDate(date, locale))
                    } ?: stringResource(R.string.history_filter_choose_from),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = onOpenThroughPicker, modifier = Modifier.weight(1f)) {
                Text(
                    text = dateThrough?.let { date ->
                        stringResource(R.string.history_filter_through, formatDate(date, locale))
                    } ?: stringResource(R.string.history_filter_choose_through),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HistoryNodeSelector(
    nodes: List<SensorNode>,
    selectedNodeId: String?,
    onSelectNode: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedNodeName = nodes.firstOrNull { it.id == selectedNodeId }?.name
        ?: selectedNodeId
        ?: stringResource(R.string.history_filter_all_nodes)

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = selectedNodeName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_filter_all_nodes)) },
                onClick = {
                    onSelectNode(null)
                    expanded = false
                },
            )
            nodes.forEach { node ->
                DropdownMenuItem(
                    text = { Text(node.name) },
                    onClick = {
                        onSelectNode(node.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HistoryDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    val earliestDate = today.minusDays(30)
    val selectedDate = initialDate?.coerceIn(earliestDate, today)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = earliestDate.year..today.year,
        selectableDates = HistorySelectableDates(earliestDate, today),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        onDateSelected(Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.history_date_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState, showModeToggle = false)
    }
}

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

@Composable
private fun HistoryLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.history_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.history_empty_30_days),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryEventCard(event: HistoryEvent, node: SensorNode?, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val timestamp = formatTimestamp(event.timestamp, locale)
    val nodeName = node?.name ?: event.nodeId
    val pk = node?.pk?.replace(Regex("^PK\\s*", RegexOption.IGNORE_CASE), "")
        ?: stringResource(R.string.map_value_not_available)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(event.type.color().copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = event.type.icon(),
                    contentDescription = null,
                    tint = event.type.color(),
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(event.type.labelRes()),
                    color = event.type.color(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = timestamp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = stringResource(R.string.history_event_node_pk, nodeName, pk),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = event.title.resolve(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = event.detail.resolve(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun HistoryEventDetails(event: HistoryEvent, node: SensorNode?) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pk = node?.pk?.replace(Regex("^PK\\s*", RegexOption.IGNORE_CASE), "")
        ?: stringResource(R.string.map_value_not_available)

    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = event.title.resolve(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        HistoryDetailRow(
            stringResource(R.string.history_detail_type),
            stringResource(event.type.labelRes()),
        )
        HistoryDetailRow(
            stringResource(R.string.history_detail_timestamp),
            formatTimestamp(event.timestamp, locale),
        )
        HistoryDetailRow(
            stringResource(R.string.history_detail_node),
            node?.name ?: event.nodeId,
        )
        HistoryDetailRow(stringResource(R.string.alert_detail_pk), pk)
        HistoryDetailRow(
            stringResource(R.string.history_detail_description),
            event.detail.resolve(),
        )

        event.telemetrySnapshot?.let { telemetry ->
            Text(
                text = stringResource(R.string.history_detail_telemetry),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            telemetry.acceleration?.let {
                HistoryDetailRow(
                    stringResource(R.string.history_detail_acceleration),
                    formatTelemetryValue(it, locale),
                )
            }
            telemetry.gyroscope?.let {
                HistoryDetailRow(
                    stringResource(R.string.history_detail_gyroscope),
                    formatTelemetryValue(it, locale),
                )
            }
            telemetry.acoustics?.let {
                HistoryDetailRow(
                    stringResource(R.string.history_detail_acoustics),
                    formatTelemetryValue(it, locale),
                )
            }
            telemetry.temperature?.let {
                HistoryDetailRow(
                    stringResource(R.string.history_detail_temperature),
                    formatTelemetryValue(it, locale),
                )
            }
            telemetry.humidity?.let {
                HistoryDetailRow(
                    stringResource(R.string.history_detail_humidity),
                    formatTelemetryValue(it, locale),
                )
            }
        }

        event.evidenceReference?.let { reference ->
            HistoryDetailRow(
                stringResource(R.string.history_detail_evidence_reference),
                reference,
            )
        }
        event.origin?.let { origin ->
            HistoryDetailRow(stringResource(R.string.history_detail_origin), origin)
        }
    }
}

@Composable
private fun HistoryDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatTimestamp(timestamp: Instant, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(ZoneId.of("America/Bogota"))
        .format(timestamp)

private fun formatTelemetryValue(value: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).format(value)

private fun EventType.icon(): ImageVector = when (this) {
    EventType.ALERT -> Icons.Filled.Warning
    EventType.INSPECTION -> Icons.Filled.Build
    EventType.CLASSIFICATION -> Icons.AutoMirrored.Filled.Label
    EventType.EVIDENCE -> Icons.Filled.PhotoCamera
    EventType.OBSERVATION -> Icons.Filled.EditNote
    EventType.MAINTENANCE -> Icons.Filled.CheckCircle
}

@Composable
@ReadOnlyComposable
private fun EventType.color(): Color = when (this) {
    EventType.ALERT -> MaterialTheme.colorScheme.error
    EventType.INSPECTION -> MaterialTheme.colorScheme.primary
    EventType.CLASSIFICATION -> MaterialTheme.colorScheme.tertiary
    EventType.EVIDENCE -> MaterialTheme.colorScheme.secondary
    EventType.OBSERVATION -> MaterialTheme.colorScheme.onSurfaceVariant
    EventType.MAINTENANCE -> MaterialTheme.colorScheme.primary
}
