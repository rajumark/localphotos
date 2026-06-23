package com.localphotos.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoFtsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(fts: PhotoFtsEntity)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE uri = :uri")
    suspend fun deletePhoto(uri: String)

    @Query("DELETE FROM photos_fts WHERE uri = :uri")
    suspend fun deleteFts(uri: String)

    @Query("DELETE FROM photos WHERE uri NOT IN (:uris)")
    suspend fun deletePhotosNotIn(uris: List<String>)

    @Query("DELETE FROM photos_fts WHERE uri NOT IN (:uris)")
    suspend fun deleteFtsNotIn(uris: List<String>)

    @Query("SELECT * FROM photos ORDER BY dateAdded DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE text != '' ORDER BY dateAdded DESC")
    fun getPhotosWithText(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE uri IN (SELECT uri FROM photos_fts WHERE photos_fts.text MATCH :query) ORDER BY dateAdded DESC")
    fun searchPhotos(query: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE text != '' AND uri IN (SELECT uri FROM photos_fts WHERE photos_fts.text MATCH :query) ORDER BY dateAdded DESC")
    fun searchPhotosWithText(query: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoritePhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isFavorite = 1 AND uri IN (SELECT uri FROM photos_fts WHERE photos_fts.text MATCH :query) ORDER BY dateAdded DESC")
    fun searchFavoritePhotos(query: String): Flow<List<PhotoEntity>>

    @Query("SELECT COUNT(*) FROM photos WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT * FROM photos WHERE uri = :uri")
    suspend fun getPhotoByUri(uri: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE isTextProcessed = 0 ORDER BY dateAdded ASC LIMIT 1")
    suspend fun getNextUnprocessed(): PhotoEntity?

    @Query("SELECT COUNT(*) FROM photos WHERE isTextProcessed = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT uri FROM photos")
    suspend fun getAllUris(): List<String>

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, isFavorite: Boolean)

    @Query("UPDATE photos SET isTextProcessed = 1, text = :text WHERE uri = :uri")
    suspend fun markProcessed(uri: String, text: String)
}
