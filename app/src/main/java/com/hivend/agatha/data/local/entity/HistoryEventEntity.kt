package com.hivend.agatha.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.model.SensorTelemetry
import java.time.Instant

@Entity(
    tableName = "history_events",
    indices = [Index(value = ["nodeId", "timestampEpochMillis"])],
)
data class HistoryEventEntity(
    @PrimaryKey val id: String,
    val nodeId: String,
    val timestampEpochMillis: Long,
    val type: String,
    val description: String,
    val acceleration: Double?,
    val gyroscope: Double?,
    val acoustics: Double?,
    val temperature: Double?,
    val humidity: Double?,
    val evidenceReference: String?,
    val origin: String?,
    val titleText: String,
    val detailText: String,
    val statusLabelText: String?,
)

fun HistoryEventEntity.toDomain(): HistoryEvent {
    val eventTimestamp = Instant.ofEpochMilli(timestampEpochMillis)
    return HistoryEvent(
        id = id,
        nodeId = nodeId,
        timestamp = eventTimestamp,
        type = type.toEventType(),
        description = description,
        telemetrySnapshot = if (
            acceleration != null || gyroscope != null || acoustics != null ||
            temperature != null || humidity != null
        ) {
            SensorTelemetry(acceleration, gyroscope, acoustics, temperature, humidity)
        } else {
            null
        },
        evidenceReference = evidenceReference,
        origin = origin,
        time = eventTimestamp.toString(),
        title = LocalizedText.Plain(titleText),
        detail = LocalizedText.Plain(detailText),
        statusLabel = statusLabelText?.let { LocalizedText.Plain(it) },
    )
}

fun HistoryEvent.toEntity(): HistoryEventEntity = HistoryEventEntity(
    id = id,
    nodeId = nodeId,
    timestampEpochMillis = timestamp.toEpochMilli(),
    type = type.name,
    description = description,
    acceleration = telemetrySnapshot?.acceleration,
    gyroscope = telemetrySnapshot?.gyroscope,
    acoustics = telemetrySnapshot?.acoustics,
    temperature = telemetrySnapshot?.temperature,
    humidity = telemetrySnapshot?.humidity,
    evidenceReference = evidenceReference,
    origin = origin,
    titleText = title.toStoredText(description),
    detailText = detail.toStoredText(description),
    statusLabelText = statusLabel?.toStoredText(),
)

private fun LocalizedText.toStoredText(fallback: String = ""): String = when (this) {
    is LocalizedText.Plain -> value
    is LocalizedText.Resource -> fallback
}

private fun String.toEventType(): EventType =
    runCatching { EventType.valueOf(trim().uppercase()) }.getOrDefault(EventType.OBSERVATION)

