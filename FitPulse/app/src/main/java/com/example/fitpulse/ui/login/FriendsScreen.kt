package com.example.fitpulse.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fitpulse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun FriendsScreen(
    db: FirebaseFirestore,
    navController: NavController // Add NavController to navigate to profile screen
) {
    val friendsViewModel: FriendsViewModel = viewModel(
        factory = FriendsViewModelFactory(db)
    )
    var showScanner by rememberSaveable { mutableStateOf(false) }
    var showQrGenerator by rememberSaveable { mutableStateOf(false) }
    val userId = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val friends by friendsViewModel.friends.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Buttons row with enhanced button style
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showQrGenerator = true },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Generate QR Code",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
            Button(
                onClick = { showScanner = true },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Scan QR Code")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title text with more prominent style
        Text(
            text = "Your Friends",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Scrollable List of Friends
        LazyColumn(
            modifier = Modifier.fillMaxSize(), // Allow the column to take the remaining space and be scrollable
            verticalArrangement = Arrangement.spacedBy(12.dp) // Add space between cards
        ) {
            items(friends) { friend ->
                FriendCard(friend, navController) // Pass NavController to navigate to profile
            }
        }

        // Show the QR generator dialog
        if (showQrGenerator) {
            QrCodeGeneratorDialog(
                onDismiss = { showQrGenerator = false },
                userId = userId
            )
        }

        // Show the QR code scanner dialog
        if (showScanner) {
            QrCodeScannerDialog(db = db, onDismiss = { showScanner = false })
        }
    }
}

@Composable
fun FriendCard(
    friend: String,
    navController: NavController // This will navigate to the profile screen
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Navigate to friend profile when clicked
                navController.navigate("profile_screen/$friend")
            },
        shape = MaterialTheme.shapes.large, // Use the default rounded corners or specify custom corners
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // Circular Image (replace with real friend's image)
            Image(
                painter = painterResource(id = R.drawable.lebron), // Placeholder image
                contentDescription = "Friend Avatar",
                modifier = Modifier
                    .size(60.dp)
                    .padding(end = 16.dp)
                    .clip(CircleShape), // Keep the image circular
                contentScale = ContentScale.Crop
            )
            // Friend Name with better typography
            Text(
                text = friend,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
