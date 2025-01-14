package com.example.routes.domain

import com.example.routes.data.RouteRepository
import com.example.routes.domain.model.RouteData

class RoutesLogic(private val routeRepository: RouteRepository) {

    suspend fun saveRoute(userId: String, routeData: RouteData): Result<Unit> {
        return routeRepository.saveRoute(userId, routeData)
    }
}
