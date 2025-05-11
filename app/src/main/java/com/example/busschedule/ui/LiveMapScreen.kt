package com.example.busschedule.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.busschedule.R
import com.example.busschedule.util.createBusMarkerIcon
import com.example.busschedule.viewmodel.BusPosition
import com.example.busschedule.viewmodel.GtfsViewModel
import com.example.busschedule.viewmodel.ThemeViewModel
import com.example.busschedule.viewmodel.UiRoute
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveMapScreen(
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

    val mapProperties = remember { mutableStateOf(MapProperties(isMyLocationEnabled = false)) }
    val cameraPositionState = rememberCameraPositionState()
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val mapLoaded = remember { mutableStateOf(false) }
    val availableRoutes by gtfsViewModel.availableRoutes
    val isRoutesLoaded by gtfsViewModel.isRoutesLoaded

    val expanded = remember { mutableStateOf(false) }
    val selectedRoute = remember { mutableStateOf<UiRoute?>(null) }
    val routeShapePointLists = remember { mutableStateListOf<List<Pair<Double, Double>>>() }
    var busPositions by remember { mutableStateOf<List<BusPosition>>(emptyList()) }
    var selectedBus by remember { mutableStateOf<BusPosition?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedRoute.value) {
        while (true) {
            val selectedRouteId = selectedRoute.value?.routeId
            if (selectedRouteId != null) {
                gtfsViewModel.getLiveBusPositions { allBuses ->
                    Log.d("LiveMap", "Filtering buses for routeId: $selectedRouteId")
                    allBuses.forEach { bus ->
                        Log.d("LiveMap", "🚌 Vehicle ${bus.vehicleId} — trip: ${bus.tripId}, route: ${bus.routeId}")
                    }

                    busPositions = allBuses.filter { it.routeId == selectedRouteId }
                }
            } else {
                Log.d("LiveMap", "No route selected. Clearing buses.")
                busPositions = emptyList()
            }
            delay(15_000)
        }
    }

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationProviderClient.lastLocation.await()
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                userLocation = latLng
                mapProperties.value = mapProperties.value.copy(isMyLocationEnabled = true)
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(latLng, 14f),
                    durationMs = 1000
                )
            }
        } else {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (!isRoutesLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                properties = mapProperties.value,
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                onMapLoaded = { mapLoaded.value = true }
            ) {
                userLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "You are here",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                routeShapePointLists.forEach { shapePoints ->
                    Polyline(
                        points = shapePoints.map { LatLng(it.first, it.second) },
                        color = selectedRoute.value?.color ?: Color(0xFF2196F3),
                        width = 15f,
                        geodesic = true
                    )
                }

                busPositions.forEach { bus ->
                    val routeColor = availableRoutes.find { it.routeId == bus.routeId }?.color?.toArgb() ?: 0xFF2196F3.toInt()
                    val busIcon = createBusMarkerIcon(context, routeColor, bus.bearing ?: 0f)
                    val markerIcon = BitmapDescriptorFactory.fromBitmap(busIcon)

                    Marker(
                        state = MarkerState(position = LatLng(bus.lat, bus.lon)),
                        title = bus.routeId ?: "Unknown Route",
                        snippet = "Tap for bus info",
                        icon = markerIcon,
                        flat = true,
                        anchor = Offset(0.5f, 0.5f),
                        onClick = {
                            selectedBus = bus
                            true // Returning true tells the map not to pan the camera
                        }
                    )
                }

                selectedBus?.let { bus ->
                    BusInfoBottomSheet(
                        bus = bus,
                        onDismiss = { selectedBus = null }
                    )
                }
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
                                gtfsViewModel.loadRouteData(route.shortName) { _, shapeLists ->
                                    routeShapePointLists.clear()
                                    routeShapePointLists.addAll(shapeLists)
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