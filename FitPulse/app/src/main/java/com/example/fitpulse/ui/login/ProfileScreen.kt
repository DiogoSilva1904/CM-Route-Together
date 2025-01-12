package com.example.fitpulse.ui.login

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitpulse.domain.model.RouteData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.LatLng
import com.example.fitpulse.data.RouteRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.fitpulse.R

@Composable
fun ProfileScreen(
    friendEmail: String, // Passed from the route
    db: FirebaseFirestore,
    navController: NavController
) {
    val routeRepository = RouteRepository(db) // Creating an instance of RouteRepository

    // Use viewModel() to ensure the same ViewModel instance is used across recompositions
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(routeRepository))

    // Fetch routes only when friendEmail changes or when the composable is first launched
    LaunchedEffect(friendEmail) {
        profileViewModel.fetchRoutesForFriend(friendEmail)
    }

    // Observe the list of routes
    val routes = profileViewModel.routes.collectAsState().value

    // Profile screen UI
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
            // Circular Profile Picture
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
                text = friendEmail,
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
                RouteCard(
                    route = route,
                    onClick = { selectedRoute ->
                        // Navigate to the RouteDetailsScreen with routeName and friendEmail
                        navController.navigate("route_details/${selectedRoute.routeName}/$friendEmail")
                    }
                )
            }
        }
    }
}

@Composable
fun RouteCard(route: RouteData, onClick: (RouteData) -> Unit) {
    Card(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth()
            .clickable { onClick(route) }
            .shadow(10.dp, RoundedCornerShape(8.dp)), // Adding a shadow effect
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)) // Soft grey background color
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon next to the route name
                Image(
                    painter = painterResource(id = com.example.fitpulse.R.drawable.route_4), // Assuming you have an icon
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


