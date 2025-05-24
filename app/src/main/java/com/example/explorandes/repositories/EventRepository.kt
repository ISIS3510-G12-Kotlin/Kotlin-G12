package com.example.explorandes.repositories

import android.content.Context
import android.util.Log
import com.example.explorandes.api.ApiService
import com.example.explorandes.database.AppDatabase
import com.example.explorandes.database.entity.EventEntity
import com.example.explorandes.models.Event
import com.example.explorandes.utils.ConnectivityHelper
import com.example.explorandes.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.Retrofit

class EventRepository(private val apiService: ApiService, private val context: Context? = null) {

    private val eventDao = context?.let { AppDatabase.getInstance(it).eventDao() }
    private val connectivityHelper = context?.let { ConnectivityHelper(it) }

    fun isInternetAvailable(): Boolean {
        return connectivityHelper?.isInternetAvailable() ?: false
    }

    companion object {
        @Volatile
        private var instance: EventRepository? = null

        fun getInstance(retrofit: Retrofit, context: Context): EventRepository {
            return instance ?: synchronized(this) {
                instance ?: EventRepository(
                    retrofit.create(ApiService::class.java),
                    context
                ).also { instance = it }
            }
        }

        fun getInstance(apiService: ApiService, context: Context): EventRepository {
            return instance ?: synchronized(this) {
                instance ?: EventRepository(apiService, context).also { instance = it }
            }
        }
    }

    fun getAllEvents(): Flow<NetworkResult<List<Event>>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (connectivityHelper?.isInternetAvailable() == true) {
                try {
                    val response = apiService.getAllEvents()

                    if (response.isSuccessful) {
                        val events = response.body() ?: emptyList()
                        Log.d("EventRepository", "✅ Eventos recibidos desde API: ${events.size}")

                        eventDao?.let { dao ->
                            withContext(Dispatchers.IO) {
                                val eventEntities = events.map { EventEntity.fromModel(it) }
                                dao.insertEvents(eventEntities)
                            }
                        }

                        emit(NetworkResult.Success(events))
                    } else {
                        Log.w("EventRepository", "❌ Fallo API: ${response.code()}")
                        loadFromCache(
                            emit = { result -> emit(result) },
                            errorMessage = "API error: ${response.code()}"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("EventRepository", "🌐 Error red API: ${e.message}")
                    loadFromCache(
                        emit = { result -> emit(result) },
                        errorMessage = "Network error: ${e.message}"
                    )
                }
            } else {
                Log.w("EventRepository", "⚠️ Sin conexión - intentando cache")
                loadFromCache(
                    emit = { result -> emit(result) },
                    errorMessage = "No internet connection"
                )
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Error fetching events: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun loadFromCache(
        emit: suspend (NetworkResult<List<Event>>) -> Unit,
        errorMessage: String? = null
    ) {
        if (eventDao != null) {
            try {
                val cachedEvents = eventDao.getAllEvents().first()
                val events = cachedEvents.map { it.toModel() }
                Log.d("EventRepository", "📦 Eventos desde cache: ${events.size}")

                if (events.isNotEmpty()) {
                    emit(NetworkResult.Success(events))
                } else {
                    emit(NetworkResult.Error(errorMessage ?: "No cached events available", emptyList()))
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error("Failed to load cached events: ${e.message}", emptyList()))
            }
        } else {
            emit(NetworkResult.Error(errorMessage ?: "Cache not available", emptyList()))
        }
    }

    fun getEventById(id: Long): Flow<NetworkResult<Event>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (connectivityHelper?.isInternetAvailable() == true) {
                try {
                    val response = apiService.getEventById(id)

                    if (response.isSuccessful) {
                        val event = response.body()

                        if (event != null) {
                            eventDao?.let { dao ->
                                withContext(Dispatchers.IO) {
                                    dao.insertEvent(EventEntity.fromModel(event))
                                }
                            }
                            emit(NetworkResult.Success(event))
                        } else {
                            loadEventByIdFromCache(id, emit = { result -> emit(result) })
                        }
                    } else {
                        loadEventByIdFromCache(
                            id,
                            emit = { result -> emit(result) },
                            errorMessage = "API error: ${response.code()}"
                        )
                    }
                } catch (e: Exception) {
                    loadEventByIdFromCache(
                        id,
                        emit = { result -> emit(result) },
                        errorMessage = "Network error: ${e.message}"
                    )
                }
            } else {
                loadEventByIdFromCache(
                    id,
                    emit = { result -> emit(result) },
                    errorMessage = "No internet connection"
                )
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Error fetching event: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun loadEventByIdFromCache(
        id: Long,
        emit: suspend (NetworkResult<Event>) -> Unit,
        errorMessage: String? = null
    ) {
        if (eventDao != null) {
            try {
                val cachedEvent = eventDao.getEventById(id)

                if (cachedEvent != null) {
                    emit(NetworkResult.Success(cachedEvent.toModel()))
                } else {
                    emit(NetworkResult.Error(errorMessage ?: "Event not found in cache"))
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error("Failed to load cached event: ${e.message}"))
            }
        } else {
            emit(NetworkResult.Error(errorMessage ?: "Cache not available"))
        }
    }
}
