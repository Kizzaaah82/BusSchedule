package com.example.busschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.example.busschedule.ui.components.MainScaffold
import com.example.busschedule.ui.theme.BusScheduleTheme
import com.example.busschedule.ui.theme.FontColor
import com.example.busschedule.util.NotificationUtils
import com.example.busschedule.viewmodel.GtfsViewModel
import com.example.busschedule.viewmodel.ThemeViewModel
import kotlinx.coroutines.flow.StateFlow
import com.example.busschedule.BuildConfig

class MainActivity : ComponentActivity() {
    private val themeViewModel by viewModels<ThemeViewModel>()
    private val gtfsViewModel by viewModels<GtfsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationUtils.createNotificationChannel(this)
        val weatherApiKey = BuildConfig.WEATHER_API_KEY

        setContent {
            val isThemeReady by themeViewModel.isThemeInitialized.collectAsState()



            if (isThemeReady) {
                val isDarkMode by themeViewModel.isDarkMode.collectAsState()
                val fontColor by themeViewModel.fontColor.collectAsState()

                BusScheduleTheme(darkTheme = isDarkMode, fontColor = fontColor) {
                    val navController = rememberNavController()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                    MainScaffold(
                        navController = navController,
                        drawerState = drawerState,
                        themeViewModel = themeViewModel,
                        gtfsViewModel = gtfsViewModel,
                        weatherApiKey = weatherApiKey
                    )
                }

            } else {
                // Optional: You can show a splash screen or just a blank screen while waiting
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {}

            }
        }
    }
}