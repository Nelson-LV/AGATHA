package com.hivend.agatha.push

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hivend.agatha.R
import java.util.UUID
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Del `data` del push a la notificación visible: el mismo camino de [AgathaMessagingService]. */
@RunWith(AndroidJUnit4::class)
class AlertPushNotificationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = AlertNotifier(context)
    private val deduplicator = PushMessageDeduplicator(context, Json)

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        notifier.createChannel()
        manager.cancelAll()
    }

    @After
    fun tearDown() = manager.cancelAll()

    private fun deliver(data: Map<String, String>) {
        val push = AlertPushParser.parse(data) ?: return
        if (push.messageId == null || deduplicator.isFirstDelivery(push.messageId)) notifier.show(push)
    }

    private fun payload(alertId: String, level: String, kind: String, messageId: String = UUID.randomUUID().toString()) =
        mapOf("messageId" to messageId, "alertId" to alertId, "level" to level, "kind" to kind)

    /** El sistema publica la notificación de forma asíncrona: se espera a que aparezca. */
    private fun titleOf(alertId: String): String? {
        repeat(POLL_ATTEMPTS) {
            manager.activeNotifications.firstOrNull { it.id == alertId.hashCode() }
                ?.let { return it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() }
            Thread.sleep(POLL_MILLIS)
        }
        return null
    }

    @Test
    fun everyLevelAndKindShowsItsNotification() {
        val levels = mapOf(
            "YELLOW" to R.string.level_yellow,
            "ORANGE" to R.string.level_orange,
            "RED" to R.string.level_red,
        )
        val kinds = mapOf("NEW" to R.string.push_title_new, "LEVEL_UP" to R.string.push_title_level_up)

        for ((level, levelLabel) in levels) {
            for ((kind, title) in kinds) {
                val alertId = "ALR-$level-$kind"
                deliver(payload(alertId, level, kind))
                assertEquals(context.getString(title, alertId, context.getString(levelLabel)), titleOf(alertId))
            }
        }
    }

    @Test
    fun theSameMessageDeliveredTwiceShowsOnce() {
        val messageId = UUID.randomUUID().toString()
        deliver(payload("ALR-TWICE", "RED", "NEW", messageId))
        assertTrue(titleOf("ALR-TWICE") != null)

        manager.cancelAll()
        deliver(payload("ALR-TWICE", "RED", "NEW", messageId))

        Thread.sleep(POLL_MILLIS * POLL_ATTEMPTS)
        assertFalse(manager.activeNotifications.any { it.id == "ALR-TWICE".hashCode() })
    }

    private companion object {
        const val POLL_ATTEMPTS = 20
        const val POLL_MILLIS = 100L
    }
}
