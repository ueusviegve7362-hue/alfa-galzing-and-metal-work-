package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlfaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Asynchronously initialize and pre-warm Room database on IO thread
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AppDatabase.getInstance(this@AlfaApplication)
                Log.d("AlfaApplication", "Room Database pre-warmed successfully.")
            }.onFailure { e ->
                Log.e("AlfaApplication", "Error pre-warming database", e)
            }
        }
    }
}
