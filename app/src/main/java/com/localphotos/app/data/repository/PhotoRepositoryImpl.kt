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
import com.localphotos.app.data.local.entities.AlbumInfo
import com.localphotos.app.data.local.entities.DeletedUriEntity
import com.localphotos.app.data.local.entities.LabelWithCount
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoFtsEntity
import com.localphotos.app.data.local.entities.PhotoLabelEntity
import com.localphotos.app.data.local.entities.PhotoListItem
import com.localphotos.app.labeling.LabelProcessor
import com.localphotos.app.ocr.OCRProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class PhotoRepositoryImpl(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val ocrProcessor: OCRProcessor,
    private val labelProcessor: LabelProcessor
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
                    SearchPagingSource(searchText, false, true, null, photoDao)
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
        photoDao.insertDeletedUri(DeletedUriEntity(uri))
        photoDao.deleteFts(uri)
        photoDao.deleteLabels(uri)
        photoDao.deletePhoto(uri)
    }

    override suspend fun refreshPhotos() = withContext(Dispatchers.IO) {
        val devicePhotos = getDevicePhotosWithMeta()
        val deviceUris = devicePhotos.map { it.uri }.toSet()
        val deviceMetaMap = devicePhotos.associateBy { it.uri }
        val dbUris = photoDao.getAllUris().toSet()
        val manuallyDeletedUris = photoDao.getAllDeletedUris().toSet()

        val newUris = deviceUris - dbUris - manuallyDeletedUris
        val deletedUris = dbUris - deviceUris

        if (deletedUris.isNotEmpty()) {
            photoDao.deleteFtsNotIn(deviceUris.toList())
            photoDao.deletePhotosNotIn(deviceUris.toList())
            photoDao.deleteDeletedUrisNotIn(deviceUris.toList())
        }

        if (newUris.isNotEmpty()) {
            val newPhotos = newUris.map { uri ->
                val meta = deviceMetaMap[uri]
                PhotoEntity(
                    uri = uri,
                    dateAdded = meta?.dateAdded ?: System.currentTimeMillis(),
                    bucketId = meta?.bucketId ?: "",
                    bucketDisplayName = meta?.bucketDisplayName ?: ""
                )
            }
            photoDao.insertPhotos(newPhotos)
        }
    }

    private data class PhotoMeta(
        val uri: String,
        val dateAdded: Long,
        val bucketId: String,
        val bucketDisplayName: String
    )

    private fun getDevicePhotosWithMeta(): List<PhotoMeta> {
        val result = mutableListOf<PhotoMeta>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
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
            val bucketIdCol = it.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = it.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val dateAdded = it.getLong(dateCol) * 1000L
                val bucketId = if (bucketIdCol >= 0) it.getString(bucketIdCol) ?: "" else ""
                val bucketName = if (bucketNameCol >= 0) it.getString(bucketNameCol) ?: "" else ""
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                ).toString()
                result.add(PhotoMeta(uri, dateAdded, bucketId, bucketName))
            }
        }
        return result
    }

    override suspend fun processOne(): Boolean = withContext(Dispatchers.IO) {
        val photo = photoDao.getNextUnprocessed() ?: return@withContext false
        val uri = Uri.parse(photo.uri)
        val filename = getPhotoDisplayName(uri)
        val text = ocrProcessor.extractTextFromUri(uri)
        val ftsText = if (filename != null) "$filename $text" else text
        photoDao.markProcessed(photo.uri, text)
        photoDao.insertFts(PhotoFtsEntity(uri = photo.uri, text = ftsText))
        true
    }

    override fun getAllLabelsWithCount(): Flow<List<LabelWithCount>> =
        photoDao.getAllLabelsWithCount()

    override fun getLabelPendingCount(): Flow<Int> = photoDao.getLabelPendingCount()

    override suspend fun processOneLabel(): Boolean = withContext(Dispatchers.IO) {
        val photo = photoDao.getNextUnlabeled() ?: return@withContext false
        val uri = Uri.parse(photo.uri)
        val labels = labelProcessor.extractLabelsFromUri(uri)
        if (labels.isNotEmpty()) {
            photoDao.insertPhotoLabels(labels.map {
                PhotoLabelEntity(uri = photo.uri, label = it.text, confidence = it.confidence)
            })
        }
        photoDao.markLabelProcessed(photo.uri)
        true
    }

    override fun getAlbums(): Flow<List<AlbumInfo>> {
        val priorityOrder = listOf(
            "Camera", "Screenshots", "Downloads", "Instagram", "WhatsApp Images"
        )
        return photoDao.getAlbums().map { summaries ->
            val latestUrisMap = summaries.associate { summary ->
                summary.bucketId to photoDao.getLatestPhotoUrisForAlbum(summary.bucketId)
            }
            summaries.map { summary ->
                AlbumInfo(
                    bucketId = summary.bucketId,
                    bucketDisplayName = summary.bucketDisplayName,
                    photoCount = summary.photoCount,
                    latestPhotoUris = latestUrisMap[summary.bucketId] ?: emptyList()
                )
            }.sortedWith(compareBy<AlbumInfo> { album ->
                val idx = priorityOrder.indexOf(album.bucketDisplayName)
                if (idx >= 0) idx else Int.MAX_VALUE
            }.thenBy { it.bucketDisplayName.lowercase() })
        }
    }

    override fun getPhotosPaged(
        searchText: String,
        filterTextOnly: Boolean,
        favoritesOnly: Boolean,
        bucketId: String?
    ): Flow<PagingData<PhotoListItem>> {
        return createPager(searchText, filterTextOnly, favoritesOnly, bucketId).flow.map { pagingData ->
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

    override fun getPhotosGridPaged(
        searchText: String,
        filterTextOnly: Boolean,
        favoritesOnly: Boolean,
        bucketId: String?
    ): Flow<PagingData<PhotoListItem>> {
        return createPager(searchText, filterTextOnly, favoritesOnly, bucketId).flow.map { pagingData ->
            pagingData.map<PhotoEntity, PhotoListItem> { PhotoListItem.Photo(it) }
        }
    }

    override fun getPhotosByBucketPaged(bucketId: String): Flow<PagingData<PhotoListItem>> {
        return getPhotosPaged("", false, false, bucketId)
    }

    override fun getFavoritePhotosByBucketPaged(bucketId: String): PagingSource<Int, PhotoEntity> {
        return photoDao.getFavoritePhotosByBucketPaged(bucketId)
    }

    override suspend fun getAllPhotoUris(): List<String> = photoDao.getAllUrisOrdered()

    override suspend fun getPhotoDetails(context: Context, uri: String): PhotoRepository.PhotoDetails {
        val queryUri = Uri.parse(uri)
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        var details: PhotoRepository.PhotoDetails? = null
        val cursor = context.contentResolver.query(queryUri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                val dateModified = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                val mime = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                val width = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                val height = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                val bucket = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                details = PhotoRepository.PhotoDetails(
                    displayName = name,
                    dateAdded = sdf.format(java.util.Date(dateAdded * 1000L)),
                    dateModified = sdf.format(java.util.Date(dateModified * 1000L)),
                    mimeType = mime,
                    fileSize = if (size > 0) {
                        if (size < 1024) "$size B"
                        else if (size < 1024 * 1024) "${size / 1024} KB"
                        else "%.1f MB".format(size.toDouble() / (1024 * 1024))
                    } else null,
                    resolution = if (width > 0 && height > 0) "${width}x${height}" else null,
                    bucketDisplayName = bucket,
                    uri = uri
                )
            }
        }
        return details ?: PhotoRepository.PhotoDetails(
            displayName = null, dateAdded = null, dateModified = null,
            mimeType = null, fileSize = null, resolution = null,
            bucketDisplayName = null, uri = uri
        )
    }

    private fun getPhotoDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
            null,
            null,
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }



    override fun getAllPhotosGridPaged(
        searchText: String,
        filterTextOnly: Boolean,
        favoritesOnly: Boolean
    ): Flow<PagingData<PhotoListItem>> {
        return createPager(searchText, filterTextOnly, favoritesOnly).flow.map { pagingData ->
            pagingData.map<PhotoEntity, PhotoListItem> { PhotoListItem.Photo(it) }
        }
    }

    private fun createPager(
        searchText: String,
        filterTextOnly: Boolean,
        favoritesOnly: Boolean,
        bucketId: String? = null
    ): Pager<Int, PhotoEntity> {
        return Pager(
            config = PagingConfig(pageSize = 30, initialLoadSize = 90, enablePlaceholders = true),
            pagingSourceFactory = {
                if (searchText.isNotBlank() || filterTextOnly || favoritesOnly || !bucketId.isNullOrBlank()) {
                    SearchPagingSource(searchText, filterTextOnly, favoritesOnly, bucketId, photoDao)
                } else {
                    photoDao.getAllPhotosPaged()
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
    private val bucketId: String?,
    private val photoDao: PhotoDao
) : PagingSource<Int, PhotoEntity>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PhotoEntity> {
        return try {
            val query = PhotoDao.createSearchQuery(searchText, filterTextOnly, favoritesOnly, bucketId)
            photoDao.searchPhotosPaged(query).load(params)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PhotoEntity>): Int? = null
}
