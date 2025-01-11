package com.example.fitpulse.ui.login

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitpulse.data.RouteRepository
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline

@Composable
fun MapScreen(db: FirebaseFirestore) {
    val context = LocalContext.current
    val routeRepository = RouteRepository(db)

    val mapViewModel = viewModel<MapViewModel>(
        factory = MapViewModelFactory(routeRepository)
    )

    // Observing states from ViewModel
    val isLocationPermissionGranted = rememberSaveable {
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val routePoints by mapViewModel.routePoints.collectAsState()
    val showRouteNameDialog by mapViewModel.showRouteNameDialog.collectAsState()
    val routeName by mapViewModel.routeName.collectAsState()
    val saveRouteResult by mapViewModel.saveRouteResult.collectAsState()

    var isTracking by rememberSaveable { mutableStateOf(false) }
    var totalDistance by rememberSaveable { mutableStateOf(0f) }
    var startTime by rememberSaveable { mutableStateOf<Long>(0L) }
    var elapsedTime by rememberSaveable { mutableStateOf(0L) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Handle save result
    LaunchedEffect(saveRouteResult) {
        saveRouteResult?.onSuccess {
            Log.d("MapScreen", "Route saved successfully!")
        }?.onFailure { exception ->
            Log.e("MapScreen", "Error saving route: ${exception.message}")
        }
    }

    // Box layout for map and controls
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLocationPermissionGranted) {
            MapViewComposable(
                fusedLocationClient = fusedLocationClient,
                routePoints = routePoints,
                isTracking = isTracking,
                totalDistance = totalDistance,
                startTime = startTime,
                elapsedTime = elapsedTime,
                onDistanceUpdated = { newDistance -> totalDistance = newDistance },
                onElapsedTimeUpdated = { newElapsedTime -> elapsedTime = newElapsedTime },
                onRoutePointAdded = { mapViewModel.addRoutePoint(it) },
                onRouteCleared = { mapViewModel.clearRoutePoints() }  // Clear route when tracking stops
            )

            // Start/Stop Tracking Button
            Button(
                onClick = {
                    if (isTracking) {
                        isTracking = false
                        mapViewModel.setShowRouteNameDialog(true)
                        mapViewModel.clearRoutePoints() // Clear the route points when stopped
                    } else {
                        isTracking = true
                        startTime = System.currentTimeMillis()
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                val startPoint = LatLng(it.latitude, it.longitude)
                                mapViewModel.addRoutePoint(startPoint) // Add the starting point
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(text = if (isTracking) "Stop Tracking" else "Start Tracking")
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
                    mapViewModel.clearRoutePoints()
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
    startTime: Long,
    elapsedTime: Long,
    onDistanceUpdated: (Float) -> Unit,
    onElapsedTimeUpdated: (Long) -> Unit,
    onRoutePointAdded: (LatLng) -> Unit,
    onRouteCleared: () -> Unit  // New parameter to clear the route
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

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
                        val point = LatLng(location.latitude, location.longitude)
                        onRoutePointAdded(point)

                        // Calculate and update distance only when tracking
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
                                .width(5f)
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



