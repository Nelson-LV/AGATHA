package com.hivend.agatha.push

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Recuerda los `messageId` ya mostrados: FCM puede entregar el mismo push dos veces. Se guardan
 * en disco porque la segunda entrega puede llegar con la app cerrada, en otro proceso.
 */
@Singleton
class PushMessageDeduplicator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /** true la primera vez que llega [messageId]; false si ya se había entregado. */
    @Synchronized
    fun isFirstDelivery(messageId: String): Boolean {
        val updated = rememberMessageId(seen(), messageId) ?: return false
        // Síncrono: el sistema puede matar el proceso apenas termina onMessageReceived.
        prefs.edit(commit = true) { putString(KEY_SEEN, json.encodeToString(updated)) }
        return true
    }

    private fun seen(): List<String> = prefs.getString(KEY_SEEN, null)
        ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
        .orEmpty()

    private companion object {
        const val PREFS = "agatha_push_seen"
        const val KEY_SEEN = "message_ids"
    }
}

/** Ids recientes que se recuerdan: de sobra para una entrega repetida, sin crecer sin límite. */
private const val MAX_REMEMBERED_MESSAGES = 200

/** [seen] con [messageId] agregado (conservando los más recientes), o null si ya estaba. */
internal fun rememberMessageId(
    seen: List<String>,
    messageId: String,
    max: Int = MAX_REMEMBERED_MESSAGES,
): List<String>? = if (messageId in seen) null else (seen + messageId).takeLast(max)
