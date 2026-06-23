package com.localphotos.app.data.model

data class Photo(
    val uri: String,
    val text: String = "",
    val thumbnailPath: String = "",
    val isFavorite: Boolean = false,
    val isTextProcessed: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
