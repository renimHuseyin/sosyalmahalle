package com.renim.bitirme

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Log.d("SosyalMahalle", "Firebase initialized: ${FirebaseApp.getApps(this).size} app(s) found")
    }
}