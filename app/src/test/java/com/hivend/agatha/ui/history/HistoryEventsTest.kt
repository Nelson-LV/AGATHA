package com.hivend.agatha.ui.history

import com.hivend.agatha.R
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertHistoryPhase
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertObservation
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.LevelChange
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.model.MaintenanceRecord
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.ObservationKind
import com.hivend.agatha.domain.model.PhotoEvidence
import com.hivend.agatha.domain.model.TagRecord
import com.hivend.agatha.domain.repository.DeviceHistory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryEventsTest {

    private val origin = Origin.Web

    @Test
    fun eachAlertGetsAStartRowAndAClosedAlertGetsAnEndRowInDescendingOrder() {
        val start = Instant.parse("2026-09-26T23:00:00Z")
        val end = Instant.parse("2026-09-27T06:00:00Z")
        val alert = alert(id = "ALR-000007", startedAt = start, endedAt = end)

        val events = DeviceHistory(alerts = listOf(alert)).toEvents()

        assertEquals(listOf("ALR-000007:ended", "ALR-000007:started"), events.map(HistoryEvent::id))
        assertEquals(listOf(end, start), events.map(HistoryEvent::timestamp))
        assertEquals(listOf(AlertHistoryPhase.ENDED, AlertHistoryPhase.GENERATED), events.map(HistoryEvent::alertPhase))
        assertEquals(listOf(EventType.ALERT, EventType.ALERT), events.map(HistoryEvent::type))
        assertEquals("MP-11556", events.first().nodeId)
        assertEquals(LocalizedText.Resource(R.string.history_alert_ended), events.first().title)
        assertEquals(
            LocalizedText.Resource(
                R.string.history_alert_ended_detail,
                listOf(LocalizedText.Resource(R.string.level_red)),
            ),
            events.first().detail,
        )
        assertEquals(
            LocalizedText.Resource(
                R.string.history_alert_started,
                listOf(LocalizedText.Resource(R.string.level_yellow)),
            ),
            events.last().title,
        )
        assertEquals(
            LocalizedText.Resource(
                R.string.history_alert_detail,
                listOf("ALR-000007", LocalizedText.Resource(R.string.alert_event_leak)),
            ),
            events.last().detail,
        )
    }

    @Test
    fun ongoingAlertHasNoEndRow() {
        val events = DeviceHistory(
            alerts = listOf(alert(id = "ALR-000008", startedAt = Instant.parse("2026-09-27T06:00:00Z"))),
        ).toEvents()

        assertEquals(1, events.size)
        assertEquals(AlertHistoryPhase.GENERATED, events.single().alertPhase)
    }

    @Test
    fun generatedRowSeparatesManagementStatusClassificationAndTag() {
        val classifiedWithTag = alert(
            id = "ALR-000009",
            startedAt = Instant.parse("2026-09-27T06:00:00Z"),
            classification = ClassificationRecord(
                classification = AlertClassification.CONFIRMED,
                classifiedAt = Instant.parse("2026-09-27T07:00:00Z"),
                origin = origin,
                tag = TagRecord(
                    tag = AlertTag.LEAK_CONFIRMED_IN_FIELD,
                    otherDetail = null,
                    taggedAt = Instant.parse("2026-09-27T07:30:00Z"),
                    origin = origin,
                ),
            ),
        )
        val classifiedWithoutTag = alert(
            id = "ALR-000010",
            startedAt = Instant.parse("2026-09-27T08:00:00Z"),
            classification = ClassificationRecord(
                classification = AlertClassification.FALSE_ALARM,
                classifiedAt = Instant.parse("2026-09-27T08:30:00Z"),
                origin = origin,
            ),
        )
        val unclassified = alert("ALR-000011", Instant.parse("2026-09-27T09:00:00Z"))

        val generated = DeviceHistory(alerts = listOf(classifiedWithTag, classifiedWithoutTag, unclassified)).toEvents()
            .filter { it.alertPhase == AlertHistoryPhase.GENERATED }
            .associateBy(HistoryEvent::alertId)

        assertEquals(ManagementStatus.CLASSIFIED_WITH_TAG, generated.getValue("ALR-000009").alertManagementStatus)
        assertEquals(AlertClassification.CONFIRMED, generated.getValue("ALR-000009").alertClassification)
        assertEquals(
            LocalizedText.Resource(R.string.management_status_classified_with_tag),
            generated.getValue("ALR-000009").statusLabel,
        )
        assertEquals(
            LocalizedText.Resource(
                R.string.history_alert_tag_detail,
                listOf(LocalizedText.Resource(R.string.alert_tag_leak_confirmed_in_field)),
            ),
            generated.getValue("ALR-000009").alertTagLabel,
        )
        assertEquals(ManagementStatus.CLASSIFIED_WITHOUT_TAG, generated.getValue("ALR-000010").alertManagementStatus)
        assertEquals(AlertClassification.FALSE_ALARM, generated.getValue("ALR-000010").alertClassification)
        assertEquals(ManagementStatus.UNCLASSIFIED, generated.getValue("ALR-000011").alertManagementStatus)
        assertNull(generated.getValue("ALR-000011").alertClassification)
        assertEquals(
            LocalizedText.Resource(R.string.management_status_unclassified),
            generated.getValue("ALR-000011").statusLabel,
        )
        assertEquals(3, generated.size)
    }

    @Test
    fun inspectionProducesAnInspectionEventAtItsRecordedTimestamp() {
        val recordedAt = Instant.parse("2026-10-10T12:00:00Z")
        val report = Inspection(
            id = "report-1",
            deviceId = "MP-11556",
            alertId = null,
            recordedAt = recordedAt,
            origin = origin,
        )

        val event = DeviceHistory(inspections = listOf(report)).toEvents().single()

        assertEquals("inspection:report-1", event.id)
        assertEquals("MP-11556", event.nodeId)
        assertEquals(recordedAt, event.timestamp)
        assertEquals(EventType.INSPECTION, event.type)
        assertEquals(LocalizedText.Resource(R.string.history_inspection_title), event.title)
        assertEquals(LocalizedText.Resource(R.string.history_inspection_detail_default), event.detail)
    }

    @Test
    fun reportFieldsProduceObservationEvidenceAndMaintenanceEventsOnlyWhenPresent() {
        val recordedAt = Instant.parse("2026-10-10T12:00:00Z")
        val report = Inspection(
            id = "report-2",
            deviceId = "MP-11556",
            alertId = "ALR-000009",
            observations = "Se detecta erosión leve",
            maintenance = MaintenanceRecord(
                types = setOf(MaintenanceType.CLEANING),
                description = "Limpieza del sensor",
            ),
            photos = listOf(PhotoEvidence("uri-1", "Vista norte"), PhotoEvidence("uri-2", "Unión")),
            recordedAt = recordedAt,
            origin = origin,
        )

        val events = DeviceHistory(inspections = listOf(report)).toHistoryEvents()
        val byType = events.associateBy(HistoryEvent::type)

        assertEquals(
            setOf(EventType.INSPECTION, EventType.OBSERVATION, EventType.MAINTENANCE, EventType.EVIDENCE),
            events.map(HistoryEvent::type).toSet(),
        )
        assertEquals(LocalizedText.Plain("Se detecta erosión leve"), byType.getValue(EventType.OBSERVATION).detail)
        assertEquals(LocalizedText.Plain("Limpieza del sensor"), byType.getValue(EventType.MAINTENANCE).detail)
        assertEquals(
            listOf(
                LocalizedText.Resource(R.string.maintenance_type_cleaning),
                LocalizedText.Plain("Limpieza del sensor"),
            ),
            byType.getValue(EventType.MAINTENANCE).detailParts,
        )
        assertEquals(
            LocalizedText.Resource(R.string.history_maintenance_completed),
            byType.getValue(EventType.MAINTENANCE).statusLabel,
        )
        assertTrue(events.all { it.alertId == "ALR-000009" })
        assertEquals(
            listOf(
                LocalizedText.Resource(R.string.history_evidence_detail, listOf(2)),
                LocalizedText.Plain("Vista norte"),
                LocalizedText.Plain("Unión"),
            ),
            byType.getValue(EventType.EVIDENCE).detailParts,
        )
        assertTrue(events.all { it.timestamp == recordedAt })
    }

    @Test
    fun classificationProducesSeparateRowWhenTheApiProvidesAnEventTimestamp() {
        val classifiedAt = Instant.parse("2026-10-10T13:00:00Z")
        val classification = ClassificationRecord(
            classification = AlertClassification.CONFIRMED,
            classifiedAt = classifiedAt,
            origin = origin,
            tag = TagRecord(
                tag = AlertTag.GROUND_MOVEMENT,
                otherDetail = null,
                taggedAt = Instant.parse("2026-10-10T13:30:00Z"),
                origin = origin,
            ),
        )

        val event = DeviceHistory(
            alerts = listOf(alert("ALR-000011", Instant.parse("2026-10-10T12:00:00Z"), classification = classification)),
        ).toHistoryEvents().single { it.type == EventType.CLASSIFICATION }

        assertEquals(classifiedAt, event.timestamp)
        assertEquals("ALR-000011:classification", event.id)
        assertEquals(
            LocalizedText.Resource(
                R.string.history_classification_event_tag_detail,
                listOf(
                    LocalizedText.Resource(R.string.classification_confirmed),
                    LocalizedText.Resource(R.string.alert_tag_ground_movement),
                ),
            ),
            event.detail,
        )
        assertEquals(
            listOf(event.detail, LocalizedText.Resource(R.string.origin_web)),
            event.detailParts,
        )
    }

    @Test
    fun classificationWithoutTimestampStaysOnTheAlertChipWithoutAnInventedRow() {
        val alert = alert(
            "ALR-000014",
            Instant.parse("2026-10-10T12:00:00Z"),
            classification = ClassificationRecord(
                classification = AlertClassification.CONFIRMED,
                classifiedAt = null,
                origin = origin,
                tag = TagRecord(
                    tag = AlertTag.GROUND_MOVEMENT,
                    otherDetail = null,
                    taggedAt = Instant.parse("2026-10-10T13:30:00Z"),
                    origin = origin,
                ),
            ),
        )

        val events = DeviceHistory(alerts = listOf(alert)).toHistoryEvents()

        assertEquals(1, events.size)
        assertEquals(AlertClassification.CONFIRMED, events.single().alertClassification)
        assertFalse(events.any { it.type == EventType.CLASSIFICATION })
    }

    @Test
    fun alertObservationWithoutTimestampIsNotGivenAnInventedDate() {
        val alert = alert("ALR-000012", Instant.parse("2026-10-10T12:00:00Z")).copy(
            observations = listOf(
                AlertObservation(
                    id = 12,
                    text = "Nota de campo",
                    kind = ObservationKind.NOTE,
                    origin = origin,
                    recordedAt = null,
                ),
            ),
        )

        val events = DeviceHistory(alerts = listOf(alert)).toHistoryEvents()

        assertEquals(1, events.size)
        assertTrue(events.none { it.type == EventType.OBSERVATION })
    }

    @Test
    fun reportObservationIsNotDuplicatedAfterItAppearsInTheAlertApi() {
        val recordedAt = Instant.parse("2026-10-10T12:30:00Z")
        val text = "Se detecta erosión leve"
        val alert = alert("ALR-000013", Instant.parse("2026-10-10T12:00:00Z")).copy(
            observations = listOf(
                AlertObservation(13, text, ObservationKind.NOTE, origin, recordedAt),
            ),
        )
        val inspection = Inspection(
            id = "report-13",
            deviceId = "MP-11556",
            alertId = alert.id,
            observations = text,
            recordedAt = recordedAt,
            origin = origin,
        )

        val observations = DeviceHistory(alerts = listOf(alert), inspections = listOf(inspection))
            .toHistoryEvents()
            .filter { it.type == EventType.OBSERVATION }

        assertEquals(1, observations.size)
        assertEquals("inspection:report-13:observation", observations.single().id)
    }

    @Test
    fun recentHistoryConversionKeepsOnlyTheExistingAlertAndInspectionRows() {
        val alert = alert(
            id = "ALR-000015",
            startedAt = Instant.parse("2026-10-10T12:00:00Z"),
            classification = ClassificationRecord(
                classification = AlertClassification.CONFIRMED,
                classifiedAt = Instant.parse("2026-10-10T12:30:00Z"),
                origin = origin,
            ),
        ).copy(
            observations = listOf(
                AlertObservation(15, "Observación", ObservationKind.NOTE, origin, Instant.parse("2026-10-10T12:45:00Z")),
            ),
        )
        val inspection = Inspection(
            id = "report-15",
            deviceId = "MP-11556",
            alertId = alert.id,
            observations = "Observación de inspección",
            recordedAt = Instant.parse("2026-10-10T13:00:00Z"),
            origin = origin,
        )

        val events = DeviceHistory(alerts = listOf(alert), inspections = listOf(inspection)).toEvents()

        assertEquals(
            listOf("inspection:report-15", "ALR-000015:started"),
            events.map(HistoryEvent::id),
        )
        assertTrue(events.all { it.type == EventType.ALERT || it.type == EventType.INSPECTION })
    }

    private fun alert(
        id: String,
        startedAt: Instant,
        endedAt: Instant? = null,
        classification: ClassificationRecord? = null,
    ) = Alert(
        id = id,
        deviceId = 11556,
        pk = "PK34-470",
        event = AlertEvent.LEAK,
        level = AlertLevel.YELLOW,
        maxLevel = AlertLevel.RED,
        levelTimeline = listOf(LevelChange(AlertLevel.YELLOW, startedAt)),
        startedAt = startedAt,
        endedAt = endedAt,
        classification = classification,
    )
}
