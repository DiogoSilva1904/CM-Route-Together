package com.example.routes

import android.app.Application
import com.google.firebase.FirebaseApp

class RouteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}