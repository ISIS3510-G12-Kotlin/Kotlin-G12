package com.example.explorandes.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.explorandes.models.Building
import com.example.explorandes.models.Event
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {

    private val _favoriteBuildings = MutableLiveData<List<Building>>()
    val favoriteBuildings: LiveData<List<Building>> = _favoriteBuildings

    private val _favoriteEvents = MutableLiveData<List<Event>>()
    val favoriteEvents: LiveData<List<Event>> = _favoriteEvents

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Add a building to favorites
    fun addBuildingToFavorites(building: Building) {
        val currentList = _favoriteBuildings.value?.toMutableList() ?: mutableListOf()
        if (!currentList.contains(building)) {
            currentList.add(building)
            _favoriteBuildings.value = currentList
            // Save to persistent storage (would be implemented in a real app)
        }
    }

    // Remove a building from favorites
    fun removeBuildingFromFavorites(building: Building) {
        val currentList = _favoriteBuildings.value?.toMutableList() ?: mutableListOf()
        currentList.removeIf { it.id == building.id }
        _favoriteBuildings.value = currentList
        // Save to persistent storage (would be implemented in a real app)
    }

    // Add an event to favorites
    fun addEventToFavorites(event: Event) {
        val currentList = _favoriteEvents.value?.toMutableList() ?: mutableListOf()
        if (!currentList.contains(event)) {
            currentList.add(event)
            _favoriteEvents.value = currentList
            // Save to persistent storage (would be implemented in a real app)
        }
    }

    // Remove an event from favorites
    fun removeEventFromFavorites(event: Event) {
        val currentList = _favoriteEvents.value?.toMutableList() ?: mutableListOf()
        currentList.removeIf { it.id == event.id }
        _favoriteEvents.value = currentList
        // Save to persistent storage (would be implemented in a real app)
    }

    // Check if a building is in favorites
    fun isBuildingInFavorites(buildingId: Long): Boolean {
        return _favoriteBuildings.value?.any { it.id == buildingId } ?: false
    }

    // Check if an event is in favorites
    fun isEventInFavorites(eventId: Long): Boolean {
        return _favoriteEvents.value?.any { it.id == eventId } ?: false
    }
}