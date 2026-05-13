package dev.a10101100.snipcraft.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Snippet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val snippetRepository: SnippetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            snippetRepository.observeEnabled().collect { snippets ->
                _uiState.update { it.copy(allSnippets = snippets, isLoading = false) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSortOrderChange(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun onDeleteSnippet(snippet: Snippet) {
        viewModelScope.launch { snippetRepository.delete(snippet) }
    }

    fun onTogglePin(snippet: Snippet) {
        viewModelScope.launch {
            snippetRepository.upsert(snippet.copy(isPinned = !snippet.isPinned))
        }
    }
}
