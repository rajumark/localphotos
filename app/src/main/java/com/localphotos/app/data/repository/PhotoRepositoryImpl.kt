package com.localphotos.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.localphotos.app.data.local.PhotoDao
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoFtsEntity
import com.localphotos.app.ocr.OCRProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PhotoRepositoryImpl(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val ocrProcessor: OCRProcessor
) : PhotoRepository {

    override fun getAllPhotos(
        searchText: String,
        filterTextOnly: Boolean
    ): Flow<List<PhotoEntity>> {
        return when {
            searchText.isNotBlank() && filterTextOnly ->
                photoDao.searchPhotosWithText(buildFtsQuery(searchText))
            searchText.isNotBlank() ->
                photoDao.searchPhotos(buildFtsQuery(searchText))
            filterTextOnly ->
                photoDao.getPhotosWithText()
            else ->
                photoDao.getAllPhotos()
        }
    }

    override fun getFavoritePhotos(searchText: String): Flow<List<PhotoEntity>> {
        return if (searchText.isNotBlank()) {
            photoDao.searchFavoritePhotos(buildFtsQuery(searchText))
        } else {
            photoDao.getFavoritePhotos()
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
        val deviceUris = getDevicePhotoUris()
        val dbUris = photoDao.getAllUris().toSet()

        val newUris = deviceUris - dbUris
        val deletedUris = dbUris - deviceUris

        if (deletedUris.isNotEmpty()) {
            photoDao.deleteFtsNotIn(deviceUris)
            photoDao.deletePhotosNotIn(deviceUris)
        }

        if (newUris.isNotEmpty()) {
            val newPhotos = newUris.map { uri ->
                PhotoEntity(uri = uri, dateAdded = getDateAdded(uri))
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

    private fun getDevicePhotoUris(): List<String> {
        val uris = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        cursor?.use {
            val idColumn = it.getColumnIndex(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                uris.add(uri.toString())
            }
        }
        return uris
    }

    private fun getDateAdded(uriStr: String): Long {
        return try {
            val cursor = context.contentResolver.query(
                Uri.parse(uriStr),
                arrayOf(MediaStore.Images.Media.DATE_ADDED),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)) * 1000L
                } else System.currentTimeMillis()
            } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    companion object {
        fun buildFtsQuery(searchText: String): String {
            val parts = searchText.trim().split(Regex("\\s+"))
            return parts.joinToString(" ") { "${it}*" }
        }
    }
}
