package dev.a10101100.snipcraft.feature.editor

data class EditorUiState(
    val shortcut: String = "",
    val body: String = "",
    val description: String = "",
    val isEnabled: Boolean = true,
    val isPinned: Boolean = false,
    val caseSensitive: Boolean = false,
    val isNewSnippet: Boolean = true,
    val isSaving: Boolean = false,
    val shortcutError: String? = null,
    val bodyError: String? = null,
) {
    val canSave: Boolean get() = shortcut.isNotBlank() && body.isNotBlank() && !isSaving
}

sealed interface EditorEvent {
    data object NavigateBack : EditorEvent
    data class ShowError(val message: String) : EditorEvent
}
