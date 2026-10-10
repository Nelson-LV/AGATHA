package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.Inspection
import kotlinx.coroutines.flow.Flow

/**
 * Puerto (patrón Repository) hacia el origen de datos de alertas. La UI y los ViewModels
 * dependen únicamente de esta interfaz — nunca de Room ni de Retrofit directamente — para
 * que la fuente real de datos pueda cambiarse (in-memory hoy, Room+Retrofit en Sprint 3)
 * sin tocar la capa de presentación. Ver docs/ARQUITECTURA_Y_DISENO.md § "Repository".
 *
 * La app no cambia el ciclo de vida de una alerta: termina sola (RN-04) y solo se clasifica
 * o etiqueta desde un reporte de inspección (HU-5.2).
 */
interface AlertRepository {
    fun observeAlerts(): Flow<List<Alert>>
    fun observeAlert(alertId: String): Flow<Alert?>
    suspend fun saveObservations(alertId: String, observations: String)
    suspend fun registerInspection(inspection: Inspection)
}
