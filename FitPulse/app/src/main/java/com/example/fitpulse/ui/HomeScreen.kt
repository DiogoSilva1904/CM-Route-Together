package com.example.fitpulse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to FitPulse")

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate("loginScreen") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Button
        Button(
            onClick = { navController.navigate("signUpScreen") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Sign Up")
        }
    }
}
