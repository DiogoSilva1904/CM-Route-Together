/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.routes.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.SampleDataPoint
import androidx.health.services.client.unregisterMeasureCallback
import com.example.routes.presentation.theme.RoutesTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await
import androidx.health.services.client.data.*
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds




class MainActivity : ComponentActivity() {
    private var heartbeat by mutableStateOf(0)
    private var isRouteActive by mutableStateOf(false)
    private var connectionStatus by mutableStateOf("Checking connection...")
    private var deviceConnectionStatus by mutableStateOf("Checking device...")
    private var measureClient by mutableStateOf<androidx.health.services.client.MeasureClient?>(null)
    private val heartRateReadings = mutableListOf<Pair<Long, Int>>()
    private var heartRateJob: Job? = null
    private var connectionCheckJob: Job? = null
    private var elapsedTime by mutableStateOf(Duration.ZERO)
    private var timerJob: Job? = null
    private var routeName by mutableStateOf("")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startConnectionChecking()

        lifecycleScope.launch {
            initializeHealthServices()
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                WearableContent(
                    heartbeat = heartbeat,
                    isRouteActive = isRouteActive,
                    connectionStatus = connectionStatus,
                    deviceConnectionStatus = deviceConnectionStatus,
                    elapsedTime = elapsedTime,
                    onRouteToggle = { toggleRoute() },
                    onRouteStop = { routeName -> toggleRoute(routeName) }

                )
            }
        }
    }

    private fun startConnectionChecking() {
        connectionCheckJob = lifecycleScope.launch {
            while (isActive) {
                checkDeviceConnection()
                delay(5000) // Check every 5 seconds
            }
        }
    }

    @SuppressLint("VisibleForTests")
    private suspend fun checkDeviceConnection() {
        try {
            val nodes = Wearable.getNodeClient(this).connectedNodes.await()
            deviceConnectionStatus = if (nodes.isNotEmpty()) {
                "Phone connected: " + nodes[0].displayName
            } else {
                "No phone connected"
            }
        } catch (e: Exception) {
            deviceConnectionStatus = "Connection check failed"
            Log.e("WearApp", "Connection check error", e)
        }
    }

    private fun startTimer() {
        timerJob = lifecycleScope.launch {
            var seconds = 0L
            while (isActive) {
                elapsedTime = seconds.seconds
                delay(1000)
                seconds++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    private fun resetTimer() {
        elapsedTime = Duration.ZERO
    }

    override fun onResume() {
        super.onResume()
        startHeartRateTracking()
        lifecycleScope.launch {
            checkDeviceConnection()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isRouteActive) {
            stopHeartRateTracking()
        }
    }

    private suspend fun initializeHealthServices() {
        try {
            val healthClient = HealthServices.getClient(this@MainActivity)
            measureClient = healthClient.measureClient
            startHeartRateTracking()
            Log.d("Health", "Health client initialized")
        } catch (e: Exception) {
            Log.e("Health", "Error initializing health client", e)
            connectionStatus = "Health Services not available"
        }
    }

    private fun startHeartRateTracking() {
        lifecycleScope.launch {
            try {
                measureClient?.let { client ->
                    client.registerMeasureCallback(
                        DataType.HEART_RATE_BPM,
                        measureCallback
                    )
                    Log.d("Health", "Heart rate tracking started")
                } ?: run {
                    connectionStatus = "Health Services not initialized"
                }
            } catch (e: Exception) {
                connectionStatus = "Heart rate tracking failed"
                Log.e("Health", "Error starting tracking", e)
            }
        }
    }

    private fun stopHeartRateTracking() {
        lifecycleScope.launch {
            try {
                measureClient?.unregisterMeasureCallback(
                    DataType.HEART_RATE_BPM,
                    measureCallback
                )
            } catch (e: Exception) {
                Log.e("Health", "Error stopping tracking", e)
            }
        }
    }

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            connectionStatus = "Sensor status changed"
            Log.d("Health", "Availability changed: $availability")
        }

        override fun onDataReceived(data: DataPointContainer) {
            Log.d("Health", "Data received: $data")
            data.getData(DataType.HEART_RATE_BPM).forEach { dataPoint ->
                if (dataPoint is SampleDataPoint) {
                    heartbeat = dataPoint.value.toInt()
                    // Send heart rate to phone immediately
                    lifecycleScope.launch {
                        sendHeartRateToPhone(heartbeat)
                    }
                    if (isRouteActive && heartbeat > 0) {
                        heartRateReadings.add(Pair(System.currentTimeMillis(), heartbeat))
                    }
                    // If we're getting data, the sensor must be available
                    connectionStatus = "Sensor ready"
                }
            }
        }
    }

    private fun toggleRoute(routeName: String = "") {
        if (!isRouteActive) {
            startRoute()
        } else {
            stopRoute(routeName)
        }
        isRouteActive = !isRouteActive
    }


    private fun startRoute() {
        heartRateReadings.clear()
        resetTimer()
        startTimer()
        heartRateJob = lifecycleScope.launch {
            sendStartRouteMessageToPhone()

        }
    }

    private fun stopRoute(routeName: String) {
        heartRateJob?.cancel()
        Log.d("Elapsed time", "Elapsed time: $elapsedTime")
        stopTimer()
        lifecycleScope.launch {
            Log.d("Health", "Heart rate readings: $heartRateReadings")
            Log.d("Health", "Sending heart rate list to phone")
            sendStopRouteMessageToPhone(heartRateReadings, elapsedTime, routeName)
        }
    }


    @SuppressLint("VisibleForTests")
    private suspend fun sendStartRouteMessageToPhone() {
        try {
            val dataRequest = PutDataMapRequest.create("/startRoute").apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(this).putDataItem(dataRequest).await()
            Log.d("WearApp", "Start route message sent to phone")
        } catch (e: Exception) {
            Log.e("WearApp", "Error sending start route message", e)
        }
    }

    @SuppressLint("VisibleForTests")
    private suspend fun sendHeartRateToPhone(heartRate: Int) {
        try {
            val dataRequest = PutDataMapRequest.create("/heartRate").apply {
                dataMap.putInt("heartRate", heartRate)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(this).putDataItem(dataRequest).await()
            Log.d("WearApp", "Heart rate sent to phone: $heartRate")
        } catch (e: Exception) {
            Log.e("WearApp", "Error sending heart rate", e)
        }
    }

    @SuppressLint("VisibleForTests")
    private suspend fun sendStopRouteMessageToPhone(readings: List<Pair<Long, Int>>, elapsedTime: Duration, routeName: String) {
        try {
            val dataRequest = PutDataMapRequest.create("/stopRoute").apply {
                dataMap.putLongArray("timestamps", readings.map { it.first }.toLongArray())
                val heartRates = ArrayList<Int>(readings.map { it.second })
                dataMap.putIntegerArrayList("heartRates", heartRates)
                dataMap.putLong("elapsedTime", elapsedTime.inWholeMilliseconds)
                dataMap.putString("routeName", routeName)
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(this).putDataItem(dataRequest).await()
            Log.d("WearApp", "Heart rate list sent to phone")
            Log.d("WearApp", "Elapsed time sent to phone: $elapsedTime")
            Log.d("WearApp", "Route name sent to phone: $routeName")
            Log.d("WearApp", "Heart rate list: $readings")
        } catch (e: Exception) {
            Log.e("WearApp", "Error sending heart rate list", e)
        }
    }


    @Composable
    fun RouteNameDialog(
        onNameEntered: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        var name by remember { mutableStateOf("") }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Enter Route Name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Route Name", fontSize = 12.sp) },
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text("Cancel", fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onNameEntered(name) },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text("Save", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }




    @Composable
    fun WearableContent(
        heartbeat: Int,
        isRouteActive: Boolean,
        connectionStatus: String,
        deviceConnectionStatus: String,
        elapsedTime: Duration,
        onRouteToggle: () -> Unit,
        onRouteStop: (String) -> Unit
    ) {
        var showDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Route Tracker",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "♥️ $heartbeat BPM",
                        fontSize = 16.sp,
                    )
                    if (isRouteActive) {
                        Text(
                            text = formatDuration(elapsedTime),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Status Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = connectionStatus,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = deviceConnectionStatus,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (isRouteActive) {
                        showDialog = true
                    } else {
                        onRouteToggle()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(40.dp)
            ) {
                Text(
                    text = if (isRouteActive) "Stop Route" else "Start Route",
                    fontSize = 14.sp
                )
            }

            if (showDialog) {
                RouteNameDialog(
                    onNameEntered = { name ->
                        onRouteStop(name)
                        showDialog = false
                    },
                    onDismiss = { showDialog = false }
                )
            }
        }
    }

    private fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHeartRateTracking()
        heartRateJob?.cancel()
        connectionCheckJob?.cancel()
        timerJob?.cancel()
    }
}