package com.utn.sisve.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.utn.sisve.ui.camera.CameraScreen
import com.utn.sisve.ui.dashboard.DashboardScreen
import com.utn.sisve.ui.dispatch.DispatchScreen
import com.utn.sisve.ui.login.LoginScreen
import com.utn.sisve.ui.map.MapScreen
import com.utn.sisve.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoute.Login.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(AppRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoute.Camera.route) {
                        popUpTo(AppRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.Camera.route) {
            CameraScreen(
                onPhotoCaptured = {
                    navController.navigate(AppRoute.Dashboard.route) {
                        popUpTo(AppRoute.Camera.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.Dashboard.route) {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate(AppRoute.Settings.route) },
                onDispatchReceived = { callId ->
                    navController.navigate(AppRoute.Dispatch.createRoute(callId))
                }
            )
        }

        composable(AppRoute.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = AppRoute.Dispatch.route,
            arguments = listOf(navArgument("callId") { type = NavType.StringType })
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            DispatchScreen(
                callId = callId,
                onAccepted = {
                    navController.navigate(AppRoute.Map.route) {
                        popUpTo(AppRoute.Dispatch.route) { inclusive = true }
                    }
                },
                onRejected = { navController.popBackStack() }
            )
        }

        composable(AppRoute.Map.route) {
            MapScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
