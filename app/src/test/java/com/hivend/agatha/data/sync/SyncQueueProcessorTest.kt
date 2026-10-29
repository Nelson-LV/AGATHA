package com.hivend.agatha.data.sync

import com.hivend.agatha.data.local.sync.SyncQueueDao
import com.hivend.agatha.data.local.sync.SyncQueueEntity
import com.hivend.agatha.data.remote.ApiError
import com.hivend.agatha.data.remote.ApiErrorBody
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.domain.model.SyncState
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncQueueProcessorTest {

    /** Cola en memoria con la misma semántica que el DAO de Room. */
    private class FakeQueue : SyncQueueDao {
        val entries = linkedMapOf<String, SyncQueueEntity>()

        override suspend fun enqueue(entry: SyncQueueEntity): Long {
            if (entries.values.any { it.recordType == entry.recordType && it.recordId == entry.recordId }) return -1
            entries[entry.id] = entry
            return entries.size.toLong()
        }

        override suspend fun get(id: String) = entries[id]

        override fun observeUnsynced(): Flow<List<SyncQueueEntity>> = flowOf(unsyncedNow())

        override suspend fun unsynced() = unsyncedNow()

        override suspend fun updateState(id: String, state: SyncState, attempts: Int, lastError: String?) {
            entries[id] = entries.getValue(id).copy(state = state, attempts = attempts, lastError = lastError)
        }

        override suspend fun resetError(id: String): Int {
            val entry = entries[id]?.takeIf { it.state == SyncState.ERROR } ?: return 0
            entries[id] = entry.copy(state = SyncState.PENDING, attempts = 0, lastError = null)
            return 1
        }

        override suspend fun resetAllErrors(): Int =
            entries.values.filter { it.state == SyncState.ERROR }.sumOf { resetError(it.id) }

        private fun unsyncedNow() = entries.values.filter { it.state != SyncState.SYNCED }.sortedBy { it.createdAt }
    }

    /** Responde con [results] en orden (el último se repite) y anota qué registros envió. */
    private class FakeSender(
        override val type: SyncRecordType,
        vararg results: ApiResult<Unit>,
    ) : SyncRecordSender {
        private val pending = ArrayDeque(results.toList())
        val sent = mutableListOf<String>()

        override suspend fun send(recordId: String): ApiResult<Unit> {
            sent += recordId
            return if (pending.size > 1) pending.removeFirst() else pending.first()
        }
    }

    private val ok = ApiResult.Success(Unit)
    private val offline = ApiResult.Failure(ApiError.NoConnection)
    private val t0 = Instant.parse("2026-10-29T15:00:00Z")
    private val queue = FakeQueue()

    private fun enqueue(id: String, type: SyncRecordType, second: Long, dependsOn: String? = null) = runBlocking {
        queue.enqueue(SyncQueueEntity(id, type, "rec-$id", dependsOn, t0.plusSeconds(second)))
    }

    private fun processor(vararg senders: SyncRecordSender) = SyncQueueProcessor(queue, senders.toSet())

    private fun state(id: String) = queue.entries.getValue(id).state

    @Test
    fun `sends in registration order, a dependency before what depends on it`() = runBlocking {
        enqueue("photo", SyncRecordType.PHOTO, 120, dependsOn = "report")
        enqueue("classification", SyncRecordType.CLASSIFICATION, 0)
        enqueue("report", SyncRecordType.INSPECTION_REPORT, 60, dependsOn = "classification")
        val classifications = FakeSender(SyncRecordType.CLASSIFICATION, ok)
        val reports = FakeSender(SyncRecordType.INSPECTION_REPORT, ok)
        val photos = FakeSender(SyncRecordType.PHOTO, ok)

        val run = processor(classifications, reports, photos).process()

        assertEquals(SyncRun(sent = 3, retryLater = false), run)
        assertEquals(listOf("rec-photo"), photos.sent)
        assertTrue(queue.entries.values.all { it.state == SyncState.SYNCED && it.attempts == 1 })
    }

    @Test
    fun `a record waits while its dependency has not synced`() = runBlocking {
        enqueue("report", SyncRecordType.INSPECTION_REPORT, 0)
        enqueue("photo", SyncRecordType.PHOTO, 60, dependsOn = "report")
        val photos = FakeSender(SyncRecordType.PHOTO, ok)

        processor(FakeSender(SyncRecordType.INSPECTION_REPORT, offline), photos).process()

        assertEquals(SyncState.PENDING, state("report"))
        assertEquals(SyncState.PENDING, state("photo"))
        assertTrue(photos.sent.isEmpty())
    }

    @Test
    fun `a retryable failure stays pending until three retries, then becomes an error`() = runBlocking {
        enqueue("a", SyncRecordType.OBSERVATION, 0)
        val processor = processor(FakeSender(SyncRecordType.OBSERVATION, offline))

        repeat(SyncQueueProcessor.MAX_RETRIES) {
            assertTrue(processor.process().retryLater)
            assertEquals(SyncState.PENDING, state("a"))
        }
        assertFalse(processor.process().retryLater)

        val entry = queue.entries.getValue("a")
        assertEquals(SyncState.ERROR, entry.state)
        assertEquals(1 + SyncQueueProcessor.MAX_RETRIES, entry.attempts)
        assertEquals("NO_CONNECTION", entry.lastError)
    }

    @Test
    fun `a server rejection is an error at once, with the backend reason`() = runBlocking {
        enqueue("a", SyncRecordType.CLASSIFICATION, 0)
        val rejected = ApiResult.Failure(ApiError.Http(400, ApiErrorBody("INVALID_INPUT", "tag")))

        val run = processor(FakeSender(SyncRecordType.CLASSIFICATION, rejected)).process()

        assertFalse(run.retryLater)
        assertEquals(SyncState.ERROR, state("a"))
        assertEquals("HTTP 400 INVALID_INPUT: tag", queue.entries.getValue("a").lastError)
    }

    @Test
    fun `an entry already in error is only resent by a manual retry`() = runBlocking {
        enqueue("a", SyncRecordType.PHOTO, 0)
        queue.updateState("a", SyncState.ERROR, attempts = 4, lastError = "NO_CONNECTION")
        val photos = FakeSender(SyncRecordType.PHOTO, ok)

        processor(photos).process()

        assertTrue(photos.sent.isEmpty())
        assertEquals(SyncState.ERROR, state("a"))
    }

    @Test
    fun `a type without a sender waits pending and does not block the others`() = runBlocking {
        enqueue("token", SyncRecordType.PUSH_TOKEN, 0)
        enqueue("note", SyncRecordType.OBSERVATION, 60)

        val run = processor(FakeSender(SyncRecordType.OBSERVATION, ok)).process()

        assertEquals(1, run.sent)
        assertEquals(SyncState.PENDING, state("token"))
        assertEquals(SyncState.SYNCED, state("note"))
    }

    @Test
    fun `an entry left syncing by a killed process is sent again`() = runBlocking {
        enqueue("a", SyncRecordType.OBSERVATION, 0)
        queue.updateState("a", SyncState.SYNCING, attempts = 0, lastError = null)

        processor(FakeSender(SyncRecordType.OBSERVATION, ok)).process()

        assertEquals(SyncState.SYNCED, state("a"))
    }

    @Test
    fun `a manual retry after a network failure sends the record and it syncs`() = runBlocking {
        enqueue("a", SyncRecordType.CLASSIFICATION, 0)
        val sender = FakeSender(SyncRecordType.CLASSIFICATION, offline, offline, offline, offline, ok)
        val processor = processor(sender)
        repeat(1 + SyncQueueProcessor.MAX_RETRIES) { processor.process() }
        assertEquals(SyncState.ERROR, state("a"))

        // "Reintentar" (HU-7.3): vuelve a Pendiente con los reintentos automáticos de nuevo.
        assertEquals(1, queue.resetError("a"))
        processor.process()

        val entry = queue.entries.getValue("a")
        assertEquals(SyncState.SYNCED, entry.state)
        assertEquals(null, entry.lastError)
        assertEquals(5, sender.sent.size)
    }
}
