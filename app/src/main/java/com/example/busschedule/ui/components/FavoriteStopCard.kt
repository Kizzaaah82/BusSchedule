package com.example.busschedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.busschedule.model.GtfsStop
import com.example.busschedule.viewmodel.GtfsViewModel
import com.example.busschedule.viewmodel.ArrivalDisplay

@Composable
fun FavoriteStopCard(
    stopName: String,
    stopId: String,
    gtfsViewModel: GtfsViewModel,
    routeColorHex: String? = null
) {
    val context = LocalContext.current
    var arrivals by remember { mutableStateOf<List<ArrivalDisplay>>(emptyList()) }
    var schedule by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTimetableDialog by remember { mutableStateOf(false) }

    val earlyColor = Color(0xFF64B5F6)
    val onTimeColor = Color(0xFF81C784)
    val slightlyLateColor = Color(0xFFFFB74D)
    val superLateColor = Color(0xFFE57373)
    val scheduledColor = Color(0xFFB0BEC5)
    val noBusColor = Color(0xFFFF5252)

    LaunchedEffect(stopId) {
        gtfsViewModel.getUpcomingArrivalsForStop(
            GtfsStop(stopId, stopId, stopName, 0.0, 0.0) // Lat/lon not used here
        ) { result ->
            arrivals = result
        }

        gtfsViewModel.getStaticTimetableForStop(stopId) { result ->
            schedule = result
        }
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
        .clickable { showTimetableDialog = true } // <— open dialog
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stopName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Stop ID: $stopId", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))

            if (arrivals.isNotEmpty()) {
                val firstArrival = arrivals.first()
                val color = when (firstArrival.status) {
                    "early" -> earlyColor
                    "on time" -> onTimeColor
                    "late" -> slightlyLateColor
                    "nearby" -> Color.Cyan
                    else -> superLateColor
                }
                val label = when (firstArrival.status) {
                    "early" -> "early"
                    "on time" -> "on time"
                    "late" -> "late"
                    "nearby" -> "nearby"
                    else -> "unknown"
                }

                Text(
                    text = "${firstArrival.minutesAway} min ($label)",
                    color = color,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = "No real-time data. Bus might be hiding.",
                    color = noBusColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (schedule.isNotEmpty()) {
                Text(
                    text = "Upcoming: ${schedule.take(2).joinToString(", ")}",
                    color = scheduledColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "No scheduled times found.",
                    color = scheduledColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            }
        }
    }

    if (showTimetableDialog) {
        AlertDialog(
            onDismissRequest = { showTimetableDialog = false },
            title = { Text("Timetable for $stopName") },
            text = {
                if (schedule.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(schedule) { time ->
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No scheduled buses. Either it's Sunday, or you're cursed. 💀",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Red
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimetableDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}