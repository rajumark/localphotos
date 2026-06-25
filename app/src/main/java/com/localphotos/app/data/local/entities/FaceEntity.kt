package com.localphotos.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "faces",
    foreignKeys = [ForeignKey(
        entity = PhotoEntity::class,
        parentColumns = ["uri"],
        childColumns = ["uri"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("uri")]
)
data class FaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val faceIndex: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

enum class FaceCountFilter(val label: String) {
    ALL("All"),
    ONE("1 face"),
    TWO("2 face"),
    THREE("3 face"),
    FOUR("4 face"),
    GROUP("Group"),
    NO_FACE("No Face")
}
