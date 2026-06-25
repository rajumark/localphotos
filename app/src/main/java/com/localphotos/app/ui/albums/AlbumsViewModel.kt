package com.localphotos.app.ui.albums

import androidx.lifecycle.ViewModel
import com.localphotos.app.data.local.entities.AlbumInfo
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlbumsViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    val albums: Flow<List<AlbumInfo>> = repository.getAlbums()
}
