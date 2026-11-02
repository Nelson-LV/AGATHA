package com.hivend.agatha.data.remote.alerts

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertObservation
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.LevelChange
import com.hivend.agatha.domain.model.ObservationKind
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.TagRecord
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Valor de `origin` que la API espera de los envíos de la app. */
internal const val ORIGIN_MOBILE_APP = "APP_MOVIL"

internal fun parseInstant(value: String?): Instant? =
    value?.let { try { OffsetDateTime.parse(it).toInstant() } catch (_: DateTimeParseException) { null } }

internal fun levelFromApi(value: String?): AlertLevel? = when (value) {
    "AMARILLO" -> AlertLevel.YELLOW
    "NARANJA" -> AlertLevel.ORANGE
    "ROJO" -> AlertLevel.RED
    else -> null
}

internal fun eventFromApi(value: String): AlertEvent? = when (value) {
    "FUGA" -> AlertEvent.LEAK
    "MOVIMIENTO" -> AlertEvent.MOVEMENT
    else -> null
}

internal fun classificationFromApi(value: String?): AlertClassification? = when (value) {
    "CONFIRMADA" -> AlertClassification.CONFIRMED
    "FALSA_ALERTA" -> AlertClassification.FALSE_ALARM
    else -> null
}

internal fun AlertClassification.toApi(): String = when (this) {
    AlertClassification.CONFIRMED -> "CONFIRMADA"
    AlertClassification.FALSE_ALARM -> "FALSA_ALERTA"
}

internal fun AlertTag.toApi(): String = when (this) {
    AlertTag.LEAK_CONFIRMED_IN_FIELD -> "FUGA_CONFIRMADA_CAMPO"
    AlertTag.GROUND_MOVEMENT -> "MOVIMIENTO_TIERRA"
    AlertTag.THIRD_PARTY_MACHINERY -> "MAQUINARIA_AJENA"
    AlertTag.EXTREME_WEATHER -> "CLIMA_EXTREMO"
    AlertTag.HEAVY_VEHICLE_TRAFFIC -> "TRANSITO_PESADO"
    AlertTag.SCHEDULED_MAINTENANCE -> "MANTENIMIENTO_PROGRAMADO"
    AlertTag.OTHER_CONFIRMED, AlertTag.OTHER_FALSE_ALARM -> "OTRO"
}

/** La etiqueta de la API es relativa a la clasificación: "OTRO" existe en las dos listas. */
internal fun tagFromApi(classification: AlertClassification, value: String): AlertTag? =
    AlertTag.forClassification(classification).firstOrNull { it.toApi() == value }

internal fun originFromApi(origin: String?, deviceIdentifier: String?): Origin? = when {
    origin == null -> null
    origin == ORIGIN_MOBILE_APP -> Origin.MobileApp(deviceIdentifier.orEmpty())
    else -> Origin.Web
}

private val PK_PATTERN = Regex("""PK\s*\d+(?:[+-]\d+)?""")

/** El dispositivo llega como "Sensor MP-11556 PK34-470": el PK es la parte "PK34-470". */
internal fun pkFromDeviceName(name: String): String = PK_PATTERN.find(name)?.value?.replace(" ", "") ?: name

/** "Otro ¿Cuál?" siempre trae texto; si el servidor no lo manda, se muestra un guion. */
private const val EMPTY_DETAIL = "—"

/**
 * Convierte una fila de la lista o un detalle. null si la API trae un evento o nivel que la app
 * no conoce: es mejor omitir esa alerta que mostrarla mal.
 */
internal fun AlertDto.toDomain(): Alert? {
    val event = eventFromApi(event) ?: return null
    val max = levelFromApi(maxLevel) ?: levelFromApi(currentLevel) ?: return null
    val started = parseInstant(startedAt) ?: return null
    val classificationValue = classificationFromApi(classification)
    return Alert(
        id = code,
        deviceId = device.id,
        pk = pkFromDeviceName(device.pk),
        event = event,
        level = levelFromApi(currentLevel) ?: max,
        maxLevel = max,
        levelTimeline = levels.mapNotNull { it.toDomain() },
        startedAt = started,
        endedAt = if (inProgress) null else parseInstant(endedAt),
        indicatorValue = lastIndicatorValue,
        classification = classificationValue?.let { classificationRecord(it) },
        observations = observations.map { it.toDomain() },
    )
}

private fun LevelDto.toDomain(): LevelChange? {
    val level = levelFromApi(level) ?: return null
    val at = parseInstant(startedAt) ?: return null
    return LevelChange(level, at, messages.firstNotNullOfOrNull { it.indicatorValue })
}

private fun ObservationDto.toDomain() = AlertObservation(
    id = id,
    text = text,
    kind = when (kind) {
        "CONFLICTO_CLASIFICACION" -> ObservationKind.CLASSIFICATION_CONFLICT
        "CONFLICTO_ETIQUETA" -> ObservationKind.TAG_CONFLICT
        else -> ObservationKind.NOTE
    },
    origin = originFromApi(origin, deviceIdentifier),
    recordedAt = parseInstant(recordedAt) ?: parseInstant(receivedAt),
)

private fun AlertDto.classificationRecord(value: AlertClassification): ClassificationRecord {
    val detail = classificationDetail
    val tag = label?.let { tagFromApi(value, it) }?.let { tag ->
        val tagDetail = labelDetail
        TagRecord(
            tag = tag,
            otherDetail = if (tag.requiresDetail) labelOtherText?.takeIf { it.isNotBlank() } ?: EMPTY_DETAIL else null,
            taggedAt = parseInstant(tagDetail?.at) ?: parseInstant(tagDetail?.recordedAt),
            origin = originFromApi(tagDetail?.origin, tagDetail?.deviceIdentifier),
        )
    }
    return ClassificationRecord(
        classification = value,
        classifiedAt = parseInstant(detail?.at) ?: parseInstant(detail?.recordedAt),
        origin = originFromApi(detail?.origin, detail?.deviceIdentifier),
        tag = tag,
    )
}

internal fun ExistingClassificationDto.toDomain(): ClassificationRecord? {
    val value = classificationFromApi(classification) ?: return null
    return ClassificationRecord(value, parseInstant(classifiedAt), originFromApi(origin, deviceIdentifier))
}
