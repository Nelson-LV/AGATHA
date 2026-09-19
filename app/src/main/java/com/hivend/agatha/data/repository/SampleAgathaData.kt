package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.model.Alerta
import com.hivend.agatha.domain.model.EstadoAlerta
import com.hivend.agatha.domain.model.EventoHistorial
import com.hivend.agatha.domain.model.NivelAlerta
import com.hivend.agatha.domain.model.NodoSensor
import com.hivend.agatha.domain.model.TelemetriaAlerta
import com.hivend.agatha.domain.model.TipoEvento

/**
 * Datos de muestra usados por los repositorios en memoria mientras HE-04..HE-08 no
 * consumen todavía la API central real (ver docs/ARQUITECTURA_Y_DISENO.md § "Datos de
 * ejemplo vs. API real"). El contenido reproduce exactamente los prototipos de Figma
 * (sitio Güepsa – San José de Pare, sensores MP-11xx) para que la demo de navegación se
 * sienta como el diseño aprobado por 0G Colombia.
 */
internal object SampleAgathaData {

    const val SITIO = "Güepsa – San José de Pare"

    val alertas: List<Alerta> = listOf(
        Alerta(
            id = "MP-1156",
            punto = "Punto 3",
            sensorId = "MP-1156",
            pk = "PK37+800",
            sitio = SITIO,
            descripcion = "Riesgo de deslizamiento — El tubo se movió 5 cm de su posición original",
            nivel = NivelAlerta.ROJO,
            estado = EstadoAlerta.EN_INSPECCION,
            tiempoRelativo = "hace 5 min",
            telemetria = TelemetriaAlerta(
                confianzaPorcentaje = 96,
                aceleracion = "Alta",
                tiempoRelativo = "5 min",
                bateriaPorcentaje = 78,
                esDatoReal = true,
            ),
            siguientePaso = "Registrar inspección de campo",
        ),
        Alerta(
            id = "MP-1189",
            punto = "Punto 2",
            sensorId = "MP-1189",
            pk = "PK22+300",
            sitio = SITIO,
            descripcion = "Posible fuga de gas",
            nivel = NivelAlerta.AMARILLO,
            estado = EstadoAlerta.RECIBIDA,
            tiempoRelativo = "hace 40 min",
            telemetria = TelemetriaAlerta(
                confianzaPorcentaje = 81,
                aceleracion = "Media",
                tiempoRelativo = "40 min",
                bateriaPorcentaje = 64,
                esDatoReal = true,
            ),
            siguientePaso = "Iniciar inspección",
        ),
        Alerta(
            id = "MP-1122",
            punto = "Punto 1",
            sensorId = "MP-1122",
            pk = "PK10+050",
            sitio = SITIO,
            descripcion = "Escenario de prueba — simulación",
            nivel = NivelAlerta.VERDE,
            estado = EstadoAlerta.CLASIFICADA,
            tiempoRelativo = "hace 2 h",
            telemetria = TelemetriaAlerta(
                confianzaPorcentaje = 100,
                aceleracion = "Baja",
                tiempoRelativo = "2 h",
                bateriaPorcentaje = 91,
                esDatoReal = false,
            ),
            siguientePaso = "Revisar clasificación",
        ),
        Alerta(
            id = "MP-1201",
            punto = "Punto 4",
            sensorId = "MP-1201",
            pk = "PK44+120",
            sitio = SITIO,
            descripcion = "Condiciones normales",
            nivel = NivelAlerta.VERDE,
            estado = EstadoAlerta.CERRADA,
            tiempoRelativo = "hace 3 h",
            telemetria = TelemetriaAlerta(
                confianzaPorcentaje = 98,
                aceleracion = "Baja",
                tiempoRelativo = "3 h",
                bateriaPorcentaje = 85,
                esDatoReal = true,
            ),
            siguientePaso = "Sin acción pendiente",
        ),
    )

    val nodos: List<NodoSensor> = listOf(
        NodoSensor(
            id = "MP-1156",
            nombre = "Nodo MP-1156",
            pk = "PK37+800",
            sitio = SITIO,
            tipoSensor = "Acelerómetro + giroscopio",
            estado = NivelAlerta.ROJO,
            bateriaPorcentaje = 78,
            ultimaComunicacion = "hace 5 min",
            xNormalizado = 0.78f,
            yNormalizado = 0.22f,
        ),
        NodoSensor(
            id = "MP-1189",
            nombre = "Nodo MP-1189",
            pk = "PK22+300",
            sitio = SITIO,
            tipoSensor = "Acústico",
            estado = NivelAlerta.AMARILLO,
            bateriaPorcentaje = 64,
            ultimaComunicacion = "hace 40 min",
            xNormalizado = 0.45f,
            yNormalizado = 0.48f,
        ),
        NodoSensor(
            id = "MP-1122",
            nombre = "Nodo MP-1122",
            pk = "PK10+050",
            sitio = SITIO,
            tipoSensor = "Condiciones ambientales",
            estado = NivelAlerta.VERDE,
            bateriaPorcentaje = 91,
            ultimaComunicacion = "hace 2 h",
            xNormalizado = 0.20f,
            yNormalizado = 0.75f,
        ),
        NodoSensor(
            id = "MP-1201",
            nombre = "Nodo MP-1201",
            pk = "PK44+120",
            sitio = SITIO,
            tipoSensor = "Energía / batería",
            estado = NivelAlerta.VERDE,
            bateriaPorcentaje = 85,
            ultimaComunicacion = "hace 3 h",
            xNormalizado = 0.83f,
            yNormalizado = 0.80f,
        ),
    )

    fun historialPara(dispositivoId: String): List<EventoHistorial> = when (dispositivoId) {
        "MP-1156" -> listOf(
            EventoHistorial("12:42", TipoEvento.ALERTA, "Alerta roja generada", "Riesgo de deslizamiento — el tubo se movió 5 cm", "Pendiente de confirmar"),
            EventoHistorial("13:05", TipoEvento.INSPECCION, "Inspección registrada", "Resultado: mantenimiento requerido"),
            EventoHistorial("13:07", TipoEvento.CLASIFICACION, "Clasificación del evento", "Movimiento de tierra o desprendimiento menor"),
            EventoHistorial("13:10", TipoEvento.EVIDENCIA, "Evidencia adjunta", "2 fotografías con descripción"),
            EventoHistorial("13:12", TipoEvento.OBSERVACION, "Observación de campo", "Se detecta erosión leve en el talud norte"),
            EventoHistorial("13:20", TipoEvento.MANTENIMIENTO, "Mantenimiento realizado", "Ajuste de anclaje y limpieza del sensor", "Completado"),
            EventoHistorial("13:25", TipoEvento.ALERTA, "Alerta cerrada", "Estado final: Clasificada — sin anomalías adicionales", "Cerrada"),
        )
        else -> listOf(
            EventoHistorial("08:50", TipoEvento.ALERTA, "Condiciones normales", "Todos los puntos estables"),
        )
    }
}
