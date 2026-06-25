package com.localphotos.app.ui.labels

import androidx.lifecycle.ViewModel
import com.localphotos.app.data.local.entities.LabelWithCount
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow

class LabelsViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    val labels: Flow<List<LabelWithCount>> = repository.getAllLabelsWithCount()
}
