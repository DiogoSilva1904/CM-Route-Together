package com.example.fitpulse.ui.login

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.util.GeoPoint
import org.osmdroid.config.Configuration

@Composable
fun MapScreen() {
    var isLocationPermissionGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Check if the location permission is granted
    LaunchedEffect(true) {
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!isLocationPermissionGranted) {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
    }

    // Trigger re-composition when permission is granted
    if (isLocationPermissionGranted) {
        MapViewComposable()  // Show the map when permission is granted
    } else {
        Text("Permission to access location is required.")
    }
}


@Composable
fun MapViewComposable() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val mapController: IMapController = mapView.controller
    val myLocationOverlay = MyLocationNewOverlay(mapView)

    // Set up the map and location overlay
    LaunchedEffect(true) {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Set initial zoom level and center point (San Francisco as fallback)
        mapController.setZoom(15.0)
        mapController.setCenter(GeoPoint(37.7749, -122.4194)) // San (maybe change this later,i tries but stopped working)

        // Initialize the location overlay and enable location updates
        myLocationOverlay.enableMyLocation()

        // When the location is fixed, move the map to the current location
        myLocationOverlay.runOnFirstFix {
            val location = myLocationOverlay.myLocation
            location?.let {
                val geoPoint = GeoPoint(it.latitude, it.longitude)
                mapController.setCenter(geoPoint)
                mapView.invalidate() // Refresh the map view to reflect the new location
            }
        }

        mapView.overlays.add(myLocationOverlay)
    }

    // Display the MapView
    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}
