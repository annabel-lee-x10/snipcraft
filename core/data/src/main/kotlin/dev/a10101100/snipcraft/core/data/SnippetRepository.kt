package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.domain.Snippet
import kotlinx.coroutines.flow.Flow

interface SnippetRepository {
    fun observeEnabled(): Flow<List<Snippet>>
    suspend fun getByShortcut(shortcut: String): Snippet?
    suspend fun upsert(snippet: Snippet)
    suspend fun upsertAll(snippets: List<Snippet>)
    suspend fun delete(snippet: Snippet)
    suspend fun incrementUsage(id: String, timestampMs: Long)
    suspend fun count(): Int
}
