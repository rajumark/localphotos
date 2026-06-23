package com.localphotos.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.repository.PhotoRepository
import com.localphotos.app.ui.components.PhotoFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val photos: Flow<PagingData<PhotoEntity>> = combine(
        _searchQuery, _selectedFilter
    ) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        repository.getAllPhotosPaged(query, filter == PhotoFilter.WithText)
    }.cachedIn(viewModelScope)

    private var processingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getPendingCount().collect { count ->
                _pendingCount.value = count
                if (count > 0 && processingJob == null) {
                    startProcessing()
                }
            }
        }
        viewModelScope.launch {
            refreshAndProcess()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: PhotoFilter) {
        _selectedFilter.value = filter
    }

    fun onResume() {
        viewModelScope.launch {
            refreshAndProcess()
        }
    }

    private suspend fun refreshAndProcess() {
        repository.refreshPhotos()
        if (_pendingCount.value > 0) startProcessing()
    }

    private fun startProcessing() {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _isProcessing.value = true
            while (true) {
                val hasMore = repository.processOne()
                if (!hasMore) break
                delay(100)
            }
            _isProcessing.value = false
            processingJob = null
        }
    }

    override fun onCleared() {
        processingJob?.cancel()
        super.onCleared()
    }
}
