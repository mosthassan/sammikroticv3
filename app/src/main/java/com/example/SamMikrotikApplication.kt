package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class SamMikrotikApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        com.example.data.local.DeletedRecordsTracker.init(this)

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            Log.i("SamMikrotikApplication", "FirebaseApp initialized successfully")
        } catch (e: Throwable) {
            Log.e("SamMikrotikApplication", "FirebaseApp init fallback: ${e.message}", e)
        }
    }
}
