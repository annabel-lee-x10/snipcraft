package dev.a10101100.snipcraft.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.a10101100.snipcraft.core.designsystem.SnipTheme
import dev.a10101100.snipcraft.core.designsystem.components.EmptyState
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode

@Composable
fun LibraryScreen(
    onSnippetClick: (String) -> Unit,
    onCreateSnippet: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryContent(
        uiState = uiState,
        onSnippetClick = onSnippetClick,
        onCreateSnippet = onCreateSnippet,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSortOrderChange = viewModel::onSortOrderChange,
        onDeleteSnippet = viewModel::onDeleteSnippet,
        onTogglePin = viewModel::onTogglePin,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryContent(
    uiState: LibraryUiState,
    onSnippetClick: (String) -> Unit,
    onCreateSnippet: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onDeleteSnippet: (Snippet) -> Unit,
    onTogglePin: (Snippet) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by remember { mutableStateOf(false) }
    val snackbarState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search snippets…") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_field"),
                        )
                    } else {
                        Text("Snipcraft")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        searchActive = !searchActive
                        if (!searchActive) onSearchQueryChange("")
                    }) {
                        Icon(
                            if (searchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (searchActive) "Close search" else "Search",
                        )
                    }
                    SortMenu(
                        currentOrder = uiState.sortOrder,
                        onSortOrderChange = onSortOrderChange,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateSnippet) {
                Icon(Icons.Default.Add, contentDescription = "Create snippet")
            }
        },
        snackbarHost = { SnackbarHost(snackbarState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.snippets.isEmpty() -> EmptyState(
                icon = Icons.Default.TextSnippet,
                title = "No snippets yet",
                subtitle = "Create your first snippet to start expanding text anywhere",
                actionLabel = "Create snippet",
                onAction = onCreateSnippet,
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("snippet_list"),
            ) {
                items(uiState.snippets, key = { it.id }) { snippet ->
                    SnippetListItem(
                        snippet = snippet,
                        onClick = { onSnippetClick(snippet.id) },
                        onTogglePin = { onTogglePin(snippet) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    currentOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Sort")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(order.label) },
                onClick = { onSortOrderChange(order); expanded = false },
                leadingIcon = if (order == currentOrder) ({
                    Text("✓", style = MaterialTheme.typography.labelMedium)
                }) else null,
            )
        }
    }
}

@Composable
private fun SnippetListItem(
    snippet: Snippet,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snippet.shortcut,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = snippet.body.take(80),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    if (snippet.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (snippet.isPinned) "Unpin" else "Pin",
                    tint = if (snippet.isPinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val SortOrder.label: String
    get() = when (this) {
        SortOrder.FREQUENCY -> "By frequency"
        SortOrder.RECENT -> "By recent"
        SortOrder.ALPHA -> "Alphabetical"
    }

@Preview(showBackground = true)
@Composable
private fun LibraryEmptyPreview() {
    SnipTheme {
        LibraryContent(
            uiState = LibraryUiState(isLoading = false),
            onSnippetClick = {}, onCreateSnippet = {}, onSearchQueryChange = {},
            onSortOrderChange = {}, onDeleteSnippet = {}, onTogglePin = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryPopulatedPreview() {
    SnipTheme {
        LibraryContent(
            uiState = LibraryUiState(
                isLoading = false,
                allSnippets = listOf(
                    Snippet("1", "/sig", "John Smith", SnippetType.PLAIN, TriggerMode.ON_DELIMITER, usageCount = 5),
                    Snippet("2", "/today", "{{date}}", SnippetType.PLAIN, TriggerMode.ON_DELIMITER, isPinned = true),
                ),
            ),
            onSnippetClick = {}, onCreateSnippet = {}, onSearchQueryChange = {},
            onSortOrderChange = {}, onDeleteSnippet = {}, onTogglePin = {},
        )
    }
}
