package com.hivend.agatha.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
 * Muestra el push de una alerta (HU-4.1) en el canal de alertas. Título y cuerpo salen de
 * strings.xml, así que la notificación sale en el idioma del dispositivo. Al tocarla abre
 * [MainActivity] con [EXTRA_ALERT_ID], que navega al detalle de la alerta.
 */
@Singleton
class AlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Crea (o actualiza el nombre de) el canal de alertas. Se llama al arrancar la app. */
    fun createChannel() {
        val channel = NotificationChannel(
            context.getString(R.string.push_channel_alerts_id),
            context.getString(R.string.push_channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.push_channel_alerts_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(push: AlertPush) {
        // Sin permiso la app sigue funcionando; la lista de alertas avisa y lleva a ajustes.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, context.getString(R.string.push_channel_alerts_id))
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setColor(push.level.notificationColor())
            .setContentTitle(title(push))
            .setContentText(body(push))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body(push)))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAlert(push.alertId))
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

    /** "Dispositivo MP-11556 · PK 12+300 · Fuga · Inicio 18/10/2026 14:30", omitiendo lo que no llegó. */
    private fun body(push: AlertPush): String {
        val parts = listOfNotNull(
            push.sensorId?.let { context.getString(R.string.push_body_device, it) },
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

    private fun openAlert(alertId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ALERT_ID, alertId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Mismo color de nivel que los chips de la app (RN-03), en su variante clara. */
    private fun AlertLevel.notificationColor(): Int = when (this) {
        AlertLevel.RED -> LightAgathaColors.critical
        AlertLevel.ORANGE -> LightAgathaColors.warning
        AlertLevel.YELLOW -> LightAgathaColors.caution
        AlertLevel.GREEN -> LightAgathaColors.positive
    }.toArgb()

    companion object {
        const val EXTRA_ALERT_ID = "com.hivend.agatha.extra.ALERT_ID"
    }
}
