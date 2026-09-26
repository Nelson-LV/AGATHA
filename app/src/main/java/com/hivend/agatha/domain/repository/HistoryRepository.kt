package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.HistoryEvent
import kotlinx.coroutines.flow.Flow

/** Puerto hacia la línea de tiempo trazable de un dispositivo (HU-8.3, RNF-MOV-07). */
interface HistoryRepository {
    fun observeHistory(deviceId: String): Flow<List<HistoryEvent>>
}
