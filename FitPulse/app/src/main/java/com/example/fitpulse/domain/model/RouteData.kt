package com.example.fitpulse.domain.model

import com.google.android.gms.maps.model.LatLng

data class RouteData(
    val routeName: String,
    val routePoints: List<LatLng>,
    val totalDistance: Float,
    val elapsedTime: Long,
    val isPublic: Boolean
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "routeName" to routeName,
            "routePoints" to routePoints.map { mapOf("lat" to it.latitude, "lng" to it.longitude) },
            "totalDistance" to totalDistance,
            "elapsedTime" to elapsedTime,
            "isPublic" to isPublic
        )
    }
}