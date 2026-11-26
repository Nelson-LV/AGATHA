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

    /** Un evento que esta versión de la app no conoce (guía §10): se muestra genérico. */
    UNKNOWN,
}

/** Un punto de la línea de tiempo de la alerta: el nivel que tomó y cuándo (RN-05). */
data class LevelChange(
    val level: AlertLevel,
    val at: Instant,
    /** Valor del indicador cuando tomó ese nivel, si el backend lo informa. */
    val indicatorValue: Double? = null,
)

/** Estado de la alerta según el backend (guía §2): una Finalizada puede volver a Activa. */
enum class AlertState {
    ACTIVE,
    FINISHED,
}

/** Acción de gestión que el backend habilita en una alerta (`available_actions`, guía §4). */
enum class ManagementAction {
    CONFIRM,
    FALSE_ALERT,
    ADD_LABEL,
    VIEW,
}

/**
 * Lo que el backend calcula de una alerta y la app muestra tal cual, sin reglas propias (guía
 * §4, §5.4). Cada campo es null si la respuesta no lo trajo: las filas de la lista no traen los
 * del detalle, y entonces [Alert] usa su regla derivada.
 */
data class AlertServerState(
    val state: AlertState? = null,
    val managementStatus: ManagementStatus? = null,
    val availableActions: Set<ManagementAction>? = null,
    /** ACTIVA + ROJO + Sin clasificar: la UI la destaca. */
    val blinking: Boolean? = null,
    /** Horas transcurridas hasta el fin o la última hora procesada (el número del mensaje de fin). */
    val durationHours: Int? = null,
    val reactivationCount: Int? = null,
    val lastReactivatedAt: Instant? = null,
    val openingIndicatorValue: Double? = null,
    val maxIndicatorValue: Double? = null,
    val lastEvaluatedAt: Instant? = null,
) {
    /** Completa lo que esta respuesta no trajo con lo que ya se sabía (p. ej. una fila sobre un detalle). */
    fun orElse(known: AlertServerState) = AlertServerState(
        state = state ?: known.state,
        managementStatus = managementStatus ?: known.managementStatus,
        availableActions = availableActions ?: known.availableActions,
        blinking = blinking ?: known.blinking,
        durationHours = durationHours ?: known.durationHours,
        reactivationCount = reactivationCount ?: known.reactivationCount,
        lastReactivatedAt = lastReactivatedAt ?: known.lastReactivatedAt,
        openingIndicatorValue = openingIndicatorValue ?: known.openingIndicatorValue,
        maxIndicatorValue = maxIndicatorValue ?: known.maxIndicatorValue,
        lastEvaluatedAt = lastEvaluatedAt ?: known.lastEvaluatedAt,
    )
}

/** Acciones que corresponden a un estado de gestión cuando el backend no las informa (guía §4). */
fun ManagementStatus.defaultActions(): Set<ManagementAction> = when (this) {
    ManagementStatus.UNCLASSIFIED -> setOf(ManagementAction.CONFIRM, ManagementAction.FALSE_ALERT, ManagementAction.VIEW)
    ManagementStatus.CLASSIFIED_WITHOUT_TAG -> setOf(ManagementAction.ADD_LABEL, ManagementAction.VIEW)
    ManagementStatus.CLASSIFIED_WITH_TAG -> setOf(ManagementAction.VIEW)
}

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
    /** Nombre del dispositivo tal como lo envía el backend (`device.pk`, "Sensor PK34-470"). */
    val deviceName: String = "",
    /** Nivel actual del indicador: lo que muestran la lista y el mapa (RN-05). Al terminar, el máximo. */
    val level: AlertLevel,
    /** Nivel máximo alcanzado: lo que muestra el historial (RN-05). */
    val maxLevel: AlertLevel,
    /** Niveles que tomó la alerta, en orden cronológico (RN-05). Vacía hasta cargar el detalle. */
    val levelTimeline: List<LevelChange> = emptyList(),
    val startedAt: Instant,
    /** Fin que informa el backend; null = "En curso". La app nunca lo inventa. */
    val endedAt: Instant? = null,
    /** Último valor calculado del indicador; null si todavía no hay (aviso, RN-02). */
    val indicatorValue: Double? = null,
    val classification: ClassificationRecord? = null,
    /** Observaciones y conflictos registrados en la alerta, en orden cronológico. */
    val observations: List<AlertObservation> = emptyList(),
    /** Estado, acciones y datos calculados por el backend; vacío en datos de ejemplo. */
    val server: AlertServerState = AlertServerState(),
) {
    init {
        require(levelTimeline.all { it.level.isAlert }) { "The timeline only holds alert levels" }
    }

    /** Identificador del dispositivo como lo nombra la app y la telemetría: "MP-11556". */
    val sensorId: String get() = "MP-$deviceId"

    /** Lo que se muestra como dispositivo: el nombre del backend tal cual, o [sensorId] si no vino. */
    val displayName: String get() = deviceName.ifBlank { sensorId }

    /** Activa según el backend; sin ese dato, mientras no tenga fin. */
    val isOngoing: Boolean get() = server.state?.let { it == AlertState.ACTIVE } ?: (endedAt == null)

    /**
     * Estado de gestión (RN-06). Solo avanza, así que vale el mayor entre el del backend y el que
     * se deriva de la clasificación y la etiqueta: lo hecho en este celular que aún no se
     * sincronizó se ve aunque la última respuesta del servidor no lo tenga.
     */
    val managementStatus: ManagementStatus
        get() {
            val derived = when {
                classification == null -> ManagementStatus.UNCLASSIFIED
                classification.tag == null -> ManagementStatus.CLASSIFIED_WITHOUT_TAG
                else -> ManagementStatus.CLASSIFIED_WITH_TAG
            }
            return maxOf(derived, server.managementStatus ?: derived)
        }

    /**
     * Botones de gestión (guía §4): los del backend mientras correspondan al estado que se ve; si
     * el celular ya avanzó el estado sin sincronizar, los que corresponden a ese estado.
     */
    val availableActions: Set<ManagementAction>
        get() {
            val status = managementStatus
            return server.availableActions?.takeIf { server.managementStatus == status } ?: status.defaultActions()
        }

    /** Destacar (parpadeo): ACTIVA + ROJO + Sin clasificar, según el backend (`blinking`). */
    val isBlinking: Boolean
        get() = managementStatus == ManagementStatus.UNCLASSIFIED && isOngoing &&
            (server.blinking ?: (level == AlertLevel.RED))

    /**
     * Clasifica una alerta Sin clasificar (HU-5.2). No cambia el fin: si la alerta termina lo
     * informa el backend. Falla si la alerta ya estaba clasificada: no se edita (RN-06).
     */
    fun classify(classification: AlertClassification, at: Instant, origin: Origin): Alert {
        check(this.classification == null) { "Alert $id is already classified" }
        return copy(classification = ClassificationRecord(classification, at, origin))
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
