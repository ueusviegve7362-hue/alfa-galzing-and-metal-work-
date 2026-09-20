package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlfaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Asynchronously initialize and pre-warm dependencies on IO thread
        // so that heavy DB creation, Firebase, and location background services
        // never block the main thread or crash app startup.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                // Pre-warm Room Database asynchronously
                AppDatabase.getInstance(this@AlfaApplication)
                Log.d("AlfaApplication", "Room Database pre-warmed successfully.")
            }.onFailure { e ->
                Log.e("AlfaApplication", "Error pre-warming database", e)
            }

            runCatching {
                // Ensure FirebaseApp is safely initialized if present
                if (FirebaseApp.getApps(this@AlfaApplication).isEmpty()) {
                    FirebaseApp.initializeApp(this@AlfaApplication)
                    Log.d("AlfaApplication", "FirebaseApp initialized successfully.")
                }
            }.onFailure { e ->
                Log.w("AlfaApplication", "Firebase initialization skipped or safely caught: ${e.message}")
            }
        }
    }
}
