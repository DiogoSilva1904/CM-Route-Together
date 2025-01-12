package com.example.fitpulse.ui.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpulse.data.RouteRepository
import com.example.fitpulse.domain.model.RouteData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.layout.ContentScale

@Composable
fun RouteDetailsScreen(
    routeName: String,
    userEmail: String,
    db: FirebaseFirestore
) {
    val routeRepository = RouteRepository(db)

    // Remember the routeData to prevent fetching it on each recomposition
    var routeData by remember { mutableStateOf<RouteData?>(null) }

    // Only fetch data once, not every time the composable recomposes
    LaunchedEffect(routeName, userEmail) {
        // Fetch data only if routeData is null (meaning it's not loaded yet)
        if (routeData == null) {
            val fetchedRoutes = routeRepository.fetchRouteByName(userEmail,routeName)
            routeData = fetchedRoutes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Make the column scrollable
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (routeData != null) {
            val routePoints = routeData?.routePoints ?: emptyList()

            // Route Name at the Top Center
            Text(
                text = routeData?.routeName ?: "Unnamed Route",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            // Map Section
            if (routePoints.isNotEmpty()) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(vertical = 8.dp),
                    cameraPositionState = rememberCameraPositionState {
                        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                            routePoints.first(), 15f
                        )
                    }
                ) {
                    Polyline(
                        points = routePoints,
                        width = 5f,
                        color = Color.Blue
                    )
                }
            } else {
                Text(
                    "No route points available.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Distance and Time Cards in a Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, // Adjust the cards placement
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TimeCard on the Left
                TimeCard(elapsedTime = routeData?.elapsedTime, modifier = Modifier.weight(1f))

                // DistanceCard on the Right
                DistanceCard(totalDistance = routeData?.totalDistance, modifier = Modifier.weight(1f))
            }
        } else {
            // Loading Indicator
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}


@Composable
fun DistanceCard(totalDistance: Float?,modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = "Distance", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Distance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${totalDistance ?: 0.0} km",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TimeCard(elapsedTime: Long?,modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.DateRange, contentDescription = "Elapsed Time", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${elapsedTime ?: 0} sec",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


