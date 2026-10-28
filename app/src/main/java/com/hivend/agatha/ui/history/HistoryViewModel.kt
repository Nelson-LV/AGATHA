package com.hivend.agatha.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    val deviceId: String =
        checkNotNull(savedStateHandle[BottomTab.History.ARG_DEVICE_ID])

    val events: StateFlow<List<HistoryEvent>> = historyRepository.observeHistory(deviceId)
        .map { it.toEvents() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Falla de la última descarga del historial; null = al día. */
    private val _failure = MutableStateFlow<AlertFailure?>(null)
    val failure: StateFlow<AlertFailure?> = _failure

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _failure.value = (historyRepository.refresh(deviceId) as? AlertResult.Failure)?.failure
        }
    }
}
