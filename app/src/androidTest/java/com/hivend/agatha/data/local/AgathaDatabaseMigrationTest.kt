package com.hivend.agatha.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hivend.agatha.data.local.alert.AlertConflictEntity
import com.hivend.agatha.data.local.cache.RoomOfflineCache
import com.hivend.agatha.data.remote.alerts.AlertDto
import com.hivend.agatha.data.remote.alerts.DeviceRefDto
import com.hivend.agatha.data.remote.alerts.ObservationDto
import com.hivend.agatha.domain.model.TelemetryDevice
import kotlinx.serialization.json.Json
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

    @Test
    fun migrate4To5KeepsTheConflictsAndAddsTheOfflineCache() = runTest {
        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL(
                "INSERT INTO alert_conflicts (id, alertId, type, localClassification, localTag, localTagDetail, " +
                    "existingClassification, existingClassifiedAt, existingOrigin, existingTag, existingTagDetail, detectedAt, acknowledged) " +
                    "VALUES ('c1', 'ALR-000001', 'CLASSIFICATION', 'CONFIRMED', NULL, NULL, 'FALSE_ALARM', 0, 'WEB', NULL, NULL, 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(dbName, 5, true).close()

        val database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AgathaDatabase::class.java, dbName)
            .addMigrations(MIGRATION_2_3)
            .build()
        try {
            assertEquals(listOf("c1"), database.alertConflictDao().observeOpen().first().map { it.id })

            // La caché guarda y devuelve lo descargado: la lista, el detalle y los dispositivos.
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }
            val cache = RoomOfflineCache(database, json)
            val row = AlertDto(id = 1, code = "ALR-000001", device = DeviceRefDto(11556, "Sensor PK34-470"), event = "FUGA", startedAt = "2026-11-20T08:00:00-05:00", maxLevel = "ROJO")
            cache.saveList(listOf(row), Instant.EPOCH)
            cache.saveDetail(row.copy(observations = listOf(ObservationDto(id = 9, text = "Revisado"))))
            cache.saveDevices(listOf(TelemetryDevice(11556, "Sensor MP-11556 PK34-470", Instant.EPOCH, 87.0)), Instant.EPOCH)

            val saved = cache.loadAlerts().single()
            assertEquals(true, saved.inList)
            assertEquals(null, saved.row)
            assertEquals("Revisado", saved.detail!!.observations.single().text)
            assertEquals(Instant.EPOCH, cache.observeUpdatedAt().first())
            assertEquals(87.0, cache.loadDevices()!!.devices.single().lastBatteryLevel!!, 1e-9)

            cache.saveList(emptyList(), Instant.EPOCH.plusSeconds(60))
            assertEquals(emptyList<String>(), cache.loadAlerts().map { it.detail?.code })
        } finally {
            database.close()
        }
    }
}
