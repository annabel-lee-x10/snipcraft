package dev.a10101100.snipcraft.core.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FolderTest {

    @Test
    fun `folder has id and name`() {
        val folder = Folder(id = "f1", name = "Work")
        assertEquals("f1", folder.id)
        assertEquals("Work", folder.name)
    }

    @Test
    fun `folder color defaults to null`() {
        val folder = Folder(id = "f1", name = "Work")
        assertNull(folder.color)
    }

    @Test
    fun `folder can have a color`() {
        val folder = Folder(id = "f1", name = "Work", color = 0xFF6200EE.toInt())
        assertEquals(0xFF6200EE.toInt(), folder.color)
    }
}
