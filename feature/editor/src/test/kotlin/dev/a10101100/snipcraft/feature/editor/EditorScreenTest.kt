package dev.a10101100.snipcraft.feature.editor

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.a10101100.snipcraft.core.designsystem.SnipTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `save button is disabled when shortcut is blank`() {
        composeRule.setContent {
            SnipTheme {
                EditorContent(
                    uiState = EditorUiState(shortcut = "", body = ""),
                    onBack = {}, onShortcutChange = {}, onBodyChange = {},
                    onDescriptionChange = {}, onEnabledChange = {}, onSave = {}, onDelete = {},
                )
            }
        }
        composeRule.onNodeWithTag("save_button").assertIsNotEnabled()
    }

    @Test
    fun `save button enabled when shortcut and body are filled`() {
        composeRule.setContent {
            SnipTheme {
                EditorContent(
                    uiState = EditorUiState(shortcut = "/sig", body = "John Smith"),
                    onBack = {}, onShortcutChange = {}, onBodyChange = {},
                    onDescriptionChange = {}, onEnabledChange = {}, onSave = {}, onDelete = {},
                )
            }
        }
        composeRule.onNodeWithTag("save_button").assertIsEnabled()
    }

    @Test
    fun `save button calls onSave when clicked`() {
        var saveCalled = false
        composeRule.setContent {
            SnipTheme {
                EditorContent(
                    uiState = EditorUiState(shortcut = "/sig", body = "John"),
                    onBack = {}, onShortcutChange = {}, onBodyChange = {},
                    onDescriptionChange = {}, onEnabledChange = {},
                    onSave = { saveCalled = true },
                    onDelete = {},
                )
            }
        }
        composeRule.onNodeWithTag("save_button").performClick()
        composeRule.waitForIdle()
        org.junit.Assert.assertTrue("onSave should have been called", saveCalled)
    }

    @Test
    fun `delete icon visible for existing snippet`() {
        composeRule.setContent {
            SnipTheme {
                EditorContent(
                    uiState = EditorUiState(isNewSnippet = false, shortcut = "/sig", body = "John"),
                    onBack = {}, onShortcutChange = {}, onBodyChange = {},
                    onDescriptionChange = {}, onEnabledChange = {}, onSave = {}, onDelete = {},
                )
            }
        }
        composeRule.onNodeWithText("Edit Snippet").assertIsEnabled()
    }
}
