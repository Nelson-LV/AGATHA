package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.SensorNode
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistorySiteCatalogTest {

    @Test
    fun siteOnlyOffersTheLocallyMappedMeasuringPoints() {
        val nodes = listOf(
            SensorNode(id = "MP-11556", name = "Sensor MP-11556", pk = "PK34-470", municipality = "San José de Pare"),
            SensorNode(id = "MP-1189", name = "Sensor MP-1189", pk = "PK22-300", municipality = "San José de Pare"),
            SensorNode(id = "Punto 2", name = "Punto 2", pk = "PK38-174", municipality = "San José de Pare"),
        )

        val options = HistorySiteCatalog.sensorOptions(HistorySiteCatalog.GUEPSA, nodes)

        assertEquals(setOf("MP-11556", "MP-1189", "MP-1122"), options.map(HistorySensorOption::id).toSet())
        assertEquals("Sensor MP-11556", options.first { it.id == "MP-11556" }.label)
        assertEquals("MP-1122", options.first { it.id == "MP-1122" }.label)
        assertNull(HistorySiteCatalog.siteForSensor("Punto 2"))
    }

    @Test
    fun selectedSensorFilterDropsEventsFromOtherSensors() {
        val events = listOf(
            event(id = "one", sensor = "MP-11556"),
            event(id = "two", sensor = "MP-1189"),
        )

        assertEquals(listOf("one"), events.forSensor("MP-11556").map(HistoryEvent::id))
        assertEquals(emptyList<HistoryEvent>(), events.forSensor(null))
    }

    private fun event(id: String, sensor: String) = HistoryEvent(
        id = id,
        nodeId = sensor,
        timestamp = Instant.EPOCH,
        type = EventType.ALERT,
        description = "",
    )
}
