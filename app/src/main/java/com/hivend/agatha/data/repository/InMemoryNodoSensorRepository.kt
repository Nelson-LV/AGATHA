package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.repository.NodoSensorRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class InMemoryNodoSensorRepository @Inject constructor() : NodoSensorRepository {

    private val nodos = MutableStateFlow(SampleAgathaData.nodos)

    override fun observarNodos() = nodos

    override fun observarNodo(nodoId: String) =
        nodos.map { lista -> lista.find { it.id == nodoId } }
}
