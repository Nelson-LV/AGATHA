package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.HistoryEvent
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Flow-based access to locally available sensor-node history. */
interface HistoryRepository {
    fun observeHistoryForNode(nodeId: String): Flow<List<HistoryEvent>>

    fun getHistoryEvents(
        nodeId: String,
        from: Instant? = null,
        through: Instant? = null,
    ): Flow<List<HistoryEvent>> = observeHistoryForNode(nodeId).map { events ->
        events.filter { event ->
            (from == null || !event.timestamp.isBefore(from)) &&
                (through == null || !event.timestamp.isAfter(through))
        }
    }

    /** Compatibility alias for existing alert and history consumers. */
    fun observeHistory(deviceId: String): Flow<List<HistoryEvent>> = observeHistoryForNode(deviceId)
}
