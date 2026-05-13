package dev.a10101100.snipcraft.core.engine

private val DELIMITERS = setOf(' ', '\t', '\n', '.', ',', '!', '?', ';', ':', ')', ']', '}', '\'', '"')

/**
 * Evaluates whether the current text ends with a delimiter at the cursor position,
 * and if so, returns the shortcut text that precedes it (or null if not applicable).
 *
 * Contract: [cursorPos] is the position AFTER the most recently typed character.
 * The trigger fires when the character just before the cursor is a delimiter.
 */
class OnDelimiterTrigger {

    fun evaluate(text: String, cursorPos: Int): String? {
        if (text.isEmpty() || cursorPos == 0 || cursorPos > text.length) return null

        val delimChar = text[cursorPos - 1]
        if (delimChar !in DELIMITERS) return null

        // The shortcut candidate is the text before the delimiter
        val beforeDelim = text.substring(0, cursorPos - 1)
        if (beforeDelim.isEmpty()) return null

        // Find the start of the word (non-whitespace, non-delimiter run)
        var start = beforeDelim.length - 1
        while (start > 0 && beforeDelim[start - 1] !in DELIMITERS && beforeDelim[start - 1] != ' ') {
            start--
        }

        return beforeDelim.substring(start).ifEmpty { null }
    }
}
