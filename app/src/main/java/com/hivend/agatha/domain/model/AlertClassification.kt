package com.hivend.agatha.domain.model

import java.time.Instant

/** Clasificación de una alerta (RN-06). Una vez guardada no se edita desde ningún cliente. */
enum class AlertClassification {
    CONFIRMED,
    FALSE_ALARM,
}

/**
 * Estado de gestión de una alerta (RN-06). Solo avanza en el orden de declaración y se
 * deriva de la clasificación y la etiqueta — ver [Alert.managementStatus].
 */
enum class ManagementStatus {
    UNCLASSIFIED,
    CLASSIFIED_WITHOUT_TAG,
    CLASSIFIED_WITH_TAG,
}

/**
 * Etiquetas de causa de la web (RN-07). Cada lista pertenece a una clasificación; "Falla del
 * sensor" se retiró en la v1.3. Las variantes OTHER_* son "Otro ¿Cuál?" y exigen detalle.
 */
enum class AlertTag(val classification: AlertClassification) {
    LEAK_CONFIRMED_IN_FIELD(AlertClassification.CONFIRMED),
    GROUND_MOVEMENT(AlertClassification.CONFIRMED),
    THIRD_PARTY_MACHINERY(AlertClassification.CONFIRMED),
    OTHER_CONFIRMED(AlertClassification.CONFIRMED),
    EXTREME_WEATHER(AlertClassification.FALSE_ALARM),
    HEAVY_VEHICLE_TRAFFIC(AlertClassification.FALSE_ALARM),
    SCHEDULED_MAINTENANCE(AlertClassification.FALSE_ALARM),
    OTHER_FALSE_ALARM(AlertClassification.FALSE_ALARM),
    ;

    /** "Otro ¿Cuál?" pide un texto obligatorio de 1 a [OTHER_DETAIL_MAX_LENGTH] caracteres. */
    val requiresDetail: Boolean get() = this == OTHER_CONFIRMED || this == OTHER_FALSE_ALARM

    companion object {
        const val OTHER_DETAIL_MAX_LENGTH = 100

        /** Lista de etiquetas que la UI ofrece para una clasificación, en el orden de la web. */
        fun forClassification(classification: AlertClassification): List<AlertTag> =
            entries.filter { it.classification == classification }
    }
}

/**
 * Etiqueta guardada sobre una alerta clasificada. [otherDetail] es el texto de
 * "Otro ¿Cuál?" (lo escribe una persona, no se traduce) y solo existe en las variantes OTHER_*.
 */
data class TagRecord(
    val tag: AlertTag,
    val otherDetail: String?,
    val taggedAt: Instant,
    val origin: Origin,
) {
    init {
        if (tag.requiresDetail) {
            require(!otherDetail.isNullOrBlank() && otherDetail.length <= AlertTag.OTHER_DETAIL_MAX_LENGTH) {
                "\"Otro ¿Cuál?\" requires 1..${AlertTag.OTHER_DETAIL_MAX_LENGTH} characters"
            }
        } else {
            require(otherDetail == null) { "Only OTHER_* tags carry a detail" }
        }
    }
}

/**
 * Clasificación guardada de una alerta con su fecha-hora y origen (RN-06, RN-10). La
 * etiqueta es opcional y se puede agregar después, una sola vez; ninguno de los dos se edita.
 */
data class ClassificationRecord(
    val classification: AlertClassification,
    val classifiedAt: Instant,
    val origin: Origin,
    val tag: TagRecord? = null,
) {
    init {
        require(tag == null || tag.tag.classification == classification) {
            "Tag ${tag?.tag} does not belong to $classification"
        }
    }

    /** Agrega la etiqueta faltante. Falla si la alerta ya tenía una (no editable). */
    fun withTag(tag: TagRecord): ClassificationRecord {
        check(this.tag == null) { "The alert is already tagged" }
        return copy(tag = tag)
    }
}
