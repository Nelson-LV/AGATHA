package com.hivend.agatha.data.local.alert

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertActionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(action: AlertActionEntity)

    @Query("SELECT * FROM alert_actions WHERE id = :id")
    suspend fun get(id: String): AlertActionEntity?

    /** Envíos que todavía no llegan al servidor (su entrada de la cola no está Sincronizada). */
    @Query(
        """
        SELECT a.* FROM alert_actions a
        JOIN sync_queue q ON q.recordId = a.id AND q.recordType = a.type
        WHERE q.state != 'SYNCED'
        ORDER BY a.recordedAt
        """,
    )
    fun observeUnsynced(): Flow<List<AlertActionEntity>>

    /** Entrada de la cola de la clasificación de [alertId] que aún no se envía, si la hay. */
    @Query(
        """
        SELECT q.id FROM sync_queue q
        JOIN alert_actions a ON q.recordId = a.id AND q.recordType = a.type
        WHERE a.alertId = :alertId AND a.type = 'CLASSIFICATION' AND q.state != 'SYNCED'
        LIMIT 1
        """,
    )
    suspend fun unsyncedClassificationEntry(alertId: String): String?
}
