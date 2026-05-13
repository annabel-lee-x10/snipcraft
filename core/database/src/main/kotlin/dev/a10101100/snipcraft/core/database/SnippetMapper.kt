package dev.a10101100.snipcraft.core.database

import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode

fun SnippetEntity.toDomain() = Snippet(
    id = id,
    shortcut = shortcut,
    body = body,
    type = SnippetType.valueOf(type),
    triggerMode = TriggerMode.valueOf(triggerMode),
    folderId = folderId,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isEnabled = isEnabled,
    caseSensitive = caseSensitive,
    description = description,
    syncVersion = syncVersion,
)

fun Snippet.toEntity(schemaVersion: Int = 1) = SnippetEntity(
    id = id,
    shortcut = shortcut,
    body = body,
    type = type.name,
    triggerMode = triggerMode.name,
    folderId = folderId,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isEnabled = isEnabled,
    caseSensitive = caseSensitive,
    description = description,
    schemaVersion = schemaVersion,
    syncVersion = syncVersion,
)
