package com.hivend.agatha.data.repository

import com.hivend.agatha.R
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.model.SensorTelemetry
import com.hivend.agatha.domain.repository.HistoryRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf

@Singleton
class InMemoryHistoryRepository @Inject constructor() : HistoryRepository {

    private val sampleEvents = createSampleEvents()

    override fun observeHistoryForNode(nodeId: String) =
        flowOf(sampleEvents.filter { event -> event.nodeId == nodeId })

    private fun createSampleEvents(): List<HistoryEvent> {
        val now = Instant.now()
        return listOf(
            HistoryEvent(
                id = "history-alert-mp11556-01",
                nodeId = PRIMARY_NODE_ID,
                timestamp = now.minus(Duration.ofMinutes(45)),
                type = EventType.ALERT,
                description = "",
                telemetrySnapshot = SensorTelemetry(
                    acceleration = 0.82,
                    gyroscope = 1.34,
                    acoustics = 68.2,
                    temperature = 21.7,
                    humidity = 62.0,
                ),
                title = LocalizedText.Resource(R.string.sample_history_red_alert_title),
                detail = LocalizedText.Resource(R.string.sample_history_red_alert_detail, listOf(5)),
                statusLabel = LocalizedText.Resource(R.string.management_status_unclassified),
            ),
            HistoryEvent(
                id = "history-inspection-mp11556-01",
                nodeId = PRIMARY_NODE_ID,
                timestamp = now.minus(Duration.ofHours(3)),
                type = EventType.INSPECTION,
                description = "",
                telemetrySnapshot = SensorTelemetry(
                    acceleration = 0.31,
                    gyroscope = 0.42,
                    acoustics = 52.8,
                    temperature = 21.3,
                    humidity = 60.5,
                ),
                origin = MOBILE_DEVICE_ORIGIN,
                title = LocalizedText.Resource(R.string.sample_history_inspection_title),
                detail = LocalizedText.Resource(R.string.sample_history_inspection_detail),
            ),
            HistoryEvent(
                id = "history-evidence-mp11556-01",
                nodeId = PRIMARY_NODE_ID,
                timestamp = now.minus(Duration.ofHours(4)),
                type = EventType.EVIDENCE,
                description = "",
                evidenceReference = "evidence://MP-11556/photo-01",
                title = LocalizedText.Resource(R.string.sample_history_evidence_title),
                detail = LocalizedText.Resource(R.string.sample_history_evidence_detail, listOf(2)),
            ),
            HistoryEvent(
                id = "history-observation-mp11556-01",
                nodeId = PRIMARY_NODE_ID,
                timestamp = now.minus(Duration.ofDays(1)),
                type = EventType.OBSERVATION,
                description = "",
                title = LocalizedText.Resource(R.string.sample_history_observation_title),
                detail = LocalizedText.Resource(R.string.sample_history_observation_detail),
            ),
            HistoryEvent(
                id = "history-maintenance-mp11556-01",
                nodeId = PRIMARY_NODE_ID,
                timestamp = now.minus(Duration.ofDays(3)),
                type = EventType.MAINTENANCE,
                description = "",
                title = LocalizedText.Resource(R.string.sample_history_maintenance_title),
                detail = LocalizedText.Resource(R.string.sample_history_maintenance_detail),
            ),
            HistoryEvent(
                id = "history-observation-point2-01",
                nodeId = SECONDARY_NODE_ID,
                timestamp = now.minus(Duration.ofDays(2)),
                type = EventType.OBSERVATION,
                description = "",
                title = LocalizedText.Resource(R.string.map_device_not_installed),
                detail = LocalizedText.Resource(R.string.map_device_not_installed),
            ),
            HistoryEvent(
                id = "history-observation-point3-01",
                nodeId = TERTIARY_NODE_ID,
                timestamp = now.minus(Duration.ofDays(5)),
                type = EventType.OBSERVATION,
                description = "",
                title = LocalizedText.Resource(R.string.map_device_not_installed),
                detail = LocalizedText.Resource(R.string.map_device_not_installed),
            ),
        )
    }

    private companion object {
        const val PRIMARY_NODE_ID = "MP-11556"
        const val SECONDARY_NODE_ID = "Punto 2"
        const val TERTIARY_NODE_ID = "Punto 3"
        const val MOBILE_DEVICE_ORIGIN = "CEL-XXXXXX"
    }
}
