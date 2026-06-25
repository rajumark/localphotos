package com.localphotos.app.ui.faces

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.localphotos.app.data.local.entities.FaceCountFilter
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FaceGridViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _faceFilter = MutableStateFlow(FaceCountFilter.ALL)
    val faceFilter: StateFlow<FaceCountFilter> = _faceFilter.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris: StateFlow<Set<String>> = _selectedUris.asStateFlow()

    val photos: Flow<PagingData<PhotoEntity>> = _faceFilter
        .flatMapLatest { filter -> repository.getFacePhotosPaged(filter) }
        .cachedIn(viewModelScope)

    fun setFaceFilter(filter: FaceCountFilter) {
        _faceFilter.value = filter
        clearSelection()
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
}
