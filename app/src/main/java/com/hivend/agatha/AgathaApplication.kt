package com.hivend.agatha

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hivend.agatha.data.sync.SyncScheduler
import com.hivend.agatha.push.AlertNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Punto de entrada de Hilt: construye el grafo de dependencias @Singleton de toda la app. */
@HiltAndroidApp
class AgathaApplication : Application(), Configuration.Provider {

    @Inject lateinit var alertNotifier: AlertNotifier
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler

    // WorkManager crea los workers con la fábrica de Hilt para que reciban sus dependencias.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // El canal debe existir antes del primer push (HU-4.1).
        alertNotifier.createChannel()
        // Lo que quedó pendiente de una sesión anterior sale en cuanto haya red (HU-7.3).
        syncScheduler.schedule()
    }
}
