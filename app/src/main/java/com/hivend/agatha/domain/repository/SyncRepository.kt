package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/** Puerto hacia la cola de sincronización offline-first (HE-07, RNF-MOV-02/03/04). */
interface SyncRepository {
    fun observeStatus(): Flow<SyncStatus>

    /** Pide enviar ya lo pendiente; sin red, sale solo al recuperarla. */
    suspend fun syncNow()

    /** Vuelve a enviar un registro en Error, con sus reintentos automáticos de nuevo (HU-7.3). */
    suspend fun retry(recordId: String)

    /** Vuelve a enviar todos los registros en Error ("Reintentar todo", HU-7.3). */
    suspend fun retryAll()

    suspend fun confirmConflict(alertId: String)
}
