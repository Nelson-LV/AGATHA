package com.hivend.agatha.ui.history

import com.hivend.agatha.R
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.repository.DeviceHistory
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.shortLabelRes

private fun res(id: Int, vararg args: Any) = LocalizedText.Resource(id, args.toList())

/**
 * Entradas del historial de un dispositivo (HU-8.3), la más reciente primero: el inicio, el fin y
 * la clasificación de cada alerta (API) y los reportes de inspección de este celular.
 */
fun DeviceHistory.toEvents(): List<HistoryEvent> =
    (alerts.flatMap { it.toEvents() } + inspections.flatMap { it.toEvents() })
        .sortedByDescending { it.at }

private fun Alert.toEvents(): List<HistoryEvent> = buildList {
    val firstLevel = levelTimeline.firstOrNull()?.level ?: maxLevel
    add(
        HistoryEvent(
            at = startedAt,
            type = EventType.ALERT,
            title = res(R.string.history_alert_started, res(firstLevel.shortLabelRes())),
            detail = res(R.string.history_alert_detail, id, res(event.labelRes())),
            statusLabel = res(managementStatus.labelRes()),
        ),
    )
    classification?.let { record ->
        val classifiedAt = record.classifiedAt ?: return@let
        val tag = record.tag
        add(
            HistoryEvent(
                at = classifiedAt,
                type = EventType.CLASSIFICATION,
                title = res(R.string.history_classification_title),
                detail = res(
                    R.string.history_classification_detail,
                    res(record.classification.labelRes()),
                    when {
                        tag == null -> res(R.string.alert_detail_no_tag)
                        tag.otherDetail != null -> LocalizedText.Plain(tag.otherDetail)
                        else -> res(tag.tag.labelRes())
                    },
                ),
            ),
        )
    }
    endedAt?.let {
        add(
            HistoryEvent(
                at = it,
                type = EventType.ALERT,
                title = res(R.string.history_alert_ended),
                detail = res(R.string.history_alert_ended_detail, res(maxLevel.shortLabelRes())),
            ),
        )
    }
}

private fun Inspection.toEvents(): List<HistoryEvent> = buildList {
    add(
        HistoryEvent(
            at = recordedAt,
            type = EventType.INSPECTION,
            title = res(R.string.history_inspection_title),
            detail = observations?.let { LocalizedText.Plain(it) } ?: res(R.string.history_inspection_detail_default),
        ),
    )
    maintenance?.let {
        add(
            HistoryEvent(
                at = recordedAt,
                type = EventType.MAINTENANCE,
                title = res(R.string.history_maintenance_title),
                detail = it.description?.let { text -> LocalizedText.Plain(text) }
                    ?: res(R.string.history_maintenance_detail_default),
            ),
        )
    }
    if (photos.isNotEmpty()) {
        add(
            HistoryEvent(
                at = recordedAt,
                type = EventType.EVIDENCE,
                title = res(R.string.history_evidence_title),
                detail = res(R.string.history_evidence_detail, photos.size),
            ),
        )
    }
}
