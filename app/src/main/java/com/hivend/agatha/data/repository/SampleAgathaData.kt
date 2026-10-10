package com.hivend.agatha.data.repository

import androidx.annotation.StringRes
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.AccelerationLevel
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.AlertTelemetry
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.IndicatorReading
import com.hivend.agatha.domain.model.LevelChange
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorType
import com.hivend.agatha.domain.model.TagRecord
import java.time.Duration
import java.time.Instant

/**
 * Datos de muestra usados por los repositorios en memoria mientras HE-04..HE-08 no
 * consumen todavía la API central real (ver docs/ARQUITECTURA_Y_DISENO.md § "Datos de
 * ejemplo vs. API real"). El contenido reproduce los prototipos de Figma (sitio Güepsa –
 * San José de Pare, sensores MP-11xx) para que la demo de navegación se sienta como el
 * diseño aprobado por 0G Colombia.
 *
 * Ningún texto visible se escribe aquí: los textos de ejemplo son claves sample_* de
 * res/values(-en)/strings.xml envueltas en [LocalizedText.Resource], y los datos
 * estructurados (tiempos, tipos, niveles) son números o enums que la UI formatea.
 */
internal object SampleAgathaData {

    const val SITE = "Güepsa – San José de Pare"
    const val MUNICIPALITY = "San José de Pare"

    /** Celular de ejemplo que registró los datos de campo (no hay login, RN-10). */
    private val SAMPLE_PHONE = Origin.MobileApp("CEL-01")

    /** Las fechas de ejemplo son relativas al arranque para que "hace 5 min" siga siendo cierto. */
    private val startup: Instant = Instant.now()

    private fun minutesAgo(minutes: Long): Instant = startup.minus(Duration.ofMinutes(minutes))

    private fun text(@StringRes id: Int, vararg args: Any) = LocalizedText.Resource(id, args.toList())

    val alerts: List<Alert> = listOf(
        Alert(
            id = "ALR-0142",
            pointNumber = 3,
            sensorId = "MP-1156",
            pk = "PK37+800",
            site = SITE,
            municipality = MUNICIPALITY,
            event = AlertEvent.MOVEMENT,
            description = text(R.string.sample_alert_landslide_risk, 5),
            level = AlertLevel.RED,
            levelTimeline = listOf(
                LevelChange(AlertLevel.ORANGE, minutesAgo(5)),
                LevelChange(AlertLevel.RED, minutesAgo(3)),
            ),
            startedAt = minutesAgo(5),
            indicator = IndicatorReading(value = 2.8, threshold = 1.5),
            telemetry = AlertTelemetry(
                confidencePercentage = 96,
                acceleration = AccelerationLevel.HIGH,
                batteryPercentage = 78,
                isRealData = true,
            ),
        ),
        Alert(
            id = "ALR-0139",
            pointNumber = 2,
            sensorId = "MP-1189",
            pk = "PK22+300",
            site = SITE,
            municipality = MUNICIPALITY,
            event = AlertEvent.LEAK,
            description = text(R.string.sample_alert_possible_gas_leak),
            level = AlertLevel.ORANGE,
            levelTimeline = listOf(
                LevelChange(AlertLevel.YELLOW, minutesAgo(40)),
                LevelChange(AlertLevel.ORANGE, minutesAgo(25)),
            ),
            startedAt = minutesAgo(40),
            indicator = IndicatorReading(value = 1.9, threshold = 1.5),
            telemetry = AlertTelemetry(
                confidencePercentage = 81,
                acceleration = AccelerationLevel.MEDIUM,
                batteryPercentage = 64,
                isRealData = true,
            ),
        ),
        Alert(
            id = "ALR-0136",
            pointNumber = 1,
            sensorId = "MP-1122",
            pk = "PK10+050",
            site = SITE,
            municipality = MUNICIPALITY,
            event = AlertEvent.LEAK_AND_MOVEMENT,
            description = text(R.string.sample_alert_test_scenario),
            level = AlertLevel.GREEN,
            levelTimeline = listOf(LevelChange(AlertLevel.RED, minutesAgo(120))),
            startedAt = minutesAgo(120),
            endedAt = minutesAgo(60),
            indicator = IndicatorReading(value = 0.4, threshold = 1.5),
            classification = ClassificationRecord(AlertClassification.CONFIRMED, minutesAgo(90), Origin.Web),
            telemetry = AlertTelemetry(
                confidencePercentage = 100,
                acceleration = AccelerationLevel.LOW,
                batteryPercentage = 91,
                isRealData = false,
            ),
        ),
        Alert(
            id = "ALR-0133",
            pointNumber = 4,
            sensorId = "MP-1201",
            pk = "PK44+120",
            site = SITE,
            municipality = MUNICIPALITY,
            event = AlertEvent.LEAK,
            description = text(R.string.sample_normal_conditions),
            level = AlertLevel.GREEN,
            levelTimeline = listOf(LevelChange(AlertLevel.ORANGE, minutesAgo(180))),
            startedAt = minutesAgo(180),
            endedAt = minutesAgo(150),
            indicator = IndicatorReading(value = 0.6, threshold = 1.5),
            classification = ClassificationRecord(
                AlertClassification.FALSE_ALARM, minutesAgo(150), SAMPLE_PHONE,
                tag = TagRecord(AlertTag.HEAVY_VEHICLE_TRAFFIC, null, minutesAgo(150), SAMPLE_PHONE),
            ),
            telemetry = AlertTelemetry(
                confidencePercentage = 98,
                acceleration = AccelerationLevel.LOW,
                batteryPercentage = 85,
                isRealData = true,
            ),
        ),
    )

