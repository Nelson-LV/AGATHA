package com.hivend.agatha.data.remote.dto

import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.domain.model.SensorTelemetry
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable

@Serializable
data class SensorNodeDto(
    val id: String,
    val code: String = id,
    val name: String = code,
    val pk: String,
    val municipality: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val nodeStatus: String? = null,
    val batteryLevel: Int? = null,
    val lastReportTimestamp: String? = null,
    val latestTelemetry: SensorTelemetryDto? = null,
)

@Serializable
data class SensorTelemetryDto(
    val acceleration: Double? = null,
    val gyroscope: Double? = null,
    val acoustics: Double? = null,
    val temperature: Double? = null,
    val humidity: Double? = null,
)

fun SensorNodeDto.toDomain(): SensorNode = SensorNode(
    id = id,
    code = code,
    name = name,
    pk = normalizeSensorPk(pk),
    municipality = municipality,
    latitude = latitude,
    longitude = longitude,
    nodeStatus = nodeStatus.toSensorNodeStatus(),
    batteryLevel = batteryLevel,
    lastReportTimestamp = lastReportTimestamp.toInstantOrNull(),
    latestTelemetry = latestTelemetry?.let {
        SensorTelemetry(
            acceleration = it.acceleration,
            gyroscope = it.gyroscope,
            acoustics = it.acoustics,
            temperature = it.temperature,
            humidity = it.humidity,
        )
    } ?: SensorTelemetry(),
    site = municipality,
)

private fun String?.toSensorNodeStatus(): SensorNodeStatus = when (this?.trim()?.uppercase()) {
    "NORMAL" -> SensorNodeStatus.NORMAL
    "YELLOW" -> SensorNodeStatus.YELLOW
    "ORANGE" -> SensorNodeStatus.ORANGE
    "RED" -> SensorNodeStatus.RED
    "NO_COMMUNICATION" -> SensorNodeStatus.NO_COMMUNICATION
    else -> SensorNodeStatus.NO_COMMUNICATION
}

private fun String?.toInstantOrNull(): Instant? = try {
    this?.let { OffsetDateTime.parse(it).toInstant() }
} catch (_: DateTimeParseException) {
    null
}

private fun normalizeSensorPk(value: String): String =
    value.trim().replace(Regex("^PK\\s*", RegexOption.IGNORE_CASE), "")

