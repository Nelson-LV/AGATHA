package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.repository.HistorialRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf

@Singleton
class InMemoryHistorialRepository @Inject constructor() : HistorialRepository {

    override fun observarHistorial(dispositivoId: String) =
        flowOf(SampleAgathaData.historialPara(dispositivoId))
}
