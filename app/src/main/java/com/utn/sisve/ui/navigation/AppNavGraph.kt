package com.utn.sisve.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.utn.sisve.ui.arrival.PostArrivalScreen
import com.utn.sisve.ui.camera.CameraScreen
import com.utn.sisve.ui.dashboard.DashboardScreen
import com.utn.sisve.ui.dispatch.DispatchScreen
import com.utn.sisve.ui.history.HistoryScreen
import com.utn.sisve.ui.login.LoginScreen
import com.utn.sisve.ui.map.MapScreen
import com.utn.sisve.ui.settings.SettingsScreen
import com.utn.sisve.ui.setup.SetupScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoute.Setup.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(AppRoute.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.Setup.route) { inclusive = true }
                    }
                }
            )
        }

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
                onNavigateToHistory = { navController.navigate(AppRoute.History.route) },
                onNavigateToLogin = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                    }
                },
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
            MapScreen(
                onNavigateBack = { navController.popBackStack() },
                onArrived = {
                    navController.navigate(AppRoute.PostArrival.route)
                }
            )
        }

        composable(AppRoute.History.route) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(AppRoute.PostArrival.route) {
            PostArrivalScreen(
                onTransferToHospital = {
                    // Vuelve al mapa con un nuevo destino (Centro de Salud)
                    navController.navigate(AppRoute.Map.route)
                },
                onFinalizeEmergency = {
                    navController.navigate(AppRoute.Dashboard.route) {
                        popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
