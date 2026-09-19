package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.Alerta
import com.hivend.agatha.domain.model.EstadoAlerta
import com.hivend.agatha.domain.model.EvidenciaFoto
import com.hivend.agatha.domain.model.Inspeccion
import kotlinx.coroutines.flow.Flow

/**
 * Puerto (patrón Repository) hacia el origen de datos de alertas. La UI y los ViewModels
 * dependen únicamente de esta interfaz — nunca de Room ni de Retrofit directamente — para
 * que la fuente real de datos pueda cambiarse (in-memory hoy, Room+Retrofit en Sprint 3)
 * sin tocar la capa de presentación. Ver docs/ARQUITECTURA_Y_DISENO.md § "Repository".
 */
interface AlertaRepository {
    fun observarAlertas(): Flow<List<Alerta>>
    fun observarAlerta(alertaId: String): Flow<Alerta?>
    suspend fun actualizarEstado(alertaId: String, nuevoEstado: EstadoAlerta)
    suspend fun registrarInspeccion(inspeccion: Inspeccion)
    suspend fun registrarEvidencias(alertaId: String, evidencias: List<EvidenciaFoto>)
}
