package com.localphotos.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey
    val uri: String,
    val text: String = "",
    val thumbnailPath: String = "",
    val isFavorite: Boolean = false,
    val isTextProcessed: Boolean = false,
    val isLabelProcessed: Boolean = false,
    val isFaceProcessed: Boolean = false,
    val faceCount: Int = 0,
    val bucketId: String = "",
    val bucketDisplayName: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis()
)
