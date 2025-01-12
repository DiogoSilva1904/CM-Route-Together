package com.example.fitpulse.ui.login

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitpulse.data.RouteRepository
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

    init {
        fetchFriends()
    }

    // Fetch friends from the repository
    private fun fetchFriends() {
        viewModelScope.launch {
            try {
                _friends.value = routeRepository.fetchFriends(currentUserEmail)
            } catch (e: Exception) {
                _message.value = "Error fetching friends."
            }
        }
    }

    // Add a friend only if the email does not exist already in the friends list
    fun addFriend(friendEmail: String) {
        viewModelScope.launch {
            if (friendEmail in _friends.value) {
                _message.value = "$friendEmail is already your friend."
                return@launch
            }

            try {
                routeRepository.addFriend(currentUserEmail, friendEmail)
                _message.value = "$friendEmail added as a friend."
                fetchFriends() // Refresh the friend list
            } catch (e: Exception) {
                _message.value = "Failed to add $friendEmail as a friend."
            }
        }
    }

    // Add current user as a friend to the scanned user only if not already friends
    fun addFriendToScannedUser(friendEmail: String) {
        viewModelScope.launch {
            try {
                val isFriend = routeRepository.checkIfFriend(friendEmail, currentUserEmail)
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

    fun handleScannedQRCode(result: String) {
        // Assuming the result is the friend's email
        addFriendToScannedUser(result)
        deactivateScanner()  // Close the scanner after processing
    }

    private fun deactivateScanner() {

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