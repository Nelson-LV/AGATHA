package com.hivend.agatha.ui.history

import com.hivend.agatha.R
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertHistoryPhase
import com.hivend.agatha.domain.model.AlertObservation
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.repository.DeviceHistory
import com.hivend.agatha.ui.components.labelRes as alertTagLabelRes
import com.hivend.agatha.ui.components.labelRes as maintenanceLabelRes
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.shortLabelRes

private fun res(id: Int, vararg args: Any) = LocalizedText.Resource(id, args.toList())

/**
 * Historial base, compartido con la tarjeta de actividad reciente del Detalle de Alerta.
 * Conserva las filas existentes: inicio/fin de alerta y una fila por reporte de inspección.
 */
fun DeviceHistory.toEvents(): List<HistoryEvent> = toEvents(includeAdditionalTypes = false)

/** Historial expandido para la pestaña Historial, con filas por cada tipo de dato real. */
fun DeviceHistory.toHistoryEvents(): List<HistoryEvent> = toEvents(includeAdditionalTypes = true)

private fun DeviceHistory.toEvents(includeAdditionalTypes: Boolean): List<HistoryEvent> {
    val events = alerts.flatMap { alert -> alert.toEvents(includeAdditionalTypes) } +
        if (includeAdditionalTypes) inspections.flatMap(Inspection::toEvents)
        else inspections.map(Inspection::toHistoryEvent)
    return (if (includeAdditionalTypes) events.filterNot { event ->
            event.type == EventType.OBSERVATION &&
                !event.id.startsWith("inspection:") &&
                inspections.any { report ->
                    report.alertId != null && report.alertId == event.alertId &&
                        report.deviceId == event.nodeId && report.recordedAt == event.timestamp &&
                        report.observations?.trim() == event.description
                }
        } else events)
        .sortedByDescending(HistoryEvent::timestamp)
}

private fun Inspection.toEvents(): List<HistoryEvent> = buildList {
    add(
        HistoryEvent(
            id = "inspection:$id",
            nodeId = deviceId,
            timestamp = recordedAt,
            type = EventType.INSPECTION,
            description = observations.orEmpty(),
            title = res(R.string.history_inspection_title),
            detail = historyDetail(),
            alertId = alertId,
        ),
    )

    observations?.trim()?.takeIf(String::isNotEmpty)?.let { text ->
        add(
            HistoryEvent(
                id = "inspection:$id:observation",
                nodeId = deviceId,
                timestamp = recordedAt,
                type = EventType.OBSERVATION,
                description = text,
                title = res(R.string.history_observation_title),
                detail = LocalizedText.Plain(text),
                alertId = alertId,
            ),
        )
    }

    maintenance?.let { record ->
        val detail = record.description?.trim()?.takeIf(String::isNotEmpty)
            ?.let(LocalizedText::Plain)
            ?: res(R.string.history_maintenance_detail_default)
        val detailParts = buildList {
            record.types.sortedBy { it.ordinal }.forEach { type ->
                add(
                    if (type == MaintenanceType.OTHER) {
                        LocalizedText.Plain(record.otherDetail.orEmpty())
                    } else {
                        res(type.maintenanceLabelRes())
                    },
                )
            }
            record.description?.trim()?.takeIf(String::isNotEmpty)?.let { add(LocalizedText.Plain(it)) }
        }
        add(
            HistoryEvent(
                id = "inspection:$id:maintenance",
                nodeId = deviceId,
                timestamp = recordedAt,
                type = EventType.MAINTENANCE,
                description = record.description.orEmpty(),
                title = res(R.string.history_maintenance_title),
                detail = detail,
                detailParts = detailParts,
                statusLabel = res(R.string.history_maintenance_completed),
                alertId = alertId,
            ),
        )
    }

    if (photos.isNotEmpty()) {
        val descriptions = photos.map { it.description.trim() }.filter(String::isNotEmpty)
        val detail = res(R.string.history_evidence_detail, photos.size)
        val detailParts = descriptions.takeIf { it.isNotEmpty() }?.let { values ->
            listOf(detail) + values.map(LocalizedText::Plain)
        }
        add(
            HistoryEvent(
                id = "inspection:$id:evidence",
                nodeId = deviceId,
                timestamp = recordedAt,
                type = EventType.EVIDENCE,
                description = descriptions.joinToString(" · "),
                title = res(R.string.history_evidence_title),
                detail = detail,
                detailParts = detailParts,
                alertId = alertId,
            ),
        )
    }
}

private fun Inspection.toHistoryEvent(): HistoryEvent = HistoryEvent(
    id = "inspection:$id",
    nodeId = deviceId,
    timestamp = recordedAt,
    type = EventType.INSPECTION,
    description = observations.orEmpty(),
    title = res(R.string.history_inspection_title),
    detail = historyDetail(),
    alertId = alertId,
)

