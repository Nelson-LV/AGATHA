package com.hivend.agatha.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hivend.agatha.core.navigation.AgathaDestination.AlertDetail
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.core.navigation.AgathaDestination.InspectionForm
import com.hivend.agatha.core.navigation.AgathaDestination.NotificationPreview
import com.hivend.agatha.core.navigation.AgathaDestination.PhotoEvidence
import com.hivend.agatha.ui.alertdetail.AlertDetailScreen
import com.hivend.agatha.ui.alerts.AlertInboxScreen
import com.hivend.agatha.ui.components.AgathaBottomNavBar
import com.hivend.agatha.ui.components.rootRoute
import com.hivend.agatha.ui.evidence.PhotoEvidenceScreen
import com.hivend.agatha.ui.history.DeviceHistoryScreen
import com.hivend.agatha.ui.inspection.InspectionFormScreen
import com.hivend.agatha.ui.map.SensorMapScreen
import com.hivend.agatha.ui.notification.NotificationPreviewScreen
import com.hivend.agatha.ui.sync.SyncScreen

/**
 * Composición raíz de AGATHA: un único [NavHost] con barra inferior condicional. Las 4
 * pestañas ([BottomTab]) muestran [AgathaBottomNavBar]; el resto de pantallas se apilan a
 * pantalla completa, igual que en el prototipo Figma. Ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Navegación" para el diagrama completo de rutas.
 */
@Composable
fun AgathaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = BottomTab.all.find { it.route == backStackEntry?.destination?.route }

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                AgathaBottomNavBar(currentTab = currentTab) { tab -> navController.navigateToTab(tab) }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NotificationPreview.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(NotificationPreview.route) {
                NotificationPreviewScreen(
                    onOpenDetail = { alertId ->
                        navController.navigate(BottomTab.Alerts.route)
                        navController.navigate(AlertDetail.buildRoute(alertId))
                    },
                )
            }

            composable(BottomTab.Alerts.route) {
                AlertInboxScreen(
                    onAlertClick = { alertId -> navController.navigate(AlertDetail.buildRoute(alertId)) },
                    onNavigateToMap = { navController.navigateToTab(BottomTab.Map) },
                    onNavigateToHistory = { navController.navigateToTab(BottomTab.History) },
                    onNavigateToSync = { navController.navigateToTab(BottomTab.Sync) },
                )
            }

            composable(BottomTab.Map.route) {
                SensorMapScreen(
                    onViewHistory = { deviceId -> navController.navigate(BottomTab.History.buildRoute(deviceId)) },
                )
            }

            composable(
                route = BottomTab.History.route,
                arguments = listOf(navArgument(BottomTab.History.ARG_DEVICE_ID) { type = NavType.StringType }),
            ) {
                DeviceHistoryScreen()
            }

            composable(BottomTab.Sync.route) {
                SyncScreen()
            }

            composable(
                route = AlertDetail.route,
                arguments = listOf(navArgument(AlertDetail.ARG_ALERT_ID) { type = NavType.StringType }),
            ) {
                AlertDetailScreen(
                    onBack = { navController.popBackStack() },
                    onRegisterInspection = { alertId -> navController.navigate(InspectionForm.buildRoute(alertId)) },
                    onViewHistory = { deviceId -> navController.navigate(BottomTab.History.buildRoute(deviceId)) },
                )
            }

            composable(
                route = InspectionForm.route,
                arguments = listOf(navArgument(InspectionForm.ARG_ALERT_ID) { type = NavType.StringType }),
            ) {
                InspectionFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { alertId -> navController.navigate(PhotoEvidence.buildRoute(alertId)) },
                )
            }

            composable(
                route = PhotoEvidence.route,
                arguments = listOf(navArgument(PhotoEvidence.ARG_ALERT_ID) { type = NavType.StringType }),
            ) {
                PhotoEvidenceScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { alertId ->
                        navController.popBackStack(route = AlertDetail.buildRoute(alertId), inclusive = false)
                    },
                )
            }
        }
    }
}

/** Cambia de pestaña conservando el estado de cada una (patrón estándar de bottom nav + Navigation Compose). */
private fun NavHostController.navigateToTab(tab: BottomTab) {
    navigate(tab.rootRoute()) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
