package com.example.fitpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitpulse.data.FirebaseAuthRepository
import com.example.fitpulse.ui.login.LoginScreen
import com.example.fitpulse.ui.login.LoginViewModel
import com.example.fitpulse.ui.login.LoginViewModelFactory
import com.example.fitpulse.ui.login.SignUpScreen
import com.example.fitpulse.ui.theme.FitPulseTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fitpulse.ui.HomeScreen
import com.example.fitpulse.ui.login.DashboardScreen
import com.example.fitpulse.ui.login.FriendsScreen
import com.example.fitpulse.ui.login.ProfileScreen
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = FirebaseFirestore.getInstance()


        setContent {
            FitPulseTheme {
                MyApp(db)
            }
        }
    }
}

@Composable
fun MyApp(db: FirebaseFirestore) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "homeScreen") {
        // Home Screen - initial entry point of the app
        composable("homeScreen") {
            HomeScreen(navController = navController)
        }

        // Login Screen
        composable("loginScreen") {
            LoginScreen(navController = navController)
        }

        // SignUp Screen
        composable("signUpScreen") {
            SignUpScreen(navController = navController)
        }

        composable("dashboardScreen") {
            DashboardScreen(db=db)
        }
    }
}