package dev.a10101100.snipcraft.core.engine

data class UndoEntry(
    val snippetId: String,
    val fieldId: String,
    val originalText: String,
    val originalCursorPos: Int,
    val expansionTimestampMs: Long,
)

class UndoStack(private val maxDepth: Int = 5) {

    private val entries = ArrayDeque<UndoEntry>()

    fun push(entry: UndoEntry) {
        entries.addLast(entry)
        while (entries.size > maxDepth) entries.removeFirst()
    }

    fun peek(): UndoEntry? = entries.lastOrNull()

    fun pop(): UndoEntry? = if (entries.isEmpty()) null else entries.removeLast()

    fun clear() = entries.clear()

    fun size(): Int = entries.size
}
