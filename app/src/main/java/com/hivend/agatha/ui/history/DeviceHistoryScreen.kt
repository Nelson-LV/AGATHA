package com.hivend.agatha.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertHistoryPhase
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.ManagementStatusChip
import com.hivend.agatha.ui.components.StatusChip
import com.hivend.agatha.ui.components.formatDateTime
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.levelColor
import com.hivend.agatha.ui.components.resolve
import com.hivend.agatha.ui.theme.AgathaTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@Composable
fun DeviceHistoryScreen(
    onAlertClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.listScrollIndex,
        initialFirstVisibleItemScrollOffset = state.listScrollOffset,
    )
    var restoredListPosition by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading, state.events.size, state.listScrollIndex, state.listScrollOffset) {
        if (!restoredListPosition && !state.isLoading) {
            if (state.events.isNotEmpty()) {
                listState.scrollToItem(
                    index = state.listScrollIndex.coerceAtMost(state.events.lastIndex),
                    scrollOffset = state.listScrollOffset,
                )
            }
            restoredListPosition = true
        }
    }
    LaunchedEffect(listState, state.isLoading, restoredListPosition) {
        if (state.isLoading || !restoredListPosition) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .drop(1)
            .collect { (index, offset) -> viewModel.updateListScrollPosition(index, offset) }
    }
    val connectivityMessage = when {
        !state.isConnected -> state.lastSyncedAt?.let { lastSync ->
            stringResource(R.string.history_offline_data_as_of, formatDateTime(lastSync))
        } ?: stringResource(R.string.history_offline_no_timestamp)
        state.isRefreshing -> stringResource(R.string.connectivity_syncing)
        state.failure != null -> failureMessage(state.failure)
        else -> state.lastSyncedAt?.let { lastSync ->
            stringResource(R.string.connectivity_connected_synced, formatDateTime(lastSync))
        } ?: stringResource(R.string.connectivity_connected_live)
    }

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        AgathaHeader()
        ConnectivityBar(
            connected = state.isConnected && state.failure == null,
            message = connectivityMessage,
        )
        HistorySelectors(
            state = state,
            onSiteSelected = viewModel::selectSite,
            onSensorSelected = viewModel::selectSensor,
        )
        HistoryEventFilterControls(
            selectedEventType = state.selectedEventType,
            dateFrom = state.dateFrom,
            dateThrough = state.dateThrough,
            hasAdditionalFilters = state.hasAdditionalFilters,
            onSelectEventType = viewModel::setEventTypeFilter,
            onDateRangeSelected = viewModel::setDateRange,
            onClearFilters = viewModel::clearFilters,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        when {
            state.isLoading && state.events.isEmpty() -> HistoryLoadingState(Modifier.weight(1f))
            state.events.isEmpty() && state.hasAdditionalFilters -> HistoryEmptyState(
                messageRes = R.string.history_no_filter_results,
                modifier = Modifier.weight(1f),
            )
            state.events.isEmpty() && state.failure != null -> HistoryFailureState(
                failure = state.failure,
                onRetry = viewModel::refresh,
                modifier = Modifier.weight(1f),
            )
            state.events.isEmpty() -> HistoryEmptyState(modifier = Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.events, key = HistoryEvent::id) { event ->
                    when (event.type) {
                        EventType.ALERT -> HistoryAlertEventRow(event, onAlertClick)
                        EventType.INSPECTION,
                        EventType.EVIDENCE,
                        EventType.MAINTENANCE,
                        EventType.OBSERVATION,
                        EventType.CLASSIFICATION -> HistoryTypedEventRow(event, onAlertClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySelectors(
    state: HistoryState,
    onSiteSelected: (String) -> Unit,
    onSensorSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectorDropdown(
            label = stringResource(R.string.history_site_selector),
            selectedLabel = state.selectedSite?.let { stringResource(it.nameRes) }
                ?: stringResource(R.string.history_site_unknown),
            options = state.sites.map { site -> site.id to stringResource(site.nameRes) },
            onSelected = onSiteSelected,
        )
        SelectorDropdown(
            label = stringResource(R.string.history_sensor_selector),
            selectedLabel = state.selectedSensor?.label ?: stringResource(R.string.history_no_sensor_selected),
            options = state.sensors.map { sensor -> sensor.id to sensor.label },
            onSelected = onSensorSelected,
            enabled = state.sensors.isNotEmpty(),
        )
    }
}

@Composable
private fun SelectorDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun HistoryAlertEventRow(
    event: HistoryEvent,
    onAlertClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val level = event.alertLevel ?: AlertLevel.GREEN
    val levelColor = level.levelColor()

    Surface(
        modifier = modifier.fillMaxWidth().then(event.alertClickModifier(onAlertClick)),
        shape = RoundedCornerShape(14.dp),
        color = AgathaTheme.colors.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(levelColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = levelColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_alert_timestamp, formatDateTime(event.timestamp)),
                    color = AgathaTheme.colors.critical,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = event.title.resolve(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = event.detail.resolve(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (event.alertPhase == AlertHistoryPhase.GENERATED) {
                    ManagementStatusChip(
                        status = event.alertManagementStatus ?: ManagementStatus.UNCLASSIFIED,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    event.alertClassification?.let { classification ->
                        Text(
                            text = stringResource(classification.labelRes()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    event.alertTagLabel?.let { tag ->
                        Text(
                            text = tag.resolve(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTypedEventRow(
    event: HistoryEvent,
    onAlertClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val eventColor = event.type.color()
    Surface(
        modifier = modifier.fillMaxWidth().then(event.alertClickModifier(onAlertClick)),
        shape = RoundedCornerShape(14.dp),
        color = AgathaTheme.colors.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(eventColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = event.type.icon(),
                    contentDescription = null,
                    tint = eventColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.history_event_timestamp,
                        formatDateTime(event.timestamp),
                        stringResource(event.type.labelRes()),
                    ),
                    color = eventColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = event.title.resolve(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = event.resolvedDetail(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
                event.statusLabel?.let { status ->
                    StatusChip(
                        text = status.resolve(),
                        containerColor = AgathaTheme.colors.stateClosedContainer,
                        contentColor = AgathaTheme.colors.stateClosed,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEvent.alertClickModifier(onAlertClick: (String) -> Unit): Modifier {
    val alertId = alertIdForNavigation() ?: return Modifier
    val actionLabel = stringResource(R.string.history_open_alert_detail_action)
    val description = stringResource(R.string.history_open_alert_detail_description, alertId)
    return Modifier
        .semantics(mergeDescendants = true) { contentDescription = description }
        .clickable(role = Role.Button, onClickLabel = actionLabel) { onAlertClick(alertId) }
}

@Composable
private fun HistoryEvent.resolvedDetail(): String {
    val parts = detailParts?.takeIf { it.isNotEmpty() } ?: return detail.resolve()
    return parts.map { it.resolve() }.reduceOrNull { first, next ->
        stringResource(R.string.common_dot_separated, first, next)
    }.orEmpty()
}

@Composable
private fun EventType.color(): Color = when (this) {
    EventType.ALERT -> AgathaTheme.colors.critical
    EventType.INSPECTION, EventType.EVIDENCE -> AgathaTheme.colors.brand
    EventType.CLASSIFICATION -> AgathaTheme.colors.warning
    EventType.OBSERVATION -> AgathaTheme.colors.notice
    EventType.MAINTENANCE -> AgathaTheme.colors.positive
}

private fun EventType.icon(): ImageVector = when (this) {
    EventType.ALERT -> Icons.Filled.Warning
    EventType.INSPECTION -> Icons.Filled.Build
    EventType.CLASSIFICATION -> Icons.AutoMirrored.Filled.Label
    EventType.EVIDENCE -> Icons.Filled.PhotoCamera
    EventType.OBSERVATION -> Icons.Filled.EditNote
    EventType.MAINTENANCE -> Icons.Filled.CheckCircle
}

@Composable
private fun HistoryLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
private fun HistoryEmptyState(
    @StringRes messageRes: Int = R.string.history_empty_alerts,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryFailureState(
    failure: AlertFailure?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = failureMessage(failure),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.common_retry))
        }
    }
}

@Composable
private fun failureMessage(failure: AlertFailure?): String = when (failure) {
    AlertFailure.NoConnection -> stringResource(R.string.alert_error_no_connection)
    AlertFailure.Server -> stringResource(R.string.alert_error_server)
    AlertFailure.NotFound -> stringResource(R.string.alert_detail_not_found)
    is AlertFailure.Rejected -> failure.message ?: stringResource(R.string.alert_error_rejected)
    null -> stringResource(R.string.alert_error_server)
}