private fun Inspection.historyDetail(): LocalizedText {
    observations?.trim()?.takeIf(String::isNotEmpty)?.let { return LocalizedText.Plain(it) }
    tag?.let { value ->
        val label = res(value.alertTagLabelRes())
        return tagOtherDetail?.takeIf(String::isNotBlank)?.let { otherDetail ->
            res(R.string.history_inspection_tag_other_detail, label, otherDetail)
        } ?: res(R.string.history_inspection_tag_detail, label)
    }
    maintenance?.description?.trim()?.takeIf(String::isNotEmpty)?.let { return LocalizedText.Plain(it) }
    maintenance?.let { return res(R.string.history_maintenance_detail_default) }
    if (photos.isNotEmpty()) return res(R.string.history_evidence_detail, photos.size)
    return res(R.string.history_inspection_detail_default)
}

private fun Alert.toEvents(includeAdditionalTypes: Boolean): List<HistoryEvent> = buildList {
    val firstLevel = levelTimeline.firstOrNull()?.level ?: maxLevel
    val classificationValue = classification?.classification
    val managementStatus = this@toEvents.managementStatus
    val tag = classification?.tag
    val tagLabel = tag?.let { record ->
        val label = res(record.tag.alertTagLabelRes())
        if (record.tag.requiresDetail) {
            res(R.string.history_alert_tag_other_detail, label, record.otherDetail.orEmpty())
        } else {
            res(R.string.history_alert_tag_detail, label)
        }
    }

    add(
        HistoryEvent(
            id = "$id:started",
            nodeId = sensorId,
            timestamp = startedAt,
            type = EventType.ALERT,
            description = "",
            title = res(R.string.history_alert_started, res(firstLevel.shortLabelRes())),
            detail = res(R.string.history_alert_detail, id, res(event.labelRes())),
            statusLabel = res(managementStatus.labelRes()),
            alertId = id,
            alertLevel = firstLevel,
            alertClassification = classificationValue,
            alertManagementStatus = managementStatus,
            alertTagLabel = tagLabel,
            alertPhase = AlertHistoryPhase.GENERATED,
        ),
    )

    if (includeAdditionalTypes) {
        classification?.toHistoryEvent(this@toEvents)?.let(::add)
        observations.mapNotNull { observation -> observation.toHistoryEvent(this@toEvents) }.forEach(::add)
    }

    endedAt?.let { ended ->
        add(
            HistoryEvent(
                id = "$id:ended",
                nodeId = sensorId,
                timestamp = ended,
                type = EventType.ALERT,
                description = "",
                title = res(R.string.history_alert_ended),
                detail = res(R.string.history_alert_ended_detail, res(maxLevel.shortLabelRes())),
                alertId = id,
                alertLevel = maxLevel,
                alertPhase = AlertHistoryPhase.ENDED,
            ),
        )
    }
}

private fun ClassificationRecord.toHistoryEvent(alert: Alert): HistoryEvent? {
    val eventTime = classifiedAt ?: tag?.taggedAt ?: return null
    val classificationText = res(classification.labelRes())
    val tagRecord = tag
    val detail = when {
        tagRecord == null -> classificationText
        !tagRecord.otherDetail.isNullOrBlank() -> res(
            R.string.history_classification_event_tag_other_detail,
            classificationText,
            res(tagRecord.tag.alertTagLabelRes()),
            tagRecord.otherDetail,
        )
        else -> res(
            R.string.history_classification_event_tag_detail,
            classificationText,
            res(tagRecord.tag.alertTagLabelRes()),
        )
    }
    return HistoryEvent(
        id = "${alert.id}:classification",
        nodeId = alert.sensorId,
        timestamp = eventTime,
        type = EventType.CLASSIFICATION,
        description = "",
        title = res(R.string.history_classification_title),
        detail = detail,
        detailParts = origin?.toLocalizedText()?.let { originText -> listOf(detail, originText) },
        alertId = alert.id,
    )
}

private fun Origin.toLocalizedText(): LocalizedText = when (this) {
    Origin.Web -> res(R.string.origin_web)
    is Origin.MobileApp -> res(R.string.origin_mobile_app, deviceId)
}

private fun AlertObservation.toHistoryEvent(alert: Alert): HistoryEvent? {
    val eventTime = recordedAt ?: return null
    return HistoryEvent(
        id = "${alert.id}:observation:$id",
        nodeId = alert.sensorId,
        timestamp = eventTime,
        type = EventType.OBSERVATION,
        description = text,
        title = res(R.string.history_observation_title),
        detail = LocalizedText.Plain(text),
        alertId = alert.id,
    )
}

private fun resClassification(classification: AlertClassification): LocalizedText =
    res(classification.labelRes())
