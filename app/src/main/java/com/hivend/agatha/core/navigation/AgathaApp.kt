package com.hivend.agatha.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavBackStackEntry
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
import com.hivend.agatha.ui.alertdetail.AlertDetailScreen
import com.hivend.agatha.ui.alerts.AlertInboxScreen
import com.hivend.agatha.ui.components.AgathaBottomNavBar
import com.hivend.agatha.ui.components.rootRoute
import com.hivend.agatha.ui.devices.DeviceListScreen
import com.hivend.agatha.ui.history.DeviceHistoryScreen
import com.hivend.agatha.ui.inspection.InspectionFormScreen
import com.hivend.agatha.ui.map.SensorMapScreen
import com.hivend.agatha.ui.sync.SyncScreen

/**
 * Composición raíz de AGATHA: un único [NavHost] con barra inferior condicional. Las 5
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
            startDestination = BottomTab.Alerts.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { if (isTabSwitch()) tabEnter() else forwardEnter() },
            exitTransition = { if (isTabSwitch()) tabExit() else forwardExit() },
            popEnterTransition = { if (isTabSwitch()) tabEnter() else backEnter() },
            popExitTransition = { if (isTabSwitch()) tabExit() else backExit() },
        ) {
            composable(BottomTab.Alerts.route) {
                AlertInboxScreen(
                    onAlertClick = { alertId -> navController.navigate(AlertDetail.buildRoute(alertId)) },
                    onNavigateToMap = { navController.navigateToTab(BottomTab.Map) },
                    onNavigateToHistory = { navController.navigateToTab(BottomTab.History) },
                    onNavigateToSync = { navController.navigateToTab(BottomTab.Sync) },
                )
            }

            composable(BottomTab.Devices.route) {
                DeviceListScreen(
                    onViewHistory = { deviceId -> navController.navigate(BottomTab.History.buildRoute(deviceId)) },
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
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}

/*
 * Transiciones (Material motion). Entre pestañas: "fade through" corto, porque son destinos
 * hermanos sin relación espacial. Al apilar una pantalla (detalle, reporte): "shared axis X",
 * la nueva entra desde la derecha y la anterior se desplaza un poco a la izquierda; al volver,
 * al revés. Reemplaza el fundido largo por defecto de Navigation Compose.
 */
private const val TAB_FADE_OUT_MS = 90
private const val TAB_FADE_IN_MS = 210
private const val AXIS_MS = 300
private const val AXIS_FADE_MS = 150

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route.isTabRoute() && targetState.destination.route.isTabRoute()

private fun String?.isTabRoute(): Boolean = BottomTab.all.any { it.route == this }

private fun tabEnter(): EnterTransition =
    fadeIn(tween(TAB_FADE_IN_MS, delayMillis = TAB_FADE_OUT_MS, easing = LinearOutSlowInEasing)) +
        scaleIn(tween(TAB_FADE_IN_MS, delayMillis = TAB_FADE_OUT_MS, easing = LinearOutSlowInEasing), initialScale = 0.96f)

private fun tabExit(): ExitTransition = fadeOut(tween(TAB_FADE_OUT_MS, easing = FastOutLinearInEasing))

private fun forwardEnter(): EnterTransition =
    slideInHorizontally(tween(AXIS_MS, easing = FastOutSlowInEasing)) { it / 3 } +
        fadeIn(tween(AXIS_FADE_MS, delayMillis = AXIS_MS - AXIS_FADE_MS))

private fun forwardExit(): ExitTransition =
    slideOutHorizontally(tween(AXIS_MS, easing = FastOutSlowInEasing)) { -it / 3 } +
        fadeOut(tween(AXIS_FADE_MS))

private fun backEnter(): EnterTransition =
    slideInHorizontally(tween(AXIS_MS, easing = FastOutSlowInEasing)) { -it / 3 } +
        fadeIn(tween(AXIS_FADE_MS, delayMillis = AXIS_MS - AXIS_FADE_MS))

private fun backExit(): ExitTransition =
    slideOutHorizontally(tween(AXIS_MS, easing = FastOutSlowInEasing)) { it / 3 } +
        fadeOut(tween(AXIS_FADE_MS))

/** Cambia de pestaña conservando el estado de cada una (patrón estándar de bottom nav + Navigation Compose). */
private fun NavHostController.navigateToTab(tab: BottomTab) {
    navigate(tab.rootRoute()) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
