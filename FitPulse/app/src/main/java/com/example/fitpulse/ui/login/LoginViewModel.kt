package com.example.fitpulse.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitpulse.data.FirebaseAuthRepository
import com.example.fitpulse.ui.login.AuthViewModel.SignUpState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow(LogInState()) // MutableStateFlow to hold the state
    val loginState: StateFlow<LogInState> get() = _loginState // Exposing as StateFlow

    fun resetLogInState() {
        _loginState.value = LogInState()
    }

    fun login(email: String, password: String) {
        _loginState.value = LogInState(isLoading = true) // Set loading state

        repository.login(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _loginState.value = LogInState(isSuccess = true)
                } else {
                    val exceptionMessage = task.exception?.localizedMessage
                        ?: "Unknown error occurred. Please try again later."

                    _loginState.value = LogInState(
                        isSuccess = false,
                        errorMessage = exceptionMessage
                    )
                }
            }
    }


    data class LogInState(
        val isSuccess: Boolean = false,
        val errorMessage: String? = null,
        val isLoading: Boolean = false,
    )
}

class LoginViewModelFactory(private val repository: FirebaseAuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


