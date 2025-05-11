package com.example.busschedule.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object LiveMap : Screen("live_map")
    object Routes : Screen("routes")
    object InteractiveMap : Screen("interactive_map")
    object Settings : Screen("settings")
    object About : Screen("about")
}