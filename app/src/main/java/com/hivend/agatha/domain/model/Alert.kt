package com.hivend.agatha.domain.model

/**
 * Nivel de severidad de una alerta, tal como lo codifica el color del punto de monitoreo
 * en la plataforma web y en la app móvil (HE-01 / HE-04).
 */
enum class AlertLevel {
    GREEN,
    YELLOW,
    RED,
}

/**
 * Estado del ciclo de vida de una alerta en campo (HU-4.4). El flujo normal avanza en orden
 * de declaración; [EstadoAlerta.CERRADA] es terminal.
 */
enum class AlertStatus {
    GENERATED,
    RECEIVED,
    IN_INSPECTION,
    CLASSIFIED,
    CLOSED,
}

/** Confianza reportada por el motor de reglas/IA y variables de telemetría más recientes. */
data class AlertTelemetry(
    val confidencePercentage: Int,
    val acceleration: String,
    val relativeTime: String,
    val batteryPercentage: Int,
    val isRealData: Boolean,
)

/**
 * Alerta temprana generada por un sensor del gasoducto y gestionada en la app de campo
 * (HE-04). Es el agregado raíz que enlaza con [com.hivend.agatha.domain.model.Inspeccion]
 * y [com.hivend.agatha.domain.model.EventoHistorial] del mismo dispositivo.
 */
data class Alert(
    val id: String,
    val point: String,
    val sensorId: String,
    val pk: String,
    val site: String,
    val description: String,
    val level: AlertLevel,
    val status: AlertStatus,
    val relativeTime: String,
    val telemetry: AlertTelemetry,
    val nextStep: String,
)
