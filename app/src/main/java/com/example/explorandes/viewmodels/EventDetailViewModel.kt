package com.example.explorandes.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.explorandes.database.entity.EventDetailEntity
import com.example.explorandes.models.EventDetail
import com.example.explorandes.repositories.EventDetailRepository
import com.example.explorandes.utils.ConnectivityHelper
import com.example.explorandes.utils.NetworkResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EventDetailViewModel(private val context: Context) : ViewModel() {

    private val repository = EventDetailRepository(context)
    private val connectivityHelper = ConnectivityHelper(context)

    private val _eventDetail = MutableLiveData<NetworkResult<EventDetail>>()
    val eventDetail: LiveData<NetworkResult<EventDetail>> = _eventDetail

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus

    init {
        checkConnectivity()
        setupConnectivityMonitoring()
    }

    fun checkConnectivity(): Boolean {
        val isAvailable = connectivityHelper.isInternetAvailable()
        _isConnected.value = isAvailable
        return isAvailable
    }
    
    private fun setupConnectivityMonitoring() {
        repository.setupConnectivityListener { isConnected ->
            _isConnected.postValue(isConnected)
            
            // Si recuperamos la conexión, intentar sincronizar eventos pendientes
            if (isConnected) {
                syncPendingEvents()
            }
        }
    }

    fun loadEventDetail(eventId: Long) {
        viewModelScope.launch {
            repository.getEventDetail(eventId).collectLatest { result ->
                _eventDetail.value = result
                
                // Si la carga fue exitosa, verificar si hay cambios pendientes
                if (result is NetworkResult.Success) {
                    // Verificar el estado de sincronización
                    checkPendingSyncStatus(eventId)
                }
            }
        }
    }

    private fun checkPendingSyncStatus(eventId: Long) {
        viewModelScope.launch {
            // Verificar si hay eventos pendientes de sincronización
            val pendingEvents = repository.syncPendingEvents()
            if (pendingEvents.contains(eventId)) {
                _syncStatus.value = "pending_sync"
            }
        }
    }

    fun saveEventDetail(eventDetail: EventDetail) {
        viewModelScope.launch {
            repository.saveEventDetail(eventDetail)
            
            // Actualizar el estado de sincronización basado en la conectividad
            _syncStatus.value = if (isConnected.value == true) "synced" else "pending_sync"
            
            // Si estamos conectados, intentar sincronizar inmediatamente
            if (isConnected.value == true) {
                syncPendingEvents()
            }
        }
    }

    fun syncPendingEvents() {
        if (!isConnected.value!!) {
            _syncStatus.value = "pending_sync"
            return
        }
        
        viewModelScope.launch {
            _syncStatus.value = "syncing"
            
            try {
                val syncedEventIds = repository.syncPendingEvents()
                
                if (syncedEventIds.isNotEmpty()) {
                    _syncStatus.value = "sync_completed"
                } else {
                    _syncStatus.value = "synced" // No había eventos pendientes
                }
            } catch (e: Exception) {
                _syncStatus.value = "sync_error"
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventDetailViewModel::class.java)) {
                return EventDetailViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}