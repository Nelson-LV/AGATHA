package com.hivend.agatha.ui.components

import com.hivend.agatha.ui.theme.AgathaTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.core.navigation.DEFAULT_HISTORY_DEVICE_ID
import androidx.compose.ui.res.stringResource

private fun BottomTab.icon(): ImageVector = when (this) {
    BottomTab.Alerts -> Icons.Filled.Notifications
    BottomTab.Devices -> Icons.Filled.Sensors
    BottomTab.Map -> Icons.Filled.Map
    BottomTab.History -> Icons.AutoMirrored.Filled.Assignment
    BottomTab.Sync -> Icons.Filled.Sync
}

/** Barra inferior de 5 pestañas persistente en las pantallas raíz (Figma + Dispositivos). */
@Composable
fun AgathaBottomNavBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        BottomTab.all.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon(), contentDescription = stringResource(tab.labelRes())) },
                label = { Text(stringResource(tab.labelRes())) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AgathaTheme.colors.brand,
                    selectedTextColor = AgathaTheme.colors.brand,
                    unselectedIconColor = AgathaTheme.colors.textTertiary,
                    unselectedTextColor = AgathaTheme.colors.textTertiary,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

/** Ruta concreta a la que navega cada pestaña (Historial abre el dispositivo por defecto). */
fun BottomTab.rootRoute(): String = when (this) {
    BottomTab.History -> BottomTab.History.buildRoute(DEFAULT_HISTORY_DEVICE_ID)
    else -> route
}
