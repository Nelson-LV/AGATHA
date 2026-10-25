package com.hivend.agatha.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hivend.agatha.data.local.entity.SensorNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorNodeDao {
    @Query("SELECT * FROM sensor_nodes ORDER BY pk, id")
    fun observeAll(): Flow<List<SensorNodeEntity>>

    @Query("SELECT * FROM sensor_nodes WHERE id = :nodeId LIMIT 1")
    fun observeById(nodeId: String): Flow<SensorNodeEntity?>

    @Upsert
    suspend fun upsertAll(nodes: List<SensorNodeEntity>)
}