    val nodes: List<SensorNode> = listOf(
        SensorNode(
            id = "MP-1156",
            pk = "PK37+800",
            site = SITE,
            sensorType = SensorType.MOTION,
            status = AlertLevel.RED,
            batteryPercentage = 78,
            lastCommunicationMinutesAgo = 5,
            normalizedX = 0.78f,
            normalizedY = 0.22f,
        ),
        SensorNode(
            id = "MP-1189",
            pk = "PK22+300",
            site = SITE,
            sensorType = SensorType.ACOUSTIC,
            status = AlertLevel.ORANGE,
            batteryPercentage = 64,
            lastCommunicationMinutesAgo = 40,
            normalizedX = 0.45f,
            normalizedY = 0.48f,
        ),
        SensorNode(
            id = "MP-1122",
            pk = "PK10+050",
            site = SITE,
            sensorType = SensorType.ENVIRONMENT,
            status = AlertLevel.GREEN,
            batteryPercentage = 91,
            lastCommunicationMinutesAgo = 120,
            normalizedX = 0.20f,
            normalizedY = 0.75f,
        ),
        SensorNode(
            id = "MP-1201",
            pk = "PK44+120",
            site = SITE,
            sensorType = SensorType.POWER,
            status = AlertLevel.GREEN,
            batteryPercentage = 85,
            lastCommunicationMinutesAgo = 180,
            normalizedX = 0.83f,
            normalizedY = 0.80f,
        ),
    )

    fun historyFor(deviceId: String): List<HistoryEvent> = when (deviceId) {
        "MP-1156" -> listOf(
            HistoryEvent(
                "12:42", EventType.ALERT,
                text(R.string.sample_history_red_alert_title),
                text(R.string.sample_history_red_alert_detail, 5),
                text(R.string.management_status_unclassified),
            ),
            HistoryEvent(
                "13:05", EventType.INSPECTION,
                text(R.string.sample_history_inspection_title),
                text(R.string.sample_history_inspection_detail),
            ),
            HistoryEvent(
                "13:07", EventType.CLASSIFICATION,
                text(R.string.sample_history_classification_title),
                text(R.string.alert_tag_ground_movement),
            ),
            HistoryEvent(
                "13:10", EventType.EVIDENCE,
                text(R.string.sample_history_evidence_title),
                text(R.string.sample_history_evidence_detail, 2),
            ),
            HistoryEvent(
                "13:12", EventType.OBSERVATION,
                text(R.string.sample_history_observation_title),
                text(R.string.sample_history_observation_detail),
            ),
            HistoryEvent(
                "13:20", EventType.MAINTENANCE,
                text(R.string.sample_history_maintenance_title),
                text(R.string.sample_history_maintenance_detail),
                text(R.string.sample_history_completed),
            ),
            HistoryEvent(
                "13:25", EventType.ALERT,
                text(R.string.sample_history_alert_ended_title),
                text(R.string.sample_history_alert_ended_detail),
                text(R.string.management_status_classified_with_tag),
            ),
        )
        else -> listOf(
            HistoryEvent(
                "08:50", EventType.ALERT,
                text(R.string.sample_normal_conditions),
                text(R.string.sample_history_all_points_stable),
            ),
        )
    }
}
