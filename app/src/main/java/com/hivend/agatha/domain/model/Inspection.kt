package com.hivend.agatha.domain.model

import java.time.Instant

/** Resultado de la inspección (RN-16). Uno, obligatorio. [OTHER] es "Otro ¿Cuál?". */
enum class InspectionResult {
    NO_ISSUES,
    MAINTENANCE_REQUIRED,
    DEVICE_OR_SENSOR_PROBLEM,
    PHYSICAL_DAMAGE,
    LOW_BATTERY,
    NO_COMMUNICATION,
    OTHER,
}

/** Tipo de mantenimiento realizado en campo (RN-16, HU-5.4). Opcional, varios. */
enum class MaintenanceType {
    BATTERY_CHANGE_OR_RECHARGE,
    CLEANING,
    ADJUSTMENT_OR_REINSTALLATION,
    COMPONENT_REPLACEMENT,
    OTHER,
}

/** Límites de texto de la web (Backlog 1.3 § 2). */
object TextLimits {
    const val OBSERVATIONS = 500
    const val OTHER_DETAIL = AlertTag.OTHER_DETAIL_MAX_LENGTH
}

private fun requireOtherDetail(required: Boolean, detail: String?, field: String) {
    if (required) {
        require(!detail.isNullOrBlank() && detail.length <= TextLimits.OTHER_DETAIL) {
            "$field \"Otro ¿Cuál?\" requires 1..${TextLimits.OTHER_DETAIL} characters"
        }
    } else {
        require(detail == null) { "$field only carries a detail for OTHER" }
    }
}

/** Mantenimiento hecho durante la visita (HU-5.4). Aparece en el historial del dispositivo. */
data class MaintenanceRecord(
    val types: Set<MaintenanceType>,
    val otherDetail: String? = null,
    val description: String? = null,
) {
    init {
        require(types.isNotEmpty()) { "Select at least one maintenance type" }
        requireOtherDetail(MaintenanceType.OTHER in types, otherDetail, "Maintenance")
        require((description?.length ?: 0) <= TextLimits.OBSERVATIONS) { "Description too long" }
    }
}

/**
 * Reporte de inspección (HE-05, HU-5.1). Pertenece a un dispositivo y opcionalmente a una
 * alerta. No se edita: un reporte nuevo lo complementa. Guarda fecha-hora y [origin] en vez de
 * un usuario (RN-10) y se crea sin conexión con un [id] generado en el celular (HU-7.2).
 *
 * Si la alerta asociada estaba Sin clasificar, el reporte puede clasificarla ([classification]);
 * si ya estaba clasificada sin etiqueta, puede agregar la etiqueta ([tag]) — HU-5.2.
 */
data class Inspection(
    val id: String,
    val deviceId: String,
    val alertId: String?,
    val result: InspectionResult,
    val resultOtherDetail: String? = null,
    val classification: AlertClassification? = null,
    val tag: AlertTag? = null,
    val tagOtherDetail: String? = null,
    /** Observación libre (HU-5.3). La escribe una persona: no se traduce. */
    val observations: String? = null,
    val maintenance: MaintenanceRecord? = null,
    val photos: List<PhotoEvidence> = emptyList(),
    val recordedAt: Instant,
    val origin: Origin,
) {
    init {
        requireOtherDetail(result == InspectionResult.OTHER, resultOtherDetail, "Result")
        require((observations?.length ?: 0) <= TextLimits.OBSERVATIONS) { "Observations too long" }
        require(alertId != null || (classification == null && tag == null)) {
            "Only a report tied to an alert can classify or tag it"
        }
        if (tag != null) requireOtherDetail(tag.requiresDetail, tagOtherDetail, "Tag")
        else require(tagOtherDetail == null) { "Tag detail without a tag" }
        require(classification == null || tag == null || tag.classification == classification) {
            "Tag $tag does not belong to $classification"
        }
        require(photos.size <= PhotoEvidence.MAX_PER_REPORT) { "At most ${PhotoEvidence.MAX_PER_REPORT} photos" }
    }
}
