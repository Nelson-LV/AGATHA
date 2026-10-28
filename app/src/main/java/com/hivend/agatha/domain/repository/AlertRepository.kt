package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.ClassifyOutcome
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.Origin
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Puerto hacia las alertas de la plataforma (API de gestión de alertas, la misma de la web).
 * La UI y los ViewModels dependen únicamente de esta interfaz. Las lecturas se observan como
 * [Flow] sobre una caché que [refreshAlerts], [loadAlert] y [refreshDevice] mantienen al día.
 *
 * La app no cambia el ciclo de vida de una alerta: termina sola (RN-04). Se clasifica desde el
 * detalle ([classifyAlert], primer envío) y la etiqueta llega después en un reporte de
 * inspección ([registerInspection], segundo envío) — HU-5.2.
 */
interface AlertRepository {
    fun observeAlerts(): Flow<List<Alert>>
    fun observeAlert(alertId: String): Flow<Alert?>

    /** Descarga la lista de alertas del celular (Sin clasificar y activas) y reenvía lo pendiente. */
    suspend fun refreshAlerts(): AlertResult<Unit>

    /** Descarga el detalle de una alerta: niveles de la línea de tiempo, clasificación y observaciones. */
    suspend fun loadAlert(alertId: String): AlertResult<Unit>

    /** Descarga las alertas de un dispositivo (historial) y las suma a la caché. */
    suspend fun refreshDevice(deviceId: Int): AlertResult<Unit>

    /** Agrega una observación de campo. Se guardan una a una y no se editan. */
    suspend fun addObservation(alertId: String, text: String, origin: Origin): AlertResult<Unit>

    /**
     * Clasifica una alerta Sin clasificar (RN-06). Con conexión se guarda en el servidor; sin
     * ella se muestra localmente y se reintenta con el mismo `client_record_id` en la próxima
     * sincronización. Si ya estaba clasificada desde otro cliente, devuelve
     * [ClassifyOutcome.SavedAsConflict].
     */
    suspend fun classifyAlert(
        alertId: String,
        classification: AlertClassification,
        at: Instant,
        origin: Origin,
    ): AlertResult<ClassifyOutcome>

    /** true mientras la clasificación hecha en este celular no se haya enviado a la web. */
    fun observeClassificationPendingSync(alertId: String): Flow<Boolean>

    /**
     * Guarda el reporte en el celular y envía lo que la API recibe: la etiqueta y la observación.
     * El mantenimiento y las fotos solo se guardan localmente (la API aún no tiene endpoint).
     * Si el envío falla por red, queda pendiente y se reintenta.
     */
    suspend fun registerInspection(inspection: Inspection)

    /** Reportes de inspección asociados a la alerta, el más reciente primero (HU-4.2). */
    fun observeInspections(alertId: String): Flow<List<Inspection>>

    /** Reportes del dispositivo [deviceId] ("MP-11556"), para su historial. */
    fun observeDeviceInspections(deviceId: String): Flow<List<Inspection>>

    /** Ids de reportes cuya etiqueta u observación aún no llegó a la API. */
    fun observePendingInspectionIds(): Flow<Set<String>>
}
