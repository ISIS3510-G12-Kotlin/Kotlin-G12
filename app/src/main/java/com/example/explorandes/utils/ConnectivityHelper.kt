package com.example.explorandes.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * Clase para ayudar a verificar el estado de conectividad de la app
 * y notificar cambios en la conectividad
 */
class ConnectivityHelper(private val context: Context) {
    
    private val TAG = "ConnectivityHelper"
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Lista de callbacks para notificar cambios de conectividad
    private val connectivityListeners = mutableListOf<(Boolean) -> Unit>()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Red disponible")
            notifyListeners(true)
        }
        
        override fun onLost(network: Network) {
            Log.d(TAG, "Red perdida")
            notifyListeners(false)
        }
    }
    
    init {
        // Registrar para recibir actualizaciones de conectividad
        registerNetworkCallback()
    }
    
    /**
     * Registra el callback para recibir actualizaciones de conectividad
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    /**
     * Verifica si hay una conexión a Internet disponible
     */
    fun isInternetAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Log.d(TAG, "Internet availability check: $hasInternet")
        return hasInternet
    }
    
    /**
     * Notifica a todos los listeners sobre el cambio de conectividad
     */
    private fun notifyListeners(isConnected: Boolean) {
        connectivityListeners.forEach { it(isConnected) }
    }
    
    /**
     * Agrega un listener para ser notificado sobre cambios de conectividad
     */
    fun addConnectivityListener(listener: (Boolean) -> Unit) {
        if (!connectivityListeners.contains(listener)) {
            connectivityListeners.add(listener)
            // Notificar el estado actual inmediatamente
            listener(isInternetAvailable())
        }
    }
    
    /**
     * Elimina un listener registrado
     */
    fun removeConnectivityListener(listener: (Boolean) -> Unit) {
        connectivityListeners.remove(listener)
    }
    
    /**
     * Liberar recursos cuando ya no se necesita el helper
     */
    fun cleanup() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar networkCallback", e)
        }
        connectivityListeners.clear()
    }

    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ConnectivityHelper? = null
        
        fun getInstance(context: Context): ConnectivityHelper {
            return INSTANCE ?: synchronized(this) {
                ConnectivityHelper(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}