package com.example.explorandes.utils

import com.example.explorandes.models.EventDetail

object VisitedEventsManager {
    private val visitedEvents = mutableListOf<Pair<EventDetail, Boolean>>()

    fun addEvent(event: EventDetail, wasSynced: Boolean) {

        visitedEvents.removeAll { it.first.id == event.id }
        visitedEvents.add(0, Pair(event, wasSynced))
    }

    fun getVisitedEvents(): List<Pair<EventDetail, Boolean>> {
        return visitedEvents
    }

    fun markAllAsSynced() {
        visitedEvents.replaceAll { (event, _) -> Pair(event, true) }
    }
}
