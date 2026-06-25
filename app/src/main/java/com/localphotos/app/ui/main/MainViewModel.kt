package com.localphotos.app.ui.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.localphotos.app.data.local.entities.PhotoListItem
import com.localphotos.app.data.repository.PhotoRepository
import com.localphotos.app.ui.components.FilterMode
import com.localphotos.app.worker.ProcessingWorker
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
    application: Application,
    private val repository: PhotoRepository,
    private val prefs: SharedPreferences,
    private val filter: GalleryFilter? = null
) : AndroidViewModel(application) {
    private val bucketId: String? get() = filter?.bucketId
    private val label: String? get() = filter?.label

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _totalPhotoCount = MutableStateFlow(0)
    val totalPhotoCount: StateFlow<Int> = _totalPhotoCount.asStateFlow()

    private val _parsedCount = MutableStateFlow(0)
    val parsedCount: StateFlow<Int> = _parsedCount.asStateFlow()

    private val _processingChip = MutableStateFlow<String?>(null)
    val processingChip: StateFlow<String?> = _processingChip.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isGridView = MutableStateFlow(prefs.getBoolean("is_grid_view", true))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris: StateFlow<Set<String>> = _selectedUris.asStateFlow()

    val isAlbum: Boolean get() = bucketId != null
    val isLabelFilter: Boolean get() = label != null

    val photos: Flow<PagingData<PhotoListItem>> = combine(
        _searchQuery.debounce(300), _isGridView, _filterMode
    ) { query, isGrid, mode -> Triple(query, isGrid, mode) }
        .flatMapLatest { (query, isGrid, mode) ->
            val (filterTextOnly, favoritesOnly) = when (mode) {
                FilterMode.ALL -> false to false
                FilterMode.TEXT_ONLY -> true to false
                FilterMode.FAVORITES -> false to true
            }
            if (isGrid) {
                repository.getPhotosGridPaged(query, filterTextOnly, favoritesOnly, bucketId, label)
            } else {
                repository.getPhotosPaged(query, filterTextOnly, favoritesOnly, bucketId, label)
            }
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            _totalPhotoCount.value = repository.getTotalPhotoCount()
        }
        viewModelScope.launch {
            var lastParsed = 0
            var lastTime = 0L
            combine(
                repository.getPendingCount(),
                repository.getLabelPendingCount(),
                repository.getFacePendingCount()
            ) { ocr, label, face -> ocr + label + face }
                .collect { unprocessed ->
                    val total = _totalPhotoCount.value
                    val parsed = (total - unprocessed).coerceAtLeast(0)
                    _isProcessing.value = unprocessed > 0
                    _parsedCount.value = parsed

                    if (unprocessed > 0) {
                        val now = System.currentTimeMillis()
                        if (parsed > lastParsed && lastParsed > 0) {
                            val phaseTime = (now - lastTime) / (parsed - lastParsed)
                            val etaMin = ((phaseTime * unprocessed) / 60000).toInt().coerceAtLeast(1)
                            _processingChip.value = "$parsed : ~${etaMin}m"
                        } else if (parsed > 0) {
                            _processingChip.value = "$parsed : ~?m"
                        } else {
                            _processingChip.value = "0"
                        }
                        lastParsed = parsed
                        lastTime = now
                    } else {
                        _processingChip.value = null
                        lastParsed = 0
                    }
                }
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

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
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
            repository.refreshPhotos()
            val pending = repository.getTotalUnprocessedCount()
            if (pending == 0) return@launch

            val work = OneTimeWorkRequestBuilder<ProcessingWorker>()
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "photo_processing",
                ExistingWorkPolicy.KEEP,
                work
            )
        }
    }
}
