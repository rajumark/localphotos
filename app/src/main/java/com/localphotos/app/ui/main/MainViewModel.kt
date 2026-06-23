package com.localphotos.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.localphotos.app.data.local.entities.PhotoListItem
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val listPhotos: Flow<PagingData<PhotoListItem>> = _searchQuery
        .flatMapLatest { query ->
            repository.getAllPhotosPaged(query, true)
        }.cachedIn(viewModelScope)

    val gridPhotos: Flow<PagingData<PhotoListItem>> = _searchQuery
        .flatMapLatest { query ->
            repository.getAllPhotosGridPaged(query, true)
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

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
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
