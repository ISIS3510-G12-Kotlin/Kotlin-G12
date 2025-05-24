package com.example.explorandes.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visited_items")
data class VisitedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Long,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wasSynced: Boolean = false
)
