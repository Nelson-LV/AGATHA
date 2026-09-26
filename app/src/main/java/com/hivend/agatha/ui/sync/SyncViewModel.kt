package com.hivend.agatha.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.domain.model.SyncStatus
import com.hivend.agatha.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
) : ViewModel() {

    val status: StateFlow<SyncStatus?> = syncRepository.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun syncNow() {
        viewModelScope.launch { syncRepository.syncNow() }
    }

    fun retry(recordId: String) {
        viewModelScope.launch { syncRepository.retry(recordId) }
    }

    fun confirmConflict(alertId: String) {
        viewModelScope.launch { syncRepository.confirmConflict(alertId) }
    }
}
