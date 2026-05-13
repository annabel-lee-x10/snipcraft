package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.domain.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun observeAll(): Flow<List<Folder>>
    suspend fun upsert(folder: Folder)
    suspend fun delete(folder: Folder)
}
