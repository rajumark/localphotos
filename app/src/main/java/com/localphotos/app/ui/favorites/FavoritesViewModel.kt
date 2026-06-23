package com.localphotos.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val photos = MutableStateFlow(
        repository.getFavoritePhotosPaged().cachedIn(viewModelScope)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        photos.value = repository.getFavoritePhotosPaged(query).cachedIn(viewModelScope)
    }
}
