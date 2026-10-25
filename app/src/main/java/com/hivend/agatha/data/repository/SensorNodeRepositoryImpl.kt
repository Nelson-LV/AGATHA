package com.hivend.agatha.data.repository

import com.hivend.agatha.data.local.dao.SensorNodeDao
import com.hivend.agatha.data.local.entity.toDomain
import com.hivend.agatha.data.local.entity.toEntity
import com.hivend.agatha.data.remote.api.SensorNodeApi
import com.hivend.agatha.data.remote.dto.toDomain
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.repository.SensorNodeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class SensorNodeRepositoryImpl @Inject constructor(
    private val sensorNodeDao: SensorNodeDao,
    retrofit: Retrofit,
) : SensorNodeRepository {

    private val api: SensorNodeApi = retrofit.create(SensorNodeApi::class.java)
    private val refreshMutex = Mutex()

    override fun observeSensorNodes(): Flow<List<SensorNode>> = callbackFlow {
        val refreshJob = launch { refreshNodes() }
        val databaseJob = launch {
            sensorNodeDao.observeAll()
                .map { entities -> entities.map { it.toDomain() } }
                .collect { nodes -> send(nodes) }
        }
        awaitClose {
            refreshJob.cancel()
            databaseJob.cancel()
        }
    }

    override fun getSensorNodeById(nodeId: String): Flow<SensorNode?> =
        observeSensorNodes().map { nodes -> nodes.firstOrNull { it.id == nodeId } }

    private suspend fun refreshNodes() = refreshMutex.withLock {
        try {
            val entities = api.getSensorNodes()
                .map { it.toDomain() }
                .map { it.toEntity() }
            sensorNodeDao.upsertAll(entities)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Room remains the source of truth when the API cannot be reached.
        }
    }
}
