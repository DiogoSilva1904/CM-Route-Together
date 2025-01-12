package com.example.fitpulse.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitpulse.data.RouteRepository
import com.example.fitpulse.domain.model.RouteData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val routeRepository: RouteRepository) : ViewModel() {

    private val _routes = MutableStateFlow<List<RouteData>>(emptyList())
    val routes: StateFlow<List<RouteData>> = _routes

    fun fetchRoutesForFriend(friendEmail: String) {
        viewModelScope.launch {
            // Fetch the routes from the repository
            val fetchedRoutes = routeRepository.fetchRoutes(friendEmail)

            // Filter the routes to include only public ones
            val publicRoutes = fetchedRoutes.filter { it.isPublic }

            // Only update the state if the filtered routes are different from the current ones
            if (_routes.value != publicRoutes) {
                _routes.value = publicRoutes
            }
        }
    }

}

class ProfileViewModelFactory(private val routeRepository: RouteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(routeRepository) as T
    }
}
