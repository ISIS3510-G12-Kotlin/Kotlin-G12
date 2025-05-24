package com.example.explorandes.repositories

import android.content.Context
import com.example.explorandes.database.AppDatabase
import com.example.explorandes.models.VisitedItem
import kotlinx.coroutines.flow.Flow

class VisitedRepository(context: Context) {
    private val visitedDao = AppDatabase.getInstance(context).visitedDao()

    fun getAllVisits(): Flow<List<VisitedItem>> = visitedDao.getAllVisits()

    suspend fun insertVisit(visit: VisitedItem) = visitedDao.insertVisit(visit)

    suspend fun markAllAsSynced() = visitedDao.markAllAsSynced()
}
