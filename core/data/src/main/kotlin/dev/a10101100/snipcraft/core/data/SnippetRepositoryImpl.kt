package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.SnippetDao
import dev.a10101100.snipcraft.core.database.toDomain
import dev.a10101100.snipcraft.core.database.toEntity
import dev.a10101100.snipcraft.core.domain.Snippet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SnippetRepositoryImpl @Inject constructor(
    private val dao: SnippetDao,
) : SnippetRepository {

    override fun observeEnabled(): Flow<List<Snippet>> =
        dao.observeEnabled().map { entities -> entities.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Snippet>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getByShortcut(shortcut: String): Snippet? =
        dao.getByShortcut(shortcut)?.toDomain()

    override suspend fun upsert(snippet: Snippet) = dao.upsert(snippet.toEntity())

    override suspend fun upsertAll(snippets: List<Snippet>) =
        dao.upsertAll(snippets.map { it.toEntity() })

    override suspend fun delete(snippet: Snippet) = dao.delete(snippet.toEntity())

    override suspend fun incrementUsage(id: String, timestampMs: Long) =
        dao.incrementUsage(id, timestampMs)

    override suspend fun count(): Int = dao.count()
}
