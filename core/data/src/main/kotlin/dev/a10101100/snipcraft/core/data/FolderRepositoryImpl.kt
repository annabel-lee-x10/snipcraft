package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.FolderDao
import dev.a10101100.snipcraft.core.database.toDomain
import dev.a10101100.snipcraft.core.database.toEntity
import dev.a10101100.snipcraft.core.domain.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FolderRepositoryImpl @Inject constructor(
    private val dao: FolderDao,
) : FolderRepository {
    override fun observeAll(): Flow<List<Folder>> =
        dao.observeAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun upsert(folder: Folder) = dao.upsert(folder.toEntity())
    override suspend fun delete(folder: Folder) = dao.delete(folder.toEntity())
}
