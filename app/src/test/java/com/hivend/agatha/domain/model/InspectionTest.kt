package com.hivend.agatha.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class InspectionTest {

    private val now = Instant.parse("2026-10-10T15:00:00Z")
    private val app = Origin.MobileApp("CEL-01")

    private fun report(
        result: InspectionResult = InspectionResult.NO_ISSUES,
        resultOtherDetail: String? = null,
        alertId: String? = "ALR-0001",
        classification: AlertClassification? = null,
        tag: AlertTag? = null,
        tagOtherDetail: String? = null,
    ) = Inspection(
        id = "r1", deviceId = "MP-1", alertId = alertId, result = result, resultOtherDetail = resultOtherDetail,
        classification = classification, tag = tag, tagOtherDetail = tagOtherDetail, recordedAt = now, origin = app,
    )

    private val alert = Alert(
        id = "ALR-0001", pointNumber = 1, sensorId = "MP-1", pk = "PK1", site = "S", municipality = "M",
        event = AlertEvent.LEAK, description = LocalizedText.Plain(""), level = AlertLevel.RED,
        levelTimeline = listOf(LevelChange(AlertLevel.RED, now)), startedAt = now,
        telemetry = AlertTelemetry(90, AccelerationLevel.LOW, 80, isRealData = true),
    )

    @Test(expected = IllegalArgumentException::class)
    fun otherResultRequiresDetail() {
        report(result = InspectionResult.OTHER)
    }

    @Test(expected = IllegalArgumentException::class)
    fun reportWithoutAlertCannotClassify() {
        report(alertId = null, classification = AlertClassification.CONFIRMED)
    }

    @Test(expected = IllegalArgumentException::class)
    fun maintenanceNeedsAType() {
        MaintenanceRecord(types = emptySet())
    }

    @Test(expected = IllegalArgumentException::class)
    fun reportAcceptsAtMostFivePhotos() {
        report().copy(photos = List(6) { PhotoEvidence("uri$it", "") })
    }

    @Test
    fun reportClassifiesAndTagsTheAlertWithItsOrigin() {
        val updated = alert.applyInspection(
            report(classification = AlertClassification.FALSE_ALARM, tag = AlertTag.OTHER_FALSE_ALARM, tagOtherDetail = "Prueba de presión"),
        )
        assertEquals(ManagementStatus.CLASSIFIED_WITH_TAG, updated.managementStatus)
        assertEquals(app, updated.classification?.origin)
        assertEquals("Prueba de presión", updated.classification?.tag?.otherDetail)
        assertEquals(now, updated.endedAt)
    }

    @Test
    fun reportOnlyTagsAnAlreadyClassifiedAlert() {
        val classified = alert.classify(AlertClassification.CONFIRMED, now, Origin.Web)
        val updated = classified.applyInspection(report(tag = AlertTag.GROUND_MOVEMENT))
        assertEquals(Origin.Web, updated.classification?.origin)
        assertEquals(AlertTag.GROUND_MOVEMENT, updated.classification?.tag?.tag)
    }
}
