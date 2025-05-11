package com.example.busschedule.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.busschedule.R
import com.example.busschedule.model.GtfsStop
import com.example.busschedule.viewmodel.GtfsViewModel
import com.example.busschedule.viewmodel.ThemeViewModel
import com.example.busschedule.viewmodel.UiRoute
import com.example.busschedule.viewmodel.ArrivalDisplay
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

fun formatArrivalTime(rawTime: String): String {
    return try {
        val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        outputFormat.format(inputFormat.parse(rawTime)!!)
    } catch (e: Exception) {
        rawTime
    }
}

fun sortTimetableTimes(times: List<String>): List<String> {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return times.sortedBy { time ->
        try {
            formatter.parse(time)
        } catch (e: Exception) {
            null
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InteractiveMapScreen(
    navController: NavHostController,
    themeViewModel: ThemeViewModel,
    gtfsViewModel: GtfsViewModel = viewModel()
) {
    val context = LocalContext.current
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val isRoutesLoaded by gtfsViewModel.isRoutesLoaded
    val availableRoutes by gtfsViewModel.availableRoutes

    val mapProperties = remember { mutableStateOf(MapProperties(isMyLocationEnabled = false)) }
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }
    val cameraPositionState = rememberCameraPositionState()
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val expanded = remember { mutableStateOf(false) }
    val selectedRoute = remember { mutableStateOf<UiRoute?>(null) }

    val routeStops = remember { mutableStateListOf<GtfsStop>() }
    val routeShapePointLists = remember { mutableStateListOf<List<Pair<Double, Double>>>() }
    var showTimetableDialog by remember { mutableStateOf(false) }
    var selectedStop by remember { mutableStateOf<GtfsStop?>(null) }

    var upcomingArrivals by remember { mutableStateOf<List<ArrivalDisplay>>(emptyList()) }
    var timetable by remember { mutableStateOf<List<String>>(emptyList()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationProviderClient.lastLocation.await()
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                userLocation = latLng
                mapProperties.value = mapProperties.value.copy(isMyLocationEnabled = true)
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                    durationMs = 1000
                )
            }
        } else {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    val earlyColor = Color(0xFF64B5F6)
    val onTimeColor = Color(0xFF81C784)
    val slightlyLateColor = Color(0xFFFFB74D)
    val superLateColor = Color(0xFFE57373)
    val noBusColor = Color(0xFFFF5252)
    val scheduledColor = Color(0xFFB0BEC5) // Light gray for scheduled arrivals

    Box(Modifier.fillMaxSize()) {
        if (!isRoutesLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                properties = mapProperties.value,
                uiSettings = uiSettings,
                cameraPositionState = cameraPositionState
            ) {
                userLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "You are here",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                routeStops.forEach { stop ->
                    Marker(
                        state = MarkerState(position = LatLng(stop.lat, stop.lon)),
                        title = stop.name,
                        snippet = "Stop Code: ${stop.stopCode}",
                        icon = BitmapDescriptorFactory.fromBitmap(
                            Bitmap.createScaledBitmap(
                                BitmapFactory.decodeResource(
                                    context.resources,
                                    R.drawable.ic_bus_stop
                                ),
                                110, 110, false
                            )
                        ),
                        onClick = {
                            selectedStop = stop
                            gtfsViewModel.getUpcomingArrivalsForStop(stop) { arrivals ->
                            if (arrivals.isEmpty()) {
                                    gtfsViewModel.isBusNearStop(stop.lat, stop.lon) { isNear ->
                                        if (isNear) {
                                            // Fake an "arrival" entry just to show the bus exists
                                            upcomingArrivals = listOf(
                                                ArrivalDisplay(
                                                    minutesAway = 0,
                                                    status = "nearby",
                                                    isRealtime = true
                                                )
                                            )
                                        } else {
                                            upcomingArrivals = emptyList()
                                        }
                                    }
                                } else {
                                    upcomingArrivals = arrivals
                                }
                            }
                            true
                        }
                    )
                }

                // Iterate through each list of points (each direction/shape)
                routeShapePointLists.forEachIndexed { index, shapePoints ->
                    if (shapePoints.isNotEmpty()) {
                        Polyline(
                            points = shapePoints.map { LatLng(it.first, it.second) },
                            // Optional: Use slightly different colors/widths for clarity,
                            // or use the route color directly. Example using route color:
                            color = selectedRoute.value?.color
                                ?: Color(0xFF2196F3), // Use the route's color
                            // Example: slightly different alpha per direction if needed:
                            // color = (selectedRoute.value?.color ?: Color(0xFF2196F3)).copy(alpha = if(index == 0) 1f else 0.7f),
                            width = 15f, // Keep width consistent or vary slightly
                            geodesic = true
                        )
                    }
                }
            }

            selectedStop?.let { stop ->
                AlertDialog(
                    onDismissRequest = { selectedStop = null },
                    title = { Text(stop.name) },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Stop Code: ${stop.stopCode}")

                                val isFavorite = themeViewModel.favoriteStops.collectAsState().value
                                    .any { it.stopId == stop.stopId }

                                IconButton(onClick = { themeViewModel.toggleFavoriteStop(stop) }) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("Next buses:")

                            if (upcomingArrivals.isNotEmpty()) {
                                upcomingArrivals.take(3).forEachIndexed { index, arrival ->
                                    val color = when {
                                        arrival.status == "nearby" -> Color.Cyan
                                        !arrival.isRealtime -> scheduledColor
                                        arrival.status == "early" -> earlyColor
                                        arrival.status == "on time" -> onTimeColor
                                        arrival.status == "late" -> slightlyLateColor
                                        else -> superLateColor
                                    }

                                    val statusText = when {
                                        arrival.wasInterpolated -> "(made it up... but like, with math)"
                                        !arrival.isRealtime -> "(scheduled)"
                                        arrival.status == "early" -> "(early)"
                                        arrival.status == "on time" -> "(on time)"
                                        arrival.status == "late" -> "(late)"
                                        arrival.status == "nearby" -> "(bus near stop)"
                                        else -> "(??)"
                                    }

                                    Text(
                                        text = if (arrival.isRealtime) {
                                            "${arrival.minutesAway} min $statusText"
                                        } else {
                                            "${arrival.timeFormatted ?: "${arrival.minutesAway} min"} (scheduled)"
                                        }
                                        ,
                                        color = color,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            } else {
                                Text(
                                    text = "Bus? Bus where? Bitch, you walkin'.",
                                    color = noBusColor,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(onClick = { showTimetableDialog = true }) {
                                Text("View Full Timetable")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedStop = null }) {
                            Text("Close")
                        }
                    }
                )
            }

            LaunchedEffect(showTimetableDialog, selectedStop?.stopId) {
                if (showTimetableDialog && selectedStop != null) {
                    gtfsViewModel.getStaticTimetableForStop(selectedStop!!.stopId) { times ->
                        timetable = times
                    }
                }
            }

            if (showTimetableDialog) {
                AlertDialog(
                    onDismissRequest = { showTimetableDialog = false },
                    title = {
                        Text("Timetable for ${selectedStop?.name} (${selectedStop?.stopCode})") },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            if (timetable.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(timetable) { time ->
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
                            } else {
                                Text(
                                    text = "No scheduled buses. Bitch, you walkin'.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Red,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTimetableDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                IconButton(onClick = { expanded.value = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_hamburger1),
                        contentDescription = "Select Route",
                        modifier = Modifier.size(30.dp),
                        tint = Color.Unspecified
                    )
                }

                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false }
                ) {
                    availableRoutes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.longName, color = Color.White) },
                            onClick = {
                                selectedRoute.value = route
                                expanded.value = false
                                // Update the callback lambda to handle the list of lists
                                gtfsViewModel.loadRouteData(route.shortName) { stops, shapeLists -> // Renamed shapes -> shapeLists
                                    routeStops.clear()
                                    routeStops.addAll(stops)
                                    routeShapePointLists.clear()        // Clear the list of lists
                                    routeShapePointLists.addAll(shapeLists) // Add all the new shape lists
                                }
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.White,
                                leadingIconColor = Color.White
                            )
                        )
                    }
                }
            }

            selectedRoute.value?.let { route ->
                Text(
                    text = "Selected: ${route.shortName} - ${route.longName}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 72.dp)
                )
            }
        }
    }
}