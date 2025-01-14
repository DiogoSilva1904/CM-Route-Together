package com.example.routes.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthRepository(private val firebaseAuth: FirebaseAuth) {
    fun login(email: String, password: String): Task<AuthResult> {
        Log.d("ahhh",firebaseAuth.signInWithEmailAndPassword(email, password).toString())
        return firebaseAuth.signInWithEmailAndPassword(email, password)
    }

    fun register(email: String, password: String): Task<AuthResult> {
        return firebaseAuth.createUserWithEmailAndPassword(email, password)
    }
}

