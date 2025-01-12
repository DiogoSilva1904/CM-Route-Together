package com.example.fitpulse.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitpulse.data.RouteRepository
import com.example.fitpulse.domain.model.RouteData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val routeRepository: RouteRepository) : ViewModel() {
    private val _routes = MutableStateFlow<List<RouteData>>(emptyList())
    val routes: StateFlow<List<RouteData>> = _routes

    fun fetchRoutesForUser(userEmail: String) {
        viewModelScope.launch {
            val userRoutes = routeRepository.fetchRoutes(userEmail)
            _routes.value = userRoutes
        }
    }
    fun updateRouteVisibility(routeName: String, isPublic: Boolean,userEmail: String) {
        viewModelScope.launch {
            // Call the repository to update Firestore
            Log.d("ahhhh","state"+isPublic)
            routeRepository.updateRouteVisibility(
                userEmail,
                routeName = routeName,
                isPublic = isPublic
            )

            // Update the local state (optional, if you want to refresh the UI instantly)
            _routes.value = _routes.value.map { route ->
                if (route.routeName == routeName) {
                    route.copy(isPublic = isPublic)
                } else {
                    route
                }
            }
        }
    }
}

class HomeViewModelFactory(private val routeRepository: RouteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(routeRepository) as T
    }
}
