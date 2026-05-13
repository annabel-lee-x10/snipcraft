package dev.a10101100.snipcraft.core.sync

import dev.a10101100.snipcraft.core.backup.BackupData
import dev.a10101100.snipcraft.core.backup.FolderBackup
import dev.a10101100.snipcraft.core.backup.SnippetBackup
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
class SyncEngine @Inject constructor(
    private val webDavClient: WebDavClient,
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    @SyncJson private val json: Json,
) {
    suspend fun sync(config: SyncConfig): SyncResult {
        val creds = WebDavClient.Credentials(config.username, config.password)
        val fileUrl = config.remoteFileUrl()
        val dirUrl  = config.remoteDirUrl()

        val localSnippets = snippetRepository.observeAll().first()
        val localFolders  = folderRepository.observeAll().first()

        return when (val propResult = webDavClient.propFind(fileUrl, creds, !config.requiresHttps())) {
            is WebDavClient.PropFindResult.Error ->
                SyncResult(errors = listOf(propResult.message))

            WebDavClient.PropFindResult.NotFound -> {
                webDavClient.mkCol(dirUrl, creds)
                val snapshot = buildSnapshot(localSnippets, localFolders)
                val ok = webDavClient.put(fileUrl, creds, encodeSnapshot(snapshot))
                if (ok) SyncResult(pushed = localSnippets.size + localFolders.size)
                else SyncResult(errors = listOf("PUT failed during initial push"))
            }

            WebDavClient.PropFindResult.Found -> {
                val body = webDavClient.get(fileUrl, creds)
                    ?: return SyncResult(errors = listOf("GET failed"))
                val remoteData = runCatching { json.decodeFromString<BackupData>(body) }
                    .getOrElse { return SyncResult(errors = listOf("Parse error: ${it.message}")) }

                val (snippetsToPull, snippetsToPush, conflicts) =
                    mergeSnippets(localSnippets, remoteData.snippets)
                val (foldersToPull, foldersToPush, _) =
                    mergeFolders(localFolders, remoteData.folders)

                snippetsToPull.forEach { snippetRepository.upsert(it) }
                foldersToPull.forEach { folderRepository.upsert(it) }

                val mergedSnippets = buildMergedSnippets(localSnippets, snippetsToPull)
                val mergedFolders  = buildMergedFolders(localFolders, foldersToPull)
                val newSnapshot = buildSnapshot(mergedSnippets, mergedFolders)
                webDavClient.put(fileUrl, creds, encodeSnapshot(newSnapshot))

                SyncResult(
                    pushed = snippetsToPush.size + foldersToPush.size,
                    pulled = snippetsToPull.size + foldersToPull.size,
                    conflicts = conflicts,
                )
            }
        }
    }

    private fun mergeSnippets(
        local: List<Snippet>,
        remote: List<SnippetBackup>,
    ): Triple<List<Snippet>, List<Snippet>, Int> {
        val localById  = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val allIds = (localById.keys + remoteById.keys).toSet()

        val toPull    = mutableListOf<Snippet>()
        val toPush    = mutableListOf<Snippet>()
        var conflicts = 0

        for (id in allIds) {
            val loc = localById[id]
            val rem = remoteById[id]
            when {
                loc == null -> toPull.add(rem!!.toDomain())
                rem == null -> toPush.add(loc)
                else -> when (resolveConflict(loc.syncVersion, loc.updatedAt, rem.syncVersion, rem.updatedAt)) {
                    Winner.LOCAL  -> { toPush.add(loc) }
                    Winner.REMOTE -> {
                        toPull.add(rem.toDomain())
                        if (loc.syncVersion != rem.syncVersion || loc.updatedAt != rem.updatedAt) conflicts++
                    }
                }
            }
        }
        return Triple(toPull, toPush, conflicts)
    }

    private fun mergeFolders(
        local: List<Folder>,
        remote: List<FolderBackup>,
    ): Triple<List<Folder>, List<Folder>, Int> {
        val localById  = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val allIds = (localById.keys + remoteById.keys).toSet()

        val toPull = mutableListOf<Folder>()
        val toPush = mutableListOf<Folder>()

        for (id in allIds) {
            val loc = localById[id]
            val rem = remoteById[id]
            when {
                loc == null -> toPull.add(rem!!.toDomain())
                rem == null -> toPush.add(loc)
                else -> when (resolveConflict(loc.syncVersion, loc.updatedAt, rem.syncVersion, rem.updatedAt)) {
                    Winner.LOCAL  -> toPush.add(loc)
                    Winner.REMOTE -> toPull.add(rem.toDomain())
                }
            }
        }
        return Triple(toPull, toPush, 0)
    }

    private enum class Winner { LOCAL, REMOTE }

    private fun resolveConflict(
        localSyncVersion: Long, localUpdatedAt: Long,
        remoteSyncVersion: Long, remoteUpdatedAt: Long,
    ): Winner = when {
        localSyncVersion > remoteSyncVersion -> Winner.LOCAL
        remoteSyncVersion > localSyncVersion -> Winner.REMOTE
        localUpdatedAt > remoteUpdatedAt     -> Winner.LOCAL
        else -> Winner.REMOTE
    }

    private fun buildMergedSnippets(local: List<Snippet>, pulled: List<Snippet>): List<Snippet> {
        val map = local.associateBy { it.id }.toMutableMap()
        pulled.forEach { map[it.id] = it }
        return map.values.toList()
    }

    private fun buildMergedFolders(local: List<Folder>, pulled: List<Folder>): List<Folder> {
        val map = local.associateBy { it.id }.toMutableMap()
        pulled.forEach { map[it.id] = it }
        return map.values.toList()
    }

    private fun buildSnapshot(snippets: List<Snippet>, folders: List<Folder>): BackupData =
        BackupData(
            exportedAt = Instant.now().toString(),
            snippets = snippets.map { it.toBackup() },
            folders  = folders.map  { it.toBackup() },
        )

    private fun Snippet.toBackup() = SnippetBackup(
        id = id, shortcut = shortcut, body = body, type = type.name,
        triggerMode = triggerMode.name, folderId = folderId, usageCount = usageCount,
        createdAt = createdAt, updatedAt = updatedAt, isPinned = isPinned,
        isEnabled = isEnabled, caseSensitive = caseSensitive, description = description,
        syncVersion = syncVersion,
    )

    private fun Folder.toBackup() = FolderBackup(
        id = id, name = name, color = color, sortOrder = sortOrder,
        createdAt = createdAt, updatedAt = updatedAt, syncVersion = syncVersion,
    )

    private fun SnippetBackup.toDomain() = Snippet(
        id = id, shortcut = shortcut, body = body,
        type = runCatching { SnippetType.valueOf(type) }.getOrDefault(SnippetType.PLAIN),
        triggerMode = runCatching { TriggerMode.valueOf(triggerMode) }.getOrDefault(TriggerMode.ON_DELIMITER),
        folderId = folderId, usageCount = usageCount, createdAt = createdAt, updatedAt = updatedAt,
        isPinned = isPinned, isEnabled = isEnabled, caseSensitive = caseSensitive,
        description = description, syncVersion = syncVersion,
    )

    private fun FolderBackup.toDomain() = Folder(
        id = id, name = name, color = color, sortOrder = sortOrder,
        createdAt = createdAt, updatedAt = updatedAt, syncVersion = syncVersion,
    )

    private fun encodeSnapshot(data: BackupData): String =
        json.encodeToString(BackupData.serializer(), data)
}
