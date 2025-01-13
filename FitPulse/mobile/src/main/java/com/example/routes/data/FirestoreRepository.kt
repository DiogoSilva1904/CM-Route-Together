package com.example.routes.data

import android.util.Log
import com.example.routes.domain.model.RouteData
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RouteRepository(private val db: FirebaseFirestore) {
    suspend fun saveRoute(userId: String, routeData: RouteData): Result<Unit> {
        return try {
            Log.d("RouteRepository", "Route data: $routeData")
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

    suspend fun fetchFriends(userEmail: String): List<String> {
        return try {
            val documents = db.collection("users")
                .document(userEmail)
                .collection("friends")
                .get()
                .await()

            // Return the list of friends' emails
            documents.map { it.getString("email") ?: "" }
        } catch (e: Exception) {
            emptyList() // Return an empty list in case of error
        }
    }

    // Add a friend to the user's friend list
    suspend fun addFriend(userEmail: String, friendEmail: String) {
        try {
            val friendData = hashMapOf("email" to friendEmail)
            db.collection("users")
                .document(userEmail)
                .collection("friends")
                .add(friendData)
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to add friend.")
        }
    }

    // Check if the current user is already a friend of another user
    suspend fun checkIfFriend(userEmail: String, friendEmail: String): Boolean {
        return try {
            val documents = db.collection("users")
                .document(userEmail)
                .collection("friends")
                .whereEqualTo("email", friendEmail)
                .get()
                .await()

            !documents.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchRoutes(userEmail: String): List<RouteData> {
        return try {
            val documents = db.collection("users")
                .document(userEmail)
                .collection("routes")
                .get()
                .await()

            // Map the documents to RouteData objects
            documents.map { document ->
                val routeName = document.getString("routeName") ?: "Unknown"
                val routePoints = (document.get("routePoints") as? List<Map<String, Any>>)?.map {
                    val lat = it["lat"] as? Double ?: 0.0
                    val lng = it["lng"] as? Double ?: 0.0
                    LatLng(lat, lng)
                } ?: emptyList()

                val totalDistance = document.getDouble("totalDistance")?.toFloat() ?: 0f
                val elapsedTime = document.getLong("elapsedTime") ?: 0L

                val isPublic = document.getBoolean("isPublic") ?: false

                val routeHeartRates = document.get("routeHeartRates") as? List<Int> ?: emptyList()
                val routeTimestamps = document.get("routeTimestamps") as? List<Long> ?: emptyList()


                RouteData(routeName, routePoints, totalDistance, elapsedTime,isPublic, routeHeartRates, routeTimestamps)
            }
        } catch (e: Exception) {
            Log.e("fetchRoutes", "Error fetching routes for user $userEmail", e)
            emptyList()
        }
    }

    suspend fun fetchRouteByName(userEmail: String, routeName: String): RouteData? {
        return try {
            val documentSnapshot = db.collection("users")
                .document(userEmail)
                .collection("routes")
                .whereEqualTo("routeName", routeName) // Filter by routeName
                .get()
                .await()

            // Check if a document with the specified routeName exists
            if (documentSnapshot.isEmpty) {
                return null // No route found with the specified name
            }

            // Assume there's only one document, as we're filtering by routeName
            val document = documentSnapshot.documents.first()

            // Extract route points from Firestore and map to LatLng
            val routePoints = (document.get("routePoints") as? List<Map<String, Any>>)?.map { point ->
                // Extract lat/lng values, with defaulting to 0.0 in case of missing or invalid data
                val lat = point["latitude"] as? Double ?: 0.0
                val lng = point["longitude"] as? Double ?: 0.0

                LatLng(lat, lng)
            } ?: emptyList()


            val totalDistance = document.getDouble("totalDistance")?.toFloat() ?: 0f
            val elapsedTime = document.getLong("elapsedTime") ?: 0L

            val isPublic = document.getBoolean("isPublic") ?: false

            val routeHeartRates = document.get("routeHeartRates") as? List<Int> ?: emptyList()
            val routeTimestamps = document.get("routeTimestamps") as? List<Long> ?: emptyList()

            RouteData(routeName, routePoints, totalDistance, elapsedTime, isPublic, routeHeartRates, routeTimestamps)
        } catch (e: Exception) {
            Log.e("fetchRouteByName", "Error fetching route '$routeName' for user $userEmail", e)
            null
        }
    }

    suspend fun updateRouteVisibility(userEmail: String, routeName: String, isPublic: Boolean) {
        try {
            // Get a reference to the user's routes collection
            val userRef = db.collection("users").document(userEmail)

            // Query the routes collection to find the document with the matching routeName
            val querySnapshot = userRef.collection("routes")
                .whereEqualTo("routeName", routeName)
                .get()
                .await()

            // Check if the route document exists
            if (!querySnapshot.isEmpty) {
                // Assuming only one document matches the routeName
                val routeDocument = querySnapshot.documents.first()

                // Get the routeId (document ID)
                val routeId = routeDocument.id

                // Get a reference to the route document by ID
                val routeRef = userRef.collection("routes").document(routeId)

                // Update the visibility of the route
                routeRef.update("isPublic", isPublic)
                    .addOnSuccessListener {
                        Log.d("RouteRepository", "Route visibility updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("RouteRepository", "Error updating route visibility", e)
                    }
            } else {
                Log.e("RouteRepository", "Route document with name $routeName not found")
            }
        } catch (e: Exception) {
            Log.e("RouteRepository", "Error updating route visibility", e)
        }
    }


}
