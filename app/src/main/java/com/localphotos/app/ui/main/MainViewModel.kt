package com.localphotos.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoGroup
import com.localphotos.app.data.repository.PhotoRepository
import com.localphotos.app.ui.components.PhotoFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

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

    private val _photos = MutableStateFlow<List<PhotoGroup>>(emptyList())
    val photos: StateFlow<List<PhotoGroup>> = _photos.asStateFlow()

    private var processingJob: Job? = null

    init {
        viewModelScope.launch {
            combine(_searchQuery, _selectedFilter) { query, filter ->
                query to filter
            }.collectLatest { (query, filter) ->
                repository.getAllPhotos(query, filter == PhotoFilter.WithText)
                    .map { photoList -> groupPhotosByDate(photoList) }
                    .collect { grouped ->
                        _photos.value = grouped
                    }
            }
        }
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

    companion object {
        private val HEADER_ORDER = mapOf(
            "Today" to 0,
            "Yesterday" to 1,
            "Older" to 2
        )

        fun groupPhotosByDate(photos: List<PhotoEntity>): List<PhotoGroup> {
            if (photos.isEmpty()) return emptyList()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val startOfYesterday = calendar.timeInMillis

            return photos.groupBy { photo ->
                when {
                    photo.dateAdded >= startOfToday -> "Today"
                    photo.dateAdded >= startOfYesterday -> "Yesterday"
                    else -> "Older"
                }
            }.map { (header, groupPhotos) ->
                PhotoGroup(header = header, photos = groupPhotos)
            }.sortedBy { HEADER_ORDER[it.header] ?: 99 }
        }
    }
}
