package com.hivend.agatha.data.remote.alerts

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertState
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ManagementAction
import com.hivend.agatha.domain.model.MessageKind
import com.hivend.agatha.domain.model.SegmentOpening
import com.hivend.agatha.domain.model.filteredByLevel
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
        coerceInputValues = true
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
    fun `reads the state, actions and figures the backend computes`() {
        val alert = orFail(alert(finishedDetail.replace("\"reactivation_count\":0", "\"reactivation_count\":2,\"max_indicator_value\":0.31")))
        val server = alert.server

        assertEquals(AlertState.FINISHED, server.state)
        assertEquals(ManagementStatus.UNCLASSIFIED, server.managementStatus)
        assertEquals(setOf(ManagementAction.CONFIRM, ManagementAction.FALSE_ALERT, ManagementAction.VIEW), server.availableActions)
        assertEquals(false, server.blinking)
        assertEquals(7, server.durationHours)
        assertEquals(2, server.reactivationCount)
        assertEquals(0.1656, server.openingIndicatorValue!!, 1e-9)
        assertEquals(0.31, server.maxIndicatorValue!!, 1e-9)
        assertEquals(false, alert.isOngoing)
        assertEquals(false, alert.isBlinking)
    }

    @Test
    fun `unknown actions are ignored and the known ones kept`() {
        val alert = orFail(alert(finishedDetail.replace("\"available_actions\":[\"CONFIRM\",\"FALSE_ALERT\",\"VIEW\"]", "\"available_actions\":[\"VIEW\",\"REOPEN\"]")))

        assertEquals(setOf(ManagementAction.VIEW), alert.availableActions)
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
    fun `unknown events and levels are tolerated instead of dropping the alert`() {
        val unknownEvent = orFail(alert("""{"id":5,"code":"ALR-5","device":{"id":1},"event":"OTRO_EVENTO","started_at":"2026-10-01T10:00:00-05:00","max_level":"ROJO"}"""))
        assertEquals(AlertEvent.UNKNOWN, unknownEvent.event)
        assertEquals(AlertLevel.RED, unknownEvent.level)

        // max_level desconocido: cae al nivel actual.
        val unknownMax = orFail(alert("""{"id":6,"code":"ALR-6","device":{"id":1},"event":"FUGA","started_at":"2026-10-01T10:00:00-05:00","in_progress":true,"current_level":"NARANJA","max_level":"MORADO"}"""))
        assertEquals(AlertLevel.ORANGE, unknownMax.maxLevel)
        assertEquals(AlertLevel.ORANGE, unknownMax.level)

        // Ningún nivel conocido: el primero de alerta, sin romper.
        val noLevel = orFail(alert("""{"id":7,"code":"ALR-7","device":{"id":1},"event":null,"started_at":"2026-10-01T10:00:00-05:00","max_level":"MORADO"}"""))
        assertEquals(AlertLevel.YELLOW, noLevel.maxLevel)
        assertEquals(AlertEvent.UNKNOWN, noLevel.event)
    }

    @Test
    fun `an alert without any start date is skipped`() {
        assertNull(alert("""{"id":8,"code":"ALR-8","device":{"id":1},"event":"FUGA","started_at":"ayer","max_level":"ROJO"}"""))
    }

    @Test
    fun `the device name is kept as the backend sends it while the id still matches the map`() {
        val alert = orFail(alert("""{"id":9,"code":"ALR-9","device":{"id":11556,"pk":"Sensor PK34-470"},"event":"FUGA","started_at":"2026-10-01T10:00:00-05:00","max_level":"ROJO"}"""))

        assertEquals("Sensor PK34-470", alert.displayName)
        assertEquals("PK34-470", alert.pk)
        assertEquals("MP-11556", alert.sensorId)
    }

    @Test
    fun `a null device name falls back to the measuring point`() {
        val alert = orFail(alert("""{"id":9,"code":"ALR-9","device":{"id":11556,"pk":null},"event":"FUGA","started_at":"2026-10-01T10:00:00-05:00","max_level":"ROJO"}"""))

        assertEquals("MP-11556", alert.displayName)
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

    /** Amarillo → Naranja → Amarillo: el mismo color se repite en tramos separados (guía §5.4). */
    private val segmentsDetail = """
        {"id":142,"code":"ALR-000142","device":{"id":11556,"pk":"Sensor PK34-470"},"event":"FUGA","state":"ACTIVA",
         "started_at":"2026-11-20T08:00:00-05:00","in_progress":true,"current_level":"AMARILLO","max_level":"NARANJA",
         "levels":[
          {"seq":3,"opened_by":"DOWN","level":"AMARILLO","started_at":"2026-11-20T18:00:00-05:00","ended_at":null,
           "messages":[{"id":9020,"at":"2026-11-20T18:00:00-05:00","kind":"ACTUALIZACION","code":"DOWNGRADE","level":"AMARILLO",
             "title":"La alerta bajó a nivel Amarillo","body":"Línea 1\nLínea 2"}]},
          {"seq":1,"opened_by":"OPEN","level":"AMARILLO","started_at":"2026-11-20T08:00:00-05:00","ended_at":"2026-11-20T14:00:00-05:00",
           "messages":[
             {"id":9007,"at":"2026-11-20T12:00:00-05:00","kind":"ACTUALIZACION","code":"COUNTDOWN","level":"AMARILLO","indicator_value":0.21,"title":"Puede subir","body":"b"},
             {"id":9001,"at":"2026-11-20T08:00:00-05:00","kind":"NOTIFICACION","code":"OPEN","level":"AMARILLO","indicator_value":0.18,"title":"Alerta nueva","body":"a"}]},
          {"seq":2,"opened_by":"TELEPORT","level":"NARANJA","started_at":"2026-11-20T14:00:00-05:00","ended_at":"2026-11-20T18:00:00-05:00",
           "messages":[{"id":9010,"at":"2026-11-20T14:00:00-05:00","kind":"NUEVO_TIPO","code":"CODIGO_FUTURO","level":"NARANJA","title":"Subió","body":null},
             {"id":9011,"at":"2026-11-20T15:00:00-05:00","kind":"GESTION","code":"CLASSIFIED","level":"NARANJA","indicator_value":null,"title":"Alerta clasificada","body":""}]}
         ]}
    """.trimIndent()

    @Test
    fun `maps the level segments in seq order with their messages by time`() {
        val segments = orFail(alert(segmentsDetail)).levelSegments

        assertEquals(listOf(1, 2, 3), segments.map { it.seq })
        assertEquals(listOf(AlertLevel.YELLOW, AlertLevel.ORANGE, AlertLevel.YELLOW), segments.map { it.level })
        assertEquals(listOf(SegmentOpening.OPEN, SegmentOpening.UNKNOWN, SegmentOpening.DOWN), segments.map { it.openedBy })
        assertEquals(listOf(9001L, 9007L), segments[0].messages.map { it.id })
        assertEquals(MessageKind.NOTIFICATION, segments[0].messages[0].kind)
        assertEquals(0.18, segments[0].messages[0].indicatorValue!!, 1e-9)
        assertEquals(true, segments[2].isCurrent)
        assertEquals(false, segments[0].isCurrent)
        // El cuerpo llega sin modificar, con sus saltos de línea.
        assertEquals("Línea 1\nLínea 2", segments[2].messages.single().body)
    }

    @Test
    fun `unknown message kinds and codes are kept with the default style`() {
        val orange = orFail(alert(segmentsDetail)).levelSegments[1]

        assertEquals(MessageKind.UNKNOWN, orange.messages[0].kind)
        assertEquals("CODIGO_FUTURO", orange.messages[0].code)
        assertEquals("", orange.messages[0].body)
        assertEquals(MessageKind.MANAGEMENT, orange.messages[1].kind)
        assertNull(orange.messages[1].indicatorValue)
    }

    @Test
    fun `the color filter keeps every segment of that color without grouping them`() {
        val segments = orFail(alert(segmentsDetail)).levelSegments

        assertEquals(listOf(1, 3), segments.filteredByLevel(AlertLevel.YELLOW).map { it.seq })
        assertEquals(listOf(2), segments.filteredByLevel(AlertLevel.ORANGE).map { it.seq })
        assertTrue(segments.filteredByLevel(AlertLevel.RED).isEmpty())
        assertEquals(listOf(1, 2, 3), segments.filteredByLevel(null).map { it.seq })
    }

    @Test
    fun `a segment with an unknown level is kept without a level and without crashing`() {
        val alert = orFail(alert(segmentsDetail.replace("\"level\":\"NARANJA\",\"started_at\"", "\"level\":\"MORADO\",\"started_at\"")))

        assertNull(alert.levelSegments[1].level)
        assertEquals(3, alert.levelSegments.size)
    }
}
