package com.hivend.agatha.core.di

import com.hivend.agatha.data.repository.InMemoryAlertRepository
import com.hivend.agatha.data.repository.InMemoryHistoryRepository
import com.hivend.agatha.data.repository.InMemorySensorNodeRepository
import com.hivend.agatha.data.repository.InMemorySyncRepository
import com.hivend.agatha.domain.repository.AlertRepository
import com.hivend.agatha.domain.repository.HistoryRepository
import com.hivend.agatha.domain.repository.SensorNodeRepository
import com.hivend.agatha.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Cablea cada puerto de dominio (patrón Repository) a su adaptador concreto (patrón
 * Singleton + Inyección de Dependencias vía Hilt). Ningún ViewModel referencia una clase
 * `InMemory*Repository` directamente: solo la interfaz. Cuando la implementación real con
 * Room/Retrofit esté lista, solo este archivo cambia.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: InMemoryAlertRepository): AlertRepository

    @Binds
    @Singleton
    abstract fun bindSensorNodeRepository(impl: InMemorySensorNodeRepository): SensorNodeRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: InMemoryHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: InMemorySyncRepository): SyncRepository
}
