package com.hivend.agatha.data.local.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hivend.agatha.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    /** Encola un registro. Si ya estaba encolado no hace nada y devuelve -1. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(entry: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun get(id: String): SyncQueueEntity?

    /** Entradas todavía no sincronizadas, en orden de registro (HU-7.2). */
    @Query("SELECT * FROM sync_queue WHERE state != 'SYNCED' ORDER BY createdAt, rowid")
    fun observeUnsynced(): Flow<List<SyncQueueEntity>>

    @Query("UPDATE sync_queue SET state = :state, attempts = :attempts, lastError = :lastError WHERE id = :id")
    suspend fun updateState(id: String, state: SyncState, attempts: Int, lastError: String?)
}
