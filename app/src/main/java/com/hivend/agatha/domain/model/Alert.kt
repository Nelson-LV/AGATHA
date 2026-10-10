package com.hivend.agatha.domain.model

/**
 * Nivel calculado del indicador de un dispositivo (RN-01, Backlog 1.3). El orden de
 * declaración es el de severidad, [GREEN] < [YELLOW] < [ORANGE] < [RED], así que
 * `compareTo` sirve para hallar el nivel máximo de una alerta (RN-05). [GREEN] es
 * normalidad, no una alerta: el primer cálculo en verde termina la alerta (RN-04). Los
 * umbrales los define el backend por dispositivo; la app solo muestra el nivel.
 */
enum class AlertLevel {
    GREEN,
    YELLOW,
    ORANGE,
    RED,
    ;

    /** Amarillo, naranja y rojo son alertas (RN-01); verde es el estado normal. */
    val isAlert: Boolean get() = this != GREEN
}

/**
 * Aviso del dispositivo (RN-02): se pinta en gris y **no es una alerta**. No genera push ni
 * entra al historial de alertas. Puede coexistir con una alerta; entonces el punto toma el
 * color de la alerta y muestra el ícono de aviso (RN-03).
 */
enum class Notice {
    NO_COMMUNICATION,
    INDICATOR_UNAVAILABLE,
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

/** Intensidad de la aceleración medida por el sensor de movimiento. */
enum class AccelerationLevel {
    LOW,
    MEDIUM,
    HIGH,
}

/** Confianza reportada por el motor de reglas/IA y variables de telemetría más recientes. */
data class AlertTelemetry(
    val confidencePercentage: Int,
    val acceleration: AccelerationLevel,
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
    val pointNumber: Int,
    val sensorId: String,
    val pk: String,
    val site: String,
    val description: LocalizedText,
    val level: AlertLevel,
    val status: AlertStatus,
    /** Minutos transcurridos desde que se generó la alerta. */
    val minutesAgo: Int,
    val telemetry: AlertTelemetry,
    /** Observaciones de campo escritas por el técnico (texto libre, no se traduce). */
    val observations: String = "",
)
