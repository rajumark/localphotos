package com.localphotos.app.ui.labels

import androidx.lifecycle.ViewModel
import com.localphotos.app.data.local.entities.LabelWithPhotos
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class LabelsViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val labels: Flow<List<LabelWithPhotos>> = combine(
        repository.getLabelWithPhotos(), _searchQuery
    ) { allLabels, query ->
        if (query.isBlank()) allLabels
        else allLabels.filter { it.label.contains(query, ignoreCase = true) }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
