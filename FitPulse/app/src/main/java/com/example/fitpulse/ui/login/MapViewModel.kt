package com.example.fitpulse.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitpulse.data.RouteRepository
import com.example.fitpulse.domain.model.RouteData
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.fitpulse.domain.RoutesLogic


class MapViewModel(private val routesLogic: RoutesLogic) : ViewModel() {

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    private val _showRouteNameDialog = MutableStateFlow(false)
    val showRouteNameDialog: StateFlow<Boolean> get() = _showRouteNameDialog

    private val _routeName = MutableStateFlow("")
    val routeName: StateFlow<String> get() = _routeName

    private val _saveRouteResult = MutableStateFlow<Result<Unit>?>(null)
    val saveRouteResult: StateFlow<Result<Unit>?> get() = _saveRouteResult

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
    }

    fun clearRoutePoints() {
        viewModelScope.launch {
            _routePoints.value = emptyList()
        }
    }

    fun saveRoute(userId: String, totalDistance: Float, elapsedTime: Long) {
        viewModelScope.launch {
            val routeData = RouteData(
                routeName = _routeName.value,
                routePoints = _routePoints.value,
                totalDistance = totalDistance,
                elapsedTime = elapsedTime,
                isPublic = false
            )
            val result = routesLogic.saveRoute(userId, routeData)
            _saveRouteResult.value = result
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
