package com.hivend.agatha.domain.repository

import kotlinx.coroutines.flow.Flow

/** Puerto hacia el estado de la red del celular (HU-7.1): si hay internet utilizable ahora. */
interface ConnectivityMonitor {
    /** Emite el estado actual al suscribirse y luego cada vez que cambia. */
    val isOnline: Flow<Boolean>
}
