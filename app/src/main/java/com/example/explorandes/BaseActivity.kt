package com.example.explorandes

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.explorandes.services.BrightnessController

abstract class BaseActivity : AppCompatActivity() {
    companion object {
        private const val WRITE_SETTINGS_REQUEST_CODE = 200
    }
    
    private var app: ExplorAndesApplication? = null
    private var hasRequestedPermission = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as ExplorAndesApplication
        
        if (app?.isAutoBrightnessEnabled() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    if (!hasRequestedPermission) {
                        requestWriteSettingsPermission()
                        hasRequestedPermission = true
                    }
                } else {
                    startLightSensor()
                }
            } else {
                startLightSensor()
            }
        }
    }
    
    private fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        Toast.makeText(this, "Please grant permission to adjust screen brightness", Toast.LENGTH_LONG).show()
        startActivityForResult(intent, WRITE_SETTINGS_REQUEST_CODE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WRITE_SETTINGS_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    Log.d("BaseActivity", "Write settings permission granted")
                    startLightSensor()
                } else {
                    Log.d("BaseActivity", "Write settings permission denied")
                    Toast.makeText(this, "Permission denied to adjust brightness", Toast.LENGTH_SHORT).show()
                    // Disable auto brightness in preferences since permission was denied
                    app?.saveAutoBrightnessPreference(false)
                }
            }
        }
    }
    
    private fun startLightSensor() {
        app?.lightSensorManager?.registerCallback { lux ->
            adjustBrightness(lux)
        }
        app?.lightSensorManager?.startListening()
    }
    
    override fun onResume() {
        super.onResume()
        // When activity becomes visible, apply latest brightness
        if (app?.isAutoBrightnessEnabled() == true && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this))) {
            app?.lightSensorManager?.startListening()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // When activity is not visible, stop adjusting brightness
        if (app?.isAutoBrightnessEnabled() == true) {
            app?.lightSensorManager?.stopListening()
        }
    }
    
    private fun adjustBrightness(lux: Float) {
        // Calculate brightness based on lux value
        val brightness = when {
            lux < 10 -> 0.8f    // Very dark, brightest screen
            lux < 50 -> 0.6f    // Dark room
            lux < 200 -> 0.5f   // Indoor lighting
            lux < 500 -> 0.4f   // Bright indoor
            lux < 1000 -> 0.3f  // Very bright indoor
            else -> 0.25f       // Outdoor/sunlight, dimmest screen
        }
        
        //Log.d("BaseActivity", "Setting brightness to $brightness based on lux $lux")
        BrightnessController.setBrightness(this, brightness)
    }
}