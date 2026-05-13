package dev.a10101100.snipcraft.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int?,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val syncVersion: Long = 0L,
)
