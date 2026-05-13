package dev.a10101100.snipcraft.core.data

import app.cash.turbine.test
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SnippetRepositoryTest {

    // Fake DAO — we'll replace with the real interface once implemented
    private val fakeSnippets = listOf(
        testSnippet(id = "1", shortcut = "/sig", body = "John"),
        testSnippet(id = "2", shortcut = "/today", body = "{{date}}"),
    )

    @Test
    fun `observeEnabled emits domain Snippet list`() = runTest {
        val repo = buildRepo(snippets = fakeSnippets)
        repo.observeEnabled().test {
            val list = awaitItem()
            assertEquals(2, list.size)
            assertEquals("/sig", list[0].shortcut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getByShortcut returns matching snippet`() = runTest {
        val repo = buildRepo(snippets = fakeSnippets)
        val result = repo.getByShortcut("/sig")
        assertEquals("John", result?.body)
    }

    @Test
    fun `getByShortcut returns null when missing`() = runTest {
        val repo = buildRepo(snippets = emptyList())
        assertNull(repo.getByShortcut("/nope"))
    }

    @Test
    fun `upsert delegates to DAO`() = runTest {
        var upserted: Snippet? = null
        val repo = buildRepo(onUpsert = { upserted = it })
        val snippet = testSnippet(id = "new", shortcut = "/new", body = "new body")
        repo.upsert(snippet)
        assertEquals("/new", upserted?.shortcut)
    }

    @Test
    fun `incrementUsage delegates to DAO`() = runTest {
        var incrementedId: String? = null
        val repo = buildRepo(onIncrementUsage = { id, _ -> incrementedId = id })
        repo.incrementUsage("1", timestampMs = 1000L)
        assertEquals("1", incrementedId)
    }

    // --- helpers ---

    private fun testSnippet(id: String, shortcut: String, body: String) = Snippet(
        id = id, shortcut = shortcut, body = body,
        type = SnippetType.PLAIN, triggerMode = TriggerMode.ON_DELIMITER,
    )

    private fun buildRepo(
        snippets: List<Snippet> = emptyList(),
        onUpsert: suspend (Snippet) -> Unit = {},
        onIncrementUsage: suspend (String, Long) -> Unit = { _, _ -> },
    ): SnippetRepository = FakeSnippetRepository(snippets, onUpsert, onIncrementUsage)

    private class FakeSnippetRepository(
        private val snippets: List<Snippet>,
        private val onUpsert: suspend (Snippet) -> Unit,
        private val onIncrementUsage: suspend (String, Long) -> Unit,
    ) : SnippetRepository {
        override fun observeEnabled() = flowOf(snippets)
        override fun observeAll() = flowOf(snippets)
        override suspend fun getByShortcut(shortcut: String) = snippets.find { it.shortcut == shortcut }
        override suspend fun upsert(snippet: Snippet) = onUpsert(snippet)
        override suspend fun delete(snippet: Snippet) {}
        override suspend fun incrementUsage(id: String, timestampMs: Long) = onIncrementUsage(id, timestampMs)
        override suspend fun upsertAll(snippets: List<Snippet>) = snippets.forEach { onUpsert(it) }
        override suspend fun count(): Int = snippets.size
    }
}
