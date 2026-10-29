package com.hivend.agatha.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore(name = "agatha_sync")

/** Última sincronización correcta, la que muestra la pantalla de sincronización (HU-7.1). */
@Singleton
class LastSyncStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val lastSuccessfulSync: Flow<Instant?> =
        context.syncDataStore.data.map { prefs -> prefs[LAST_SYNC]?.let(Instant::ofEpochMilli) }

    suspend fun markSynced(at: Instant) {
        context.syncDataStore.edit { it[LAST_SYNC] = at.toEpochMilli() }
    }

    private companion object {
        val LAST_SYNC = longPreferencesKey("last_successful_sync")
    }
}
