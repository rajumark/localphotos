package com.localphotos.app.ui.dbinfo

import androidx.lifecycle.ViewModel
import com.localphotos.app.data.local.PhotoDao
import kotlinx.coroutines.flow.Flow

class DbInfoViewModel(
    private val photoDao: PhotoDao
) : ViewModel() {

    val totalPhotos: Flow<Int> = photoDao.getTotalPhotoCountFlow()
    val textProcessed: Flow<Int> = photoDao.getTextProcessedCount()
    val labelProcessed: Flow<Int> = photoDao.getLabelProcessedCount()
    val faceProcessed: Flow<Int> = photoDao.getFaceProcessedCount()
    val fullyProcessed: Flow<Int> = photoDao.getFullyProcessedCount()
    val distinctLabels: Flow<Int> = photoDao.getDistinctLabelCount()
    val totalLabelAssignments: Flow<Int> = photoDao.getTotalLabelAssignments()
    val totalFacesFound: Flow<Int> = photoDao.getTotalFacesFound()
}
