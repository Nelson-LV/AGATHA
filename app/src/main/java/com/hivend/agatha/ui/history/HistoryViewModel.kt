package com.hivend.agatha.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    historyRepository: HistoryRepository,
) : ViewModel() {

    val deviceId: String =
        checkNotNull(savedStateHandle[BottomTab.History.ARG_DEVICE_ID])

    val events: StateFlow<List<HistoryEvent>> = historyRepository.observeHistory(deviceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
