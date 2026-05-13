package dev.a10101100.snipcraft.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.a10101100.snipcraft.core.designsystem.SnipTheme

@Composable
fun EditorScreen(
    onBack: () -> Unit,
    snippetId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.NavigateBack -> onBack()
                is EditorEvent.ShowError -> { /* TODO: show snackbar */ }
            }
        }
    }

    EditorContent(
        uiState = uiState,
        onBack = onBack,
        onShortcutChange = viewModel::onShortcutChange,
        onBodyChange = viewModel::onBodyChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onEnabledChange = viewModel::onEnabledChange,
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorContent(
    uiState: EditorUiState,
    onBack: () -> Unit,
    onShortcutChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewSnippet) "New Snippet" else "Edit Snippet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = uiState.canSave,
                        modifier = Modifier.testTag("save_button"),
                    ) { Text(if (uiState.isSaving) "Saving…" else "Save") }
                    if (!uiState.isNewSnippet) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete snippet", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.shortcut,
                onValueChange = onShortcutChange,
                label = { Text("Shortcut") },
                placeholder = { Text("/sig") },
                isError = uiState.shortcutError != null,
                supportingText = uiState.shortcutError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shortcut_field"),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.body,
                onValueChange = onBodyChange,
                label = { Text("Expansion") },
                placeholder = { Text("Type what you want to expand to…") },
                isError = uiState.bodyError != null,
                supportingText = uiState.bodyError?.let { { Text(it) } },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("body_field"),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Text("Enabled", style = MaterialTheme.typography.labelLarge)
            Switch(
                checked = uiState.isEnabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "Saving…" else "Save")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorNewPreview() {
    SnipTheme {
        EditorContent(
            uiState = EditorUiState(shortcut = "/sig", body = "John Smith"),
            onBack = {}, onShortcutChange = {}, onBodyChange = {}, onDescriptionChange = {},
            onEnabledChange = {}, onSave = {}, onDelete = {},
        )
    }
}
