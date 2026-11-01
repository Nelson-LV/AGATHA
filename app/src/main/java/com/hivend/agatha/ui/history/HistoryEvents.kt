package com.hivend.agatha.ui.history

import com.hivend.agatha.R
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.repository.DeviceHistory
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.shortLabelRes

private fun res(id: Int, vararg args: Any) = LocalizedText.Resource(id, args.toList())

/**
 * Entradas del historial de un dispositivo, más recientes primero. Cada alerta genera una fila
 * de inicio y, si terminó, una de fin; la clasificación viaja como dato del evento de inicio.
 */
fun DeviceHistory.toEvents(): List<HistoryEvent> =
    (alerts.flatMap { it.toEvents() } + inspections.flatMap { it.toEvents() })
        .sortedByDescending(HistoryEvent::timestamp)

private fun Alert.toEvents(): List<HistoryEvent> = buildList {
    val firstLevel = levelTimeline.firstOrNull()?.level ?: maxLevel
    val classificationValue = classification?.classification
    val classificationLabel = classificationValue?.let(::resClassification)
        ?: res(R.string.management_status_unclassified)

    add(
        HistoryEvent(
            id = "$id:started",
            nodeId = sensorId,
            timestamp = startedAt,
            type = EventType.ALERT,
            description = "",
            title = res(R.string.history_alert_started, res(firstLevel.shortLabelRes())),
            detail = res(R.string.history_alert_detail, id, res(event.labelRes())),
            statusLabel = classificationLabel,
            alertId = id,
            alertLevel = firstLevel,
            alertClassification = classificationValue,
        ),
    )

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
            ),
        )
    }
}

private fun resClassification(classification: AlertClassification): LocalizedText =
    res(classification.labelRes())

private fun Inspection.toEvents(): List<HistoryEvent> = buildList {
    add(
        HistoryEvent(
            id = "$id:inspection",
            nodeId = deviceId,
            timestamp = recordedAt,
            type = EventType.INSPECTION,
            description = observations.orEmpty(),
            title = res(R.string.history_inspection_title),
            detail = observations?.let(LocalizedText::Plain)
                ?: res(R.string.history_inspection_detail_default),
            origin = origin.toString(),
        ),
    )
    maintenance?.let { record ->
        add(
            HistoryEvent(
                id = "$id:maintenance",
                nodeId = deviceId,
                timestamp = recordedAt,
                type = EventType.MAINTENANCE,
                description = record.description.orEmpty(),
                title = res(R.string.history_maintenance_title),
                detail = record.description?.let(LocalizedText::Plain)
                    ?: res(R.string.history_maintenance_detail_default),
                origin = origin.toString(),
            ),
        )
    }
    if (photos.isNotEmpty()) {
        add(
            HistoryEvent(
                id = "$id:evidence",
                nodeId = deviceId,
                timestamp = recordedAt,
                type = EventType.EVIDENCE,
                description = "",
                title = res(R.string.history_evidence_title),
                detail = res(R.string.history_evidence_detail, photos.size),
                origin = origin.toString(),
            ),
        )
    }
}
