package com.example.fitpulse.data

import com.example.fitpulse.domain.model.RouteData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RouteRepository(private val db: FirebaseFirestore) {
    suspend fun saveRoute(userId: String, routeData: RouteData): Result<Unit> {
        return try {
            db.collection("users")
                .document(userId)
                .collection("routes")
                .add(routeData)
                .await() // Ensure it's a coroutine-friendly operation
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
