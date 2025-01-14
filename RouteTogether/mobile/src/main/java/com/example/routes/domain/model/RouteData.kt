package com.example.routes.domain.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.PropertyName


data class RouteData(
    val routeName: String,
    val routePoints: List<LatLng>,
    val totalDistance: Float,
    val elapsedTime: Long,
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = false,
    val routeHeartRates: List<Int>,
    val routeTimestamps: List<Long>
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "routeName" to routeName,
            "routePoints" to routePoints.map { mapOf("lat" to it.latitude, "lng" to it.longitude) },
            "totalDistance" to totalDistance,
            "elapsedTime" to elapsedTime,
            "isPublic" to isPublic,
            "routeHeartRates" to routeHeartRates,
            "routeTimestamps" to routeTimestamps
        )
    }
}