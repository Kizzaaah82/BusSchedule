package com.example.busschedule.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.Alignment


@Composable
fun WeatherCard(
    temp: String,
    time: String,
    condition: String,
    lastUpdated: String,
    iconUrl: String?,
    feelsLike: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Time: $time", style = MaterialTheme.typography.bodyLarge)
            Text("Temperature: $temp", style = MaterialTheme.typography.bodyLarge)
            Text("Feels like: $feelsLike", style = MaterialTheme.typography.bodyMedium)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconUrl != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter("https:$iconUrl"),
                        contentDescription = "Weather icon",
                        modifier = Modifier.size(40.dp)
                    )
                }
                Text("Condition: $condition", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Last updated: $lastUpdated", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun RoastBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun FavoriteStopsSection(stops: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your Favourite Stops", style = MaterialTheme.typography.titleMedium)

        stops.forEach { (stopId, label) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = label, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Stop ID: $stopId", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Next bus: TBD", style = MaterialTheme.typography.bodyMedium) // placeholder
                }
            }
        }
    }
}