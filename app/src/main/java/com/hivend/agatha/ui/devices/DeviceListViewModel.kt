package com.hivend.agatha.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.repository.SensorNodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Lista de dispositivos del tramo. Ordena primero los nodos que requieren atención (rojo,
 * luego naranja) para que el técnico vea arriba lo que necesita revisar.
 */
@HiltViewModel
class DeviceListViewModel @Inject constructor(
    sensorNodeRepository: SensorNodeRepository,
) : ViewModel() {

    val nodes: StateFlow<List<SensorNode>> = sensorNodeRepository.observeNodes()
        .map { list -> list.sortedByDescending { it.status.ordinal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
