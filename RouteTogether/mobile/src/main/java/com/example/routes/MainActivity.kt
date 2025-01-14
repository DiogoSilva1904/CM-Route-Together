package com.example.routes

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.routes.ui.login.DashboardScreen
import com.example.routes.ui.login.LoginScreen
import com.example.routes.ui.login.MapViewModel
import com.example.routes.ui.login.MapViewModelFactory
import com.example.routes.ui.login.SignUpScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.patrykandpatryk.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatryk.vico.compose.axis.vertical.startAxis
import com.patrykandpatryk.vico.compose.chart.Chart
import com.patrykandpatryk.vico.compose.chart.line.lineChart
import com.patrykandpatryk.vico.core.entry.entryModelOf
import com.patrykandpatryk.vico.core.entry.FloatEntry
import com.example.routes.data.RouteRepository
import com.example.routes.ui.login.InitialScreen
import com.google.firebase.FirebaseApp


class MainActivity : ComponentActivity() {
    private var currentHeartRate by mutableStateOf(0)
    private var isRouteActive by mutableStateOf(false)
    private var watchConnectionStatus by mutableStateOf("Checking watch connection...")
    private var routeHeartRates = mutableStateListOf<Int>()
    private var routeTimestamps = mutableStateListOf<Long>()
    private var elapsedTime by mutableStateOf(0L)
    private var routeName by mutableStateOf("")

    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val nodeClient by lazy { Wearable.getNodeClient(this) }


    // Initialize these after Firebase is guaranteed to be initialized
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val routeRepository by lazy { RouteRepository(db) }
    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(routeRepository)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupWearableConnections()

        setContent {
            MaterialTheme {
                MyApp(db, viewModel)
//                MainScreen(
//                    currentHeartRate = currentHeartRate,
//                    isRouteActive = isRouteActive,
//                    watchConnectionStatus = watchConnectionStatus,
//                    routeHeartRates = routeHeartRates.toList(),
//                    routeTimestamps = routeTimestamps.toList(),
//                    elapsedTime = elapsedTime,
//                    routeName = routeName
//                )
            }
        }
    }

    private fun setupWearableConnections() {
        Log.d("PhoneApp", "Setting up Wearable connections")
        // Listen for data from watch
        dataClient.addListener { dataEvents ->
            dataEvents.forEach { event ->
                when (event.type) {
                    DataEvent.TYPE_CHANGED -> {
                        when (event.dataItem.uri.path) {
                            "/heartRate" -> handleHeartRateUpdate(DataMapItem.fromDataItem(event.dataItem))
                            "/stopRoute" -> handleRouteStop(DataMapItem.fromDataItem(event.dataItem))
                            "/startRoute" -> lifecycleScope.launch {
                                viewModel.setRouteActive(true)
                            }
                        }
                    }
                }
            }
        }

        // Check watch connection periodically
        lifecycleScope.launch {
            while (isActive) {
                checkWatchConnection()
                viewModel.updateWatchConnectionStatus(watchConnectionStatus)
                delay(5000)
            }
        }
    }

//    private fun handleHeartRateUpdate(dataMap: DataMapItem) {
//        val heartRate = dataMap.dataMap.getInt("heartRate")
//        currentHeartRate = heartRate
//    }
//
//    private fun handleRouteStop(dataMap: DataMapItem) {
//        val timestamps = dataMap.dataMap.getLongArray("timestamps") ?: longArrayOf()
//        val heartRates = dataMap.dataMap.getIntegerArrayList("heartRates") ?: arrayListOf()
//        elapsedTime = dataMap.dataMap.getLong("elapsedTime")
//        routeName = dataMap.dataMap.getString("routeName") ?: ""
//
//        routeHeartRates.clear()
//        routeTimestamps.clear()
//        routeHeartRates.addAll(heartRates)
//        routeTimestamps.addAll(timestamps.toList())
//
//        isRouteActive = false
//    }

    private fun handleHeartRateUpdate(dataMap: DataMapItem) {
        val heartRate = dataMap.dataMap.getInt("heartRate")
        viewModel.updateHeartRate(heartRate)
    }

    private fun handleRouteStop(dataMap: DataMapItem) {
        val timestamps = dataMap.dataMap.getLongArray("timestamps")?.toList() ?: emptyList()
        val heartRates = dataMap.dataMap.getIntegerArrayList("heartRates")?.toList() ?: emptyList()
        val elapsedTime = dataMap.dataMap.getLong("elapsedTime")
        val routeName = dataMap.dataMap.getString("routeName") ?: ""

        viewModel.handleRouteStop(timestamps, heartRates, elapsedTime, routeName)
    }

    private suspend fun checkWatchConnection() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            watchConnectionStatus = if (nodes.isNotEmpty()) {
                "Watch connected: ${nodes[0].displayName}"
            } else {
                "No watch connected"
            }

        } catch (e: Exception) {
            watchConnectionStatus = "Connection check failed"
        }
    }




    @Composable
    fun MainScreen(
        currentHeartRate: Int,
        isRouteActive: Boolean,
        watchConnectionStatus: String,
        routeHeartRates: List<Int>,
        routeTimestamps: List<Long>,
        elapsedTime: Long,
        routeName: String
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Connection Status
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = watchConnectionStatus,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Current Heart Rate
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Heart Rate",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$currentHeartRate BPM",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Route Status and Data
            if (routeHeartRates.isNotEmpty() && !isRouteActive) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Last Route Summary",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Route: $routeName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Duration: ${formatDuration(elapsedTime)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Average HR: ${routeHeartRates.average().toInt()} BPM",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Max HR: ${routeHeartRates.maxOrNull() ?: 0} BPM",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Min HR: ${routeHeartRates.minOrNull() ?: 0} BPM",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Heart Rate Chart
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HeartRateChart(
                        heartRates = routeHeartRates,
                        timestamps = routeTimestamps
                    )
                }
            }

            // Active Route Status
            if (isRouteActive) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Route in Progress",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun HeartRateChart(heartRates: List<Int>, timestamps: List<Long>) {
        val entries = heartRates.mapIndexed { index, heartRate ->
            FloatEntry(
                x = index.toFloat(),
                y = heartRate.toFloat()
            )
        }

        Chart(
            chart = lineChart(),
            model = entryModelOf(entries),
            startAxis = startAxis(),
            bottomAxis = bottomAxis(),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}

@Composable
fun MyApp(db: FirebaseFirestore, viewModel: MapViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "homeScreen") {
        // Home Screen - initial entry point of the app
        composable("homeScreen") {
            InitialScreen(navController = navController)
        }

        // Login Screen
        composable("loginScreen") {
            LoginScreen(navController = navController)
        }

        // SignUp Screen
        composable("signUpScreen") {
            SignUpScreen(navController = navController)
        }

        composable("dashboardScreen") {
            DashboardScreen(db=db, viewModel = viewModel)
        }
    }
}