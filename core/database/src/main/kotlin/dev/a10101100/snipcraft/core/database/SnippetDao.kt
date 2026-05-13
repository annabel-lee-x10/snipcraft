package dev.a10101100.snipcraft.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {

    @Query("SELECT * FROM snippets WHERE isEnabled = 1 ORDER BY isPinned DESC, usageCount DESC")
    fun observeEnabled(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE shortcut = :shortcut LIMIT 1")
    suspend fun getByShortcut(shortcut: String): SnippetEntity?

    @Query("SELECT * FROM snippets")
    fun observeAll(): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snippet: SnippetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(snippets: List<SnippetEntity>)

    @Delete
    suspend fun delete(snippet: SnippetEntity)

    @Query("UPDATE snippets SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM snippets")
    suspend fun count(): Int
}
