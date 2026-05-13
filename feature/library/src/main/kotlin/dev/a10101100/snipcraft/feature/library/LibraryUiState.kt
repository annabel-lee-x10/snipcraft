package dev.a10101100.snipcraft.feature.library

import dev.a10101100.snipcraft.core.domain.Snippet

enum class SortOrder { FREQUENCY, RECENT, ALPHA }

data class LibraryUiState(
    val allSnippets: List<Snippet> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.FREQUENCY,
    val isLoading: Boolean = true,
) {
    val snippets: List<Snippet>
        get() {
            val filtered = if (searchQuery.isBlank()) allSnippets
            else allSnippets.filter {
                it.shortcut.contains(searchQuery, ignoreCase = true) ||
                    it.body.contains(searchQuery, ignoreCase = true) ||
                    it.description?.contains(searchQuery, ignoreCase = true) == true
            }
            return when (sortOrder) {
                SortOrder.FREQUENCY -> filtered.sortedWith(
                    compareByDescending<Snippet> { it.isPinned }.thenByDescending { it.usageCount }
                )
                SortOrder.RECENT -> filtered.sortedWith(
                    compareByDescending<Snippet> { it.isPinned }.thenByDescending { it.lastUsedAt ?: 0L }
                )
                SortOrder.ALPHA -> filtered.sortedWith(
                    compareByDescending<Snippet> { it.isPinned }.thenBy { it.shortcut }
                )
            }
        }
}
