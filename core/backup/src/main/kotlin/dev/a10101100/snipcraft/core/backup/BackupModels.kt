package dev.a10101100.snipcraft.core.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String,
    val snippets: List<SnippetBackup>,
    val folders: List<FolderBackup>,
)

@Serializable
data class SnippetBackup(
    val id: String,
    val shortcut: String,
    val body: String,
    val type: String,
    val triggerMode: String,
    val folderId: String? = null,
    val usageCount: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isPinned: Boolean = false,
    val isEnabled: Boolean = true,
    val caseSensitive: Boolean = false,
    val description: String? = null,
)

@Serializable
data class FolderBackup(
    val id: String,
    val name: String,
    val color: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class ConflictStrategy { SKIP_EXISTING, OVERWRITE }

data class ImportResult(
    val snippetsImported: Int,
    val foldersImported: Int,
    val skipped: Int,
)
