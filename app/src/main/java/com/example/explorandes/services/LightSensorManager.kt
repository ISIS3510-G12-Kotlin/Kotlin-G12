package com.example.explorandes.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class LightSensorManager(context: Context, private val onLightChanged: (Float) -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    fun startListening() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val lux = event?.values?.get(0) ?: return
        Log.d("LightSensor", "Ambient light level: $lux lux")
        onLightChanged(lux) // Send lux value to the brightness controller
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
