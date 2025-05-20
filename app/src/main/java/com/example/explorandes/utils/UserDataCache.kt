package com.example.explorandes.utils

import android.util.Log
import androidx.collection.ArrayMap

/**
 * Memory-efficient cache for user profile data using ArrayMap
 */
object UserDataCache {
    // Use ArrayMap instead of HashMap for memory efficiency
    private val cache = ArrayMap<String, Any>()

    // Constants for common cache keys
    const val KEY_PROFILE = "user_profile"
    const val KEY_PREFERENCES = "user_preferences"
    const val KEY_RECENT_SEARCHES = "recent_searches"
    const val KEY_FAVORITE_BUILDINGS = "favorite_buildings"
    const val KEY_VISITED_BUILDINGS = "visited_buildings"

    // Tag for logging
    private const val TAG = "UserDataCache"

    /**
     * Puts user data in the cache with the specified key
     */
    fun put(key: String, value: Any) {
        // Add or update item in cache
        cache[key] = value
        Log.d(TAG, "Cached user data: $key")
    }

    /**
     * Gets user data from the cache by key
     * @return The cached value, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val value = cache[key]
        val hit = value != null

        Log.d(TAG, "Cache ${if (hit) "HIT" else "MISS"} for user data: $key")

        return value as? T
    }

    /**
     * Checks if the cache contains user data with the specified key
     */
    fun contains(key: String): Boolean {
        return cache.containsKey(key)
    }

    /**
     * Removes user data from the cache
     * @return true if data was removed, false otherwise
     */
    fun remove(key: String): Boolean {
        val removed = cache.remove(key) != null
        if (removed) {
            Log.d(TAG, "Removed user data from cache: $key")
        }
        return removed
    }

    /**
     * Clears all user data from the cache
     */
    fun clear() {
        val size = cache.size
        cache.clear()
        Log.d(TAG, "User data cache cleared: $size items removed")
    }

    /**
     * Gets the current size of the cache
     */
    fun size(): Int {
        return cache.size
    }

    /**
     * Adds a visited building to the user's history
     */
    fun addVisitedBuilding(buildingId: Long, buildingName: String) {
        // Get current visited buildings or create a new list
        val visitedBuildings = get<MutableList<Map<String, Any>>>(KEY_VISITED_BUILDINGS) ?: mutableListOf()

        // Add new building visit with timestamp
        val visit = mapOf(
            "id" to buildingId,
            "name" to buildingName,
            "timestamp" to System.currentTimeMillis()
        )

        // Add to the beginning of the list (most recent first)
        visitedBuildings.add(0, visit)

        // Keep only the last 10 visits
        val trimmedList = if (visitedBuildings.size > 10) {
            visitedBuildings.take(10).toMutableList()
        } else {
            visitedBuildings
        }

        // Update the cache
        put(KEY_VISITED_BUILDINGS, trimmedList)
    }

    /**
     * Gets the user's visited buildings history
     * @return List of visited buildings or empty list if none
     */
    fun getVisitedBuildings(): List<Map<String, Any>> {
        return get<List<Map<String, Any>>>(KEY_VISITED_BUILDINGS) ?: emptyList()
    }

    /**
     * Adds a search query to the user's recent searches
     */
    fun addRecentSearch(query: String) {
        // Get current searches or create a new list
        val searches = get<MutableList<String>>(KEY_RECENT_SEARCHES) ?: mutableListOf()

        // Remove if already in list to avoid duplicates
        searches.remove(query)

        // Add to the beginning (most recent first)
        searches.add(0, query)

        // Keep only the last 5 searches
        val trimmedList = if (searches.size > 5) {
            searches.take(5).toMutableList()
        } else {
            searches
        }

        // Update the cache
        put(KEY_RECENT_SEARCHES, trimmedList)
    }

    /**
     * Gets the user's recent searches
     * @return List of recent searches or empty list if none
     */
    fun getRecentSearches(): List<String> {
        return get<List<String>>(KEY_RECENT_SEARCHES) ?: emptyList()
    }

    /**
     * Toggles a building as favorite
     * @return true if added to favorites, false if removed
     */
    fun toggleFavoriteBuilding(buildingId: Long, buildingName: String): Boolean {
        // Get current favorites or create a new map
        val favorites = get<MutableMap<Long, String>>(KEY_FAVORITE_BUILDINGS) ?: mutableMapOf()

        val result = if (favorites.containsKey(buildingId)) {
            // Remove from favorites
            favorites.remove(buildingId)
            false
        } else {
            // Add to favorites
            favorites[buildingId] = buildingName
            true
        }

        // Update the cache
        put(KEY_FAVORITE_BUILDINGS, favorites)

        return result
    }

    /**
     * Checks if a building is in favorites
     */
    fun isBuildingFavorite(buildingId: Long): Boolean {
        val favorites = get<Map<Long, String>>(KEY_FAVORITE_BUILDINGS) ?: emptyMap()
        return favorites.containsKey(buildingId)
    }

    /**
     * Gets all favorite buildings
     * @return Map of building IDs to names, or empty map if none
     */
    fun getFavoriteBuildings(): Map<Long, String> {
        return get<Map<Long, String>>(KEY_FAVORITE_BUILDINGS) ?: emptyMap()
    }
}