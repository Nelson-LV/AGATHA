package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.ManagementStatus
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lo elegido en el Historial de alertas. [sensorId] es el dispositivo ("MP-11556") o null para
 * todos.
 */
data class HistoryFilterSelection(
    val sensorId: String? = null,
    val dateFrom: LocalDate? = null,
    val dateThrough: LocalDate? = null,
    val maxLevels: Set<AlertLevel> = emptySet(),
    val alertEvents: Set<AlertEvent> = emptySet(),
    val statuses: Set<ManagementStatus> = emptySet(),
    val listScrollIndex: Int = 0,
    val listScrollOffset: Int = 0,
)

/** Keeps the last history filters while the user switches between bottom tabs. */
@Singleton
class HistorySelectionStore @Inject constructor() {
    private val mutableSelection = MutableStateFlow(HistoryFilterSelection())

    val selection: StateFlow<HistoryFilterSelection> = mutableSelection.asStateFlow()

    /** Elige el dispositivo, o todos con null. Cambiar de dispositivo vuelve al inicio de la lista. */
    fun select(sensorId: String?) = change { it.copy(sensorId = sensorId) }

    fun updateDates(dateFrom: LocalDate?, dateThrough: LocalDate?) = change {
        it.copy(dateFrom = dateFrom, dateThrough = dateThrough)
    }

    fun updateAlertFilters(
        maxLevels: Set<AlertLevel>,
        alertEvents: Set<AlertEvent>,
        statuses: Set<ManagementStatus>,
    ) = change { it.copy(maxLevels = maxLevels, alertEvents = alertEvents, statuses = statuses) }

    fun updateListScrollPosition(index: Int, offset: Int) {
        mutableSelection.value = mutableSelection.value.copy(
            listScrollIndex = index.coerceAtLeast(0),
            listScrollOffset = offset.coerceAtLeast(0),
        )
    }

    /** Un filtro distinto es otra lista: se vuelve a la primera fila (y a la página 1). */
    private fun change(transform: (HistoryFilterSelection) -> HistoryFilterSelection) {
        val current = mutableSelection.value
        val next = transform(current)
        if (next != current) mutableSelection.value = next.copy(listScrollIndex = 0, listScrollOffset = 0)
    }
}
