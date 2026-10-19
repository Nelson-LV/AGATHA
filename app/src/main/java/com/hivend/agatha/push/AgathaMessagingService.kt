package com.hivend.agatha.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recibe los mensajes de Firebase Cloud Messaging (HU-4.1). El backend manda mensajes de
 * solo datos, así que este servicio corre también con la app en segundo plano o cerrada y
 * decide si avisar ([AlertPushParser]) y con qué texto ([AlertNotifier]).
 */
@AndroidEntryPoint
class AgathaMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notifier: AlertNotifier

    @Inject lateinit var tokenStore: PushTokenStore

    /** Token nuevo del celular (firebase-messaging 26+). */
    override fun onRegistered(token: String) {
        tokenStore.save(token)
    }

    override fun onUnregistered(token: String) {
        tokenStore.clear(token)
    }

    /** Ruta anterior a [onRegistered]; se atiende mientras el SDK siga enviando NEW_TOKEN. */
    @Deprecated("Replaced by onRegistered in firebase-messaging 26")
    override fun onNewToken(token: String) {
        tokenStore.save(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        AlertPushParser.parse(message.data)?.let(notifier::show)
    }
}
