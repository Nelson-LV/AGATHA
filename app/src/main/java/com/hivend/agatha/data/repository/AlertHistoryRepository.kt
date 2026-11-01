package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.repository.AlertRepository
import com.hivend.agatha.domain.repository.DeviceHistory
import com.hivend.agatha.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Historial de un dispositivo: sus alertas, que baja la API (`GET /api/alerts?device=`), y los
 * reportes de inspección hechos en este celular. Comparte la caché de [AlertRepository].
 */
@Singleton
class AlertHistoryRepository @Inject constructor(
    private val alertRepository: AlertRepository,
) : HistoryRepository {

    override fun observeHistory(deviceId: String): Flow<DeviceHistory> = combine(
        alertRepository.observeAlerts().map { list -> list.filter { it.sensorId == deviceId } },
        alertRepository.observeDeviceInspections(deviceId),
    ) { alerts, inspections -> DeviceHistory(alerts, inspections) }

    override suspend fun refresh(deviceId: String): AlertResult<Unit> {
        val numericId = deviceId.substringAfterLast('-').toIntOrNull() ?: return AlertResult.Success(Unit)
        return alertRepository.refreshDevice(numericId)
    }
}
