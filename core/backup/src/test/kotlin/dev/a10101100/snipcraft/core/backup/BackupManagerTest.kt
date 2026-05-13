package dev.a10101100.snipcraft.core.backup

import dev.a10101100.snipcraft.core.data.FolderRepository
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Folder
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
import org.junit.jupiter.api.Test

class BackupManagerTest {

    private val snippetRepo = mockk<SnippetRepository>(relaxed = true)
    private val folderRepo = mockk<FolderRepository>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val manager = BackupManager(snippetRepo, folderRepo, json)

    @Test
    fun `export produces JSON with version 1`() = runTest {
        every { snippetRepo.observeAll() } returns flowOf(emptyList())
        every { folderRepo.observeAll() } returns flowOf(emptyList())

        val result = manager.export()
        val parsed = json.decodeFromString<BackupData>(result)

        assertEquals(1, parsed.version)
        assertTrue(parsed.exportedAt.isNotBlank())
    }

    @Test
    fun `export serializes snippet fields correctly`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";today", body = "{{date:yyyy-MM-dd}}")
        every { snippetRepo.observeAll() } returns flowOf(listOf(snippet))
        every { folderRepo.observeAll() } returns flowOf(emptyList())

        val result = manager.export()
        val parsed = json.decodeFromString<BackupData>(result)

        assertEquals(1, parsed.snippets.size)
        assertEquals(";today", parsed.snippets[0].shortcut)
        assertEquals("{{date:yyyy-MM-dd}}", parsed.snippets[0].body)
    }

    @Test
    fun `export serializes folder fields correctly`() = runTest {
        val folder = Folder(id = "f1", name = "Work")
        every { snippetRepo.observeAll() } returns flowOf(emptyList())
        every { folderRepo.observeAll() } returns flowOf(listOf(folder))

        val result = manager.export()
        val parsed = json.decodeFromString<BackupData>(result)

        assertEquals(1, parsed.folders.size)
        assertEquals("Work", parsed.folders[0].name)
    }

    @Test
    fun `import SKIP_EXISTING skips snippet with same shortcut`() = runTest {
        val existing = Snippet(id = "s1", shortcut = ";today", body = "old body")
        every { snippetRepo.observeAll() } returns flowOf(listOf(existing))
        every { folderRepo.observeAll() } returns flowOf(emptyList())
        coEvery { snippetRepo.getByShortcut(";today") } returns existing

        val exportJson = manager.export()
        val result = manager.import(exportJson, ConflictStrategy.SKIP_EXISTING)

        assertEquals(0, result.snippetsImported)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `import OVERWRITE upserts snippet regardless of existing`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";today", body = "{{date:yyyy-MM-dd}}")
        every { snippetRepo.observeAll() } returns flowOf(listOf(snippet))
        every { folderRepo.observeAll() } returns flowOf(emptyList())
        coEvery { snippetRepo.getByShortcut(";today") } returns snippet

        val exportJson = manager.export()
        manager.import(exportJson, ConflictStrategy.OVERWRITE)

        coVerify { snippetRepo.upsert(any()) }
    }

    @Test
    fun `round-trip export then import restores snippet`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";sig", body = "Best regards")
        every { snippetRepo.observeAll() } returns flowOf(listOf(snippet))
        every { folderRepo.observeAll() } returns flowOf(emptyList())
        coEvery { snippetRepo.getByShortcut(";sig") } returns null

        val exportJson = manager.export()
        val result = manager.import(exportJson, ConflictStrategy.SKIP_EXISTING)

        assertEquals(1, result.snippetsImported)
        assertEquals(0, result.skipped)
        coVerify { snippetRepo.upsert(match { it.shortcut == ";sig" }) }
    }
}
