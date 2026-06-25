package com.localphotos.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "photo_labels",
    primaryKeys = ["uri", "label"],
    foreignKeys = [ForeignKey(
        entity = PhotoEntity::class,
        parentColumns = ["uri"],
        childColumns = ["uri"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PhotoLabelEntity(
    val uri: String,
    val label: String,
    val confidence: Float
)

data class LabelWithCount(
    val label: String,
    val count: Int
)

data class LabelWithPhotos(
    val label: String,
    val count: Int,
    val latestPhotoUris: List<String>
)
