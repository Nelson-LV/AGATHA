package com.hivend.agatha

import android.app.Application
import com.hivend.agatha.push.AlertNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Punto de entrada de Hilt: construye el grafo de dependencias @Singleton de toda la app. */
@HiltAndroidApp
class AgathaApplication : Application() {

    @Inject lateinit var alertNotifier: AlertNotifier

    override fun onCreate() {
        super.onCreate()
        // El canal debe existir antes del primer push (HU-4.1).
        alertNotifier.createChannel()
    }
}
