package com.example.busschedule.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Explore

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Main.route, Icons.Outlined.House),
    BottomNavItem("Live Map", Screen.LiveMap.route, Icons.Outlined.Route),
    BottomNavItem("Routes", Screen.Routes.route, Icons.Outlined.DirectionsTransit),
    BottomNavItem("Map", Screen.InteractiveMap.route, Icons.Outlined.Explore)
)