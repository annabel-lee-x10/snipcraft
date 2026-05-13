package dev.a10101100.snipcraft.core.sync

import dev.a10101100.snipcraft.core.backup.BackupData
import dev.a10101100.snipcraft.core.backup.SnippetBackup
import dev.a10101100.snipcraft.core.data.FolderRepository
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Snippet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SyncEngineTest {

    private val webDavClient = mockk<WebDavClient>()
    private val snippetRepo = mockk<SnippetRepository>(relaxed = true)
    private val folderRepo = mockk<FolderRepository>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val engine = SyncEngine(webDavClient, snippetRepo, folderRepo, json)

    private val config = SyncConfig(
        serverUrl = "https://example.com",
        remotePath = "/snipcraft/snippets.json",
        username = "user",
        password = "pass",
    )
    private val creds = WebDavClient.Credentials("user", "pass")

    @BeforeEach
    fun setUp() {
        every { snippetRepo.observeAll() } returns flowOf(emptyList())
        every { folderRepo.observeAll() } returns flowOf(emptyList())
    }

    @Test
    fun `no remote file — pushes empty snapshot and returns success`() = runTest {
        coEvery { webDavClient.propFind(any(), any(), any()) } returns WebDavClient.PropFindResult.NotFound
        coEvery { webDavClient.mkCol(any(), any()) } returns true
        coEvery { webDavClient.put(any(), any(), any()) } returns true

        val result = engine.sync(config)

        assertTrue(result.isSuccess)
        coVerify { webDavClient.put(any(), any(), any()) }
    }

    @Test
    fun `local has snippets and no remote — counts as pushed`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";today", body = "{{date}}", syncVersion = 1L)
        every { snippetRepo.observeAll() } returns flowOf(listOf(snippet))
        coEvery { webDavClient.propFind(any(), any(), any()) } returns WebDavClient.PropFindResult.NotFound
        coEvery { webDavClient.mkCol(any(), any()) } returns true
        coEvery { webDavClient.put(any(), any(), any()) } returns true

        val result = engine.sync(config)

        assertTrue(result.isSuccess)
        assertEquals(1, result.pushed)
        coVerify { webDavClient.put(any(), any(), any()) }
    }

    @Test
    fun `remote has snippets and local is empty — pulls remote snippets`() = runTest {
        val remoteSnippet = SnippetBackup(
            id = "r1", shortcut = ";hello", body = "Hello!",
            type = "PLAIN", triggerMode = "ON_DELIMITER", syncVersion = 1L,
        )
        val remoteData = BackupData(exportedAt = "", snippets = listOf(remoteSnippet), folders = emptyList())
        coEvery { webDavClient.propFind(any(), any(), any()) } returns WebDavClient.PropFindResult.Found
        coEvery { webDavClient.get(any(), any()) } returns json.encodeToString(remoteData)
        coEvery { webDavClient.put(any(), any(), any()) } returns true

        val result = engine.sync(config)

        assertEquals(1, result.pulled)
        coVerify { snippetRepo.upsert(match { it.shortcut == ";hello" }) }
    }

    @Test
    fun `conflict — remote higher syncVersion wins`() = runTest {
        val local = Snippet(id = "s1", shortcut = ";a", body = "local", syncVersion = 1L, updatedAt = 100L)
        val remote = SnippetBackup(
            id = "s1", shortcut = ";a", body = "remote",
            type = "PLAIN", triggerMode = "ON_DELIMITER", syncVersion = 2L, updatedAt = 50L,
        )
        every { snippetRepo.observeAll() } returns flowOf(listOf(local))
        val remoteData = BackupData(exportedAt = "", snippets = listOf(remote), folders = emptyList())
        coEvery { webDavClient.propFind(any(), any(), any()) } returns WebDavClient.PropFindResult.Found
        coEvery { webDavClient.get(any(), any()) } returns json.encodeToString(remoteData)
        coEvery { webDavClient.put(any(), any(), any()) } returns true

        val result = engine.sync(config)

        assertEquals(1, result.pulled)
        coVerify { snippetRepo.upsert(match { it.body == "remote" }) }
    }

    @Test
    fun `conflict — local higher syncVersion wins, no pull`() = runTest {
        val local = Snippet(id = "s1", shortcut = ";a", body = "local", syncVersion = 5L, updatedAt = 100L)
        val remote = SnippetBackup(
            id = "s1", shortcut = ";a", body = "remote",
            type = "PLAIN", triggerMode = "ON_DELIMITER", syncVersion = 2L, updatedAt = 200L,
        )
        every { snippetRepo.observeAll() } returns flowOf(listOf(local))
        val remoteData = BackupData(exportedAt = "", snippets = listOf(remote), folders = emptyList())
        coEvery { webDavClient.propFind(any(), any(), any()) } returns WebDavClient.PropFindResult.Found
        coEvery { webDavClient.get(any(), any()) } returns json.encodeToString(remoteData)
        coEvery { webDavClient.put(any(), any(), any()) } returns true

        val result = engine.sync(config)

        assertEquals(1, result.pushed)
        coVerify(exactly = 0) { snippetRepo.upsert(any()) }
    }

    @Test
    fun `equal syncVersion and updatedAt — remote wins (deterministic)`() = runTest {
        val local = Snippet(id = "s1", shortcut = ";a", body = "local", syncVersion = 1L, updatedAt = 100L)
        val remote = SnippetBackup(
            id = "s1", shortcut = ";a", body = "remote",
            type = "PLAIN", triggerMode = "ON_DELIMITER", syncVersion = 1L, updatedAt = 100L,
        )
        every { snippetRepo.observeAll() } returns flowOf(listOf(local))
        val remoteData = BackupData(exportedAt = "", snippets = listOf(remote), folders = emptyList())
        coEvery { webDavClient.propFind(any(), any(), any()) } returns WebDavClient.PropFindResult.Found
        coEvery { webDavClient.get(any(), any()) } returns json.encodeToString(remoteData)
        coEvery { webDavClient.put(any(), any(), any()) } returns true

        engine.sync(config)

        coVerify { snippetRepo.upsert(match { it.body == "remote" }) }
    }

    @Test
    fun `propFind error — returns error result without touching DB`() = runTest {
        coEvery { webDavClient.propFind(any(), any(), any()) } returns
            WebDavClient.PropFindResult.Error("connection refused")

        val result = engine.sync(config)

        assertTrue(!result.isSuccess)
        coVerify(exactly = 0) { snippetRepo.upsert(any()) }
    }
}
