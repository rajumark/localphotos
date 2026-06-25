package com.localphotos.app.data.local.entities

data class AlbumInfo(
    val bucketId: String,
    val bucketDisplayName: String,
    val photoCount: Int,
    val latestPhotoUris: List<String>
)
