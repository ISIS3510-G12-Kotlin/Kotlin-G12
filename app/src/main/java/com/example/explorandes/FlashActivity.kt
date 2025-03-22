package com.explorandes.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.core.content.edit

class FlashActivity : Activity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences
    private var isFirstRun: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("FlashActivity", "Initializing FlashActivity")
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        progressBar = ProgressBar(this)
        progressBar.visibility = View.VISIBLE

        // Simulación de carga de configuraciones
        checkFirstRun()
        initializeSettings()
        preloadData()
    }

    private fun checkFirstRun() {
        isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)
        if (isFirstRun) {
            Log.d("FlashActivity", "First run detected, applying initial configurations")
            sharedPreferences.edit {
                putBoolean("isFirstRun", false)
                apply()
            }
        } else {
            Log.d("FlashActivity", "Normal launch detected")
        }
    }

    private fun initializeSettings() {
        Log.d("FlashActivity", "Applying user preferences")
        val themeMode = sharedPreferences.getString("themeMode", "light") ?: "light"
        applyTheme(themeMode)
    }

    private fun applyTheme(mode: String) {
        when (mode) {
            "dark" -> Log.d("FlashActivity", "Applying dark theme")
            "light" -> Log.d("FlashActivity", "Applying light theme")
            else -> Log.d("FlashActivity", "Applying default theme")
        }
    }

    private fun preloadData() {
        Log.d("FlashActivity", "Preloading essential data")

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("FlashActivity", "Data preloaded successfully")

            // Finaliza la actividad y regresa al flujo principal
            finishActivity()
        }, 3000) // Simulación de carga de datos
    }

    private fun finishActivity() {
        Log.d("FlashActivity", "Finishing FlashActivity")
        progressBar.visibility = View.GONE
        finish()
    }
}
