package com.hivend.agatha.data.repository

import com.hivend.agatha.data.local.sync.SyncQueueDao
import com.hivend.agatha.data.local.sync.SyncQueueEntity
import com.hivend.agatha.data.sync.LastSyncStore
import com.hivend.agatha.data.sync.SyncErrorText
import com.hivend.agatha.data.sync.SyncScheduler
import com.hivend.agatha.domain.model.PendingRecord
import com.hivend.agatha.domain.model.SyncStatus
import com.hivend.agatha.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * La bandeja de sincronización leída de la cola real del celular (`sync_queue`). Lo que muestra
 * sobrevive al cierre de la app porque vive en la base de datos (CA-7.3.2). Reintentar devuelve
 * la entrada a Pendiente y pide el envío de inmediato; sin red, WorkManager lo deja esperando y
 * sale solo al recuperarla.
 */
@Singleton
class RoomSyncRepository @Inject constructor(
    private val queue: SyncQueueDao,
    private val scheduler: SyncScheduler,
    private val lastSync: LastSyncStore,
) : SyncRepository {

    override fun observeStatus(): Flow<SyncStatus> =
        combine(queue.observeUnsynced(), lastSync.lastSuccessfulSync) { entries, last ->
            SyncStatus(pending = entries.map { it.toDomain() }, conflict = null, lastSuccessfulSync = last)
        }

    override suspend fun syncNow() = scheduler.syncNow()

    override suspend fun retry(recordId: String) {
        queue.resetError(recordId)
        scheduler.retryNow()
    }

    override suspend fun retryAll() {
        queue.resetAllErrors()
        scheduler.retryNow()
    }

    // Los conflictos llegan con HU-7.4 (AGT-131); hasta entonces no hay ninguno que confirmar.
    override suspend fun confirmConflict(alertId: String) = Unit

    private fun SyncQueueEntity.toDomain() = PendingRecord(
        id = id,
        type = recordType,
        state = state,
        createdAt = createdAt,
        attempts = attempts,
        error = SyncErrorText.decode(lastError),
    )
}
