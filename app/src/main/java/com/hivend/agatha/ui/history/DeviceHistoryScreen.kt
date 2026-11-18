package com.hivend.agatha.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.core.content.FileProvider
import java.io.File
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.ui.alerts.AlertCard
import com.hivend.agatha.ui.components.AgathaHeader
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.formatDateTime
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
    LaunchedEffect(state.isLoading, state.shownCount, state.listScrollIndex, state.listScrollOffset) {
        if (!restoredListPosition && !state.isLoading) {
            if (state.shownCount > 0) {
                listState.scrollToItem(
                    index = state.listScrollIndex.coerceAtMost(state.shownCount - 1),
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
    // Al cambiar un filtro la lista vuelve al inicio; sin esto seguiría a la fila que estaba arriba
    // y dejaría ocultos los registros más recientes.
    val filterKey = listOf(
        state.dateFrom,
        state.dateThrough,
        state.selectedDevice?.id,
        state.maxLevels,
        state.alertEvents,
        state.statuses,
    )
    var appliedFilterKey by remember { mutableStateOf<List<Any?>?>(null) }
    LaunchedEffect(filterKey, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        val previous = appliedFilterKey
        appliedFilterKey = filterKey
        if (previous != null && previous != filterKey) listState.scrollToItem(0)
    }
    // Al acercarse al final de lo cargado se pide la página siguiente (10 alertas por página).
    LaunchedEffect(listState, state.canLoadMore) {
        if (!state.canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val layout = listState.layoutInfo
            (layout.visibleItemsInfo.lastOrNull()?.index ?: 0) >= layout.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .collect { nearEnd -> if (nearEnd) viewModel.loadMore() }
    }
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.history_export_chooser)
    LaunchedEffect(state.export) {
        val ready = state.export as? HistoryExportState.Ready ?: return@LaunchedEffect
        shareExport(context, ready.file, shareTitle)
        viewModel.onExportHandled()
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
        if (state.isRefreshing && state.shownCount > 0) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AgathaTheme.colors.brand)
        }
        DeviceSelector(state = state, onDeviceSelected = viewModel::selectDevice)
        HistoryEventFilterControls(
            dateFrom = state.dateFrom,
            dateThrough = state.dateThrough,
            maxLevels = state.maxLevels,
            alertEvents = state.alertEvents,
            statuses = state.statuses,
            hasAdditionalFilters = state.hasAdditionalFilters,
            onDateRangeSelected = viewModel::setDateRange,
            onToggleMaxLevel = viewModel::toggleMaxLevel,
            onToggleAlertEvent = viewModel::toggleAlertEvent,
            onToggleStatus = viewModel::toggleStatus,
            onClearFilters = viewModel::clearFilters,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        if (!state.isLoading) {
            AlertSummaryRow(state = state, onExport = viewModel::export)
        }

        when {
            state.isLoading && state.shownCount == 0 -> HistoryLoadingState(Modifier.weight(1f))
            state.shownCount == 0 && state.failure != null && !state.showingLocalAlerts -> HistoryFailureState(
                failure = state.failure,
                onRetry = viewModel::refresh,
                modifier = Modifier.weight(1f),
            )
            state.shownCount == 0 && state.hasAdditionalFilters -> HistoryEmptyState(
                messageRes = R.string.history_no_alert_results,
                modifier = Modifier.weight(1f),
            )
            state.shownCount == 0 -> HistoryEmptyState(
                messageRes = if (state.selectedDevice == null) R.string.history_empty_all_alerts else R.string.history_empty_alerts,
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.alerts, key = Alert::id) { alert ->
                    HistoryAlertRow(alert, onClick = { onAlertClick(alert.id) })
                }
                if (state.canLoadMore) {
                    item(key = LOAD_MORE_KEY) { LoadingMoreRow() }
                }
            }
        }
    }
}

@Composable
private fun DeviceSelector(
    state: HistoryState,
    onDeviceSelected: (String?) -> Unit,
) {
    val allDevices = stringResource(R.string.history_all_devices)
    SelectorDropdown(
        label = stringResource(R.string.history_device_selector),
        selectedLabel = state.selectedDevice?.label ?: allDevices,
        options = listOf(ALL_DEVICES_OPTION to allDevices) + state.devices.map { device -> device.id to device.label },
        onSelected = { id -> onDeviceSelected(id.takeUnless { it == ALL_DEVICES_OPTION }) },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** "N alertas" y Exportar: el Excel trae lo mismo que la lista con los filtros elegidos. */
@Composable
private fun AlertSummaryRow(state: HistoryState, onExport: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(R.plurals.history_alert_count, state.totalAlerts, state.totalAlerts),
                style = MaterialTheme.typography.titleSmall,
                color = AgathaTheme.colors.textPrimary,
            )
            OutlinedButton(onClick = onExport, enabled = state.canExport) {
                if (state.export == HistoryExportState.Running) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(stringResource(R.string.history_export), modifier = Modifier.padding(start = 8.dp))
            }
        }
        val hint = when {
            state.export is HistoryExportState.Failed -> failureMessage(state.export.failure)
            state.exceedsExportLimit -> stringResource(R.string.history_export_too_many)
            state.showingLocalAlerts -> stringResource(R.string.history_export_needs_connection)
            else -> null
        }
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = AgathaTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun LoadingMoreRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(R.string.history_loading_more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** Abre el diálogo de compartir del sistema con el Excel ("Abrir con", correo, Drive…). */
private fun shareExport(context: Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType(XLSX_MIME_TYPE)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, title))
}

private const val ALL_DEVICES_OPTION = "_all"
private const val LOAD_MORE_KEY = "load-more"
private const val LOAD_MORE_THRESHOLD = 3
private const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@Composable
private fun SelectorDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
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
private fun HistoryAlertRow(alert: Alert, onClick: () -> Unit) {
    AlertCard(alert = alert, onClick = onClick)
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
