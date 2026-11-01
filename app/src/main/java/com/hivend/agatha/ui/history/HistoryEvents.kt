package com.hivend.agatha.ui.history

import com.hivend.agatha.R
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertHistoryPhase
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
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
    alerts.flatMap { it.toEvents() }
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
            alertPhase = AlertHistoryPhase.GENERATED,
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
                alertPhase = AlertHistoryPhase.ENDED,
            ),
        )
    }
}

private fun resClassification(classification: AlertClassification): LocalizedText =
    res(classification.labelRes())
