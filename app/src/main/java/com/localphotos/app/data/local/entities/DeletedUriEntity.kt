package com.localphotos.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_uris")
data class DeletedUriEntity(
    @PrimaryKey
    val uri: String
)
