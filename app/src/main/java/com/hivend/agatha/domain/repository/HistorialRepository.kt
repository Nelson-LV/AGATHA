package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.EventoHistorial
import kotlinx.coroutines.flow.Flow

/** Puerto hacia la línea de tiempo trazable de un dispositivo (HU-8.3, RNF-MOV-07). */
interface HistorialRepository {
    fun observarHistorial(dispositivoId: String): Flow<List<EventoHistorial>>
}
