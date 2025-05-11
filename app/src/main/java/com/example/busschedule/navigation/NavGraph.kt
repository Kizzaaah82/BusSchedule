package com.example.busschedule.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.busschedule.ui.InteractiveMapScreen
import com.example.busschedule.ui.LiveMapScreen
import com.example.busschedule.ui.MainScreen
import com.example.busschedule.ui.RoutesScreen
import com.example.busschedule.ui.SettingsScreen
import com.example.busschedule.ui.AboutScreen
import com.example.busschedule.ui.ScheduleScreen
import com.example.busschedule.viewmodel.GtfsViewModel
import com.example.busschedule.viewmodel.ThemeViewModel

@Composable
fun AppNavHost(navController: NavHostController, themeViewModel: ThemeViewModel, gtfsViewModel: GtfsViewModel,  weatherApiKey: String) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController = navController, weatherApiKey = weatherApiKey, themeViewModel = themeViewModel, gtfsViewModel = gtfsViewModel)
        }
        composable(Screen.LiveMap.route) { LiveMapScreen(navController, themeViewModel = themeViewModel) }
        composable(Screen.Routes.route) { RoutesScreen(navController) }
        composable(Screen.InteractiveMap.route) {
            InteractiveMapScreen(navController = navController, themeViewModel = themeViewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController, themeViewModel = themeViewModel, gtfsViewModel = gtfsViewModel) }
        composable(Screen.About.route) { AboutScreen(navController) }

        // 🚍 Schedule Screen
        composable("schedule") {
            ScheduleScreen(navController = navController, themeViewModel = themeViewModel)
        }
    }
}