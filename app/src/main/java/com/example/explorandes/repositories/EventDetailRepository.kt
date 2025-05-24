package com.example.explorandes.repositories

import android.content.Context
import android.util.Log
import com.example.explorandes.api.ApiClient
import com.example.explorandes.database.AppDatabase
import com.example.explorandes.database.entity.EventDetailEntity
import com.example.explorandes.models.EventDetail
import com.example.explorandes.utils.ConnectivityHelper
import com.example.explorandes.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class EventDetailRepository(private val context: Context) {
    
    private val db = AppDatabase.getInstance(context)
    private val eventDetailDao = db.eventDetailDao()
    private val connectivityHelper = ConnectivityHelper(context)
    
    // Verificar si hay conexión a internet
    fun isNetworkAvailable(): Boolean {
        return connectivityHelper.isInternetAvailable()
    }
    
    // Obtener detalle del evento con manejo de caché
    fun getEventDetail(eventId: Long): Flow<NetworkResult<EventDetail>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // Primero intentar cargar desde la caché
            val cachedDetail = eventDetailDao.getEventDetailById(eventId)
            
            // Si hay datos en caché, emitirlos primero
            if (cachedDetail != null) {
                emit(NetworkResult.Success(cachedDetail.toModel()))
            }
            
            // Si hay conexión a internet, intentar actualizar los datos
            if (isNetworkAvailable()) {
                try {
                    val response = ApiClient.apiService.getEventById(eventId)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val eventFromNetwork = response.body()!!
                        
                        // Convertir Event básico a EventDetail y añadir detalles adicionales
                        val eventDetail = EventDetail(
                            id = eventFromNetwork.id,
                            title = eventFromNetwork.title,
                            description = eventFromNetwork.description,
                            imageUrl = eventFromNetwork.imageUrl,
                            type = eventFromNetwork.type,
                            startTime = eventFromNetwork.startTime,
                            endTime = eventFromNetwork.endTime,
                            locationId = eventFromNetwork.locationId,
                            locationName = eventFromNetwork.locationName,
                            // Aquí podrías agregar más detalles específicos que vengan del API
                            organizerName = null,
                            capacity = null,
                            registrationUrl = null,
                            additionalInfo = null
                        )
                        
                        // Guardar en caché con estado de sincronización "synced"
                        val entity = EventDetailEntity.fromModel(eventDetail).copy(
                            lastSyncStatus = "synced",
                            lastUpdated = LocalDateTime.now()
                        )
                        eventDetailDao.insertEventDetail(entity)
                        
                        // Emitir los nuevos datos
                        emit(NetworkResult.Success(eventDetail))
                    } else {
                        // Si hay error en la API pero tenemos caché, no emitimos error
                        if (cachedDetail == null) {
                            emit(NetworkResult.Error("Error cargando el evento: ${response.code()}"))
                        }
                    }
                } catch (e: Exception) {
                    // Si hay error de red pero tenemos caché, no emitimos error
                    if (cachedDetail == null) {
                        emit(NetworkResult.Error("Error de red: ${e.localizedMessage}"))
                    } else {
                        // Si tenemos caché pero hubo un error de red, indicamos que los datos son de caché
                        emit(NetworkResult.Error("Mostrando datos almacenados localmente", cachedDetail.toModel()))
                    }
                }
            } else {
                // Si no hay internet y tenemos caché, indicamos que los datos son de caché
                if (cachedDetail != null) {
                    emit(NetworkResult.Error("Sin conexión. Mostrando datos almacenados localmente", cachedDetail.toModel()))
                } else {
                    // Si no hay internet y no hay caché, emitir error
                    emit(NetworkResult.Error("No hay conexión a Internet y no hay datos en caché"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Error al acceder a la base de datos: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)
    
    // Observar un evento específico desde la base de datos
    fun observeEventDetail(eventId: Long): Flow<EventDetail?> {
        return eventDetailDao.observeEventDetailById(eventId)
            .map { entity -> entity?.toModel() }
            .flowOn(Dispatchers.IO)
    }
    
    // Guardar cambios locales y marcarlos para sincronización
    suspend fun saveEventDetail(eventDetail: EventDetail) {
        withContext(Dispatchers.IO) {
            val syncStatus = if (isNetworkAvailable()) {
                try {
                    // Intentar sincronizar inmediatamente si hay conexión
                    // (simplificado aquí - en un caso real enviarías al backend)
                    Log.d("EventDetailRepository", "Sincronizando evento en tiempo real: ${eventDetail.id}")
                    "synced"
                } catch (e: Exception) {
                    Log.e("EventDetailRepository", "Error al sincronizar: ${e.message}")
                    "sync_error"
                }
            } else {
                // Marcar para sincronización posterior si no hay conexión
                Log.d("EventDetailRepository", "Marcando evento para sincronización posterior: ${eventDetail.id}")
                "pending_sync"
            }
            
            val entity = EventDetailEntity.fromModel(eventDetail).copy(
                lastSyncStatus = syncStatus,
                lastUpdated = LocalDateTime.now()
            )
            eventDetailDao.insertEventDetail(entity)
        }
    }
    
    // Sincronizar eventos pendientes
    suspend fun syncPendingEvents(): List<Long> {
        return withContext(Dispatchers.IO) {
            if (!isNetworkAvailable()) return@withContext emptyList()
            
            val pendingEvents = eventDetailDao.getPendingSyncEventDetails()
            val syncedEventIds = mutableListOf<Long>()
            
            for (event in pendingEvents) {
                try {
                    // Simulamos una sincronización exitosa con el backend
                    // En un caso real, aquí enviarías los datos al servidor
                    Log.d("EventDetailRepository", "Sincronizando evento pendiente: ${event.id}")
                    
                    // Actualizar el estado de sincronización a "synced"
                    eventDetailDao.updateSyncStatus(event.id, "synced")
                    syncedEventIds.add(event.id)
                    
                    Log.d("EventDetailRepository", "Evento sincronizado: ${event.id}")
                } catch (e: Exception) {
                    Log.e("EventDetailRepository", "Error sincronizando evento: ${e.localizedMessage}")
                    eventDetailDao.updateSyncStatus(event.id, "sync_error")
                }
            }
            
            return@withContext syncedEventIds
        }
    }
    
    // Configurar un listener para detectar cambios en la conectividad
    fun setupConnectivityListener(onConnectivityChanged: (Boolean) -> Unit) {
        connectivityHelper.addConnectivityListener { isConnected ->
            onConnectivityChanged(isConnected)
            

        }
    }
}