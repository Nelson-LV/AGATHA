package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.SensorNode
import kotlinx.coroutines.flow.Flow

/** Puerto hacia los nodos de sensores mostrados en el mapa de campo (HE-08). */
interface SensorNodeRepository {
    fun observeNodes(): Flow<List<SensorNode>>
    fun observeNode(nodeId: String): Flow<SensorNode?>
}
