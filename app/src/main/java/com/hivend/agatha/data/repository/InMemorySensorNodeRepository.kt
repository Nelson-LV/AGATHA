package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.repository.SensorNodeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class InMemorySensorNodeRepository @Inject constructor() : SensorNodeRepository {

    private val nodes = MutableStateFlow(SampleAgathaData.nodes)

    override fun observeSensorNodes() = nodes

    override fun getSensorNodeById(nodeId: String) =
        nodes.map { list -> list.find { it.id == nodeId } }
}
