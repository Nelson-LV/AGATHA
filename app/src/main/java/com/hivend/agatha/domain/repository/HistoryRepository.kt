package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.Inspection
import kotlinx.coroutines.flow.Flow

/** Lo que se sabe de un dispositivo: sus alertas (API) y los reportes hechos en este celular. */
data class DeviceHistory(
    val alerts: List<Alert> = emptyList(),
    val inspections: List<Inspection> = emptyList(),
)

/** Puerto hacia el historial trazable de un dispositivo (HU-8.3, RNF-MOV-07). */
interface HistoryRepository {
    fun observeHistory(deviceId: String): Flow<DeviceHistory>

    /** Descarga las alertas del dispositivo ("MP-11556"); [observeHistory] se actualiza solo. */
    suspend fun refresh(deviceId: String): AlertResult<Unit>
}
