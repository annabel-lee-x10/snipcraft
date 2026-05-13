package dev.a10101100.snipcraft.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val snippetRepository: SnippetRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Supports both Hilt (SavedStateHandle) and direct construction (tests)
    internal constructor(
        snippetRepository: SnippetRepository,
        snippetId: String?,
    ) : this(snippetRepository, SavedStateHandle(mapOf("snippetId" to snippetId)))

    private val snippetId: String? = savedStateHandle["snippetId"]
    private var loadedSnippet: Snippet? = null

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (snippetId != null) loadSnippet(snippetId)
    }

    private fun loadSnippet(id: String) {
        viewModelScope.launch {
            val snippet = snippetRepository.observeEnabled()
                .let { flow ->
                    var result: Snippet? = null
                    flow.collect { list -> result = list.find { it.id == id }; return@collect }
                    result
                }
                ?: return@launch
            loadedSnippet = snippet
            _uiState.update {
                it.copy(
                    shortcut = snippet.shortcut,
                    body = snippet.body,
                    description = snippet.description ?: "",
                    isEnabled = snippet.isEnabled,
                    isPinned = snippet.isPinned,
                    caseSensitive = snippet.caseSensitive,
                    isNewSnippet = false,
                )
            }
        }
    }

    fun onShortcutChange(value: String) =
        _uiState.update { it.copy(shortcut = value, shortcutError = null) }

    fun onBodyChange(value: String) =
        _uiState.update { it.copy(body = value, bodyError = null) }

    fun onDescriptionChange(value: String) =
        _uiState.update { it.copy(description = value) }

    fun onEnabledChange(enabled: Boolean) =
        _uiState.update { it.copy(isEnabled = enabled) }

    fun onSave() {
        val state = _uiState.value
        if (state.shortcut.isBlank()) {
            _uiState.update { it.copy(shortcutError = "Shortcut cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            val snippet = (loadedSnippet ?: Snippet(
                id = UUID.randomUUID().toString(),
                shortcut = "",
                body = "",
                createdAt = now,
            )).copy(
                shortcut = state.shortcut.trim(),
                body = state.body,
                description = state.description.ifBlank { null },
                isEnabled = state.isEnabled,
                isPinned = state.isPinned,
                caseSensitive = state.caseSensitive,
                type = SnippetType.PLAIN,
                triggerMode = TriggerMode.ON_DELIMITER,
                updatedAt = now,
            )
            snippetRepository.upsert(snippet)
            _events.send(EditorEvent.NavigateBack)
        }
    }

    fun onDelete() {
        val snippet = loadedSnippet ?: return
        viewModelScope.launch {
            snippetRepository.delete(snippet)
            _events.send(EditorEvent.NavigateBack)
        }
    }
}
