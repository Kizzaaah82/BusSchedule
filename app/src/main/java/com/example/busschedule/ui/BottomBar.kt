package com.example.busschedule.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.busschedule.navigation.BottomNavItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.busschedule.R

@Composable
fun BottomBar(navController: NavController, items: List<BottomNavItem>,  containerColor: Color) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val altFont = FontFamily(Font(R.font.roboto_condensed_semibold))

    val selectedColor = MaterialTheme.colorScheme.onSurface
    val unselectedColor = MaterialTheme.colorScheme.primary

    NavigationBar(
        containerColor = containerColor,
        tonalElevation = 10.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            val pulseSize by animateDpAsState(
                targetValue = if (isSelected) 42.dp else 36.dp,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Pulse Animation"
            )

            NavigationBarItem(
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(pulseSize)
                                    .background(
                                        color = unselectedColor,
                                        shape = CircleShape
                                    )
                            )
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = if (isSelected) selectedColor else unselectedColor
                        )
                    }
                },
                label = {
                    Text(
                        text = item.name,
                        fontFamily = altFont,
                        color = if (isSelected) selectedColor else unselectedColor
                    )
                },
                selected = isSelected,
                onClick = {
                    if (item.route == "main") {
                        navController.navigate("main") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = containerColor,
                    selectedIconColor = selectedColor,
                    unselectedIconColor = unselectedColor,
                    selectedTextColor = selectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
        }
    }
}