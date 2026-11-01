package com.hivend.agatha.core.di

import com.hivend.agatha.data.repository.AlertHistoryRepository
import com.hivend.agatha.data.repository.InMemorySensorNodeRepository
import com.hivend.agatha.data.repository.InMemorySyncRepository
import com.hivend.agatha.data.repository.RemoteAlertRepository
import com.hivend.agatha.data.repository.RemoteTelemetryRepository
import com.hivend.agatha.domain.repository.AlertRepository
import com.hivend.agatha.domain.repository.HistoryRepository
import com.hivend.agatha.domain.repository.SensorNodeRepository
import com.hivend.agatha.domain.repository.SyncRepository
import com.hivend.agatha.domain.repository.TelemetryRepository
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
    abstract fun bindAlertRepository(impl: RemoteAlertRepository): AlertRepository

    @Binds
    @Singleton
    abstract fun bindSensorNodeRepository(impl: InMemorySensorNodeRepository): SensorNodeRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: AlertHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: InMemorySyncRepository): SyncRepository

    @Binds
    @Singleton
    abstract fun bindTelemetryRepository(impl: RemoteTelemetryRepository): TelemetryRepository
}
