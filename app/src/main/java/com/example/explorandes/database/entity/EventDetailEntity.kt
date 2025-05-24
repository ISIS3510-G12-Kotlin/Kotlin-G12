package com.example.explorandes.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.explorandes.models.EventDetail
import java.time.LocalDateTime

@Entity(tableName = "event_details")
data class EventDetailEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val type: String?,
    val startTime: String,
    val endTime: String,
    val locationId: Long?,
    val locationName: String?,
    val organizerName: String?,
    val capacity: Int?,
    val registrationUrl: String?,
    val additionalInfo: String?,
    val lastUpdated: LocalDateTime = LocalDateTime.now(),
    val lastSyncStatus: String = "synced" // Puede ser "pending_sync", "synced", "sync_error"
) {
    fun toModel(): EventDetail {
        return EventDetail(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            type = type,
            startTime = startTime,
            endTime = endTime,
            locationId = locationId,
            locationName = locationName,
            organizerName = organizerName,
            capacity = capacity,
            registrationUrl = registrationUrl,
            additionalInfo = additionalInfo
        )
    }

    companion object {
        fun fromModel(eventDetail: EventDetail): EventDetailEntity {
            return EventDetailEntity(
                id = eventDetail.id,
                title = eventDetail.title,
                description = eventDetail.description,
                imageUrl = eventDetail.imageUrl,
                type = eventDetail.type,
                startTime = eventDetail.startTime,
                endTime = eventDetail.endTime,
                locationId = eventDetail.locationId,
                locationName = eventDetail.locationName,
                organizerName = eventDetail.organizerName,
                capacity = eventDetail.capacity,
                registrationUrl = eventDetail.registrationUrl,
                additionalInfo = eventDetail.additionalInfo
            )
        }
    }
}