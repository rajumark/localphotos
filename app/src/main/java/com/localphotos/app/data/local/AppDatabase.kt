package com.localphotos.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoFtsEntity

@Database(
    entities = [PhotoEntity::class, PhotoFtsEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
}
