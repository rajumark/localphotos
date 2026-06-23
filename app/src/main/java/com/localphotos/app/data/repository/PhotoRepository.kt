package com.localphotos.app.data.repository

import androidx.paging.PagingData
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoListItem
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getAllPhotosPaged(searchText: String = "", filterTextOnly: Boolean = false): Flow<PagingData<PhotoListItem>>
    fun getAllPhotosGridPaged(searchText: String = "", filterTextOnly: Boolean = false): Flow<PagingData<PhotoListItem>>
    fun getFavoritePhotosPaged(searchText: String = ""): Flow<PagingData<PhotoListItem>>
    fun getPendingCount(): Flow<Int>
    val favoriteCount: Flow<Int>
    suspend fun getPhotoByUri(uri: String): PhotoEntity?
    suspend fun setFavorite(uri: String, isFavorite: Boolean)
    suspend fun deletePhoto(uri: String)
    suspend fun refreshPhotos()
    suspend fun processOne(): Boolean
}
