package com.hivend.agatha.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hivend.agatha.data.local.alert.AlertActionDao
import com.hivend.agatha.data.local.alert.AlertActionEntity
import com.hivend.agatha.data.local.alert.AlertConflictDao
import com.hivend.agatha.data.local.alert.AlertConflictEntity
import com.hivend.agatha.data.local.alert.InspectionPhotoEntity
import com.hivend.agatha.data.local.alert.InspectionReportDao
import com.hivend.agatha.data.local.alert.InspectionReportEntity
import com.hivend.agatha.data.local.cache.CacheInfoEntity
import com.hivend.agatha.data.local.cache.CachedAlertEntity
import com.hivend.agatha.data.local.cache.CachedDeviceEntity
import com.hivend.agatha.data.local.cache.OfflineCacheDao
import com.hivend.agatha.data.local.sync.SyncQueueDao
import com.hivend.agatha.data.local.sync.SyncQueueEntity

/**
 * Base de datos local de la app (offline-first, HE-07). Cada feature guarda sus entidades y su
 * DAO en su propio subpaquete (`data/local/sync`, `data/local/alert`…) y los expone aquí.
 *
 * Para agregar una tabla: sumar la entidad a `entities`, subir `version` con su migración
 * (`autoMigrations` o una `Migration` en [DatabaseModule]), agregar el getter del DAO y su
 * `@Provides` en [DatabaseModule]. El esquema de cada versión queda en `app/schemas/` y se
 * commitea.
 */
@Database(
    entities = [
        SyncQueueEntity::class,
        AlertActionEntity::class,
        InspectionReportEntity::class,
        InspectionPhotoEntity::class,
        AlertConflictEntity::class,
        CachedAlertEntity::class,
        CachedDeviceEntity::class,
        CacheInfoEntity::class,
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
@TypeConverters(Converters::class)
abstract class AgathaDatabase : RoomDatabase() {

    abstract fun syncQueueDao(): SyncQueueDao

    abstract fun alertActionDao(): AlertActionDao

    abstract fun inspectionReportDao(): InspectionReportDao

    abstract fun alertConflictDao(): AlertConflictDao

    abstract fun offlineCacheDao(): OfflineCacheDao

    companion object {
        const val NAME = "agatha.db"
    }
}
