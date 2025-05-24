package com.example.explorandes.database.dao

import androidx.room.*
import com.example.explorandes.database.entity.EventDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDetailDao {
    @Query("SELECT * FROM event_details WHERE id = :id")
    suspend fun getEventDetailById(id: Long): EventDetailEntity?
    
    @Query("SELECT * FROM event_details WHERE id = :id")
    fun observeEventDetailById(id: Long): Flow<EventDetailEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventDetail(eventDetail: EventDetailEntity)
    
    @Update
    suspend fun updateEventDetail(eventDetail: EventDetailEntity)
    
    @Delete
    suspend fun deleteEventDetail(eventDetail: EventDetailEntity)
    
    @Query("DELETE FROM event_details WHERE id = :id")
    suspend fun deleteEventDetailById(id: Long)
    
    @Query("SELECT * FROM event_details WHERE lastSyncStatus = 'pending_sync'")
    suspend fun getPendingSyncEventDetails(): List<EventDetailEntity>
    
    @Query("UPDATE event_details SET lastSyncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String)
}