package com.renim.bitirme

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase başlat
        FirebaseApp.initializeApp(this)
        Log.d(
            "SosyalMahalle",
            "Firebase initialized: ${FirebaseApp.getApps(this).size} app(s) found"
        )

        // App Check (Play Integrity) kurulumu
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d("SosyalMahalle", "App Check: Play Integrity successfully initialized")
        } catch (e: Exception) {
            Log.e("SosyalMahalle", "App Check initialization error: ${e.message}")
        }
    }
}
