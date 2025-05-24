package com.example.explorandes.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.explorandes.repositories.EventDetailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncWorker", "Iniciando sincronización de eventos")
            // Crear instancia del repositorio
            val repository = EventDetailRepository(applicationContext)

            // Sincronizar eventos pendientes
            repository.syncPendingEvents()

            Log.d("SyncWorker", "Sincronización completada con éxito")
            // Devolver éxito
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error durante la sincronización: ${e.message}")
            // Reintentar más tarde en caso de error
            Result.retry()
        }
    }
}