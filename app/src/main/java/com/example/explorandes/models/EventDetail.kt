package com.example.explorandes.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Parcelize
data class EventDetail(
    val id: Long,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val type: String?,
    val startTime: String,
    val endTime: String,
    val locationId: Long?,
    val locationName: String?,
    val organizerName: String? = null,
    val capacity: Int? = null,
    val registrationUrl: String? = null,
    val additionalInfo: String? = null
) : Parcelable {

    fun getFormattedStartTime(): String {
        return formatDateTime(startTime)
    }

    fun getFormattedEndTime(): String {
        return formatDateTime(endTime)
    }

    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val dateTime = LocalDateTime.parse(startTime, formatter)
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }

    fun getFormattedTimeRange(): String {
        val startFormatter = DateTimeFormatter.ISO_DATE_TIME
        val startDateTime = LocalDateTime.parse(startTime, startFormatter)
        val endDateTime = LocalDateTime.parse(endTime, startFormatter)

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        return "${startDateTime.format(timeFormatter)} - ${endDateTime.format(timeFormatter)}"
    }

    // Check if this event is happening now
    fun isHappeningNow(): Boolean {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val now = LocalDateTime.now()
        val start = LocalDateTime.parse(startTime, formatter)
        val end = LocalDateTime.parse(endTime, formatter)

        return (now.isAfter(start) || now.isEqual(start)) &&
                (now.isBefore(end) || now.isEqual(end))
    }

    // Check if this event is in the future
    fun isUpcoming(): Boolean {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val now = LocalDateTime.now()
        val start = LocalDateTime.parse(startTime, formatter)

        return start.isAfter(now)
    }

    // Simplify to basic Event object if needed
    fun toEvent(): Event {
        return Event(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            type = type,
            startTime = startTime,
            endTime = endTime,
            locationId = locationId,
            locationName = locationName
        )
    }

    private fun formatDateTime(dateTimeStr: String): String {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val dateTime = LocalDateTime.parse(dateTimeStr, formatter)
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a"))
    }
}