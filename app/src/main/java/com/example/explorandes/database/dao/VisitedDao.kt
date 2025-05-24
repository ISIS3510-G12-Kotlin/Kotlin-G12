package com.example.explorandes.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.explorandes.models.VisitedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitedDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVisit(visit: VisitedItem)

    @Query("SELECT * FROM visited_items ORDER BY timestamp DESC")
    fun getAllVisits(): Flow<List<VisitedItem>>

    @Query("UPDATE visited_items SET wasSynced = 1 WHERE wasSynced = 0")
    suspend fun markAllAsSynced()
}
