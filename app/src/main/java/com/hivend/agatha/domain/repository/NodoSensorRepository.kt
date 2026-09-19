package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.NodoSensor
import kotlinx.coroutines.flow.Flow

/** Puerto hacia los nodos de sensores mostrados en el mapa de campo (HE-08). */
interface NodoSensorRepository {
    fun observarNodos(): Flow<List<NodoSensor>>
    fun observarNodo(nodoId: String): Flow<NodoSensor?>
}
