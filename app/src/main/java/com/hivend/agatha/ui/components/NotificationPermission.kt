package com.hivend.agatha.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hivend.agatha.R
import com.hivend.agatha.ui.theme.AgathaTheme

/**
 * Permiso de notificaciones para el push de alertas (HU-4.1). Con minSdk 33 siempre hay que
 * pedirlo en tiempo de ejecución. La primera vez explica el motivo antes de pedirlo; si no se
 * concede, la app sigue funcionando y muestra una franja con acceso a los ajustes del sistema.
 * El estado se vuelve a leer al regresar a la app (por si se activó desde ajustes).
 */
@Composable
fun NotificationPermissionNotice(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasNotificationPermission()) }
    var showRationale by remember { mutableStateOf(!granted && !context.wasNotificationPermissionAsked()) }

    LifecycleResumeEffect(Unit) {
        granted = context.hasNotificationPermission()
        onPauseOrDispose {}
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
    }

    if (showRationale) {
        val dismiss = {
            showRationale = false
            context.markNotificationPermissionAsked()
        }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.notifications_permission_title)) },
            text = { Text(stringResource(R.string.notifications_permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    dismiss()
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.notifications_permission_allow)) }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(R.string.notifications_permission_later)) }
            },
        )
    }

    if (!granted && !showRationale) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .fillMaxWidth()
                .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        ) {
            Icon(Icons.Outlined.NotificationsOff, contentDescription = null, tint = AgathaTheme.colors.warning)
            Text(
                stringResource(R.string.notifications_disabled_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = AgathaTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { context.openNotificationSettings() }) {
                Text(stringResource(R.string.notifications_open_settings))
            }
        }
    }
}

private fun Context.hasNotificationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.permissionPrefs() = getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)

private fun Context.wasNotificationPermissionAsked(): Boolean =
    permissionPrefs().getBoolean(KEY_NOTIFICATIONS_ASKED, false)

private fun Context.markNotificationPermissionAsked() =
    permissionPrefs().edit { putBoolean(KEY_NOTIFICATIONS_ASKED, true) }

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
    )
}

private const val PERMISSION_PREFS = "agatha_permissions"
private const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"
