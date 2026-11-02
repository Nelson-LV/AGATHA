package com.hivend.agatha.data.local.alert

import androidx.room.withTransaction
import com.hivend.agatha.data.local.AgathaDatabase
import com.hivend.agatha.data.local.sync.SyncQueueEntity
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.data.sync.SyncScheduler
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Envíos de alertas guardados en el celular hasta que llegan a la API (HU-7.2, RN-17). Separa a
 * [com.hivend.agatha.data.repository.RemoteAlertRepository] de Room y WorkManager para probarlo
 * en la JVM.
 */
interface AlertOutbox {
    fun observeUnsynced(): Flow<List<AlertActionEntity>>

    suspend fun get(id: String): AlertActionEntity?

    /**
     * Guarda [actions] y las encola en ese orden. Una etiqueta espera a la clasificación de su
     * alerta si esta sigue en la cola. Pide un envío en cuanto haya red.
     */
    suspend fun enqueue(actions: List<AlertActionEntity>)
}

@Singleton
class RoomAlertOutbox @Inject constructor(
    private val database: AgathaDatabase,
    private val scheduler: SyncScheduler,
) : AlertOutbox {

    private val actions get() = database.alertActionDao()
    private val queue get() = database.syncQueueDao()

    override fun observeUnsynced(): Flow<List<AlertActionEntity>> = actions.observeUnsynced()

    override suspend fun get(id: String): AlertActionEntity? = actions.get(id)

    override suspend fun enqueue(actions: List<AlertActionEntity>) {
        if (actions.isEmpty()) return
        database.withTransaction {
            val now = Instant.now()
            actions.forEachIndexed { index, action ->
                this.actions.insert(action)
                val dependsOn = if (action.type == SyncRecordType.INSPECTION_REPORT) {
                    this.actions.unsyncedClassificationEntry(action.alertId)
                } else {
                    null
                }
                queue.enqueue(
                    SyncQueueEntity(
                        id = UUID.randomUUID().toString(),
                        recordType = action.type,
                        recordId = action.id,
                        dependsOn = dependsOn,
                        // Mismo instante para todo el lote: el índice conserva el orden (etiqueta, observación).
                        createdAt = now.plusMillis(index.toLong()),
                    ),
                )
            }
        }
        scheduler.syncNow()
    }
}
