package com.localphotos.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class FavoritesViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val photos: Flow<PagingData<PhotoEntity>> = _searchQuery
        .flatMapLatest { query ->
            repository.getFavoritePhotosPaged(query)
        }.cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
