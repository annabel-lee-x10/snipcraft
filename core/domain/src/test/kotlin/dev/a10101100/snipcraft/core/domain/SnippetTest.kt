package dev.a10101100.snipcraft.core.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SnippetTest {

    @Test
    fun `snippet has shortcut and body`() {
        val snippet = Snippet(
            id = "1",
            shortcut = "/today",
            body = "2026-05-13",
        )
        assertEquals("/today", snippet.shortcut)
        assertEquals("2026-05-13", snippet.body)
    }

    @Test
    fun `snippet defaults to enabled ON_DELIMITER PLAIN`() {
        val snippet = Snippet(id = "1", shortcut = "/s", body = "hello")
        assertTrue(snippet.isEnabled)
        assertEquals(TriggerMode.ON_DELIMITER, snippet.triggerMode)
        assertEquals(SnippetType.PLAIN, snippet.type)
    }

    @Test
    fun `snippet case sensitivity defaults to false`() {
        val snippet = Snippet(id = "1", shortcut = "/s", body = "hello")
        assertFalse(snippet.caseSensitive)
    }

    @Test
    fun `snippet can be pinned`() {
        val snippet = Snippet(id = "1", shortcut = "/s", body = "hello", isPinned = true)
        assertTrue(snippet.isPinned)
    }

    @Test
    fun `snippet with folder id`() {
        val snippet = Snippet(id = "1", shortcut = "/s", body = "hello", folderId = "folder-42")
        assertEquals("folder-42", snippet.folderId)
    }
}
