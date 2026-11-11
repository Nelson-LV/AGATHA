package com.hivend.agatha.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hivend.agatha.data.local.entity.HistoryEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query(
        "SELECT * FROM history_events " +
            "WHERE nodeId = :nodeId AND timestampEpochMillis >= :cutoffEpochMillis " +
            "ORDER BY timestampEpochMillis DESC",
    )
    fun observeForNode(nodeId: String, cutoffEpochMillis: Long): Flow<List<HistoryEventEntity>>

    @Upsert
    suspend fun upsertAll(events: List<HistoryEventEntity>)

    @Query("DELETE FROM history_events WHERE timestampEpochMillis < :cutoffEpochMillis")
    suspend fun deleteOlderThan(cutoffEpochMillis: Long)
}
