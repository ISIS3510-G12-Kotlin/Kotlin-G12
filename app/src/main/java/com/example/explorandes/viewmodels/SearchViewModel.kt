package com.example.explorandes.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.explorandes.models.Building
import com.example.explorandes.models.Event
import com.example.explorandes.models.Place
import com.example.explorandes.repositories.BuildingRepository
import com.example.explorandes.api.ApiClient
import com.example.explorandes.utils.ConnectivityHelper
import com.example.explorandes.utils.UserDataCache
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SearchViewModel(private val context: Context) : ViewModel() {

    private val buildingRepository = BuildingRepository(context)
    private val connectivityHelper = ConnectivityHelper(context)

    // Search results
    private val _buildings = MutableLiveData<List<Building>>()
    val buildings: LiveData<List<Building>> = _buildings

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    private val _places = MutableLiveData<List<Place>>()
    val places: LiveData<List<Place>> = _places

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    // Search state
    private val _currentQuery = MutableLiveData<String>()
    val currentQuery: LiveData<String> = _currentQuery

    private val _selectedCategory = MutableLiveData<SearchCategory>()
    val selectedCategory: LiveData<SearchCategory> = _selectedCategory

    private val _selectedFilters = MutableLiveData<SearchFilters>()
    val selectedFilters: LiveData<SearchFilters> = _selectedFilters

    // Recent searches cache
    private val _recentSearches = MutableLiveData<List<String>>()
    val recentSearches: LiveData<List<String>> = _recentSearches

    private var searchJob: Job? = null

    enum class SearchCategory {
        ALL, BUILDINGS, EVENTS, PLACES
    }

    data class SearchFilters(
        val category: SearchCategory = SearchCategory.ALL,
        val onlyAvailable: Boolean = false
    )

    init {
        checkConnectivity()
        _selectedCategory.value = SearchCategory.ALL
        _selectedFilters.value = SearchFilters()
        loadRecentSearches()
    }

    fun checkConnectivity(): Boolean {
        val isAvailable = connectivityHelper.isInternetAvailable()
        _isConnected.value = isAvailable
        return isAvailable
    }

    fun setSearchCategory(category: SearchCategory) {
        _selectedCategory.value = category

        // Re-execute search if there's a current query
        _currentQuery.value?.let { query ->
            if (query.isNotBlank()) {
                performSearch(query)
            }
        }
    }

    fun updateFilters(filters: SearchFilters) {
        _selectedFilters.value = filters
        _selectedCategory.value = filters.category

        // Re-execute search if there's a current query
        _currentQuery.value?.let { query ->
            if (query.isNotBlank()) {
                performSearch(query)
            }
        }
    }

    fun performSearch(query: String) {
        // Cancel previous search job
        searchJob?.cancel()

        _currentQuery.value = query

        if (query.isBlank()) {
            clearResults()
            return
        }

        // Save to recent searches
        saveToRecentSearches(query)

        searchJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Add slight delay for better UX (debouncing)
                delay(300)

                val filters = _selectedFilters.value ?: SearchFilters()

                when (filters.category) {
                    SearchCategory.ALL -> searchAll(query, filters)
                    SearchCategory.BUILDINGS -> searchBuildings(query, filters)
                    SearchCategory.EVENTS -> searchEvents(query, filters)
                    SearchCategory.PLACES -> searchPlaces(query, filters)
                }

                Log.d("SearchViewModel", "Search completed for query: '$query' in category: ${filters.category}")

            } catch (e: Exception) {
                _error.value = "Search failed: ${e.localizedMessage}"
                Log.e("SearchViewModel", "Search error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun searchAll(query: String, filters: SearchFilters) {
        // Search in all categories concurrently
        val buildingsDeferred = viewModelScope.launch { searchBuildings(query, filters) }
        val eventsDeferred = viewModelScope.launch { searchEvents(query, filters) }
        val placesDeferred = viewModelScope.launch { searchPlaces(query, filters) }

        // Wait for all searches to complete
        buildingsDeferred.join()
        eventsDeferred.join()
        placesDeferred.join()
    }

    private suspend fun searchBuildings(query: String, filters: SearchFilters) {
        try {
            val allBuildings = buildingRepository.getAllBuildings()
            val filteredBuildings = allBuildings.filter { building ->
                // Text matching
                building.name.contains(query, ignoreCase = true) ||
                        building.code.contains(query, ignoreCase = true) ||
                        (building.description?.contains(query, ignoreCase = true) == true)
            }

            _buildings.value = filteredBuildings
            Log.d("SearchViewModel", "Found ${filteredBuildings.size} buildings matching '$query'")

        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error searching buildings", e)
            _buildings.value = emptyList()
        }
    }

    private suspend fun searchEvents(query: String, filters: SearchFilters) {
        try {
            if (connectivityHelper.isInternetAvailable()) {
                val response = ApiClient.apiService.getAllEvents()
                if (response.isSuccessful) {
                    val allEvents = response.body() ?: emptyList()
                    val filteredEvents = allEvents.filter { event ->
                        // Text matching
                        val matchesText = event.title.contains(query, ignoreCase = true) ||
                                (event.description?.contains(query, ignoreCase = true) == true) ||
                                (event.locationName?.contains(query, ignoreCase = true) == true)

                        // Availability filter (only show current/future events)
                        val isAvailable = if (filters.onlyAvailable) {
                            !event.isPastEvent()
                        } else true

                        matchesText && isAvailable
                    }

                    _events.value = filteredEvents
                    Log.d("SearchViewModel", "Found ${filteredEvents.size} events matching '$query'")
                } else {
                    _events.value = emptyList()
                }
            } else {
                // Load from cache or local storage if offline
                _events.value = emptyList()
                if (_error.value.isNullOrBlank()) {
                    _error.value = "No internet connection for event search"
                }
            }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error searching events", e)
            _events.value = emptyList()
        }
    }

    private suspend fun searchPlaces(query: String, filters: SearchFilters) {
        try {
            val allBuildings = buildingRepository.getAllBuildings()
            val allPlaces = allBuildings.flatMap { building ->
                building.places ?: emptyList()
            }

            val filteredPlaces = allPlaces.filter { place ->
                // Text matching only
                place.name.contains(query, ignoreCase = true) ||
                        (place.code?.contains(query, ignoreCase = true) == true)
            }

            _places.value = filteredPlaces
            Log.d("SearchViewModel", "Found ${filteredPlaces.size} places matching '$query'")

        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error searching places", e)
            _places.value = emptyList()
        }
    }

    private fun clearResults() {
        _buildings.value = emptyList()
        _events.value = emptyList()
        _places.value = emptyList()
    }

    private fun saveToRecentSearches(query: String) {
        try {
            // Get current recent searches from cache
            val currentSearches = UserDataCache.get(UserDataCache.KEY_RECENT_SEARCHES) as? MutableList<String>
                ?: mutableListOf()

            // Remove if already exists and add to front
            currentSearches.removeAll { it.equals(query, ignoreCase = true) }
            currentSearches.add(0, query)

            // Keep only last 10 searches
            if (currentSearches.size > 10) {
                currentSearches.removeAt(currentSearches.size - 1)
            }

            // Save back to cache
            UserDataCache.put(UserDataCache.KEY_RECENT_SEARCHES, currentSearches)
            _recentSearches.value = currentSearches.toList()

            Log.d("SearchViewModel", "Saved search query: '$query'")
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error saving recent search", e)
        }
    }

    private fun loadRecentSearches() {
        try {
            val searches = UserDataCache.get(UserDataCache.KEY_RECENT_SEARCHES) as? List<String>
                ?: emptyList()
            _recentSearches.value = searches
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error loading recent searches", e)
            _recentSearches.value = emptyList()
        }
    }

    fun clearRecentSearches() {
        UserDataCache.put(UserDataCache.KEY_RECENT_SEARCHES, mutableListOf<String>())
        _recentSearches.value = emptyList()
    }

    fun getSearchSuggestions(query: String): List<String> {
        val recent = _recentSearches.value ?: emptyList()
        return recent.filter { it.contains(query, ignoreCase = true) }.take(5)
    }

    // Helper function to get available locations for filter dropdown
    fun getAvailableLocations(): List<String> {
        val buildings = _buildings.value ?: emptyList()
        return buildings.map { it.name }.distinct().sorted()
    }

    // Factory class
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                return SearchViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// Extension function for Event to check if it's past
private fun Event.isPastEvent(): Boolean {
    return try {
        val now = java.time.LocalDateTime.now()
        val eventEnd = java.time.LocalDateTime.parse(this.endTime, java.time.format.DateTimeFormatter.ISO_DATE_TIME)
        eventEnd.isBefore(now)
    } catch (e: Exception) {
        false // If we can't parse, assume it's not past
    }
}