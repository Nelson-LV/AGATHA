package com.hivend.agatha.core.di

import com.hivend.agatha.data.repository.InMemoryAlertaRepository
import com.hivend.agatha.data.repository.InMemoryHistorialRepository
import com.hivend.agatha.data.repository.InMemoryNodoSensorRepository
import com.hivend.agatha.data.repository.InMemorySincronizacionRepository
import com.hivend.agatha.domain.repository.AlertaRepository
import com.hivend.agatha.domain.repository.HistorialRepository
import com.hivend.agatha.domain.repository.NodoSensorRepository
import com.hivend.agatha.domain.repository.SincronizacionRepository
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
    abstract fun bindAlertaRepository(impl: InMemoryAlertaRepository): AlertaRepository

    @Binds
    @Singleton
    abstract fun bindNodoSensorRepository(impl: InMemoryNodoSensorRepository): NodoSensorRepository

    @Binds
    @Singleton
    abstract fun bindHistorialRepository(impl: InMemoryHistorialRepository): HistorialRepository

    @Binds
    @Singleton
    abstract fun bindSincronizacionRepository(impl: InMemorySincronizacionRepository): SincronizacionRepository
}
