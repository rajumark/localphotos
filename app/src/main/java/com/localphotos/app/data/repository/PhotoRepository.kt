package com.localphotos.app.data.repository

import android.content.Context
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.localphotos.app.data.local.entities.AlbumInfo
import com.localphotos.app.data.local.entities.LabelWithCount
import com.localphotos.app.data.local.entities.LabelWithPhotos
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoListItem
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getAllPhotosPaged(searchText: String = "", filterTextOnly: Boolean = false): Flow<PagingData<PhotoListItem>>
    fun getAllPhotosGridPaged(searchText: String = "", filterTextOnly: Boolean = false, favoritesOnly: Boolean = false): Flow<PagingData<PhotoListItem>>
    fun getFavoritePhotosPaged(searchText: String = ""): Flow<PagingData<PhotoListItem>>
    fun getPhotosPaged(searchText: String = "", filterTextOnly: Boolean = false, favoritesOnly: Boolean = false, bucketId: String? = null, label: String? = null): Flow<PagingData<PhotoListItem>>
    fun getPhotosGridPaged(searchText: String = "", filterTextOnly: Boolean = false, favoritesOnly: Boolean = false, bucketId: String? = null, label: String? = null): Flow<PagingData<PhotoListItem>>
    fun getPendingCount(): Flow<Int>
    val favoriteCount: Flow<Int>
    suspend fun getPhotoByUri(uri: String): PhotoEntity?
    suspend fun setFavorite(uri: String, isFavorite: Boolean)
    suspend fun deletePhoto(uri: String)
    suspend fun refreshPhotos()
    suspend fun processOne(): Boolean
    suspend fun processOneLabel(): Boolean
    fun getAllLabelsWithCount(): Flow<List<LabelWithCount>>
    fun getLabelPendingCount(): Flow<Int>
    fun getAlbums(): Flow<List<AlbumInfo>>
    fun getPhotosByBucketPaged(bucketId: String): Flow<PagingData<PhotoListItem>>
    fun getFavoritePhotosByBucketPaged(bucketId: String): PagingSource<Int, PhotoEntity>
    suspend fun getAllPhotoUris(): List<String>
    data class PhotoDetails(
        val displayName: String?,
        val dateAdded: String?,
        val dateModified: String?,
        val mimeType: String?,
        val fileSize: String?,
        val resolution: String?,
        val bucketDisplayName: String?,
        val uri: String
    )
    suspend fun getPhotoDetails(context: Context, uri: String): PhotoDetails
    fun getLabelWithPhotos(): Flow<List<LabelWithPhotos>>
}
