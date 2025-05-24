package com.example.explorandes

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.explorandes.database.AppDatabase
import com.example.explorandes.services.LightSensorManager
import com.example.explorandes.utils.ConnectivityHelper
import com.example.explorandes.utils.DataStoreManager
import com.example.explorandes.utils.FileStorage
import com.example.explorandes.utils.SessionManager
import com.example.explorandes.utils.SyncWorker
import java.io.File


class ExplorAndesApplication : Application() {
    lateinit var lightSensorManager: LightSensorManager
    lateinit var connectivityHelper: ConnectivityHelper
    lateinit var database: AppDatabase
    lateinit var fileStorage: FileStorage
    lateinit var dataStoreManager: DataStoreManager
    
    override fun onCreate() {
        super.onCreate()
        
        lightSensorManager = LightSensorManager(this) { lux ->
            Log.d("ExplorAndesApp", "Light sensor reading: $lux lux")
        }
        
        // Initialize utilities
        connectivityHelper = ConnectivityHelper(this)
        database = AppDatabase.getInstance(this)
        fileStorage = FileStorage(this)
        dataStoreManager = DataStoreManager(this)
        
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val autoBrightnessEnabled = prefs.getBoolean("auto_brightness_enabled", true)
        val imageDir = File(filesDir, "image_cache")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
            Log.d("ExplorAndesApp", "Created image cache directory")
        }
        
        Log.d("ExplorAndesApp", "Auto brightness enabled: $autoBrightnessEnabled")
        val constraints = androidx.work.Constraints.Builder()
        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
        .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "event_sync_work",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    fun saveAutoBrightnessPreference(enabled: Boolean) {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_brightness_enabled", enabled).apply()
    }
    
    fun isAutoBrightnessEnabled(): Boolean {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_brightness_enabled", true)
    }
}