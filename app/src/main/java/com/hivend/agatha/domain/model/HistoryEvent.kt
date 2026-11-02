package com.hivend.agatha.domain.model

import java.time.Instant

/** Kind of event recorded for a sensor node. */
enum class EventType {
    ALERT,
    INSPECTION,
    MAINTENANCE,
    OBSERVATION,
    EVIDENCE,

    /** Retained for existing classification history entries. */
    CLASSIFICATION,
}

enum class AlertHistoryPhase {
    GENERATED,
    ENDED,
}

/** A timestamped event in a sensor node's device history. */
data class HistoryEvent(
    val id: String,
    val nodeId: String,
    val timestamp: Instant,
    val type: EventType,
    val description: String,
    val telemetrySnapshot: SensorTelemetry? = null,
    val evidenceReference: String? = null,
    /** Device origin for traceability of records created from the mobile app. */
    val origin: String? = null,
    /** Legacy presentation values retained for existing history consumers. */
    val time: String = timestamp.toString(),
    val title: LocalizedText = LocalizedText.Plain(description),
    val detail: LocalizedText = LocalizedText.Plain(description),
    /** Optional localized fragments when a record has a variable number of detail values. */
    val detailParts: List<LocalizedText>? = null,
    val statusLabel: LocalizedText? = null,
    /** Alert-specific fields keep classification distinct from its management workflow state. */
    val alertId: String? = null,
    val alertLevel: AlertLevel? = null,
    val alertClassification: AlertClassification? = null,
    val alertPhase: AlertHistoryPhase? = null,
) {
    /** Compatibility timestamp used by the alert detail's recent-history card. */
    val at: Instant get() = timestamp

    /** Compatibility constructor for the existing in-memory history preview. */
    constructor(
        time: String,
        type: EventType,
        title: LocalizedText,
        detail: LocalizedText,
        statusLabel: LocalizedText? = null,
    ) : this(
        id = "",
        nodeId = "",
        timestamp = Instant.EPOCH,
        type = type,
        description = "",
        time = time,
        title = title,
        detail = detail,
        statusLabel = statusLabel,
    )
}
