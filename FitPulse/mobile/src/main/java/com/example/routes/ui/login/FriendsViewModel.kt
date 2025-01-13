package com.example.routes.ui.login

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.routes.data.RouteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendsViewModel(private val db: FirebaseFirestore) : ViewModel() {

    private val routeRepository = RouteRepository(db)

    private val _friends = MutableStateFlow<List<String>>(emptyList())
    val friends: StateFlow<List<String>> get() = _friends

    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap: StateFlow<Bitmap?> get() = _qrCodeBitmap

    private val _showScanner = MutableStateFlow(false)
    val showScanner: StateFlow<Boolean> = _showScanner

    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> get() = _message

    init {
        fetchFriends()
    }

    // Fetch friends from the repository
    private fun fetchFriends() {
        viewModelScope.launch {
            try {
                // Fetch friends from the repository
                val allFriends = routeRepository.fetchFriends(currentUserEmail)
                Log.d("FriendsViewModel", "Fetched friends: $allFriends")

                // Filter unique friends by email
                val uniqueFriends = allFriends.distinctBy { it }

                // Update the LiveData with the filtered list
                _friends.value = uniqueFriends
                Log.d("FriendsViewModel", "Unique friends: $uniqueFriends")
            } catch (e: Exception) {
                _message.value = "Error fetching friends."
            }
        }
    }


    fun handleFriendshipCreation(friendEmail: String) {
        viewModelScope.launch {
            try {
                // First check if they're already friends
                val isFriend = routeRepository.checkIfFriend(currentUserEmail, friendEmail)
                Log.d("FriendsViewModel", "Checking if $friendEmail is already a friend of $currentUserEmail")
                if (isFriend) {
                    Log.d("FriendsViewModel", "$friendEmail is already a friend")
                    _message.value = "$friendEmail is already your friend."
                    return@launch
                }

                Log.d("FriendsViewModel", "Creating friendship connection with $friendEmail")

                // If not friends, create bidirectional friendship
                routeRepository.addFriend(currentUserEmail, friendEmail)
                Log.d("FriendsViewModel", "Added $friendEmail as a friend")
                routeRepository.addFriend(friendEmail, currentUserEmail)
                Log.d("FriendsViewModel", "Added $currentUserEmail as a friend")

                _message.value = "Successfully connected with $friendEmail"
                fetchFriends() // Refresh the friend list
            } catch (e: Exception) {
                _message.value = "Error creating friendship connection."
                Log.e("FriendsViewModel", "Error creating friendship", e)
            }
        }
    }

    // Keep these methods for other friendship management scenarios
    fun addFriend(friendEmail: String) {
        viewModelScope.launch {
            if (friendEmail in _friends.value) {
                _message.value = "$friendEmail is already your friend."
                return@launch
            }

            try {
                routeRepository.addFriend(currentUserEmail, friendEmail)
                _message.value = "$friendEmail added as a friend."
                fetchFriends()
            } catch (e: Exception) {
                _message.value = "Failed to add $friendEmail as a friend."
            }
        }
    }

    fun addFriendToScannedUser(friendEmail: String) {
        viewModelScope.launch {
            try {
                val isFriend = routeRepository.checkIfFriend(currentUserEmail, friendEmail)
                if (!isFriend) {
                    routeRepository.addFriend(friendEmail, currentUserEmail)
                    _message.value = "You are now a friend of $friendEmail."
                } else {
                    _message.value = "You are already a friend of $friendEmail."
                }
            } catch (e: Exception) {
                _message.value = "Error adding friend."
            }
        }
    }


}

class FriendsViewModelFactory(
    private val db: FirebaseFirestore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}