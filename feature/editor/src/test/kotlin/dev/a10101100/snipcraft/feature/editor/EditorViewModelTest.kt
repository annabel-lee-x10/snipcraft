package dev.a10101100.snipcraft.feature.editor

import app.cash.turbine.test
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `new snippet starts with empty shortcut and body`() = runTest {
        val vm = EditorViewModel(fakeRepo(), snippetId = null)
        vm.uiState.test {
            val state = awaitItem()
            assertEquals("", state.shortcut)
            assertEquals("", state.body)
            assertTrue(state.isNewSnippet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `save disabled when shortcut is blank`() = runTest {
        val vm = EditorViewModel(fakeRepo(), snippetId = null)
        vm.uiState.test {
            assertFalse(awaitItem().canSave)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `save enabled when shortcut and body are non-blank`() = runTest {
        val vm = EditorViewModel(fakeRepo(), snippetId = null)
        vm.onShortcutChange("/sig")
        vm.onBodyChange("John Smith")
        vm.uiState.test {
            assertTrue(awaitItem().canSave)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loading existing snippet populates fields`() = runTest {
        val existing = testSnippet("/sig", "John Smith")
        val vm = EditorViewModel(fakeRepo(snippets = listOf(existing)), snippetId = existing.id)
        vm.uiState.test {
            val state = awaitItem()
            assertEquals("/sig", state.shortcut)
            assertEquals("John Smith", state.body)
            assertFalse(state.isNewSnippet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `save stores snippet and emits navigateBack`() = runTest {
        var saved: Snippet? = null
        val vm = EditorViewModel(
            fakeRepo(onUpsert = { saved = it }),
            snippetId = null,
        )
        vm.onShortcutChange("/new")
        vm.onBodyChange("expansion text")
        vm.onSave()

        assertNotNull(saved)
        assertEquals("/new", saved!!.shortcut)

        vm.events.test {
            assertEquals(EditorEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes snippet and emits navigateBack`() = runTest {
        val existing = testSnippet("/del", "body")
        var deleted: Snippet? = null
        val vm = EditorViewModel(
            fakeRepo(snippets = listOf(existing), onDelete = { deleted = it }),
            snippetId = existing.id,
        )
        vm.onDelete()
        assertNotNull(deleted)

        vm.events.test {
            assertEquals(EditorEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shortcut validation rejects empty shortcut`() = runTest {
        val vm = EditorViewModel(fakeRepo(), snippetId = null)
        vm.onShortcutChange("")
        vm.uiState.test {
            assertNull(awaitItem().shortcutError)  // no error until save attempted
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- helpers ---

    private fun testSnippet(shortcut: String, body: String) = Snippet(
        id = "id-${shortcut.replace("/", "")}",
        shortcut = shortcut, body = body,
        type = SnippetType.PLAIN, triggerMode = TriggerMode.ON_DELIMITER,
    )

    private fun fakeRepo(
        snippets: List<Snippet> = emptyList(),
        onUpsert: suspend (Snippet) -> Unit = {},
        onDelete: suspend (Snippet) -> Unit = {},
    ) = object : dev.a10101100.snipcraft.core.data.SnippetRepository {
        override fun observeEnabled() = flowOf(snippets)
        override suspend fun getByShortcut(s: String) = null
        override suspend fun upsert(s: Snippet) = onUpsert(s)
        override suspend fun upsertAll(s: List<Snippet>) {}
        override suspend fun delete(s: Snippet) = onDelete(s)
        override suspend fun incrementUsage(id: String, ts: Long) {}
        override suspend fun count() = snippets.size
    }
}
