package com.example.fitpulse.ui.login

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fitpulse.R
import com.example.fitpulse.data.RouteRepository
import com.example.fitpulse.domain.model.RouteData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    db: FirebaseFirestore,
    navController: NavController
) {
    // Get the currently logged-in user's email
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "Unknown Email"

    val routeRepository = RouteRepository(db) // Creating an instance of RouteRepository

    // Use viewModel() to ensure the same ViewModel instance is used across recompositions
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(routeRepository))

    // Fetch routes only when the userEmail changes or when the composable is first launched
    LaunchedEffect(userEmail) {
        homeViewModel.fetchRoutesForUser(userEmail)
    }

    fun handleVisibilityChange(route: RouteData) {
        Log.d("HomeScreen", "Changing visibility for route: ${route.routeName}, current visibility: ${route.isPublic}")

        // Toggle visibility and update the UI
        val updatedRoute = route.copy(isPublic = !route.isPublic)
        Log.d("HomeScreen", "New visibility: ${updatedRoute.isPublic}")

        // Call the ViewModel method to update visibility
        homeViewModel.updateRouteVisibility(route.routeName, updatedRoute.isPublic, userEmail)
    }


    // Observe the list of routes
    val routes = homeViewModel.routes.collectAsState().value

    // Home screen UI
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Profile Picture (Replace with real profile picture if available)
            Image(
                painter = painterResource(id = R.drawable.lebron), // Replace with actual image resource
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape) // Optional border around the profile picture
            )

            // Email Address below the profile picture
            Spacer(modifier = Modifier.height(8.dp)) // Space between image and email
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        // Display routes in a LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(routes) { route ->
                // Display each route in a Card
                RouteCardUser(
                    route = route,
                    onClick = { selectedRoute ->
                        // Navigate to the RouteDetailsScreen with routeName and userEmail
                        navController.navigate("route_details/${selectedRoute.routeName}/$userEmail")
                    },
                    // Pass the visibility toggle function correctly as a lambda
                    onVisibilityChange = { handleVisibilityChange(route) }
                )
            }
        }
    }
}

@Composable
fun RouteCardUser(route: RouteData, onClick: (RouteData) -> Unit, onVisibilityChange: (RouteData) -> Unit) {
    Card(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth()
            .clickable { onClick(route) }
            .shadow(10.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon next to the route name
                Image(
                    painter = painterResource(id = R.drawable.route_4), // Assuming you have an icon
                    contentDescription = "Route Icon",
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = route.routeName.ifEmpty { "Unnamed Route" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier.weight(1f) // Push text to the left
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Eye Icon for visibility toggle
                IconButton(onClick = { onVisibilityChange(route) }) {
                    Icon(
                        painter = painterResource(id = if (route.isPublic) R.drawable.eye else R.drawable.eye_off), // Change based on visibility
                        contentDescription = if (route.isPublic) "Set Private" else "Set Public"
                    )
                }
            }

            // Add some additional route details if needed, like length, time, etc.
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Explore this route",
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 14.sp
                )
            )
        }
    }
}

