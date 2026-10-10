package com.hivend.agatha.domain.model

import java.time.Duration
import java.time.Instant

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

/** Tipo de evento que detectó el dispositivo (RN-12). */
enum class AlertEvent {
    LEAK,
    MOVEMENT,
    LEAK_AND_MOVEMENT,
}

/** Un punto de la línea de tiempo de la alerta: el nivel que tomó y cuándo (RN-05). */
data class LevelChange(
    val level: AlertLevel,
    val at: Instant,
)

/** Valor del indicador del dispositivo y el umbral con el que se compara (HU-4.2). */
data class IndicatorReading(
    val value: Double,
    val threshold: Double,
)

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
 * Alerta temprana generada por un dispositivo del gasoducto (HE-04), con los datos que
 * muestra la web (Backlog 1.3). Es el agregado raíz que enlaza con [Inspection] y con el
 * historial ([HistoryEvent]) del mismo dispositivo ([sensorId]).
 *
 * La clasificación ([classification]) y la etiqueta solo se agregan, nunca se editan
 * (RN-06): usar [classify] y [addTag], que validan esas reglas.
 */
data class Alert(
    /** Identificador de la web, "ALR-xxxx". */
    val id: String,
    val pointNumber: Int,
    /** Dispositivo que generó la alerta. */
    val sensorId: String,
    val pk: String,
    val site: String,
    val municipality: String,
    val event: AlertEvent,
    val description: LocalizedText,
    /** Nivel actual del indicador: lo que muestran la lista y el mapa (RN-05). */
    val level: AlertLevel,
    /** Niveles que tomó la alerta, en orden cronológico (RN-05). Nunca vacía. */
    val levelTimeline: List<LevelChange>,
    val startedAt: Instant,
    /** Primer cálculo Normal o clasificación como Falsa alerta (RN-04); null = "En curso". */
    val endedAt: Instant? = null,
    /** null cuando el indicador no es calculable (aviso, RN-02). */
    val indicator: IndicatorReading? = null,
    val classification: ClassificationRecord? = null,
    val telemetry: AlertTelemetry,
    /** Observaciones de campo escritas por el técnico (texto libre, no se traduce). */
    val observations: String = "",
) {
    init {
        require(levelTimeline.isNotEmpty()) { "An alert has at least one level" }
        require(levelTimeline.all { it.level.isAlert }) { "The timeline only holds alert levels" }
    }

    /** Nivel máximo alcanzado: lo que muestra el historial (RN-05). */
    val maxLevel: AlertLevel get() = levelTimeline.maxOf { it.level }

    val isOngoing: Boolean get() = endedAt == null

    val managementStatus: ManagementStatus
        get() = when {
            classification == null -> ManagementStatus.UNCLASSIFIED
            classification.tag == null -> ManagementStatus.CLASSIFIED_WITHOUT_TAG
            else -> ManagementStatus.CLASSIFIED_WITH_TAG
        }

    fun minutesSinceStart(now: Instant = Instant.now()): Int =
        Duration.between(startedAt, now).toMinutes().toInt().coerceAtLeast(0)

    /**
     * Clasifica una alerta Sin clasificar (HU-5.2). Una Falsa alerta en curso termina en ese
     * momento (RN-04). Falla si la alerta ya estaba clasificada: no se edita (RN-06).
     */
    fun classify(classification: AlertClassification, at: Instant, origin: Origin): Alert {
        check(this.classification == null) { "Alert $id is already classified" }
        val endsNow = classification == AlertClassification.FALSE_ALARM && isOngoing
        return copy(
            classification = ClassificationRecord(classification, at, origin),
            endedAt = if (endsNow) at else endedAt,
        )
    }

    /** Agrega la etiqueta a una alerta clasificada que aún no la tiene (HU-5.2). */
    fun addTag(tag: TagRecord): Alert {
        val current = checkNotNull(classification) { "Alert $id must be classified before tagging" }
        return copy(classification = current.withTag(tag))
    }
}
