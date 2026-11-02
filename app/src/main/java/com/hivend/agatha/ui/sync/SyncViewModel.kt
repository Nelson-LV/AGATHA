package com.hivend.agatha.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.SyncStatus
import com.hivend.agatha.domain.repository.ConnectivityMonitor
import com.hivend.agatha.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {

    val status: StateFlow<SyncStatus?> = syncRepository.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val messageChannel = Channel<Int>(Channel.BUFFERED)

    /** Avisos de una sola vez para el snackbar de la pantalla, como id de recurso (`@StringRes`). */
    val messages: Flow<Int> = messageChannel.receiveAsFlow()

    fun syncNow() = send { syncRepository.syncNow() }

    fun retry(recordId: String) = send { syncRepository.retry(recordId) }

    fun retryAll() = send { syncRepository.retryAll() }

    fun confirmConflict(alertId: String) {
        viewModelScope.launch { syncRepository.confirmConflict(alertId) }
    }

    private fun send(request: suspend () -> Unit) {
        viewModelScope.launch {
            request()
            // CA-7.3.3: sin red el envío queda programado y sale solo al recuperarla.
            if (!connectivity.isOnline.first()) messageChannel.send(R.string.sync_retry_offline)
        }
    }
}
