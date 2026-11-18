package com.hivend.agatha.data.remote.alerts

import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.ManagementStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertSearchQueryTest {

    @Test
    fun `no filters send no params`() {
        assertEquals(AlertSearchQuery(), AlertSearchFilters().toQuery())
    }

    @Test
    fun `filters use the values of the backend guide`() {
        val query = AlertSearchFilters(
            deviceId = 12,
            from = LocalDate.parse("2026-11-01"),
            to = LocalDate.parse("2026-11-15"),
            maxLevels = setOf(AlertLevel.RED, AlertLevel.ORANGE),
            events = setOf(AlertEvent.LEAK),
            statuses = setOf(ManagementStatus.CLASSIFIED_WITH_TAG, ManagementStatus.UNCLASSIFIED),
        ).toQuery()

        assertEquals("12", query.device)
        assertEquals("2026-11-01", query.from)
        assertEquals("2026-11-15", query.to)
        // Siempre en el orden de las opciones, sin importar el orden en que se marcaron.
        assertEquals("NARANJA,ROJO", query.maxLevel)
        assertEquals("FUGA", query.event)
        assertEquals("SIN_CLASIFICAR,CLASIFICADA_CON_ETIQUETA", query.status)
    }

    @Test
    fun `every option maps to a backend value`() {
        val query = AlertSearchFilters(
            maxLevels = AlertSearchFilters.LEVEL_OPTIONS.toSet(),
            events = AlertSearchFilters.EVENT_OPTIONS.toSet(),
            statuses = ManagementStatus.entries.toSet(),
        ).toQuery()

        assertEquals("AMARILLO,NARANJA,ROJO", query.maxLevel)
        assertEquals("FUGA,MOVIMIENTO", query.event)
        assertEquals("SIN_CLASIFICAR,CLASIFICADA_SIN_ETIQUETA,CLASIFICADA_CON_ETIQUETA", query.status)
    }

    @Test
    fun `reads the file name of Content-Disposition`() {
        assertEquals(
            "alertas_20261117_1530.xlsx",
            fileNameFromContentDisposition("""attachment; filename="alertas_20261117_1530.xlsx""""),
        )
        assertEquals("alertas.xlsx", fileNameFromContentDisposition("attachment; filename=alertas.xlsx"))
        assertEquals("evil.xlsx", fileNameFromContentDisposition("""attachment; filename="../../evil.xlsx""""))
        assertNull(fileNameFromContentDisposition("attachment"))
        assertNull(fileNameFromContentDisposition(null))
    }
}
