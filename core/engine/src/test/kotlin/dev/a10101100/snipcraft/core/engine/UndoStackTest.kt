package dev.a10101100.snipcraft.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UndoStackTest {

    private val stack = UndoStack(maxDepth = 3)

    @Test
    fun `push and pop undo entry`() {
        val entry = UndoEntry(
            snippetId = "s1",
            fieldId = "field1",
            originalText = "Hello /sig",
            originalCursorPos = 10,
            expansionTimestampMs = 1000L,
        )
        stack.push(entry)
        assertEquals(entry, stack.peek())
    }

    @Test
    fun `pop removes entry`() {
        val entry = UndoEntry("s1", "f1", "/sig", 4, 1000L)
        stack.push(entry)
        stack.pop()
        assertNull(stack.peek())
    }

    @Test
    fun `stack respects max depth`() {
        repeat(5) { i ->
            stack.push(UndoEntry("s$i", "f1", "text$i", i, i.toLong()))
        }
        // Only last 3 entries kept
        assertEquals(3, stack.size())
    }

    @Test
    fun `clear empties stack`() {
        stack.push(UndoEntry("s1", "f1", "t", 0, 0L))
        stack.clear()
        assertNull(stack.peek())
        assertEquals(0, stack.size())
    }

    @Test
    fun `peek on empty stack returns null`() {
        assertNull(stack.peek())
    }
}
