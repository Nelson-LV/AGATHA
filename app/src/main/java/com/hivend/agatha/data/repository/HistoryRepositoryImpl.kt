package com.hivend.agatha.data.repository

import com.hivend.agatha.data.local.dao.HistoryDao
import com.hivend.agatha.data.local.entity.toDomain
import com.hivend.agatha.data.local.entity.toEntity
import com.hivend.agatha.data.remote.api.HistoryApi
import com.hivend.agatha.data.remote.dto.toDomain
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.repository.HistoryRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
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
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    retrofit: Retrofit,
) : HistoryRepository {

    private val api: HistoryApi = retrofit.create(HistoryApi::class.java)
    private val refreshMutex = Mutex()

    override fun observeHistoryForNode(nodeId: String): Flow<List<HistoryEvent>> = callbackFlow {
        val refreshJob = launch { refreshHistory(nodeId) }
        val cutoffEpochMillis = Instant.now().minus(HISTORY_WINDOW).toEpochMilli()
        val databaseJob = launch {
            historyDao.observeForNode(nodeId, cutoffEpochMillis)
                .map { entities -> entities.map { it.toDomain() } }
                .collect { events -> send(events) }
        }
        awaitClose {
            refreshJob.cancel()
            databaseJob.cancel()
        }
    }

    private suspend fun refreshHistory(nodeId: String) = refreshMutex.withLock {
        val through = Instant.now()
        val from = through.minus(HISTORY_WINDOW)
        val cutoffEpochMillis = from.toEpochMilli()
        try {
            historyDao.deleteOlderThan(cutoffEpochMillis)
            val events = api.getHistoryForNode(
                nodeId = nodeId,
                from = RFC3339_FORMATTER.format(from),
                through = RFC3339_FORMATTER.format(through),
            )
                .mapNotNull { dto -> dto.toDomain(nodeId) }
                .filter { !it.timestamp.isBefore(from) && !it.timestamp.isAfter(through) }
                .map { it.toEntity() }
            historyDao.upsertAll(events)
            historyDao.deleteOlderThan(cutoffEpochMillis)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Keep cached events available while offline or when the endpoint is unavailable.
        }
    }

    private companion object {
        val HISTORY_WINDOW = java.time.Duration.ofDays(30)
        val RFC3339_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)
    }
}
