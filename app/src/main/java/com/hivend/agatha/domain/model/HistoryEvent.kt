package com.hivend.agatha.domain.model

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
    val time: String,
    val type: EventType,
    val title: String,
    val detail: String,
    val statusLabel: String? = null,
)
