package com.example.busschedule.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.busschedule.R
import com.example.busschedule.navigation.AppNavHost
import com.example.busschedule.navigation.bottomNavItems
import com.example.busschedule.ui.BottomBar
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Place
import com.example.busschedule.viewmodel.ThemeViewModel
import androidx.compose.material3.Surface
import com.example.busschedule.viewmodel.GtfsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    drawerState: DrawerState,
    themeViewModel: ThemeViewModel,
    gtfsViewModel: GtfsViewModel,
    weatherApiKey: String
) {

    val scope = rememberCoroutineScope()
    val wobbleAngle = remember { Animatable(0f) }

    // ✅ THEME COLORS
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val isDarkTheme by themeViewModel.isDarkMode.collectAsState()
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant


    val barBackgroundColor = if (isDarkTheme) {
        Color(0xFF121212) // Darkest color from your background gradient
    } else {
        Color(0xFFFFF8F2) // Creamy white color from your background gradient
    }


    // ✅ FONTS
    val roadRage = FontFamily(Font(R.font.road_rage))
    val altFont = FontFamily(Font(R.font.roboto_condensed_semibold))

    val items = listOf(
        DrawerItem("Home", Icons.Filled.Home, "main"),
        DrawerItem("Live Map", Icons.Filled.Map, "live_map"),
        DrawerItem("Routes", Icons.Filled.DirectionsBus, "routes"),
        DrawerItem("Map", Icons.Filled.Place, "interactive_map"),
        DrawerItem("Settings", Icons.Filled.Settings, "settings"),
        DrawerItem("About", Icons.Filled.Info, "about")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            Surface(
                tonalElevation = 25.dp,
                shadowElevation = 25.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                ModalDrawerSheet(drawerContainerColor = surfaceVariantColor) {
                    Spacer(modifier = Modifier.height(24.dp))
                    items.forEach { item ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    item.title,
                                    color = primaryColor,
                                    fontFamily = altFont,
                                    fontSize = 25.sp
                                )
                            },
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.title,
                                    tint = primaryColor
                                )
                            },
                            selected = false,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                scope.launch { drawerState.close() }
                            },
                                    colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = surfaceColor.copy(alpha = 0.2f),
                                selectedIconColor = textColor,
                                selectedTextColor = textColor,
                                unselectedIconColor = primaryColor,
                                unselectedTextColor = primaryColor
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 56.dp),
                        ) {
                            Text(
                                text = "Next Stop: WHO KNOWS?!",
                                color = primaryColor,
                                fontFamily = roadRage,
                                fontSize = 41.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                wobbleAngle.snapTo(0f)
                                wobbleAngle.animateTo(15f, tween(100))
                                wobbleAngle.animateTo(-15f, tween(100))
                                wobbleAngle.animateTo(10f, tween(80))
                                wobbleAngle.animateTo(-10f, tween(80))
                                wobbleAngle.animateTo(0f, tween(60))
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_hamburger1),
                                contentDescription = "Menu",
                                modifier = Modifier
                                    .size(34.dp)
                                    .rotate(wobbleAngle.value),
                                tint = Color.Unspecified
                            )
                        }
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = barBackgroundColor,
                    )
                )
            },
            bottomBar = {
                BottomBar(navController = navController, items = bottomNavItems, containerColor = barBackgroundColor)
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background( // This now expects ONLY a Brush
                        brush = if (isDarkTheme) {
                            // Dark Theme: Return the existing RadialGradient Brush
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2C2C2C).copy(alpha = 0.9f),
                                    Color(0xFF1F1F1F).copy(alpha = 0.8f),
                                    Color(0xFF121212).copy(alpha = 1f)
                                ),
                                center = Offset.Unspecified,
                                radius = 900f
                            )
                        } else {
                            // Light Theme: Use a LinearGradient with two identical colors
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFF8F2), Color(0xFFFFF8F2)) // Provide the color twice
                            )
                        }
                    )
                    .padding(innerPadding)
            ) {
                AppNavHost(navController = navController, themeViewModel = themeViewModel, gtfsViewModel = gtfsViewModel, weatherApiKey = weatherApiKey)
            }
        }
    }
}