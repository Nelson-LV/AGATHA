package com.hivend.agatha.data.local

import android.content.Context
import androidx.room.Room
import com.hivend.agatha.data.local.sync.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Una sola [AgathaDatabase] por proceso y un `@Provides` por DAO, para inyectarlos en los repositorios. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AgathaDatabase =
        Room.databaseBuilder(context, AgathaDatabase::class.java, AgathaDatabase.NAME).build()

    @Provides
    fun provideSyncQueueDao(database: AgathaDatabase): SyncQueueDao = database.syncQueueDao()
}
