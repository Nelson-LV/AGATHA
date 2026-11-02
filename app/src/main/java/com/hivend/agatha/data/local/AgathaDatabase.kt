package com.hivend.agatha.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    entities = [SyncQueueEntity::class],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AgathaDatabase : RoomDatabase() {

    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val NAME = "agatha.db"
    }
}
