package com.hivend.agatha.data.local.alert

import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.MaintenanceRecord
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.PhotoEvidence
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class InspectionReportEntityTest {

    @Test
    fun `a report with maintenance and photos survives the round trip to its tables`() {
        val report = Inspection(
            id = "report-1",
            deviceId = "MP-11556",
            alertId = "ALR-000007",
            tag = AlertTag.GROUND_MOVEMENT,
            observations = "Erosión en el talud",
            maintenance = MaintenanceRecord(
                types = setOf(MaintenanceType.CLEANING, MaintenanceType.OTHER),
                otherDetail = "Poda",
                description = "Se limpió el sensor",
            ),
            photos = listOf(PhotoEvidence("file:///a.jpg", "Talud"), PhotoEvidence("file:///b.jpg", "")),
            recordedAt = Instant.parse("2026-11-01T20:53:00Z"),
            origin = Origin.MobileApp("CEL-TEST01"),
        )

        // Room no garantiza el orden de las fotos: la posición lo recupera.
        val restored = report.toEntity().toDomain(report.photoEntities().reversed())

        assertEquals(report, restored)
    }

    @Test
    fun `a report without maintenance keeps it empty`() {
        val report = Inspection(
            id = "report-2", deviceId = "MP-11556", alertId = null, observations = "Nota",
            recordedAt = Instant.parse("2026-11-01T20:53:00Z"), origin = Origin.MobileApp("CEL-TEST01"),
        )

        assertEquals(report, report.toEntity().toDomain(emptyList()))
    }
}
