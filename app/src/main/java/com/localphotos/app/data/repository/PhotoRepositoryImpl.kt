package com.localphotos.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.insertSeparators
import androidx.paging.map
import com.localphotos.app.data.local.PhotoDao
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoFtsEntity
import com.localphotos.app.data.local.entities.PhotoListItem
import com.localphotos.app.ocr.OCRProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class PhotoRepositoryImpl(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val ocrProcessor: OCRProcessor
) : PhotoRepository {

    override fun getAllPhotosPaged(
        searchText: String,
        filterTextOnly: Boolean
    ): Flow<PagingData<PhotoListItem>> {
        return createPager(searchText, filterTextOnly, false).flow.map { pagingData ->
            pagingData
                .map<PhotoEntity, PhotoListItem> { PhotoListItem.Photo(it) }
                .insertSeparators { before, after ->
                    val beforeDate = (before as? PhotoListItem.Photo)?.entity?.dateAdded
                    val afterDate = (after as? PhotoListItem.Photo)?.entity?.dateAdded
                    when {
                        before == null && afterDate != null -> PhotoListItem.Header(getSectionLabel(afterDate))
                        after == null -> null
                        beforeDate != null && afterDate != null && getSectionLabel(beforeDate) != getSectionLabel(afterDate) ->
                            PhotoListItem.Header(getSectionLabel(afterDate))
                        else -> null
                    }
                }
        }
    }

    override fun getFavoritePhotosPaged(searchText: String): Flow<PagingData<PhotoListItem>> {
        return Pager(
            config = PagingConfig(pageSize = 30, initialLoadSize = 90, enablePlaceholders = true),
            pagingSourceFactory = {
                if (searchText.isNotBlank()) {
                    SearchPagingSource(searchText, false, true, photoDao)
                } else {
                    photoDao.getFavoritePhotosPaged()
                }
            }
        ).flow.map { pagingData ->
            pagingData
                .map<PhotoEntity, PhotoListItem> { PhotoListItem.Photo(it) }
                .insertSeparators { before, after ->
                    val beforeDate = (before as? PhotoListItem.Photo)?.entity?.dateAdded
                    val afterDate = (after as? PhotoListItem.Photo)?.entity?.dateAdded
                    when {
                        before == null && afterDate != null -> PhotoListItem.Header(getSectionLabel(afterDate))
                        after == null -> null
                        beforeDate != null && afterDate != null && getSectionLabel(beforeDate) != getSectionLabel(afterDate) ->
                            PhotoListItem.Header(getSectionLabel(afterDate))
                        else -> null
                    }
                }
        }
    }

    override fun getPendingCount(): Flow<Int> = photoDao.getPendingCount()

    override val favoriteCount: Flow<Int> = photoDao.getFavoriteCount()

    override suspend fun getPhotoByUri(uri: String): PhotoEntity? = photoDao.getPhotoByUri(uri)

    override suspend fun setFavorite(uri: String, isFavorite: Boolean) {
        photoDao.setFavorite(uri, isFavorite)
    }

    override suspend fun deletePhoto(uri: String) {
        photoDao.deleteFts(uri)
        photoDao.deletePhoto(uri)
    }

    override suspend fun refreshPhotos() = withContext(Dispatchers.IO) {
        val devicePhotos = getDevicePhotosWithDate()
        val deviceUris = devicePhotos.map { it.first }.toSet()
        val deviceUriMap = devicePhotos.associate { it.first to it.second }
        val dbUris = photoDao.getAllUris().toSet()

        val newUris = deviceUris - dbUris
        val deletedUris = dbUris - deviceUris

        if (deletedUris.isNotEmpty()) {
            photoDao.deleteFtsNotIn(deviceUris.toList())
            photoDao.deletePhotosNotIn(deviceUris.toList())
        }

        if (newUris.isNotEmpty()) {
            val newPhotos = newUris.map { uri ->
                PhotoEntity(uri = uri, dateAdded = deviceUriMap[uri] ?: System.currentTimeMillis())
            }
            photoDao.insertPhotos(newPhotos)
        }
    }

    override suspend fun processOne(): Boolean = withContext(Dispatchers.IO) {
        val photo = photoDao.getNextUnprocessed() ?: return@withContext false
        val text = ocrProcessor.extractTextFromUri(Uri.parse(photo.uri))
        photoDao.markProcessed(photo.uri, text)
        photoDao.insertFts(PhotoFtsEntity(uri = photo.uri, text = text))
        true
    }

    private fun getDevicePhotosWithDate(): List<Pair<String, Long>> {
        val result = mutableListOf<Pair<String, Long>>()
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        cursor?.use {
            val idCol = it.getColumnIndex(MediaStore.Images.Media._ID)
            val dateCol = it.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val dateAdded = it.getLong(dateCol) * 1000L
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                ).toString()
                result.add(uri to dateAdded)
            }
        }
        return result
    }

    override fun getAllPhotosGridPaged(
        searchText: String,
        filterTextOnly: Boolean
    ): Flow<PagingData<PhotoListItem>> {
        return createPager(searchText, filterTextOnly, false).flow.map { pagingData ->
            pagingData.map<PhotoEntity, PhotoListItem> { PhotoListItem.Photo(it) }
        }
    }

    private fun createPager(
        searchText: String,
        filterTextOnly: Boolean,
        favoritesOnly: Boolean
    ): Pager<Int, PhotoEntity> {
        return Pager(
            config = PagingConfig(pageSize = 30, initialLoadSize = 90, enablePlaceholders = true),
            pagingSourceFactory = {
                when {
                    searchText.isNotBlank() || filterTextOnly || favoritesOnly ->
                        SearchPagingSource(searchText, filterTextOnly, favoritesOnly, photoDao)
                    filterTextOnly -> photoDao.getPhotosWithTextPaged()
                    else -> photoDao.getAllPhotosPaged()
                }
            }
        )
    }

    companion object {
        private var cachedStartOfToday = 0L
        private var cachedStartOfYesterday = 0L
        private var lastCacheTime = 0L

        fun getSectionLabel(dateAdded: Long): String {
            val now = System.currentTimeMillis()
            if (now - lastCacheTime > 60_000) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                cachedStartOfToday = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                cachedStartOfYesterday = calendar.timeInMillis
                lastCacheTime = now
            }
            return when {
                dateAdded >= cachedStartOfToday -> "Today"
                dateAdded >= cachedStartOfYesterday -> "Yesterday"
                else -> "Older"
            }
        }
    }
}

private class SearchPagingSource(
    private val searchText: String,
    private val filterTextOnly: Boolean,
    private val favoritesOnly: Boolean,
    private val photoDao: PhotoDao
) : PagingSource<Int, PhotoEntity>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PhotoEntity> {
        return try {
            val query = PhotoDao.createSearchQuery(searchText, filterTextOnly, favoritesOnly)
            photoDao.searchPhotosPaged(query).load(params)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PhotoEntity>): Int? = null
}
