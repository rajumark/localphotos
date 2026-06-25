package com.localphotos.app.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(
    private val repository: PhotoRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isGridView = MutableStateFlow(prefs.getBoolean("is_grid_view", true))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _showFavorites = MutableStateFlow(false)
    val showFavorites: StateFlow<Boolean> = _showFavorites.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris: StateFlow<Set<String>> = _selectedUris.asStateFlow()

    val photos: Flow<PagingData<PhotoListItem>> = combine(
        _searchQuery.debounce(300), _isGridView, _showFavorites
    ) { query, isGrid, showFav -> Triple(query, isGrid, showFav) }
        .flatMapLatest { (query, isGrid, showFav) ->
            if (showFav) {
                if (isGrid) repository.getAllPhotosGridPaged(query, true)
                else repository.getFavoritePhotosPaged(query)
            } else {
                if (isGrid) repository.getAllPhotosGridPaged(query, true)
                else repository.getAllPhotosPaged(query, true)
            }
        }
        .cachedIn(viewModelScope)

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
        val newValue = !_isGridView.value
        _isGridView.value = newValue
        prefs.edit().putBoolean("is_grid_view", newValue).apply()
    }

    fun setShowFavorites(show: Boolean) {
        _showFavorites.value = show
    }

    fun toggleSelection(uri: String) {
        val current = _selectedUris.value.toMutableSet()
        if (current.contains(uri)) current.remove(uri) else current.add(uri)
        _selectedUris.value = current
    }

    fun clearSelection() {
        _selectedUris.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val uris = _selectedUris.value.toList()
            uris.forEach { repository.deletePhoto(it) }
            _selectedUris.value = emptySet()
        }
    }

    fun shareSelected(context: Context) {
        val uris = _selectedUris.value.toList()
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(uris[0]))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris.map { Uri.parse(it) }))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share photos"))
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
