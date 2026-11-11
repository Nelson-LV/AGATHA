package com.hivend.agatha.domain.model

import java.time.Instant

/** Current operational state of a sensor node. */
enum class SensorNodeStatus {
    NORMAL,
    YELLOW,
    ORANGE,
    RED,
    NO_COMMUNICATION,
}

/** A snapshot of the sensor values most recently reported by a node. */
data class SensorTelemetry(
    val acceleration: Double? = null,
    val gyroscope: Double? = null,
    val acoustics: Double? = null,
    val temperature: Double? = null,
    val humidity: Double? = null,
)

/** Sensor node data used by the offline map and device details. */
data class SensorNode(
    val id: String,
    val code: String = id,
    val name: String = code,
    val pk: String,
    val municipality: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val nodeStatus: SensorNodeStatus = SensorNodeStatus.NO_COMMUNICATION,
    val batteryLevel: Int? = null,
    val lastReportTimestamp: Instant? = null,
    val latestTelemetry: SensorTelemetry = SensorTelemetry(),
    /** Site and sensor type are retained for the existing map preview during migration. */
    val site: String = municipality,
    val sensorType: SensorType = SensorType.MOTION,
    val normalizedX: Float = 0f,
    val normalizedY: Float = 0f,
) {
    /** Compatibility mapping for existing UI components that still render alert levels. */
    val status: AlertLevel
        get() = when (nodeStatus) {
            SensorNodeStatus.NORMAL -> AlertLevel.GREEN
            SensorNodeStatus.YELLOW -> AlertLevel.YELLOW
            SensorNodeStatus.ORANGE -> AlertLevel.ORANGE
            SensorNodeStatus.RED -> AlertLevel.RED
            SensorNodeStatus.NO_COMMUNICATION -> AlertLevel.GREEN
        }

    /** Compatibility accessors for existing device cards. */
    val batteryPercentage: Int
        get() = batteryLevel ?: 0

    val lastCommunicationMinutesAgo: Int
        get() = lastReportTimestamp?.let {
            ((Instant.now().toEpochMilli() - it.toEpochMilli()) / 60_000L).coerceAtLeast(0).toInt()
        } ?: Int.MAX_VALUE

    /** Compatibility constructor for the existing in-memory map preview. */
    constructor(
        id: String,
        pk: String,
        site: String,
        sensorType: SensorType,
        status: AlertLevel,
        batteryPercentage: Int,
        lastCommunicationMinutesAgo: Int,
        normalizedX: Float,
        normalizedY: Float,
    ) : this(
        id = id,
        code = id,
        name = id,
        pk = pk,
        municipality = site,
        nodeStatus = status.toSensorNodeStatus(),
        batteryLevel = batteryPercentage,
        lastReportTimestamp = Instant.now().minusSeconds(lastCommunicationMinutesAgo.coerceAtLeast(0) * 60L),
        site = site,
        sensorType = sensorType,
        normalizedX = normalizedX,
        normalizedY = normalizedY,
    )
}

private fun AlertLevel.toSensorNodeStatus(): SensorNodeStatus = when (this) {
    AlertLevel.GREEN -> SensorNodeStatus.NORMAL
    AlertLevel.YELLOW -> SensorNodeStatus.YELLOW
    AlertLevel.ORANGE -> SensorNodeStatus.ORANGE
    AlertLevel.RED -> SensorNodeStatus.RED
}

/** Legacy preview grouping retained until the existing map UI is migrated. */
enum class SensorType {
    MOTION,
    ACOUSTIC,
    ENVIRONMENT,
    POWER,
}
