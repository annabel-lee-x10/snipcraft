package dev.a10101100.snipcraft.feature.library

import app.cash.turbine.test
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode
import io.mockk.coVerify
import io.mockk.mockk
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state shows empty list when repo is empty`() = runTest {
        val vm = LibraryViewModel(fakeRepo(emptyList()))
        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.snippets.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state shows snippets from repo`() = runTest {
        val snippets = listOf(
            testSnippet("1", "/sig", "John"),
            testSnippet("2", "/today", "{{date}}"),
        )
        val vm = LibraryViewModel(fakeRepo(snippets))
        vm.uiState.test {
            assertEquals(2, awaitItem().snippets.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search query filters snippet list`() = runTest {
        val snippets = listOf(
            testSnippet("1", "/sig", "John"),
            testSnippet("2", "/today", "date"),
        )
        val vm = LibraryViewModel(fakeRepo(snippets))
        vm.onSearchQueryChange("sig")
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.snippets.size)
            assertEquals("/sig", state.snippets.first().shortcut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing search shows all snippets`() = runTest {
        val snippets = listOf(
            testSnippet("1", "/sig", "John"),
            testSnippet("2", "/today", "date"),
        )
        val vm = LibraryViewModel(fakeRepo(snippets))
        vm.onSearchQueryChange("sig")
        vm.onSearchQueryChange("")
        vm.uiState.test {
            assertEquals(2, awaitItem().snippets.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sort by alpha orders snippets alphabetically`() = runTest {
        val snippets = listOf(
            testSnippet("1", "/z", "z"),
            testSnippet("2", "/a", "a"),
        )
        val vm = LibraryViewModel(fakeRepo(snippets))
        vm.onSortOrderChange(SortOrder.ALPHA)
        vm.uiState.test {
            val state = awaitItem()
            assertEquals("/a", state.snippets[0].shortcut)
            assertEquals("/z", state.snippets[1].shortcut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete calls repository`() = runTest {
        val mockRepo = mockk<dev.a10101100.snipcraft.core.data.SnippetRepository>(relaxed = true)
        io.mockk.every { mockRepo.observeEnabled() } returns flowOf(emptyList())
        val snippet = testSnippet("1", "/sig", "John")
        val vm = LibraryViewModel(mockRepo)
        vm.onDeleteSnippet(snippet)
        coVerify { mockRepo.delete(snippet) }
    }

    // --- helpers ---

    private fun fakeRepo(snippets: List<Snippet>) =
        object : dev.a10101100.snipcraft.core.data.SnippetRepository {
            override fun observeEnabled() = flowOf(snippets)
            override fun observeAll() = flowOf(snippets)
            override suspend fun getByShortcut(s: String) = null
            override suspend fun upsert(s: Snippet) {}
            override suspend fun upsertAll(s: List<Snippet>) {}
            override suspend fun delete(s: Snippet) {}
            override suspend fun incrementUsage(id: String, ts: Long) {}
            override suspend fun count() = snippets.size
        }

    private fun testSnippet(id: String, shortcut: String, body: String) = Snippet(
        id = id, shortcut = shortcut, body = body,
        type = SnippetType.PLAIN, triggerMode = TriggerMode.ON_DELIMITER,
    )
}
