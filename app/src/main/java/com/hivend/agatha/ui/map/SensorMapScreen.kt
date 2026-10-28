package com.hivend.agatha.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.ui.components.AgathaHeader
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SensorMapScreen(
    onViewHistory: (String) -> Unit,
    modifier: Modifier = Modifier,
    onStartInspection: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AgathaHeader()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Otero–Santana pipeline", style = MaterialTheme.typography.titleMedium)
            Text(
                if (state.isOffline) "Offline" else "Online",
                color = if (state.isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        StatusFilterRow(
            selectedStatus = state.statusFilter,
            onSelectStatus = viewModel::setStatusFilter,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            OsmdroidMapView(
                route = state.route,
                nodes = state.visibleNodes,
                selectedNodeId = state.selectedNode?.id,
                onNodeTap = viewModel::selectNode,
                modifier = Modifier.fillMaxSize(),
            )

            if (state.isLoading || state.isRouteLoading) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Loading offline map data", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (state.routeError) {
                Text(
                    "Pipeline data is unavailable",
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            MapLegend(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
        }
    }

    state.selectedNode?.let { node ->
        ModalBottomSheet(
            onDismissRequest = viewModel::clearSelection,
            sheetState = sheetState,
        ) {
            NodeDetailsSheet(
                node = node,
                onViewHistory = { onViewHistory(node.id) },
                onStartInspection = { onStartInspection(node.id) },
            )
        }
    }
}

@Composable
private fun StatusFilterRow(
    selectedStatus: SensorNodeStatus?,
    onSelectStatus: (SensorNodeStatus?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onSelectStatus(null) },
            label = { Text("All") },
        )
        SensorNodeStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onSelectStatus(status) },
                label = { Text(status.displayName()) },
            )
        }
    }
}

@Composable
private fun NodeDetailsSheet(
    node: SensorNode,
    onViewHistory: () -> Unit,
    onStartInspection: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(node.id, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        NodeDetailRow("Pipeline location", node.pk.toDisplayPk())
        NodeDetailRow("Municipality", node.municipality.ifBlank { "Not available" })
        NodeDetailRow("Communication", if (node.nodeStatus == SensorNodeStatus.NO_COMMUNICATION) "No communication" else "Connected")
        NodeDetailRow("Status", node.nodeStatus.displayName())
        NodeDetailRow("Battery", node.batteryLevel?.let { "$it%" } ?: "Not available")
        NodeDetailRow("Last reading", node.lastReportTimestamp?.let(::formatTimestamp) ?: "Not available")
        Spacer(Modifier.height(4.dp))
        Button(onClick = onViewHistory, modifier = Modifier.fillMaxWidth()) {
            Text("View history")
        }
        OutlinedButton(onClick = onStartInspection, modifier = Modifier.fillMaxWidth()) {
            Text("Start inspection")
        }
    }
}

@Composable
private fun NodeDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), tonalElevation = 4.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SensorNodeStatus.entries.forEach { status ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(status.legendColor(), CircleShape))
                    Text(status.shortDisplayName(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun SensorNodeStatus.displayName(): String = when (this) {
    SensorNodeStatus.NORMAL -> "Normal"
    SensorNodeStatus.YELLOW -> "Yellow"
    SensorNodeStatus.ORANGE -> "Orange"
    SensorNodeStatus.RED -> "Red"
    SensorNodeStatus.NO_COMMUNICATION -> "No communication"
}

private fun SensorNodeStatus.shortDisplayName(): String = when (this) {
    SensorNodeStatus.NORMAL -> "Normal"
    SensorNodeStatus.YELLOW -> "Yellow"
    SensorNodeStatus.ORANGE -> "Orange"
    SensorNodeStatus.RED -> "Red"
    SensorNodeStatus.NO_COMMUNICATION -> "Offline"
}

private fun SensorNodeStatus.legendColor(): Color = when (this) {
    SensorNodeStatus.NORMAL -> Color(0xFF2E7D32)
    SensorNodeStatus.YELLOW -> Color(0xFFF5BE00)
    SensorNodeStatus.ORANGE -> Color(0xFFEF6C00)
    SensorNodeStatus.RED -> Color(0xFFC62828)
    SensorNodeStatus.NO_COMMUNICATION -> Color(0xFF757575)
}

private fun String.toDisplayPk(): String {
    val normalized = trim().replace(Regex("^PK\\s*", RegexOption.IGNORE_CASE), "")
    return if (normalized.isEmpty()) "PK not available" else "PK $normalized"
}

private fun formatTimestamp(timestamp: java.time.Instant): String =
    DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ENGLISH)
        .withZone(ZoneId.systemDefault())
        .format(timestamp)

