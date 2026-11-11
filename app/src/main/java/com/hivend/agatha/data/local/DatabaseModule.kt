package com.hivend.agatha.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hivend.agatha.data.local.alert.AlertActionDao
import com.hivend.agatha.data.local.sync.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * El registro del token FCM salió de la cola: se borra su fila, porque el tipo PUSH_TOKEN ya no
 * existe y Room no podría leerla.
 */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM sync_queue WHERE recordType = 'PUSH_TOKEN'")
    }
}

/** Una sola [AgathaDatabase] por proceso y un `@Provides` por DAO, para inyectarlos en los repositorios. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AgathaDatabase =
        Room.databaseBuilder(context, AgathaDatabase::class.java, AgathaDatabase.NAME)
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides
    fun provideAlertActionDao(database: AgathaDatabase): AlertActionDao = database.alertActionDao()

    @Provides
    fun provideSyncQueueDao(database: AgathaDatabase): SyncQueueDao = database.syncQueueDao()
}
