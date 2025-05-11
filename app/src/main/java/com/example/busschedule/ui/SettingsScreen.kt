package com.example.busschedule.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.busschedule.model.AppDayOfWeek
import com.example.busschedule.model.BusSchedule
import com.example.busschedule.ui.theme.DarkGray
import com.example.busschedule.ui.theme.FontColor
import com.example.busschedule.util.GtfsDownloader.getDaysSinceLastDownload
import com.example.busschedule.util.formatAs12Hour
import com.example.busschedule.util.formatTimeNicely
import com.example.busschedule.util.parseTimeForPicker
import com.example.busschedule.viewmodel.GtfsViewModel
import com.example.busschedule.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavHostController,
    themeViewModel: ThemeViewModel,
    gtfsViewModel: GtfsViewModel
) {
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val fontColor by themeViewModel.fontColor.collectAsState()
    val homeLocation by themeViewModel.homeLocation.collectAsState()
    val workLocation by themeViewModel.workLocation.collectAsState()
    val showWeather by themeViewModel.showWeather.collectAsState()
    val notificationsEnabled by themeViewModel.notificationsEnabled.collectAsState()
    val weeklySchedule by themeViewModel.weeklySchedule.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val daysSinceDownload = remember { mutableStateOf(getDaysSinceLastDownload(context)) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            item {
                Text("Dark Mode", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Switch(checked = isDarkMode, onCheckedChange = { themeViewModel.toggleTheme() })
            }

            item {
                Text("Font/Icon Colour", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                FontColorPicker(selectedColor = fontColor, onColorSelected = { themeViewModel.updateFontColor(it) })
            }

            item {
                Text("Home Location", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = homeLocation,
                    onValueChange = { themeViewModel.updateHomeLocation(it) },
                    label = { Text("e.g. Stop 1010 or 'Home Stop'") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Work Location", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = workLocation,
                    onValueChange = { themeViewModel.updateWorkLocation(it) },
                    label = { Text("e.g. Stop 2020 or 'College'") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Show Weather", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Switch(checked = showWeather, onCheckedChange = { themeViewModel.updateShowWeather(it) })
            }

            item {
                Text("Weekly Bus Schedule", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("schedule") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Edit Weekly Schedule")
                }
            }

            item {
                Text("Enable Notifications", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { themeViewModel.updateNotificationsEnabled(it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        themeViewModel.resetSettings()
                        coroutineScope.launch {
                            val roastLines = listOf(
                                "Boom. Your settings just got obliterated. Try not to fuck it up this time.",
                                "Factory reset like a coward. Bold move, dipshit.",
                                "You just erased your personality. Congrats, you’re default again.",
                                "Wow. You really hit reset? The app’s disappointed. And so am I.",
                                "Reset complete. Just like your social life.",
                                "Settings nuked. That was the only thing you had going for you."
                            )
                            snackbarHostState.showSnackbar(roastLines.random())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Reset to Default")
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        gtfsViewModel.forceRefreshGtfs { success ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "GTFS refreshed! You're ready to roll."
                                    else "Refresh failed. Try again later, champ."
                                )
                                if (success) {
                                    daysSinceDownload.value = getDaysSinceLastDownload(context)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Force Refresh GTFS Data")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        daysSinceDownload.value == -1 -> "Last updated: Never"
                        daysSinceDownload.value == 0 -> "Last updated: Today"
                        daysSinceDownload.value == 1 -> "Last updated: 1 day ago"
                        else -> "Last updated: ${daysSinceDownload.value} days ago"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun FontColorPicker(
    selectedColor: FontColor,
    onColorSelected: (FontColor) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        items(FontColor.entries.toList()) { colorOption ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colorOption.color, shape = CircleShape)
                    .border(
                        width = if (colorOption == selectedColor) 3.dp else 1.dp,
                        color = if (colorOption == selectedColor) Color.Black else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorOption) }
            )
        }
    }
}