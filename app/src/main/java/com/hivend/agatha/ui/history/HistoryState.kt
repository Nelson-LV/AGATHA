package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.SensorNode
import java.time.Instant
import java.time.LocalDate

data class HistoryState(
    val events: List<HistoryEvent> = emptyList(),
    val nodes: List<SensorNode> = emptyList(),
    val selectedNodeId: String? = null,
    val selectedNode: SensorNode? = null,
    val selectedEventType: EventType? = null,
    val dateFrom: LocalDate? = null,
    val dateThrough: LocalDate? = null,
    val isLoading: Boolean = true,
    val isOffline: Boolean = true,
    val lastDataTimestamp: Instant? = null,
)
