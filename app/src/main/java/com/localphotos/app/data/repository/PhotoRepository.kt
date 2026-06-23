package com.localphotos.app.data.repository

import com.localphotos.app.data.local.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getAllPhotos(searchText: String = "", filterTextOnly: Boolean = false): Flow<List<PhotoEntity>>
    fun getFavoritePhotos(searchText: String = ""): Flow<List<PhotoEntity>>
    fun getPendingCount(): Flow<Int>
    val favoriteCount: Flow<Int>
    suspend fun getPhotoByUri(uri: String): PhotoEntity?
    suspend fun setFavorite(uri: String, isFavorite: Boolean)
    suspend fun deletePhoto(uri: String)
    suspend fun refreshPhotos()
    suspend fun processOne(): Boolean
}
