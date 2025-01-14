package com.example.routes.ui.login

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routes.data.RouteRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(db: FirebaseFirestore, mapViewModel: MapViewModel) {
    val context = LocalContext.current
//    val routeRepository = RouteRepository(db)
//
//    val mapViewModel: MapViewModel = viewModel(
//        factory = MapViewModelFactory(routeRepository),
//        key = "MapViewModel"  // Add a consistent key
//    )

    // Observing states from ViewModel
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val isLocationPermissionGranted = permissionState.status.isGranted




    val routePoints by mapViewModel.routePoints.collectAsState()
    val showRouteNameDialog by mapViewModel.showRouteNameDialog.collectAsState()
    val routeName by mapViewModel.routeName.collectAsState()
    val saveRouteResult by mapViewModel.saveRouteResult.collectAsState()
    val distance by mapViewModel.distance.collectAsState()

    val isRouteActive by mapViewModel.isRouteActive.collectAsState()
    val watchConnectionStatus by mapViewModel.watchConnectionStatus.collectAsState()
    val currentHeartRate by mapViewModel.currentHeartRate.collectAsState()
    var totalDistance by rememberSaveable { mutableStateOf(0f) }
    var startTime by rememberSaveable { mutableStateOf<Long>(0L) }
    var elapsedTime by rememberSaveable { mutableStateOf(0L) }
    var lastPointAddedTime by rememberSaveable { mutableStateOf(0L) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }


    // Timer logic
    LaunchedEffect(isRouteActive) {
        var lastUpdateTime = System.currentTimeMillis()
        while (isRouteActive) {
            delay(1000) // Update every second
            val currentTime = System.currentTimeMillis()
            elapsedTime += (currentTime - lastUpdateTime)
            lastUpdateTime = currentTime
        }
    }

    // Handle save result
    LaunchedEffect(saveRouteResult) {
        saveRouteResult?.onSuccess {
            elapsedTime = 0L
            Log.d("MapScreen", "Route saved successfully!")
            Log.d("MapScreen", "MapScreen ViewModel ID: ${mapViewModel.hashCode()}")
            mapViewModel.isRouteActive.collect { active ->
                Log.d("MapScreen", "Collected isRouteActive value: $active")
            }


        }?.onFailure { exception ->
            Log.e("MapScreen", "Error saving route: ${exception.message}")
            elapsedTime = 0L
        }
    }

    // Box layout for map and controls
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLocationPermissionGranted) {
            MapViewComposable(
                fusedLocationClient = fusedLocationClient,
                routePoints = routePoints,
                isTracking = isRouteActive,
                distance = distance,
                totalDistance = totalDistance,
                startTime = startTime,
                elapsedTime = elapsedTime,
                onDistanceUpdated = { newDistance -> totalDistance = newDistance },
                onElapsedTimeUpdated = { newElapsedTime -> elapsedTime = newElapsedTime },
                onRoutePointAdded = { mapViewModel.addRoutePoint(it) },
                onRouteCleared = { mapViewModel.clearRoutePoints() }  // Clear route when tracking stops
            )

            if (isRouteActive) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentTime = System.currentTimeMillis()
                        val MINIMUM_POINT_INTERVAL = 5000L // 5 seconds in milliseconds
                        if (currentTime - lastPointAddedTime >= MINIMUM_POINT_INTERVAL) {
                            Log.d("MapScreen", "Adding starting point to route")
                            val startPoint = LatLng(it.latitude, it.longitude)
                            mapViewModel.addRoutePoint(startPoint) // Add the starting point
                            lastPointAddedTime = currentTime
                            // Calculate and update distance only when we add a new point
                            if (routePoints.size > 1) {
                                val lastLocation = Location("").apply {
                                    latitude = routePoints[routePoints.size - 2].latitude
                                    longitude = routePoints[routePoints.size - 2].longitude
                                }
                                val distance1 = lastLocation.distanceTo(Location("").apply {
                                    latitude = location.latitude
                                    longitude = location.longitude
                                })
                                mapViewModel.setTotalDistance(distance + distance1)
                            }
                        }
                    }
                }
            }

            // Status Panel at the top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                //connection status display
                if (watchConnectionStatus.contains("Watch connected")) {
                    Text(
                        text = watchConnectionStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Not Connected to Watch",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Heart Rate Display (only show if connected)
                if (watchConnectionStatus.contains("Watch connected")) {
                    Text(
                        text = "❤️ $currentHeartRate BPM",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Route Status with Timer
                if (isRouteActive) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Route Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Time: ${formatTime(elapsedTime)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                if (!watchConnectionStatus.contains("Watch connected")) {
                    Button(
                        onClick = {
                            if (isRouteActive) {
                                mapViewModel.setRouteActive(false)
                                mapViewModel.setShowRouteNameDialog(true)
                            } else {
                                mapViewModel.setRouteActive(true)
                                startTime = System.currentTimeMillis()
                            }
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                    ) {
                        Text(text = if (isRouteActive) "Stop Tracking" else "Start Tracking")
                    }
                }
            }
        } else {
            Text(
                text = "Permission to access location is required.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }

    // Save Route Dialog
    if (showRouteNameDialog) {
        AlertDialog(
            onDismissRequest = { mapViewModel.setShowRouteNameDialog(false) },
            title = { Text("Save Route") },
            text = {
                Column {
                    Text("Enter a name for your route:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = routeName,
                        onValueChange = { mapViewModel.setRouteName(it) },
                        placeholder = { Text("Route name") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val userId = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    if (userId.isNotEmpty()) {
                        mapViewModel.saveRoute(userId, totalDistance, elapsedTime)
                    }
                    mapViewModel.setShowRouteNameDialog(false)
                    mapViewModel.resetRouteName()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = {
                    mapViewModel.setShowRouteNameDialog(false)
                    mapViewModel.resetRouteName()
                    mapViewModel.clearRoutePoints()
                    elapsedTime = 0L
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MapViewComposable(
    fusedLocationClient: FusedLocationProviderClient,
    routePoints: List<LatLng>,
    isTracking: Boolean,
    totalDistance: Float,
    distance: Float,
    startTime: Long,
    elapsedTime: Long,
    onDistanceUpdated: (Float) -> Unit,
    onElapsedTimeUpdated: (Long) -> Unit,
    onRoutePointAdded: (LatLng) -> Unit,
    onRouteCleared: () -> Unit  // New parameter to clear the route
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    // Track the last time we added a point
    var lastPointAddedTime by remember { mutableStateOf(0L) }
    val MINIMUM_POINT_INTERVAL = 5000L // 5 seconds in milliseconds

    // Location updates
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setMinUpdateIntervalMillis(2000)
        .build()

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    // Only add points if tracking is enabled
                    if (isTracking) {
                        val currentTime = System.currentTimeMillis()

                        // Only add point if enough time has passed since the last point
                        if (currentTime - lastPointAddedTime >= MINIMUM_POINT_INTERVAL) {
                            val point = LatLng(location.latitude, location.longitude)
                            onRoutePointAdded(point)
                            lastPointAddedTime = currentTime

                            // Calculate and update distance only when we add a new point
                            if (routePoints.size > 1) {
                                val lastLocation = Location("").apply {
                                    latitude = routePoints[routePoints.size - 2].latitude
                                    longitude = routePoints[routePoints.size - 2].longitude
                                }
                                val distance = lastLocation.distanceTo(Location("").apply {
                                    latitude = location.latitude
                                    longitude = location.longitude
                                })
                                onDistanceUpdated(totalDistance + distance)
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onStart()

        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { googleMap ->
            googleMap.getMapAsync { gMap ->
                // Enable zoom controls and pan gestures at all times
                gMap.uiSettings.isZoomControlsEnabled = true
                gMap.uiSettings.isScrollGesturesEnabled = true
                gMap.uiSettings.isMyLocationButtonEnabled = true

                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gMap.isMyLocationEnabled = true

                    // Only request location updates when tracking is enabled
                    if (isTracking) {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                    } else {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                    }

                    // Update polyline only if tracking is enabled
                    if (isTracking && routePoints.isNotEmpty()) {
                        gMap.clear()  // Clear existing polyline before adding the new one
                        gMap.addPolyline(
                            PolylineOptions()
                                .addAll(routePoints)
                                .color(Color.BLUE)
                                .width(25f)
                        )
                    }

                    // Center the map on the first route point or user's current location
                    if (routePoints.isNotEmpty()) {
                        val bounds = LatLngBounds.Builder()
                        routePoints.forEach { bounds.include(it) }
                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
                        gMap.animateCamera(cameraUpdate)
                    }
                }
            }
        }
    )
}

private fun formatTime(timeInMillis: Long): String {
    val seconds = (timeInMillis / 1000) % 60
    val minutes = (timeInMillis / (1000 * 60)) % 60
    val hours = (timeInMillis / (1000 * 60 * 60))
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}



