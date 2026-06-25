package com.localphotos.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.localphotos.app.data.local.entities.AlbumSummary
import com.localphotos.app.data.local.entities.DeletedUriEntity
import com.localphotos.app.data.local.entities.LabelWithCount
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoFtsEntity
import com.localphotos.app.data.local.entities.PhotoLabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(fts: PhotoFtsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeletedUri(deletedUri: DeletedUriEntity)

    @Query("SELECT uri FROM deleted_uris")
    suspend fun getAllDeletedUris(): List<String>

    @Query("DELETE FROM deleted_uris WHERE uri = :uri")
    suspend fun deleteDeletedUri(uri: String)

    @Query("DELETE FROM deleted_uris WHERE uri NOT IN (:uris)")
    suspend fun deleteDeletedUrisNotIn(uris: List<String>)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE uri = :uri")
    suspend fun deletePhoto(uri: String)

    @Query("DELETE FROM photos_fts WHERE uri = :uri")
    suspend fun deleteFts(uri: String)

    @Query("DELETE FROM photo_labels WHERE uri = :uri")
    suspend fun deleteLabels(uri: String)

    @Query("DELETE FROM photos WHERE uri NOT IN (:uris)")
    suspend fun deletePhotosNotIn(uris: List<String>)

    @Query("DELETE FROM photos_fts WHERE uri NOT IN (:uris)")
    suspend fun deleteFtsNotIn(uris: List<String>)

    @Query("SELECT * FROM photos ORDER BY dateAdded DESC")
    fun getAllPhotosPaged(): PagingSource<Int, PhotoEntity>

    @Query("SELECT * FROM photos WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoritePhotosPaged(): PagingSource<Int, PhotoEntity>

    @Query("SELECT * FROM photos WHERE text != '' ORDER BY dateAdded DESC")
    fun getPhotosWithTextPaged(): PagingSource<Int, PhotoEntity>

    @RawQuery(observedEntities = [PhotoEntity::class])
    fun searchPhotosPaged(query: SupportSQLiteQuery): PagingSource<Int, PhotoEntity>

    @Query("SELECT COUNT(*) FROM photos WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT * FROM photos WHERE uri = :uri")
    suspend fun getPhotoByUri(uri: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE isTextProcessed = 0 ORDER BY dateAdded DESC LIMIT 1")
    suspend fun getNextUnprocessed(): PhotoEntity?

    @Query("SELECT COUNT(*) FROM photos WHERE isTextProcessed = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT uri FROM photos")
    suspend fun getAllUris(): List<String>

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, isFavorite: Boolean)

    @Query("UPDATE photos SET isTextProcessed = 1, text = :text WHERE uri = :uri")
    suspend fun markProcessed(uri: String, text: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotoLabel(label: PhotoLabelEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotoLabels(labels: List<PhotoLabelEntity>)

    @Query("DELETE FROM photo_labels WHERE uri = :uri")
    suspend fun deletePhotoLabels(uri: String)

    @Query("SELECT label, COUNT(*) as count FROM photo_labels GROUP BY label ORDER BY count DESC")
    fun getAllLabelsWithCount(): Flow<List<LabelWithCount>>

    @Query("SELECT * FROM photos WHERE isTextProcessed = 1 AND isLabelProcessed = 0 ORDER BY dateAdded DESC LIMIT 1")
    suspend fun getNextUnlabeled(): PhotoEntity?

    @Query("SELECT COUNT(*) FROM photos WHERE isTextProcessed = 1 AND isLabelProcessed = 0")
    fun getLabelPendingCount(): Flow<Int>

    @Query("UPDATE photos SET isLabelProcessed = 1 WHERE uri = :uri")
    suspend fun markLabelProcessed(uri: String)

    @Query("""
        SELECT bucketId, bucketDisplayName, COUNT(*) as photoCount
        FROM photos WHERE bucketId != ''
        GROUP BY bucketId
        ORDER BY MAX(dateAdded) DESC
    """)
    fun getAlbums(): Flow<List<AlbumSummary>>

    @Query("SELECT uri FROM photos WHERE bucketId = :bucketId ORDER BY dateAdded DESC LIMIT 4")
    suspend fun getLatestPhotoUrisForAlbum(bucketId: String): List<String>

    @Query("SELECT * FROM photos WHERE bucketId = :bucketId ORDER BY dateAdded DESC")
    fun getPhotosByBucketPaged(bucketId: String): PagingSource<Int, PhotoEntity>

    @Query("SELECT * FROM photos WHERE bucketId = :bucketId AND isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoritePhotosByBucketPaged(bucketId: String): PagingSource<Int, PhotoEntity>

    @Query("SELECT bucketId FROM photos WHERE bucketId != '' GROUP BY bucketId")
    suspend fun getAllBucketIds(): List<String>

    @Query("SELECT bucketDisplayName FROM photos WHERE bucketId = :bucketId LIMIT 1")
    suspend fun getBucketDisplayName(bucketId: String): String?

    companion object {
        fun createSearchQuery(
            searchText: String,
            filterTextOnly: Boolean,
            favoritesOnly: Boolean = false,
            bucketId: String? = null
        ): SupportSQLiteQuery {
            val conditions = mutableListOf<String>()
            if (favoritesOnly) {
                conditions.add("photos.isFavorite = 1")
            }
            if (searchText.isNotBlank()) {
                val parts = searchText.trim().split(Regex("\\s+"))
                val ftsTerms = parts.joinToString(" ") { "${it}*" }
                val escaped = ftsTerms.replace("'", "''")
                conditions.add(
                    "photos.uri IN (SELECT uri FROM photos_fts WHERE photos_fts.text MATCH '$escaped')"
                )
            }
            if (filterTextOnly) {
                conditions.add("photos.text != ''")
            }
            if (!bucketId.isNullOrBlank()) {
                conditions.add("photos.bucketId = '$bucketId'")
            }
            val whereClause = if (conditions.isNotEmpty()) {
                "WHERE ${conditions.joinToString(" AND ")}"
            } else ""
            val sql = """
                SELECT photos.* FROM photos
                $whereClause
                ORDER BY photos.dateAdded DESC
            """.trimIndent()
            return SimpleSQLiteQuery(sql)
        }
    }
}
