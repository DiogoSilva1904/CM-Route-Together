package com.example.routes.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.routes.data.FirebaseAuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AuthViewModel(private val firebaseAuthRepository: FirebaseAuthRepository) : ViewModel() {

    private val _signUpState = MutableStateFlow(SignUpState()) // MutableStateFlow to hold the state
    val signUpState: StateFlow<SignUpState> get() = _signUpState // Exposing as StateFlow


    // Function to handle user registration
    fun register(email: String, password: String) {
        viewModelScope.launch {
            _signUpState.value = SignUpState(isLoading = true)
            try {
                val task: Task<AuthResult> = firebaseAuthRepository.register(email, password)
                val authResult = task.await() // Wait for result
                if (authResult.user != null) {
                    _signUpState.value = SignUpState(isSuccess = true)
                } else {
                    _signUpState.value = SignUpState(errorMessage = "Registration failed")
                }
            } catch (e: Exception) {
                _signUpState.value = SignUpState(errorMessage = e.message)
            }
        }
    }

    fun resetSignUpState() {
        _signUpState.value = SignUpState()
    }

    data class SignUpState(
        val isLoading: Boolean = false,
        val isSuccess: Boolean = false,
        val errorMessage: String? = null
    )
}

class AuthViewModelFactory(private val repository: FirebaseAuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


