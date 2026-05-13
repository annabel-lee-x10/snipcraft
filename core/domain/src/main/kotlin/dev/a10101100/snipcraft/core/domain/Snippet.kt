package dev.a10101100.snipcraft.core.domain

data class Snippet(
    val id: String,
    val shortcut: String,
    val body: String,
    val type: SnippetType = SnippetType.PLAIN,
    val triggerMode: TriggerMode = TriggerMode.ON_DELIMITER,
    val folderId: String? = null,
    val usageCount: Long = 0L,
    val lastUsedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isPinned: Boolean = false,
    val isEnabled: Boolean = true,
    val caseSensitive: Boolean = false,
    val description: String? = null,
)
