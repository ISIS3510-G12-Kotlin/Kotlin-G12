package com.example.explorandes.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.explorandes.models.VisitedItem
import com.example.explorandes.repositories.VisitedRepository
import kotlinx.coroutines.launch
import com.example.explorandes.database.AppDatabase

class VisitedViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).visitedDao()
    val allVisits: LiveData<List<VisitedItem>> = dao.getAllVisits().asLiveData()

    fun insertVisit(visit: VisitedItem) {
        viewModelScope.launch {
            dao.insertVisit(visit)
        }
    }

    fun markAllAsSynced() {
        viewModelScope.launch {
            dao.markAllAsSynced()
        }
    }
}

