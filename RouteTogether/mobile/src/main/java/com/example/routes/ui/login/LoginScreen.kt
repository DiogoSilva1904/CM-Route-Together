package com.example.routes.ui.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.routes.data.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.log

@Composable
fun LoginScreen(navController: NavHostController) {

    // Creating repository and ViewModel
    val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(authRepository))

    // Collect the login state
    val loginState by viewModel.loginState.collectAsState()

    // MutableState for email and password inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Email input field
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password input field
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Display login result state
        when {
            loginState.isLoading -> {
                // Display loading indicator when the login is in progress
                CircularProgressIndicator()
            }
            loginState.isSuccess -> {
                // Login succeeded, navigate to the next screen
                Toast.makeText(LocalContext.current, "Login Successful!", Toast.LENGTH_SHORT).show()
                navController.navigate("dashboardScreen") {
                    // Pop the current login screen from the backstack
                    popUpTo("loginScreen") { inclusive = true }
                }

                LaunchedEffect(Unit) {
                    // Reset the login state after successful login
                    viewModel.resetLogInState()
                }
            }
            loginState.errorMessage != null -> {
                // Display error message if login failed
                Text(text = "Login Failed: ${loginState.errorMessage}", color = Color.Red)
            }
            else -> {
                // Handle any other case (not loading, not success)
                Text("")
            }
        }
    }
}
