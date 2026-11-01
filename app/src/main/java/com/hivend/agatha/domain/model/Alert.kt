package com.hivend.agatha.domain.model

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
    /** Valor del indicador cuando tomó ese nivel, si el backend lo informa. */
    val indicatorValue: Double? = null,
)

/**
 * Alerta temprana generada por un dispositivo del gasoducto (HE-04), con los datos que
 * entrega la API de gestión de alertas (la misma de la web, Backlog 1.3). Es el agregado raíz que enlaza con [Inspection] y con el
 * historial ([HistoryEvent]) del mismo dispositivo ([sensorId]).
 *
 * La clasificación ([classification]) y la etiqueta solo se agregan, nunca se editan
 * (RN-06): usar [classify] y [addTag], que validan esas reglas.
 */
data class Alert(
    /** Código de la web, "ALR-xxxxxx": también sirve como {id} en la API. */
    val id: String,
    /** Id numérico del punto de medición (`measuring_point_id`), el que usa la API. */
    val deviceId: Int,
    val pk: String,
    val event: AlertEvent,
    /** Nivel actual del indicador: lo que muestran la lista y el mapa (RN-05). Al terminar, el máximo. */
    val level: AlertLevel,
    /** Nivel máximo alcanzado: lo que muestra el historial (RN-05). */
    val maxLevel: AlertLevel,
    /** Niveles que tomó la alerta, en orden cronológico (RN-05). Vacía hasta cargar el detalle. */
    val levelTimeline: List<LevelChange> = emptyList(),
    val startedAt: Instant,
    /** Primer cálculo Normal o clasificación como Falsa alerta (RN-04); null = "En curso". */
    val endedAt: Instant? = null,
    /** Último valor calculado del indicador; null si todavía no hay (aviso, RN-02). */
    val indicatorValue: Double? = null,
    val classification: ClassificationRecord? = null,
    /** Observaciones y conflictos registrados en la alerta, en orden cronológico. */
    val observations: List<AlertObservation> = emptyList(),
) {
    init {
        require(levelTimeline.all { it.level.isAlert }) { "The timeline only holds alert levels" }
    }

    /** Identificador del dispositivo como lo nombra la app y la telemetría: "MP-11556". */
    val sensorId: String get() = "MP-$deviceId"

    val isOngoing: Boolean get() = endedAt == null

    val managementStatus: ManagementStatus
        get() = when {
            classification == null -> ManagementStatus.UNCLASSIFIED
            classification.tag == null -> ManagementStatus.CLASSIFIED_WITHOUT_TAG
            else -> ManagementStatus.CLASSIFIED_WITH_TAG
        }

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

    /**
     * Aplica la etiqueta que trae un reporte de inspección (HU-5.2), con la fecha-hora y el
     * origen del reporte. La alerta ya debe estar clasificada (la clasificación se envía antes,
     * por separado). Falla si choca con lo ya guardado: ese caso es un conflicto de
     * sincronización (RN-18), no una edición.
     */
    fun applyInspection(report: Inspection): Alert {
        require(report.alertId == id) { "Report ${report.id} is not for alert $id" }
        val tag = report.tag ?: return this
        return addTag(TagRecord(tag, report.tagOtherDetail, report.recordedAt, report.origin))
    }

    /** Lo que la lista de alertas muestra (HU-4.2): todas las Sin clasificar y las activas clasificadas. */
    val belongsInAlertList: Boolean
        get() = managementStatus == ManagementStatus.UNCLASSIFIED || isOngoing

    /** Agrega la etiqueta a una alerta clasificada que aún no la tiene (HU-5.2). */
    fun addTag(tag: TagRecord): Alert {
        val current = checkNotNull(classification) { "Alert $id must be classified before tagging" }
        return copy(classification = current.withTag(tag))
    }
}

/**
 * Lista de alertas del celular (HU-4.2): primero las Sin clasificar y luego las activas ya
 * clasificadas; dentro de cada grupo, la más reciente primero.
 */
fun List<Alert>.forAlertList(): List<Alert> =
    filter { it.belongsInAlertList }
        .sortedWith(
            compareBy<Alert> { it.managementStatus != ManagementStatus.UNCLASSIFIED }
                .thenByDescending { it.startedAt },
        )

/** Tipo de entrada en las observaciones de una alerta. */
enum class ObservationKind {
    /** Observación de campo escrita por una persona. */
    NOTE,

    /** Otra clasificación que llegó cuando la alerta ya estaba clasificada: se guarda, no la reemplaza. */
    CLASSIFICATION_CONFLICT,

    /** Otra etiqueta que llegó cuando la alerta ya tenía una. */
    TAG_CONFLICT,
}

/** Observación guardada en la alerta, con fecha-hora y origen (RN-10). No se edita. */
data class AlertObservation(
    val id: Long,
    val text: String,
    val kind: ObservationKind,
    val origin: Origin?,
    val recordedAt: Instant?,
)
