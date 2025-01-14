package com.example.routes.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.routes.data.RouteRepository
import com.example.routes.domain.model.RouteData
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.routes.domain.RoutesLogic
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.log


class MapViewModel(private val routesLogic: RoutesLogic) : ViewModel() {
    init {
        Log.d("MapViewModel", "ViewModel initialized with ID: ${this.hashCode()}")
    }

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    private val _showRouteNameDialog = MutableStateFlow(false)
    val showRouteNameDialog: StateFlow<Boolean> get() = _showRouteNameDialog

    private val _routeName = MutableStateFlow("")
    val routeName: StateFlow<String> get() = _routeName

    private val _saveRouteResult = MutableStateFlow<Result<Unit>?>(null)
    val saveRouteResult: StateFlow<Result<Unit>?> get() = _saveRouteResult

    //---------------------------------//

    private val _routeHeartRates = MutableStateFlow<List<Int>>(emptyList())
    val routeHeartRates: StateFlow<List<Int>> = _routeHeartRates

    private val _routeTimestamps = MutableStateFlow<List<Long>>(emptyList())
    val routeTimestamps: StateFlow<List<Long>> = _routeTimestamps

    private val _currentHeartRate = MutableStateFlow(0)
    val currentHeartRate: StateFlow<Int> = _currentHeartRate

    private val _isRouteActive = MutableStateFlow(false)
    val isRouteActive: StateFlow<Boolean> get() = _isRouteActive

    private val _distance = MutableStateFlow(0f)
    val distance: StateFlow<Float> = _distance


    private val _watchConnectionStatus = MutableStateFlow("Checking watch connection...")
    val watchConnectionStatus: StateFlow<String> = _watchConnectionStatus

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    val userId = FirebaseAuth.getInstance().currentUser?.email ?: ""


    fun updateHeartRate(heartRate: Int) {
        _currentHeartRate.value = heartRate
    }

    fun setTotalDistance(distance: Float) {
        Log.d("MapViewModel", "Setting total distance: $distance")
        _distance.value = distance
    }

    fun setRouteActive(active: Boolean) {
        Log.d("MapViewModel", "Setting route active: $active in ViewModel: ${this.hashCode()}")
        _isRouteActive.value = active
    }

    fun updateWatchConnectionStatus(status: String) {
        _watchConnectionStatus.value = status
        Log.d("MapViewModel", "Watch connection status: $status")
    }

    fun handleRouteStop(
        timestamps: List<Long>,
        heartRates: List<Int>,
        elapsedTime: Long,
        routeName: String
    ) {
        Log.d("MapViewModel", "Route stopped")
        Log.d("MapViewModel", "Timestamps: $timestamps")
        Log.d("MapViewModel", "Heart rates: $heartRates")
        Log.d("MapViewModel", "Elapsed time: $elapsedTime")
        Log.d("MapViewModel", "Route name: $routeName")
        viewModelScope.launch {
            _routeTimestamps.value = timestamps
            _routeHeartRates.value = heartRates
            _elapsedTime.value = elapsedTime
            _routeName.value = routeName
            _isRouteActive.value = false
        }
        //saveRoute
        saveRoute(userId, _distance.value, elapsedTime)
        setRouteActive(false)
    }

    //---------------------------------//

    fun setShowRouteNameDialog(show: Boolean) {
        _showRouteNameDialog.value = show
    }

    fun setRouteName(name: String) {
        _routeName.value = name
    }

    fun resetRouteName() {
        _routeName.value = ""
    }

    fun addRoutePoint(point: LatLng) {
        viewModelScope.launch {
            _routePoints.value = _routePoints.value + point
        }
        Log.d("MapViewModel", "Added route point: $point")
        Log.d("MapViewModel", "Route points1: ${_routePoints.value}")
    }

    fun clearRoutePoints() {
        viewModelScope.launch {
            _routePoints.value = emptyList()
        }
    }

    fun saveRoute(userId: String, totalDistance: Float, elapsedTime: Long) {
        Log.d("MapViewModel", "Route_points: ${_routePoints.value}")
        viewModelScope.launch {
            val routeData = RouteData(
                routeName = _routeName.value,
                routePoints = _routePoints.value,
                totalDistance = totalDistance,
                elapsedTime = elapsedTime,
                isPublic = false,
                routeHeartRates = _routeHeartRates.value,
                routeTimestamps = _routeTimestamps.value
            )
            val result = routesLogic.saveRoute(userId, routeData)
            _saveRouteResult.value = result
            Log.d("MapViewModel", "Routepoints: ${_routePoints.value}")
            clearRoutePoints()
            setTotalDistance(0f)
        }
    }
}

class MapViewModelFactory(private val routeRepository: RouteRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(RoutesLogic(routeRepository)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
