package com.hivend.agatha.data.remote.dto

import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.SensorTelemetry
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable

@Serializable
data class HistoryEventDto(
    val id: String,
    val nodeId: String? = null,
    val timestamp: String,
    val type: String,
    val description: String,
    val telemetrySnapshot: SensorTelemetryDto? = null,
    val evidenceReference: String? = null,
    val origin: String? = null,
)

fun HistoryEventDto.toDomain(nodeIdFallback: String): HistoryEvent? {
    val eventTimestamp = timestamp.toInstantOrNull() ?: return null
    val eventType = type.trim().uppercase().toEventTypeOrNull() ?: return null
    val telemetry = telemetrySnapshot?.let {
        SensorTelemetry(
            acceleration = it.acceleration,
            gyroscope = it.gyroscope,
            acoustics = it.acoustics,
            temperature = it.temperature,
            humidity = it.humidity,
        )
    }
    return HistoryEvent(
        id = id,
        nodeId = nodeId?.takeIf(String::isNotBlank) ?: nodeIdFallback,
        timestamp = eventTimestamp,
        type = eventType,
        description = description,
        telemetrySnapshot = telemetry,
        evidenceReference = evidenceReference,
        origin = origin,
    )
}

private fun String.toEventTypeOrNull(): EventType? =
    runCatching { EventType.valueOf(this) }.getOrNull()

private fun String.toInstantOrNull(): Instant? = try {
    OffsetDateTime.parse(this).toInstant()
} catch (_: DateTimeParseException) {
    null
}
