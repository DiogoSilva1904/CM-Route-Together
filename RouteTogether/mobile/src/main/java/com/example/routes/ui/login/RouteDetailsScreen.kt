package com.example.routes.ui.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
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
import com.example.routes.data.RouteRepository
import com.example.routes.domain.model.RouteData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.layout.ContentScale
import com.patrykandpatryk.vico.compose.axis.axisLabelComponent
import com.patrykandpatryk.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatryk.vico.compose.axis.vertical.startAxis
import com.patrykandpatryk.vico.compose.chart.Chart
import com.patrykandpatryk.vico.compose.chart.line.lineChart
import com.patrykandpatryk.vico.core.entry.FloatEntry
import com.patrykandpatryk.vico.core.entry.entryModelOf

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
            val heartRates = routeData?.routeHeartRates?.map { it.toInt() } ?: emptyList()


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
                        width = 25f,
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

            // Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeCard(elapsedTime = routeData?.elapsedTime, modifier = Modifier.weight(1f))
                DistanceCard(totalDistance = routeData?.totalDistance, modifier = Modifier.weight(1f))
            }

            // Heart Rate Section
            if (heartRates.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Heart Rate Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HeartRateStat(
                                label = "Max HR",
                                value = heartRates.maxOrNull() ?: 0,
                                modifier = Modifier.weight(1f)
                            )
                            HeartRateStat(
                                label = "Min HR",
                                value = heartRates.minOrNull() ?: 0,
                                modifier = Modifier.weight(1f)
                            )
                            HeartRateStat(
                                label = "Avg HR",
                                value = heartRates.average().toInt(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Heart Rate Chart
                        val entries = heartRates.mapIndexed { index, heartRate ->
                            FloatEntry(
                                x = index.toFloat(),
                                y = heartRate.toFloat()
                            )
                        }

                        Chart(
                            chart = lineChart(),
                            model = entryModelOf(entries),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp), // Increase the height
                            startAxis = startAxis(
                                label = axisLabelComponent(color = MaterialTheme.colorScheme.onSurface),
                                valueFormatter = { value, _ -> "${value.toInt()} bpm" }
                            ),
                            bottomAxis = bottomAxis(

                            )
                        )

                    }
                }
            }
        } else {
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
                text = "${totalDistance ?: 0.0} m",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TimeCard(elapsedTime: Long?, modifier: Modifier) {
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
                text = formatElapsedTime(elapsedTime ?: 0L),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HeartRateStat(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$value bpm",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatElapsedTime(timeInMillis: Long): String {
    val seconds = (timeInMillis / 1000) % 60
    val minutes = (timeInMillis / (1000 * 60)) % 60
    val hours = (timeInMillis / (1000 * 60 * 60))
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}


