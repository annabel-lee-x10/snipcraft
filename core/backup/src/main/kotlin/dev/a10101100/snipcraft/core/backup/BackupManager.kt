package dev.a10101100.snipcraft.core.backup

import dev.a10101100.snipcraft.core.data.FolderRepository
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Folder
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val json: Json,
) {
    suspend fun export(): String {
        val snippets = snippetRepository.observeAll().first()
        val folders = folderRepository.observeAll().first()
        val data = BackupData(
            exportedAt = Instant.now().toString(),
            snippets = snippets.map { it.toBackup() },
            folders = folders.map { it.toBackup() },
        )
        return json.encodeToString(data)
    }

    suspend fun import(jsonString: String, conflictStrategy: ConflictStrategy): ImportResult {
        val data = json.decodeFromString<BackupData>(jsonString)
        var snippetsImported = 0
        var foldersImported = 0
        var skipped = 0

        data.folders.forEach { folderBackup ->
            folderRepository.upsert(folderBackup.toDomain())
            foldersImported++
        }

        data.snippets.forEach { snippetBackup ->
            val existing = snippetRepository.getByShortcut(snippetBackup.shortcut)
            if (existing != null && conflictStrategy == ConflictStrategy.SKIP_EXISTING) {
                skipped++
            } else {
                snippetRepository.upsert(snippetBackup.toDomain())
                snippetsImported++
            }
        }

        return ImportResult(
            snippetsImported = snippetsImported,
            foldersImported = foldersImported,
            skipped = skipped,
        )
    }

    private fun Snippet.toBackup() = SnippetBackup(
        id = id,
        shortcut = shortcut,
        body = body,
        type = type.name,
        triggerMode = triggerMode.name,
        folderId = folderId,
        usageCount = usageCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isEnabled = isEnabled,
        caseSensitive = caseSensitive,
        description = description,
    )

    private fun Folder.toBackup() = FolderBackup(
        id = id,
        name = name,
        color = color,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun SnippetBackup.toDomain() = Snippet(
        id = id,
        shortcut = shortcut,
        body = body,
        type = runCatching { SnippetType.valueOf(type) }.getOrDefault(SnippetType.PLAIN),
        triggerMode = runCatching { TriggerMode.valueOf(triggerMode) }.getOrDefault(TriggerMode.ON_DELIMITER),
        folderId = folderId,
        usageCount = usageCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isEnabled = isEnabled,
        caseSensitive = caseSensitive,
        description = description,
    )

    private fun FolderBackup.toDomain() = Folder(
        id = id,
        name = name,
        color = color,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
