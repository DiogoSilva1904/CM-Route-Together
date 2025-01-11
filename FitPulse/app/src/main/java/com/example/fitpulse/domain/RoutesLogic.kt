package com.example.fitpulse.domain

import com.example.fitpulse.data.RouteRepository
import com.example.fitpulse.domain.model.RouteData

class RoutesLogic(private val routeRepository: RouteRepository) {

    suspend fun saveRoute(userId: String, routeData: RouteData): Result<Unit> {
        return routeRepository.saveRoute(userId, routeData)
    }
}
