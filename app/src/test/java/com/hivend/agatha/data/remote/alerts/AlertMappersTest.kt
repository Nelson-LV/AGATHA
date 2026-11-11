package com.hivend.agatha.data.remote.alerts

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.domain.model.ObservationKind
import com.hivend.agatha.domain.model.Origin
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertMappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun orFail(alert: Alert?): Alert = alert ?: throw AssertionError("The alert was skipped")

    private fun alert(text: String) = json.decodeFromString<AlertDto>(text).toDomain()

    /** Detalle real de la API: una alerta amarilla de fuga que terminó sola. */
    private val finishedDetail = """
        {"id":7,"code":"ALR-000007","device":{"id":11556,"pk":"Sensor MP-11556 PK34-470"},"event":"FUGA",
         "state":"FINALIZADA","started_at":"2026-09-26T18:00:00-05:00","ended_at":"2026-09-27T01:00:00-05:00",
         "in_progress":false,"duration_hours":7,"current_level":null,"max_level":"AMARILLO","blinking":false,
         "classification":null,"management_status":"SIN_CLASIFICAR","label":null,"label_other_text":null,
         "available_actions":["CONFIRM","FALSE_ALERT","VIEW"],"opened_at":"2026-09-26T19:00:00-05:00",
         "reactivation_count":0,"opening_indicator_value":0.1656,"last_indicator_value":0.1271,
         "classification_detail":null,"label_detail":null,"observations":[],
         "levels":[{"seq":1,"opened_by":"OPEN","level":"AMARILLO","started_at":"2026-09-26T18:00:00-05:00",
           "ended_at":"2026-09-27T01:00:00-05:00",
           "messages":[{"id":124,"at":"2026-09-26T19:00:00-05:00","kind":"NOTIFICACION","code":"OPEN",
             "level":"AMARILLO","indicator_value":0.1656,"title":"Alerta nueva","body":"Ruido"}]}]}
    """.trimIndent()

    @Test
    fun `maps a finished alert from the API`() {
        val alert = orFail(alert(finishedDetail))
        assertEquals("ALR-000007", alert.id)
        assertEquals(11556, alert.deviceId)
        assertEquals("MP-11556", alert.sensorId)
        assertEquals("PK34-470", alert.pk)
        assertEquals(AlertEvent.LEAK, alert.event)
        // Al terminar, current_level es null: la lista muestra el nivel máximo.
        assertEquals(AlertLevel.YELLOW, alert.level)
        assertEquals(AlertLevel.YELLOW, alert.maxLevel)
        assertEquals(Instant.parse("2026-09-26T23:00:00Z"), alert.startedAt)
        assertEquals(Instant.parse("2026-09-27T06:00:00Z"), alert.endedAt)
        assertEquals(0.1271, alert.indicatorValue!!, 1e-9)
        assertEquals(ManagementStatus.UNCLASSIFIED, alert.managementStatus)
        assertEquals(1, alert.levelTimeline.size)
        assertEquals(AlertLevel.YELLOW, alert.levelTimeline.single().level)
        assertEquals(0.1656, alert.levelTimeline.single().indicatorValue!!, 1e-9)
    }

    @Test
    fun `an alert in progress has no end and keeps its current level`() {
        val alert = orFail(
            alert(
                """{"id":9,"code":"ALR-000009","device":{"id":11556,"pk":"Sensor MP-11556 PK34-470"},"event":"MOVIMIENTO",
                    "started_at":"2026-10-20T10:00:00-05:00","ended_at":null,"in_progress":true,
                    "current_level":"NARANJA","max_level":"ROJO"}""",
            ),
        )
        assertTrue(alert.isOngoing)
        assertEquals(AlertEvent.MOVEMENT, alert.event)
        assertEquals(AlertLevel.ORANGE, alert.level)
        assertEquals(AlertLevel.RED, alert.maxLevel)
        assertTrue(alert.levelTimeline.isEmpty())
    }

    @Test
    fun `maps classification, tag and observations with their origin`() {
        val alert = orFail(
            alert(
                """{"id":3,"code":"ALR-000003","device":{"id":11556,"pk":"Sensor MP-11556 PK34-470"},"event":"FUGA",
                    "started_at":"2026-10-01T10:00:00-05:00","in_progress":false,"max_level":"ROJO",
                    "classification":"FALSA_ALERTA","label":"OTRO","label_other_text":"Prueba de presión",
                    "classification_detail":{"value":"FALSA_ALERTA","at":"2026-10-01T11:00:00-05:00",
                      "origin":"APP_MOVIL","device_identifier":"CEL-ABC123","recorded_at":"2026-10-01T10:59:00-05:00"},
                    "label_detail":{"value":"OTRO","other_text":"Prueba de presión","at":"2026-10-01T11:05:00-05:00",
                      "origin":"WEB"},
                    "observations":[{"id":1,"text":"Ya clasificada","kind":"CONFLICTO_CLASIFICACION",
                      "origin":"APP_MOVIL","device_identifier":"CEL-1","received_at":"2026-10-01T12:00:00-05:00"}]}""",
            ),
        )
        val record = alert.classification!!
        assertEquals(AlertClassification.FALSE_ALARM, record.classification)
        assertEquals(Origin.MobileApp("CEL-ABC123"), record.origin)
        assertEquals(Instant.parse("2026-10-01T16:00:00Z"), record.classifiedAt)
        assertEquals(AlertTag.OTHER_FALSE_ALARM, record.tag!!.tag)
        assertEquals("Prueba de presión", record.tag!!.otherDetail)
        assertEquals(Origin.Web, record.tag!!.origin)
        assertEquals(ManagementStatus.CLASSIFIED_WITH_TAG, alert.managementStatus)
        assertEquals(ObservationKind.CLASSIFICATION_CONFLICT, alert.observations.single().kind)
        assertEquals(Origin.MobileApp("CEL-1"), alert.observations.single().origin)
    }

    @Test
    fun `a list row without detail has a classification with unknown date and origin`() {
        val alert = orFail(
            alert(
                """{"id":4,"code":"ALR-000004","device":{"id":1,"pk":"MP-1"},"event":"FUGA",
                    "started_at":"2026-10-01T10:00:00-05:00","max_level":"AMARILLO","classification":"CONFIRMADA"}""",
            ),
        )
        assertNull(alert.classification!!.classifiedAt)
        assertNull(alert.classification!!.origin)
        // Sin "PK" reconocible en el nombre, se muestra el nombre tal cual llega.
        assertEquals("MP-1", alert.pk)
    }

    @Test
    fun `an alert with an unknown event or level is skipped instead of shown wrong`() {
        assertNull(alert("""{"id":5,"code":"ALR-5","device":{"id":1},"event":"OTRO_EVENTO","started_at":"2026-10-01T10:00:00-05:00","max_level":"ROJO"}"""))
        assertNull(alert("""{"id":6,"code":"ALR-6","device":{"id":1},"event":"FUGA","started_at":"2026-10-01T10:00:00-05:00","max_level":"MORADO"}"""))
    }

    @Test
    fun `tags map both ways and OTRO belongs to either classification`() {
        AlertTag.entries.forEach { tag ->
            assertEquals(tag, tagFromApi(tag.classification, tag.toApi()))
        }
        assertEquals(AlertTag.OTHER_CONFIRMED, tagFromApi(AlertClassification.CONFIRMED, "OTRO"))
        assertEquals(AlertTag.OTHER_FALSE_ALARM, tagFromApi(AlertClassification.FALSE_ALARM, "OTRO"))
        assertNull(tagFromApi(AlertClassification.CONFIRMED, "CLIMA_EXTREMO"))
    }
}
