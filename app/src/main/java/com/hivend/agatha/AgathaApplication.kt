package com.hivend.agatha

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de Hilt: construye el grafo de dependencias @Singleton de toda la app. */
@HiltAndroidApp
class AgathaApplication : Application()
