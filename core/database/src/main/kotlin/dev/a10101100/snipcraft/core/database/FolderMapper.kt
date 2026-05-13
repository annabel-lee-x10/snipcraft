package dev.a10101100.snipcraft.core.database

import dev.a10101100.snipcraft.core.domain.Folder

fun FolderEntity.toDomain() = Folder(
    id = id, name = name, color = color, sortOrder = sortOrder,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun Folder.toEntity() = FolderEntity(
    id = id, name = name, color = color, sortOrder = sortOrder,
    createdAt = createdAt, updatedAt = updatedAt,
)
