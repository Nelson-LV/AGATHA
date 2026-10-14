package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.Origin
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Puerto (patrón Repository) hacia el origen de datos de alertas. La UI y los ViewModels
 * dependen únicamente de esta interfaz — nunca de Room ni de Retrofit directamente — para
 * que la fuente real de datos pueda cambiarse (in-memory hoy, Room+Retrofit en Sprint 3)
 * sin tocar la capa de presentación. Ver docs/ARQUITECTURA_Y_DISENO.md § "Repository".
 *
 * La app no cambia el ciclo de vida de una alerta: termina sola (RN-04). Se clasifica desde
 * el detalle ([classifyAlert], primer envío) y la etiqueta llega después en un reporte de
 * inspección ([registerInspection], segundo envío) — HU-5.2.
 */
interface AlertRepository {
    fun observeAlerts(): Flow<List<Alert>>
    fun observeAlert(alertId: String): Flow<Alert?>
    suspend fun saveObservations(alertId: String, observations: String)

    /**
     * Clasifica una alerta Sin clasificar y deja el registro pendiente de sincronizar. No hace
     * nada si la alerta ya estaba clasificada: la clasificación no se edita (RN-06).
     */
    suspend fun classifyAlert(alertId: String, classification: AlertClassification, at: Instant, origin: Origin)

    /** true mientras la clasificación hecha en este celular no se haya enviado a la web. */
    fun observeClassificationPendingSync(alertId: String): Flow<Boolean>

    suspend fun registerInspection(inspection: Inspection)

    /** Reportes de inspección asociados a la alerta, el más reciente primero (HU-4.2). */
    fun observeInspections(alertId: String): Flow<List<Inspection>>
}
