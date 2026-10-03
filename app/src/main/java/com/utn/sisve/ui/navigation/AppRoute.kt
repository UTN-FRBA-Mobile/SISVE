package com.utn.sisve.ui.navigation

sealed class AppRoute(val route: String) {
    object Login : AppRoute("login")
    object Camera : AppRoute("camera")
    object Dashboard : AppRoute("dashboard")
    object Settings : AppRoute("settings")
    object Map : AppRoute("map")
    object History : AppRoute("history")
    object PostArrival : AppRoute("post_arrival")
    object Dispatch : AppRoute("dispatch/{callId}") {
        fun createRoute(callId: String) = "dispatch/$callId"
    }
}
