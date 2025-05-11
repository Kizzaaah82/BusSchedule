package com.example.busschedule.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.busschedule.model.WeatherApiResponse
import com.example.busschedule.ui.components.FavoriteStopCard
import com.example.busschedule.util.WeatherUtils
import com.example.busschedule.viewmodel.GtfsViewModel
import com.example.busschedule.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(navController: NavHostController, weatherApiKey: String, themeViewModel: ThemeViewModel,gtfsViewModel: GtfsViewModel) {
    var weatherData by remember { mutableStateOf<WeatherApiResponse?>(null) }
    var lastUpdatedTime by remember { mutableStateOf("--:--") }
    val favoriteStops by themeViewModel.favoriteStops.collectAsState()

    val currentTime by produceState(initialValue = getCurrentTime()) {
        while (true) {
            value = getCurrentTime()
            delay(1000L) // Update every second
        }
    }

    LaunchedEffect(weatherApiKey) {
        while (true) {
            weatherData = WeatherUtils.getWeather("Windsor", weatherApiKey)
            lastUpdatedTime = getCurrentTime()
            delay(15 * 60 * 1000L) // Refresh every 15 minutes
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            WeatherCard(
                temp = "${weatherData?.current?.temp_c?.toInt() ?: "--"}°C",
                time = currentTime,
                condition = weatherData?.current?.condition?.text ?: "Loading...",
                lastUpdated = lastUpdatedTime,
                iconUrl = weatherData?.current?.condition?.icon,
                feelsLike = "${weatherData?.current?.feelslike_c?.toInt() ?: "--"}°C"
            )
        }

        item {
            RoastBanner(
                message = "Hope you're not running for this bus... again. 😏"
            )
        }

        item {
            if (favoriteStops.isNotEmpty()) {
                Text("Your Favorite Stops", style = MaterialTheme.typography.titleLarge)

                favoriteStops.forEach { stop ->
                    FavoriteStopCard(
                        stopName = stop.name,
                        stopId = stop.stopId,
                        gtfsViewModel = gtfsViewModel // already passed in, so just use it directly
                    )
                }
            } else {
                Text(
                    text = "No favorite stops yet. Tap a heart on the map to add one 💔",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

private fun getCurrentTime(): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
}