package com.hivend.agatha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.domain.model.SensorTelemetry
import com.hivend.agatha.domain.model.SensorType
import java.time.Instant

@Entity(tableName = "sensor_nodes")
data class SensorNodeEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    /** Stored without the display-only `PK` prefix. */
    val pk: String,
    val municipality: String,
    val latitude: Double?,
    val longitude: Double?,
    val nodeStatus: String,
    val batteryLevel: Int?,
    val lastReportEpochMillis: Long?,
    val acceleration: Double?,
    val gyroscope: Double?,
    val acoustics: Double?,
    val temperature: Double?,
    val humidity: Double?,
    val site: String,
    val sensorType: String,
    val normalizedX: Float,
    val normalizedY: Float,
)

fun SensorNodeEntity.toDomain(): SensorNode = SensorNode(
    id = id,
    code = code,
    name = name,
    pk = normalizePk(pk),
    municipality = municipality,
    latitude = latitude,
    longitude = longitude,
    nodeStatus = nodeStatus.toSensorNodeStatus(),
    batteryLevel = batteryLevel,
    lastReportTimestamp = lastReportEpochMillis?.let { Instant.ofEpochMilli(it) },
    latestTelemetry = SensorTelemetry(
        acceleration = acceleration,
        gyroscope = gyroscope,
        acoustics = acoustics,
        temperature = temperature,
        humidity = humidity,
    ),
    site = site,
    sensorType = sensorType.toSensorType(),
    normalizedX = normalizedX,
    normalizedY = normalizedY,
)

fun SensorNode.toEntity(): SensorNodeEntity = SensorNodeEntity(
    id = id,
    code = code,
    name = name,
    pk = normalizePk(pk),
    municipality = municipality,
    latitude = latitude,
    longitude = longitude,
    nodeStatus = nodeStatus.name,
    batteryLevel = batteryLevel,
    lastReportEpochMillis = lastReportTimestamp?.toEpochMilli(),
    acceleration = latestTelemetry.acceleration,
    gyroscope = latestTelemetry.gyroscope,
    acoustics = latestTelemetry.acoustics,
    temperature = latestTelemetry.temperature,
    humidity = latestTelemetry.humidity,
    site = site,
    sensorType = sensorType.name,
    normalizedX = normalizedX,
    normalizedY = normalizedY,
)

private fun String.toSensorNodeStatus(): SensorNodeStatus =
    runCatching { SensorNodeStatus.valueOf(trim().uppercase()) }
        .getOrDefault(SensorNodeStatus.NO_COMMUNICATION)

private fun String.toSensorType(): SensorType =
    runCatching { SensorType.valueOf(trim().uppercase()) }.getOrDefault(SensorType.MOTION)

private fun normalizePk(value: String): String =
    value.trim().replace(Regex("^PK\\s*", RegexOption.IGNORE_CASE), "")

