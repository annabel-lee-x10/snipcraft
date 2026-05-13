package dev.a10101100.snipcraft.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import dev.a10101100.snipcraft.core.designsystem.SnipTheme
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty state shows create snippet text`() {
        composeRule.setContent {
            SnipTheme {
                LibraryContent(
                    uiState = LibraryUiState(isLoading = false),
                    onSnippetClick = {}, onCreateSnippet = {},
                    onSearchQueryChange = {}, onSortOrderChange = {},
                    onDeleteSnippet = {}, onTogglePin = {},
                )
            }
        }
        composeRule.onNodeWithText("No snippets yet").assertIsDisplayed()
        composeRule.onNodeWithText("Create snippet").assertIsDisplayed()
    }

    @Test
    fun `populated state shows snippet list`() {
        val snippets = listOf(
            Snippet("1", "/sig", "John Smith", SnippetType.PLAIN, TriggerMode.ON_DELIMITER),
            Snippet("2", "/today", "{{date}}", SnippetType.PLAIN, TriggerMode.ON_DELIMITER),
        )
        composeRule.setContent {
            SnipTheme {
                LibraryContent(
                    uiState = LibraryUiState(isLoading = false, allSnippets = snippets),
                    onSnippetClick = {}, onCreateSnippet = {},
                    onSearchQueryChange = {}, onSortOrderChange = {},
                    onDeleteSnippet = {}, onTogglePin = {},
                )
            }
        }
        composeRule.onNodeWithTag("snippet_list").assertIsDisplayed()
        composeRule.onNodeWithText("/sig").assertIsDisplayed()
        composeRule.onNodeWithText("/today").assertIsDisplayed()
    }
}
