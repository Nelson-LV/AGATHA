package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.repository.SensorNodeRepository
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.domain.model.SensorType
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class InMemorySensorNodeRepository @Inject constructor() : SensorNodeRepository {

    private val nodes = MutableStateFlow(
        listOf(
            SensorNode(
                id = "MP-11556",
                code = "MP-11556",
                name = "Sensor MP-11556",
                pk = "34+470",
                municipality = SampleAgathaData.MUNICIPALITY,
                latitude = NODE_ONE_LATITUDE,
                longitude = NODE_ONE_LONGITUDE,
                nodeStatus = SensorNodeStatus.NORMAL,
                batteryLevel = 100,
                lastReportTimestamp = Instant.now().minusSeconds(10 * 60L),
                site = SampleAgathaData.SITE,
                sensorType = SensorType.MOTION,
            ),
            SensorNode(
                id = "Punto 2",
                code = "Punto 2",
                name = "Punto 2",
                pk = "38+174",
                municipality = SampleAgathaData.MUNICIPALITY,
                latitude = NODE_TWO_LATITUDE,
                longitude = NODE_TWO_LONGITUDE,
                nodeStatus = SensorNodeStatus.NO_COMMUNICATION,
                site = SampleAgathaData.SITE,
            ),
            SensorNode(
                id = "Punto 3",
                code = "Punto 3",
                name = "Punto 3",
                pk = "39+268",
                municipality = SampleAgathaData.MUNICIPALITY,
                latitude = NODE_THREE_LATITUDE,
                longitude = NODE_THREE_LONGITUDE,
                nodeStatus = SensorNodeStatus.NO_COMMUNICATION,
                site = SampleAgathaData.SITE,
            ),
        ),
    )

    override fun observeSensorNodes() = nodes

    override fun getSensorNodeById(nodeId: String) =
        nodes.map { list -> list.find { it.id == nodeId } }

    private companion object {
        // Estimated route-interpolated coordinates; replace with exact web-platform coordinates when available.
        const val NODE_ONE_LATITUDE = 6.029448
        const val NODE_ONE_LONGITUDE = -73.562440
        const val NODE_TWO_LATITUDE = 6.028749
        const val NODE_TWO_LONGITUDE = -73.539053
        const val NODE_THREE_LATITUDE = 6.035016
        const val NODE_THREE_LONGITUDE = -73.533595
    }
}
