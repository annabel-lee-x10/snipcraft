package dev.a10101100.snipcraft.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snippets",
    indices = [
        Index("shortcut"),
        Index("folderId"),
        Index(value = ["usageCount"]),
        Index(value = ["lastUsedAt"]),
    ],
)
data class SnippetEntity(
    @PrimaryKey val id: String,
    val shortcut: String,
    val body: String,
    val type: String,                  // SnippetType name
    val triggerMode: String,           // TriggerMode name
    val folderId: String?,
    val usageCount: Long,
    val lastUsedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val isEnabled: Boolean,
    val caseSensitive: Boolean,
    val description: String?,
    val schemaVersion: Int,
)
