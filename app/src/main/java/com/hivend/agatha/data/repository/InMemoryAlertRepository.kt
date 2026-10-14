package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.repository.AlertRepository
import java.time.Instant
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

    override suspend fun saveObservations(alertId: String, observations: String) {
        alerts.update { list ->
            list.map { if (it.id == alertId) it.copy(observations = observations) else it }
        }
    }

    /** Alertas clasificadas en este celular que esperan subir a la web (HU-7.2). */
    private val pendingClassifications = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun classifyAlert(alertId: String, classification: AlertClassification, at: Instant, origin: Origin) {
        var classified = false
        alerts.update { list ->
            list.map {
                if (it.id == alertId && it.classification == null) {
                    classified = true
                    it.classify(classification, at, origin)
                } else {
                    it
                }
            }
        }
        if (classified) pendingClassifications.update { it + alertId }
    }

    override fun observeClassificationPendingSync(alertId: String) =
        pendingClassifications.map { alertId in it }

    /** Reportes guardados en esta sesión. Sprint 3: Room + cola de data/sync (HU-7.2). */
    private val inspections = MutableStateFlow<List<Inspection>>(emptyList())

    override fun observeInspections(alertId: String) =
        inspections.map { list -> list.filter { it.alertId == alertId }.sortedByDescending { it.recordedAt } }

    override suspend fun registerInspection(inspection: Inspection) {
        inspections.update { it + inspection }
        val alertId = inspection.alertId ?: return
        alerts.update { list ->
            list.map { if (it.id == alertId) it.applyInspection(inspection) else it }
        }
    }
}
