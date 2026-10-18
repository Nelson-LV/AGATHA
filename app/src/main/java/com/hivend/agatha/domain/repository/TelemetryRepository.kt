package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.DeviceTelemetry
import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.domain.model.TelemetryResult
import java.time.Duration

/**
 * Puerto hacia la telemetría de los dispositivos (API de tablero del backend). Solo con
 * conexión: no se guarda en el celular (las gráficas del Backlog 1.3 son "solo con conexión").
 */
interface TelemetryRepository {

    /** Dispositivo de la plataforma que corresponde al nodo [nodeId] de la app. */
    suspend fun findDevice(nodeId: String): TelemetryResult<TelemetryDevice>

    /**
     * Series de las variables [keys] del nodo [nodeId] durante la ventana [window] que termina
     * ahora. [keys] vacío trae todas las variables.
     */
    suspend fun recentTelemetry(
        nodeId: String,
        keys: Set<String>,
        window: Duration = Duration.ofHours(24),
    ): TelemetryResult<DeviceTelemetry>
}
