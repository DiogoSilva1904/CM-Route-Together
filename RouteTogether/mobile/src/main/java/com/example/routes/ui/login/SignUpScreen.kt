package com.example.routes.ui.login

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.routes.data.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SignUpScreen(navController: NavController) {
    // Get the instance of AuthRepository (can be provided via DI or created directly)
    val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())

    // Create a ViewModel using the factory
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    // Collecting the sign-up state from the ViewModel
    val signUpState by authViewModel.signUpState.collectAsState()

    // State for user input
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Reset state on composition if signUpState has been updated
    LaunchedEffect(signUpState.isSuccess) {
        if (signUpState.isSuccess) {
            // Navigate to the login screen after success
            navController.navigate("loginScreen") {
                popUpTo("signUpScreen") { inclusive = true }
            }
        }
    }

    // Clear the success state once the user navigates away
    LaunchedEffect(Unit) {
        if (signUpState.isSuccess) {
            // Reset the state after navigation
            authViewModel.resetSignUpState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Sign Up")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { authViewModel.register(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show loading indicator
        if (signUpState.isLoading) {
            CircularProgressIndicator()
        }

        // Show error message
        signUpState.errorMessage?.let {
            Text(text = it)
        }
    }
}




