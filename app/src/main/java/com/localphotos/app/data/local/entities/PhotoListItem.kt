package com.localphotos.app.data.local.entities

sealed interface PhotoListItem {
    data class Header(val label: String) : PhotoListItem
    data class Photo(val entity: PhotoEntity) : PhotoListItem
}
