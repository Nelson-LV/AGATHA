package com.hivend.agatha.domain.model

/** Un registro creado sin conexión que todavía no llega a la API central (HE-07). */
data class RegistroPendiente(
    val id: String,
    val titulo: String,
    val detalle: String,
    val error: Boolean = false,
)

/** Conflicto detectado cuando la misma alerta cambió en la app y en la plataforma web. */
data class ConflictoSincronizacion(
    val alertaId: String,
    val tituloConflicto: String,
    val descripcion: String,
    val cambioLocal: String,
    val cambioServidor: String,
)

/** Estado agregado de la bandeja de sincronización, consumido por [com.hivend.agatha.ui.sync.SyncScreen]. */
data class EstadoSincronizacion(
    val conectado: Boolean,
    val pendientes: List<RegistroPendiente>,
    val conflicto: ConflictoSincronizacion?,
    val ultimaSincronizacionExitosa: String,
)
