package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/** Puerto hacia la cola de sincronización offline-first (HE-07, RNF-MOV-02/03/04). */
interface SyncRepository {
    fun observeStatus(): Flow<SyncStatus>
    suspend fun syncNow()
    suspend fun retry(recordId: String)
    suspend fun confirmConflict(alertId: String)
}
