package com.hivend.agatha.domain.model

import java.time.Instant

/** Tipo de entrada mostrada en la línea de tiempo de un dispositivo (HU-8.3). */
enum class EventType {
    ALERT,
    INSPECTION,
    CLASSIFICATION,
    EVIDENCE,
    OBSERVATION,
    MAINTENANCE,
}

/**
 * Entrada individual del historial trazable de un dispositivo: alertas, inspecciones,
 * clasificaciones, evidencias, observaciones y mantenimientos (HE-08, RNF-MOV-07).
 */
data class HistoryEvent(
    val at: Instant,
    val type: EventType,
    val title: LocalizedText,
    val detail: LocalizedText,
    val statusLabel: LocalizedText? = null,
)
