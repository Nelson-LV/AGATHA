package com.hivend.agatha.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.ui.components.AgathaHeader
import java.time.format.FormatStyle
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
            Text(stringResource(R.string.map_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (state.isOffline) R.string.map_status_offline else R.string.map_status_online),
                color = if (state.isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        StatusFilterRow(
            selectedStatus = state.statusFilter,
            onSelectStatus = viewModel::setStatusFilter,
        )

        val mapCardShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(mapCardShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, mapCardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clipToBounds(),
        ) {
            OsmdroidMapView(
                route = state.route,
                nodes = state.visibleNodes,
                selectedNodeId = state.selectedNode?.id,
                onNodeTap = viewModel::selectNode,
                modifier = Modifier.matchParentSize().clipToBounds(),
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
                        Text(stringResource(R.string.map_loading), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (state.routeError) {
                Text(
                    stringResource(R.string.map_route_unavailable),
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            MapLegend(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
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
            label = { Text(stringResource(R.string.map_filter_all)) },
        )
        SensorNodeStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onSelectStatus(status) },
            label = { Text(stringResource(status.filterLabelResource())) },
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
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: Locale.getDefault()
    val deviceInstalled = node.batteryLevel != null || node.lastReportTimestamp != null
    val normalizedPk = node.pk.trim().replace(Regex("^PK\\s*", RegexOption.IGNORE_CASE), "")
    val displayPk = if (normalizedPk.isEmpty()) {
        stringResource(R.string.map_value_not_available)
    } else {
        normalizedPk
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (node.name != node.id) {
            Text(node.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        NodeDetailRow(stringResource(R.string.map_node_id), node.id)
        NodeDetailRow(stringResource(R.string.alert_detail_pk), displayPk)
        NodeDetailRow(
            stringResource(R.string.alert_detail_municipality),
            node.municipality.ifBlank { stringResource(R.string.map_value_not_available) },
        )
        NodeDetailRow(
            stringResource(R.string.map_node_communication),
            stringResource(
                if (!deviceInstalled) {
                    R.string.map_device_not_installed
                } else if (node.nodeStatus == SensorNodeStatus.NO_COMMUNICATION) {
                    R.string.notice_no_communication
                } else {
                    R.string.map_communication_online
                },
            ),
        )
        NodeDetailRow(
            stringResource(R.string.map_node_status),
            stringResource(nodeStatusLabelResource(node.nodeStatus)),
        )
        if (deviceInstalled) {
            NodeDetailRow(
                stringResource(R.string.map_node_battery),
                node.batteryLevel?.let { stringResource(R.string.common_percent, it) }
                    ?: stringResource(R.string.map_value_not_available),
            )
            NodeDetailRow(
                stringResource(R.string.map_node_last_reading),
                node.lastReportTimestamp?.let { formatTimestamp(it, locale) }
                    ?: stringResource(R.string.map_value_not_available),
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onViewHistory, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.map_action_view_history))
        }
        OutlinedButton(onClick = onStartInspection, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.map_action_start_inspection))
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
    var expanded by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier) {
        val panelMaxHeight = (maxHeight - 100.dp).coerceAtLeast(64.dp)
        Surface(
            modifier = Modifier.widthIn(max = 260.dp).heightIn(max = panelMaxHeight),
            shape = RoundedCornerShape(10.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) {
            Column(modifier = Modifier.widthIn(max = 260.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.map_legend_title), style = MaterialTheme.typography.labelLarge)
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(
                            if (expanded) R.string.map_legend_collapse else R.string.map_legend_expand,
                        ),
                    )
                }
                if (expanded) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = (panelMaxHeight - 44.dp).coerceAtLeast(24.dp))
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            stringResource(R.string.map_legend_status_heading),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        LegendEntry(R.string.device_status_normal) {
                            StatusLegendDot(SensorNodeStatus.NORMAL.legendColor())
                        }
                        LegendEntry(R.string.map_legend_yellow_alert) {
                            StatusLegendDot(SensorNodeStatus.YELLOW.legendColor())
                        }
                        LegendEntry(R.string.map_legend_orange_alert) {
                            StatusLegendDot(SensorNodeStatus.ORANGE.legendColor())
                        }
                        LegendEntry(
                            labelResourceId = R.string.map_legend_red_alert,
                            secondaryTextResourceId = R.string.map_legend_red_unclassified_hint,
                        ) {
                            StatusLegendDot(SensorNodeStatus.RED.legendColor())
                        }
                        LegendEntry(R.string.map_legend_no_communication) {
                            StatusLegendDot(SensorNodeStatus.NO_COMMUNICATION.legendColor())
                        }
                        LegendEntry(
                            labelResourceId = R.string.map_legend_alert_notice,
                            secondaryTextResourceId = R.string.map_legend_alert_notice_hint,
                        ) {
                            AlertNoticeLegendIcon()
                        }
                        Text(
                            stringResource(R.string.map_legend_pipeline_heading),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        LegendEntry(R.string.map_legend_steel) { SteelPipelineLegendLine() }
                        LegendEntry(R.string.map_legend_flexible) {
                            PipelineLegendLine(LEGEND_FLEXIBLE_COLOR, dashed = true)
                        }
                        LegendEntry(R.string.map_legend_river) {
                            PipelineLegendLine(LEGEND_RIVER_COLOR)
                        }
                        LegendEntry(R.string.map_legend_creek) {
                            PipelineLegendLine(LEGEND_CREEK_COLOR)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendEntry(
    labelResourceId: Int,
    secondaryTextResourceId: Int? = null,
    sample: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(width = 18.dp, height = 16.dp), contentAlignment = Alignment.Center) {
            sample()
        }
        Column {
            Text(stringResource(labelResourceId), style = MaterialTheme.typography.bodySmall)
            secondaryTextResourceId?.let { resourceId ->
                Text(
                    stringResource(resourceId),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun StatusLegendDot(color: Color) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

@Composable
private fun AlertNoticeLegendIcon() {
    Box(modifier = Modifier.size(16.dp)) {
        Box(
            modifier = Modifier.align(Alignment.Center).size(12.dp)
                .background(SensorNodeStatus.ORANGE.legendColor(), CircleShape),
        )
        Box(
            modifier = Modifier.align(Alignment.TopEnd).size(6.dp)
                .background(Color(0xFF757575), CircleShape),
        )
    }
}

@Composable
private fun SteelPipelineLegendLine() {
    Canvas(Modifier.size(width = 36.dp, height = 16.dp)) {
        val centerY = size.height / 2f
        drawLine(
            color = Color(PIPELINE_OUTLINE_COLOR),
            start = androidx.compose.ui.geometry.Offset(0f, centerY),
            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
            strokeWidth = PIPELINE_OUTLINE_WIDTH_DP.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(PIPELINE_MAIN_COLOR),
            start = androidx.compose.ui.geometry.Offset(0f, centerY),
            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
            strokeWidth = PIPELINE_MAIN_WIDTH_DP.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PipelineLegendLine(color: Color, dashed: Boolean = false) {
    Canvas(Modifier.size(width = 36.dp, height = 16.dp)) {
        val dashEffect = if (dashed) {
            PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
        } else {
            null
        }
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = dashEffect,
        )
    }
}

private fun SensorNodeStatus.filterLabelResource(): Int = when (this) {
    SensorNodeStatus.NORMAL -> R.string.level_normal
    SensorNodeStatus.YELLOW -> R.string.level_yellow
    SensorNodeStatus.ORANGE -> R.string.level_orange
    SensorNodeStatus.RED -> R.string.level_red
    SensorNodeStatus.NO_COMMUNICATION -> R.string.notice_no_communication
}

private fun nodeStatusLabelResource(status: SensorNodeStatus): Int = when (status) {
    SensorNodeStatus.NORMAL -> R.string.level_normal
    SensorNodeStatus.YELLOW -> R.string.map_legend_yellow_alert
    SensorNodeStatus.ORANGE -> R.string.map_legend_orange_alert
    SensorNodeStatus.RED -> R.string.map_legend_red_alert
    SensorNodeStatus.NO_COMMUNICATION -> R.string.map_legend_no_communication
}

private fun SensorNodeStatus.legendColor(): Color = when (this) {
    SensorNodeStatus.NORMAL -> Color(0xFF2E7D32)
    SensorNodeStatus.YELLOW -> Color(0xFFF5BE00)
    SensorNodeStatus.ORANGE -> Color(0xFFEF6C00)
    SensorNodeStatus.RED -> Color(0xFFC62828)
    SensorNodeStatus.NO_COMMUNICATION -> Color(0xFF757575)
}

private val LEGEND_FLEXIBLE_COLOR = Color(0xFF403A36)
private val LEGEND_RIVER_COLOR = Color(0xFF35A5D6)
private val LEGEND_CREEK_COLOR = Color(0xFF74889A)

private fun formatTimestamp(timestamp: java.time.Instant, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
        .format(timestamp)

