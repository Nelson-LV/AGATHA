package com.hivend.agatha.domain.model

/**
 * Nivel de severidad de una alerta, tal como lo codifica el color del punto de monitoreo
 * en la plataforma web y en la app móvil (HE-01 / HE-04).
 */
enum class NivelAlerta {
    VERDE,
    AMARILLO,
    ROJO,
}

/**
 * Estado del ciclo de vida de una alerta en campo (HU-4.4). El flujo normal avanza en orden
 * de declaración; [EstadoAlerta.CERRADA] es terminal.
 */
enum class EstadoAlerta {
    GENERADA,
    RECIBIDA,
    EN_INSPECCION,
    CLASIFICADA,
    CERRADA,
}

/** Confianza reportada por el motor de reglas/IA y variables de telemetría más recientes. */
data class TelemetriaAlerta(
    val confianzaPorcentaje: Int,
    val aceleracion: String,
    val tiempoRelativo: String,
    val bateriaPorcentaje: Int,
    val esDatoReal: Boolean,
)

/**
 * Alerta temprana generada por un sensor del gasoducto y gestionada en la app de campo
 * (HE-04). Es el agregado raíz que enlaza con [com.hivend.agatha.domain.model.Inspeccion]
 * y [com.hivend.agatha.domain.model.EventoHistorial] del mismo dispositivo.
 */
data class Alerta(
    val id: String,
    val punto: String,
    val sensorId: String,
    val pk: String,
    val sitio: String,
    val descripcion: String,
    val nivel: NivelAlerta,
    val estado: EstadoAlerta,
    val tiempoRelativo: String,
    val telemetria: TelemetriaAlerta,
    val siguientePaso: String,
)
