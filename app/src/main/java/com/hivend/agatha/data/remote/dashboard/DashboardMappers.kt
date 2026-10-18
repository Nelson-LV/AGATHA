package com.hivend.agatha.data.remote.dashboard

import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.domain.model.TelemetryPoint
import com.hivend.agatha.domain.model.TelemetrySeries
import com.hivend.agatha.domain.model.TelemetrySource
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** RFC 3339 con "Z" o con desfase ("-05:00"): el backend usa ambos. */
internal fun parseRfc3339(value: String?): Instant? =
    value?.let {
        try {
            OffsetDateTime.parse(it).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }

internal fun DeviceDto.toDomain() = TelemetryDevice(
    id = id,
    name = name,
    lastConnection = parseRfc3339(lastConnection),
    lastBatteryLevel = lastBatteryLevel,
)

internal fun SeriesDto.toDomain() = TelemetrySeries(
    key = key,
    name = name,
    unit = unit,
    source = if (source.equals("evaluation", ignoreCase = true)) TelemetrySource.EVALUATION else TelemetrySource.MEASUREMENT,
    // Un punto sin valor o con fecha ilegible es un hueco, no un cero.
    points = data.mapNotNull { point ->
        val at = parseRfc3339(point.timestamp) ?: return@mapNotNull null
        val value = point.value ?: return@mapNotNull null
        TelemetryPoint(at, value)
    },
)
