package com.hivend.agatha.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hivend.agatha.MainActivity
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.ui.components.COLOMBIA
import com.hivend.agatha.ui.components.labelRes
import com.hivend.agatha.ui.components.shortLabelRes
import com.hivend.agatha.ui.theme.LightAgathaColors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Muestra el push de una alerta (HU-4.1) en el canal de su nivel, con su propio sonido. Título y cuerpo salen de
 * strings.xml, así que la notificación sale en el idioma del dispositivo. Al tocarla abre
 * [MainActivity] con [EXTRA_ALERT_ID], que navega al detalle de la alerta.
 */
@Singleton
class AlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * Crea un canal por nivel, cada uno con su sonido de `res/raw`, agrupados bajo "Alertas".
     * Se llama al arrancar la app. Android fija el sonido de un canal al crearlo, por eso cada
     * nivel tiene su propio canal (y un id nuevo si algún día cambia el sonido) y se borra el
     * canal único anterior, que sonaba igual para todos los niveles.
     */
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannelGroup(
            NotificationChannelGroup(CHANNEL_GROUP_ID, context.getString(R.string.push_channel_alerts_name)),
        )
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        LevelChannel.entries.forEach { level ->
            val channel = NotificationChannel(
                level.id,
                context.getString(level.nameRes),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(level.descriptionRes)
                group = CHANNEL_GROUP_ID
                setSound(soundUri(level.soundRes), audio)
                enableVibration(true)
                vibrationPattern = level.vibration
            }
            manager.createNotificationChannel(channel)
        }
        LEGACY_CHANNEL_IDS.forEach(manager::deleteNotificationChannel)
    }

    fun show(push: AlertPush) {
        // Sin permiso la app sigue funcionando; la lista de alertas avisa y lleva a ajustes.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, push.level.channel().id)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setColor(push.level.notificationColor())
            .setContentTitle(title(push))
            .setContentText(body(push))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body(push)))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAlert(push.alertId, push.messageId))
            .build()
        // Un id por alerta: si sube de nivel, la nueva notificación reemplaza a la anterior.
        NotificationManagerCompat.from(context).notify(push.alertId.hashCode(), notification)
    }

    private fun title(push: AlertPush): String = when (push.kind) {
        AlertPushKind.NEW ->
            context.getString(R.string.push_title_new, push.alertId, context.getString(push.level.shortLabelRes()))
        AlertPushKind.LEVEL_UP ->
            context.getString(R.string.push_title_level_up, push.alertId, context.getString(push.level.shortLabelRes()))
    }

    /** "Sensor PK34-470 · PK 34+470 · Fuga · Inicio 18/10/2026 14:30", omitiendo lo que no llegó. */
    private fun body(push: AlertPush): String {
        val parts = listOfNotNull(
            push.sensorId,
            push.pk?.let { context.getString(R.string.push_body_pk, it) },
            push.event?.let { context.getString(it.labelRes()) },
            push.startedAt?.let { context.getString(R.string.push_body_started, dateFormatter().format(it)) },
        )
        return parts.reduceOrNull { acc, part -> context.getString(R.string.common_dot_separated, acc, part) }
            .orEmpty()
    }

    private fun dateFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern(
            context.getString(R.string.common_date_time_pattern),
            context.resources.configuration.locales[0],
        ).withZone(COLOMBIA)

    /** Abre el detalle de la alerta en el mensaje del push (guía §8.3). */
    private fun openAlert(alertId: String, messageId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ALERT_ID, alertId)
            .putExtra(EXTRA_MESSAGE_ID, messageId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun soundUri(@RawRes sound: Int): Uri =
        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$sound")

    /** Mismo color de nivel que los chips de la app (RN-03), en su variante clara. */
    private fun AlertLevel.notificationColor(): Int = when (this) {
        AlertLevel.RED -> LightAgathaColors.critical
        AlertLevel.ORANGE -> LightAgathaColors.warning
        AlertLevel.YELLOW -> LightAgathaColors.caution
        AlertLevel.GREEN -> LightAgathaColors.positive
    }.toArgb()

    /** Canal de cada nivel con su sonido: más urgente cuanto más alto el nivel (RN-01). */
    internal enum class LevelChannel(
        val id: String,
        @param:StringRes val nameRes: Int,
        @param:StringRes val descriptionRes: Int,
        @param:RawRes val soundRes: Int,
        val vibration: LongArray,
    ) {
        RED(
            "agatha_alerts_red_v1", R.string.push_channel_red_name, R.string.push_channel_red_description,
            R.raw.alert_red, longArrayOf(0, 400, 150, 400, 150, 400),
        ),
        ORANGE(
            "agatha_alerts_orange_v1", R.string.push_channel_orange_name, R.string.push_channel_orange_description,
            R.raw.alert_orange, longArrayOf(0, 300, 200, 300),
        ),
        YELLOW(
            "agatha_alerts_yellow_v1", R.string.push_channel_yellow_name, R.string.push_channel_yellow_description,
            R.raw.alert_yellow, longArrayOf(0, 250),
        ),
    }

    companion object {
        private const val CHANNEL_GROUP_ID = "agatha_alerts_group"

        /** Canales de versiones anteriores que se borran al arrancar. */
        private val LEGACY_CHANNEL_IDS = listOf("agatha_alerts")

        /** Verde no genera push (RN-02); si llegara, usa el canal más suave. */
        internal fun AlertLevel.channel(): LevelChannel = when (this) {
            AlertLevel.RED -> LevelChannel.RED
            AlertLevel.ORANGE -> LevelChannel.ORANGE
            AlertLevel.YELLOW, AlertLevel.GREEN -> LevelChannel.YELLOW
        }

        const val EXTRA_ALERT_ID = "com.hivend.agatha.extra.ALERT_ID"
        const val EXTRA_MESSAGE_ID = "com.hivend.agatha.extra.MESSAGE_ID"
    }
}
