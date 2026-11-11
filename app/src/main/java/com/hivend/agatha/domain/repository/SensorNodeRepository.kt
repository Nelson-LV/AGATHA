package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.SensorNode
import kotlinx.coroutines.flow.Flow

/** Flow-based access to cached sensor nodes. */
interface SensorNodeRepository {
    fun observeSensorNodes(): Flow<List<SensorNode>>

    fun getSensorNodeById(nodeId: String): Flow<SensorNode?>

    /** Compatibility alias for existing map consumers. */
    fun observeNodes(): Flow<List<SensorNode>> = observeSensorNodes()

    /** Compatibility alias for existing node-detail consumers. */
    fun observeNode(nodeId: String): Flow<SensorNode?> = getSensorNodeById(nodeId)
}
