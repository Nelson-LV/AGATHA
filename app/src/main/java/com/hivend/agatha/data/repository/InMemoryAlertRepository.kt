package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertStatus
import com.hivend.agatha.domain.model.PhotoEvidence
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.repository.AlertRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Implementación en memoria de [AlertaRepository]. Es @Singleton porque todas las
 * pantallas deben observar el mismo estado mutable de alertas (patrón Singleton +
 * Observer vía [kotlinx.coroutines.flow.StateFlow] — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Patrones de diseño").
 *
 * Se reemplazará por una implementación respaldada por Room + Retrofit en el sprint de
 * integración con la API central, sin que domain/ui necesiten cambiar una sola línea.
 */
@Singleton
class InMemoryAlertRepository @Inject constructor() : AlertRepository {

    private val alerts: MutableStateFlow<List<Alert>> =
        MutableStateFlow(SampleAgathaData.alerts)

    override fun observeAlerts(): StateFlow<List<Alert>> = alerts

    override fun observeAlert(alertId: String) =
        alerts.map { list -> list.find { it.id == alertId } }

    override suspend fun updateStatus(alertId: String, newStatus: AlertStatus) {
        alerts.update { list ->
            list.map { if (it.id == alertId) it.copy(status = newStatus) else it }
        }
    }

    override suspend fun registerInspection(inspection: Inspection) {
        updateStatus(inspection.alertId, AlertStatus.CLASSIFIED)
    }

    override suspend fun registerEvidence(alertId: String, evidence: List<PhotoEvidence>) {
        // Sprint 3: persistir evidencias en Room y encolarlas en data/sync para su subida.
    }
}
