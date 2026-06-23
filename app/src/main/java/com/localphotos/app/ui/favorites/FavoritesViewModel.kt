package com.localphotos.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoGroup
import com.localphotos.app.data.repository.PhotoRepository
import com.localphotos.app.ui.main.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoGroup>>(emptyList())
    val photos: StateFlow<List<PhotoGroup>> = _photos.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery.collect { query ->
                repository.getFavoritePhotos(query)
                    .map { photoList -> MainViewModel.groupPhotosByDate(photoList) }
                    .collect { grouped ->
                        _photos.value = grouped
                    }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
