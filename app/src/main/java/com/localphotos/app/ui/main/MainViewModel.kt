package com.localphotos.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.localphotos.app.data.repository.PhotoRepository
import com.localphotos.app.ui.components.PhotoFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(PhotoFilter.All)
    val selectedFilter: StateFlow<PhotoFilter> = _selectedFilter.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    val photos = MutableStateFlow(
        repository.getAllPhotosPaged().cachedIn(viewModelScope)
    )

    init {
        viewModelScope.launch {
            repository.getPendingCount().collect { count ->
                _pendingCount.value = count
            }
        }
        viewModelScope.launch {
            refreshAndProcess()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        updatePhotos()
    }

    fun onFilterChange(filter: PhotoFilter) {
        _selectedFilter.value = filter
        updatePhotos()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshAndProcess()
        }
    }

    private suspend fun refreshAndProcess() {
        repository.refreshPhotos()
        repository.processNextBatch()
        updatePhotos()
    }

    private fun updatePhotos() {
        val query = _searchQuery.value
        val filterTextOnly = _selectedFilter.value == PhotoFilter.WithText
        photos.value = repository.getAllPhotosPaged(query, filterTextOnly).cachedIn(viewModelScope)
    }

    fun onResume() {
        viewModelScope.launch {
            refreshAndProcess()
            delay(500)
            refreshAndProcess()
        }
    }
}
