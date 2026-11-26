package com.hivend.agatha.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hivend.agatha.data.local.alert.AlertConflictEntity
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.SyncRecordType
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cada migración se prueba sobre una base creada con el esquema exportado de la versión anterior
 * (`app/schemas/`): los datos que ya estaban se conservan y las tablas nuevas quedan usables.
 */
@RunWith(AndroidJUnit4::class)
class AgathaDatabaseMigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AgathaDatabase::class.java)

    @Test
    fun migrate3To4KeepsTheQueueAndAddsTheConflicts() = runTest {
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL(
                "INSERT INTO sync_queue (id, recordType, recordId, dependsOn, state, attempts, lastError, createdAt) " +
                    "VALUES ('q1', 'CLASSIFICATION', 'rec-1', NULL, 'PENDING', 0, NULL, 1000)",
            )
        }

        helper.runMigrationsAndValidate(dbName, 4, true).close()

        val database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AgathaDatabase::class.java, dbName)
            .addMigrations(MIGRATION_2_3)
            .build()
        try {
            assertEquals("rec-1", database.syncQueueDao().get("q1")?.recordId)
            val conflicts = database.alertConflictDao()
            conflicts.insert(
                AlertConflictEntity(
                    id = "c1", alertId = "ALR-000001", type = SyncRecordType.CLASSIFICATION,
                    localClassification = AlertClassification.CONFIRMED, localTag = null, localTagDetail = null,
                    existingClassification = AlertClassification.FALSE_ALARM, existingClassifiedAt = Instant.EPOCH,
                    existingOrigin = "WEB", existingTag = null, existingTagDetail = null, detectedAt = Instant.EPOCH,
                ),
            )
            assertEquals(listOf("c1"), conflicts.observeOpen().first().map { it.id })
            conflicts.acknowledge("ALR-000001")
            assertEquals(emptyList<String>(), conflicts.observeOpen().first().map { it.id })
        } finally {
            database.close()
        }
    }
}
