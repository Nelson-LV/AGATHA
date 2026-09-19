package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.EstadoSincronizacion
import kotlinx.coroutines.flow.Flow

/** Puerto hacia la cola de sincronización offline-first (HE-07, RNF-MOV-02/03/04). */
interface SincronizacionRepository {
    fun observarEstado(): Flow<EstadoSincronizacion>
    suspend fun sincronizarAhora()
    suspend fun reintentar(registroId: String)
    suspend fun confirmarConflicto(alertaId: String)
}
