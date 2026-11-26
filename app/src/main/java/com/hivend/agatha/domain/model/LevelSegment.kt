package com.hivend.agatha.domain.model

import java.time.Instant

/** Por qué empezó un tramo de nivel (`opened_by`, guía §2). */
enum class SegmentOpening {
    /** Abrió la alerta. */
    OPEN,

    /** Subió de nivel. */
    UP,

    /** Bajó de nivel. */
    DOWN,

    /** La alerta finalizada se reactivó. */
    REACTIVATION,

    /** Un valor que esta versión de la app no conoce: el tramo se muestra sin ese texto. */
    UNKNOWN,
}

/** Tipo de mensaje de la línea de tiempo (`kind`, guía §6.2): decide solo el estilo. */
enum class MessageKind {
    /** Aviso importante (alerta nueva, subida de nivel): los que generan push. */
    NOTIFICATION,

    /** Novedad horaria: cuenta regresiva, bajada, sin datos, tendencia. */
    UPDATE,

    /** Resumen periódico sin cambios. */
    SUMMARY,

    /** La alerta terminó. */
    END,

    /** Clasificación o etiqueta registrada. */
    MANAGEMENT,

    /** Un tipo nuevo del backend: se muestra con el estilo normal, nunca falla (guía §6.4). */
    UNKNOWN,
}

/**
 * Mensaje de la alerta tal como lo redacta el backend (guía §1, §6.1): [title] y [body] ya vienen
 * en español con los datos y se muestran sin modificar (el cuerpo puede traer saltos de línea).
 * Es texto del servidor, no de la app: no se traduce.
 */
data class AlertMessage(
    val id: Long,
    val at: Instant?,
    val kind: MessageKind,
    /** Código del catálogo (OPEN, COUNTDOWN…); la app no depende de él para mostrar el texto. */
    val code: String?,
    val level: AlertLevel?,
    val title: String,
    val body: String,
    /** Valor del indicador del mensaje; null en gestión, horas sin dato o movimiento. */
    val indicatorValue: Double?,
)

/**
 * Tramo de un mismo color de la alerta (guía §5.4): un bloque propio aunque el color se repita
 * (Amarillo → Naranja → Amarillo son tres tramos). [endedAt] null = tramo vigente.
 */
data class LevelSegment(
    val seq: Int,
    val openedBy: SegmentOpening,
    /** null si el backend manda un nivel que la app no conoce. */
    val level: AlertLevel?,
    val startedAt: Instant,
    val endedAt: Instant?,
    /** Mensajes del tramo en orden cronológico. */
    val messages: List<AlertMessage>,
) {
    val isCurrent: Boolean get() = endedAt == null
}

/**
 * Tramos en orden de [LevelSegment.seq] con sus mensajes por fecha, filtrados por color en el
 * cliente (guía §6.1): [level] null = todos.
 */
fun List<LevelSegment>.filteredByLevel(level: AlertLevel?): List<LevelSegment> =
    sortedBy { it.seq }.filter { level == null || it.level == level }

/** A dónde llevar el detalle al abrirlo desde un push (guía §8.3). */
sealed interface DetailFocus {
    /** El mensaje con este id (`messageId` del push). */
    data class Message(val id: Long) : DetailFocus

    /** El tramo vigente, cuando el push no trae un mensaje. */
    data object CurrentSegment : DetailFocus
}

/** Tramo ([segmentIndex] en esta lista) y mensaje a mostrar; [messageId] null = el tramo entero. */
data class FocusTarget(val segmentIndex: Int, val messageId: Long?)

/**
 * Dónde queda [focus] en estos tramos: el mensaje con ese id, o si no está (o no vino) el tramo
 * vigente y, si la alerta ya terminó, el último. null mientras no haya tramos.
 */
fun List<LevelSegment>.focusTarget(focus: DetailFocus): FocusTarget? {
    if (isEmpty()) return null
    if (focus is DetailFocus.Message) {
        forEachIndexed { index, segment ->
            if (segment.messages.any { it.id == focus.id }) return FocusTarget(index, focus.id)
        }
    }
    val current = indexOfFirst { it.isCurrent }.takeIf { it >= 0 } ?: lastIndex
    return FocusTarget(current, null)
}
