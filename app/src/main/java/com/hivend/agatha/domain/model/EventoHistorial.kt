package com.hivend.agatha.domain.model

/** Tipo de entrada mostrada en la línea de tiempo de un dispositivo (HU-8.3). */
enum class TipoEvento {
    ALERTA,
    INSPECCION,
    CLASIFICACION,
    EVIDENCIA,
    OBSERVACION,
    MANTENIMIENTO,
}

/**
 * Entrada individual del historial trazable de un dispositivo: alertas, inspecciones,
 * clasificaciones, evidencias, observaciones y mantenimientos (HE-08, RNF-MOV-07).
 */
data class EventoHistorial(
    val hora: String,
    val tipo: TipoEvento,
    val titulo: String,
    val detalle: String,
    val etiquetaEstado: String? = null,
)
