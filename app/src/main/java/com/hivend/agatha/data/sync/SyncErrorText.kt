package com.hivend.agatha.data.sync

import com.hivend.agatha.data.remote.ApiError
import com.hivend.agatha.domain.model.SyncError

/**
 * Cómo se guarda el motivo de un fallo en `sync_queue.lastError` y cómo se lee de vuelta. El
 * texto es técnico (`NO_CONNECTION`, `HTTP 400 INVALID_INPUT: …`): sirve para depurar, y la
 * pantalla muestra el [SyncError] traducido.
 */
internal object SyncErrorText {
    private const val NO_CONNECTION = "NO_CONNECTION"
    private const val INVALID_RESPONSE = "INVALID_RESPONSE"
    private val HTTP = Regex("""^HTTP (\d{3})\b""")

    fun encode(error: ApiError): String = when (error) {
        ApiError.NoConnection -> NO_CONNECTION
        ApiError.InvalidResponse -> INVALID_RESPONSE
        is ApiError.Http -> listOfNotNull("HTTP ${error.code}", error.body?.let { "${it.code}: ${it.message}" })
            .joinToString(" ")
    }

    fun decode(text: String?): SyncError? = when {
        text == null -> null
        text == NO_CONNECTION -> SyncError.NoConnection
        else -> HTTP.find(text)?.let { SyncError.Server(it.groupValues[1].toInt()) } ?: SyncError.InvalidResponse
    }
}
