package com.hivend.agatha.data.local.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hivend.agatha.data.local.AgathaDatabase
import com.hivend.agatha.domain.model.SyncState
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncQueueDaoTest {

    private lateinit var database: AgathaDatabase
    private lateinit var dao: SyncQueueDao

    private val t0 = Instant.parse("2026-10-29T15:00:00Z")

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AgathaDatabase::class.java,
        ).build()
        dao = database.syncQueueDao()
    }

    @After
    fun closeDatabase() = database.close()

    private fun entry(id: String, type: SyncRecordType, createdAt: Instant, dependsOn: String? = null) =
        SyncQueueEntity(id, type, recordId = "rec-$id", dependsOn = dependsOn, createdAt = createdAt)

    @Test
    fun unsyncedEntriesComeInRegistrationOrder() = runTest {
        dao.enqueue(entry("photo", SyncRecordType.PHOTO, t0.plusSeconds(120), dependsOn = "report"))
        dao.enqueue(entry("classification", SyncRecordType.CLASSIFICATION, t0))
        dao.enqueue(entry("report", SyncRecordType.INSPECTION_REPORT, t0.plusSeconds(60), dependsOn = "classification"))

        assertEquals(listOf("classification", "report", "photo"), dao.observeUnsynced().first().map { it.id })
    }

    @Test
    fun syncedEntriesLeaveTheUnsyncedList() = runTest {
        dao.enqueue(entry("a", SyncRecordType.OBSERVATION, t0))
        dao.enqueue(entry("b", SyncRecordType.OBSERVATION, t0.plusSeconds(1)))

        dao.updateState("a", SyncState.SYNCED, attempts = 1, lastError = null)

        assertEquals(listOf("b"), dao.observeUnsynced().first().map { it.id })
    }

    @Test
    fun aRecordIsEnqueuedOnlyOnce() = runTest {
        dao.enqueue(entry("first", SyncRecordType.PUSH_TOKEN, t0))
        val again = entry("second", SyncRecordType.PUSH_TOKEN, t0.plusSeconds(5)).copy(recordId = "rec-first")

        assertEquals(-1L, dao.enqueue(again))
        assertNull(dao.get("second"))
    }

    @Test
    fun failedAttemptKeepsCountAndReason() = runTest {
        dao.enqueue(entry("a", SyncRecordType.PHOTO, t0))

        dao.updateState("a", SyncState.ERROR, attempts = 3, lastError = "HTTP 500")

        val stored = dao.get("a")!!
        assertEquals(SyncState.ERROR, stored.state)
        assertEquals(3, stored.attempts)
        assertEquals("HTTP 500", stored.lastError)
        assertEquals(t0, stored.createdAt)
    }
}
