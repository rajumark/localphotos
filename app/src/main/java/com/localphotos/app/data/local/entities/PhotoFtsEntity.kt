package com.localphotos.app.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "photos_fts")
data class PhotoFtsEntity(
    val uri: String,
    val text: String
)
