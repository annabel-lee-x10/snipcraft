package dev.a10101100.snipcraft.core.engine

class TrieMatcher {

    private val root = TrieNode()

    fun insert(shortcut: String) {
        var node = root
        for (ch in shortcut) {
            node = node.children.getOrPut(ch) { TrieNode() }
        }
        node.shortcut = shortcut
    }

    fun remove(shortcut: String) {
        removeFromNode(root, shortcut, 0)
    }

    fun rebuild(shortcuts: List<String>) {
        root.children.clear()
        root.shortcut = null
        shortcuts.forEach { insert(it) }
    }

    /**
     * Returns the matched shortcut if the suffix of [text] ends with any known shortcut,
     * or null if no match is found. O(L) where L is max shortcut length.
     */
    fun matchSuffix(text: String): String? {
        if (text.isEmpty()) return null
        // Walk the trie character by character starting from the end of text,
        // looking for the longest match.
        for (startIndex in text.indices.reversed()) {
            var node = root
            var matchedShortcut: String? = null
            for (i in startIndex until text.length) {
                val ch = text[i]
                node = node.children[ch] ?: break
                if (node.shortcut != null) {
                    matchedShortcut = node.shortcut
                }
            }
            if (matchedShortcut != null) {
                // Verify the match ends exactly at end of text
                val matchEnd = startIndex + matchedShortcut.length
                if (matchEnd == text.length) return matchedShortcut
            }
        }
        return null
    }

    private fun removeFromNode(node: TrieNode, shortcut: String, depth: Int): Boolean {
        if (depth == shortcut.length) {
            node.shortcut = null
            return node.children.isEmpty()
        }
        val ch = shortcut[depth]
        val child = node.children[ch] ?: return false
        val shouldDelete = removeFromNode(child, shortcut, depth + 1)
        if (shouldDelete) node.children.remove(ch)
        return node.children.isEmpty() && node.shortcut == null
    }
}

private class TrieNode {
    val children: MutableMap<Char, TrieNode> = mutableMapOf()
    var shortcut: String? = null
}
