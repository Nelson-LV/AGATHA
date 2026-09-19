package com.hivend.agatha.domain.model

/** Resultado observado sobre el dispositivo/sensor durante la inspección (HU-5.1). */
enum class ResultadoInspeccion(val etiqueta: String) {
    SIN_NOVEDAD("Sin novedad"),
    MANTENIMIENTO_REQUERIDO("Mantenimiento requerido"),
    PROBLEMA_DISPOSITIVO("Problema del dispositivo/sensor"),
    DANIO_FISICO("Daño físico"),
    BATERIA_BAJA("Batería baja"),
    SIN_COMUNICACION("Sin comunicación"),
    OTRA("Otra"),
}

/** Categoría configurable que clasifica la causa real de la alerta (HU-5.2). */
enum class CategoriaEvento(val etiqueta: String) {
    MOVIMIENTO_TIERRA("Movimiento de tierra o desprendimiento menor"),
    INTERVENCION_MAQUINARIA("Intervención de maquinaria ajena"),
    CONDICIONES_CLIMATICAS("Condiciones climáticas externas"),
    TRANSITO_VEHICULO_PESADO("Tránsito de vehículo pesado"),
    SIN_ANOMALIAS("Sin anomalías"),
    MANTENIMIENTO_PROGRAMADO("Mantenimiento programado"),
    FUGA_CONFIRMADA("Fuga confirmada"),
    OTRO("Otro (especificar)"),
}

/**
 * Registro de inspección de campo (HE-05). Se guarda localmente primero (offline-first,
 * HE-07) y se sincroniza cuando hay conectividad — ver [com.hivend.agatha.data.sync].
 */
data class Inspeccion(
    val alertaId: String,
    val resultado: ResultadoInspeccion,
    val categoria: CategoriaEvento,
    val observaciones: String,
    val registradaSinConexion: Boolean,
)
