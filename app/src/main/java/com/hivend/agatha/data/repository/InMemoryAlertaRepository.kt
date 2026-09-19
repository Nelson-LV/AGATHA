package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.model.Alerta
import com.hivend.agatha.domain.model.EstadoAlerta
import com.hivend.agatha.domain.model.EvidenciaFoto
import com.hivend.agatha.domain.model.Inspeccion
import com.hivend.agatha.domain.repository.AlertaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Implementación en memoria de [AlertaRepository]. Es @Singleton porque todas las
 * pantallas deben observar el mismo estado mutable de alertas (patrón Singleton +
 * Observer vía [kotlinx.coroutines.flow.StateFlow] — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Patrones de diseño").
 *
 * Se reemplazará por una implementación respaldada por Room + Retrofit en el sprint de
 * integración con la API central, sin que domain/ui necesiten cambiar una sola línea.
 */
@Singleton
class InMemoryAlertaRepository @Inject constructor() : AlertaRepository {

    private val alertas: MutableStateFlow<List<Alerta>> =
        MutableStateFlow(SampleAgathaData.alertas)

    override fun observarAlertas(): StateFlow<List<Alerta>> = alertas

    override fun observarAlerta(alertaId: String) =
        alertas.map { lista -> lista.find { it.id == alertaId } }

    override suspend fun actualizarEstado(alertaId: String, nuevoEstado: EstadoAlerta) {
        alertas.update { lista ->
            lista.map { if (it.id == alertaId) it.copy(estado = nuevoEstado) else it }
        }
    }

    override suspend fun registrarInspeccion(inspeccion: Inspeccion) {
        actualizarEstado(inspeccion.alertaId, EstadoAlerta.CLASIFICADA)
    }

    override suspend fun registrarEvidencias(alertaId: String, evidencias: List<EvidenciaFoto>) {
        // Sprint 3: persistir evidencias en Room y encolarlas en data/sync para su subida.
    }
}
