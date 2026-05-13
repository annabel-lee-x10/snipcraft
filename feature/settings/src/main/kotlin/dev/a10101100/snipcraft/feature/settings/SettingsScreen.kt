package dev.a10101100.snipcraft.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.a10101100.snipcraft.core.backup.ConflictStrategy
import dev.a10101100.snipcraft.core.backup.ImportResult
import dev.a10101100.snipcraft.core.designsystem.SnipTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddAppDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val content = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
        viewModel.onImportJsonReceived(content)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShareExport -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, event.json)
                        putExtra(Intent.EXTRA_SUBJECT, "Snipcraft snippets backup")
                    }
                    context.startActivity(Intent.createChooser(intent, "Export snippets"))
                }
                is SettingsEvent.ShowImportResult -> {
                    // Result is stored in uiState.importResult — shown via dialog below
                }
            }
        }
    }

    if (showAddAppDialog) {
        var addAppText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAppDialog = false; addAppText = "" },
            title = { Text("Add excluded app") },
            text = {
                OutlinedTextField(
                    value = addAppText,
                    onValueChange = { addAppText = it },
                    label = { Text("Package name") },
                    placeholder = { Text("com.example.app") },
                    singleLine = true,
                    modifier = Modifier.testTag("add_app_field"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (addAppText.isNotBlank()) {
                            viewModel.addToBlacklist(addAppText.trim())
                            addAppText = ""
                            showAddAppDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_app"),
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAppDialog = false; addAppText = "" }) { Text("Cancel") }
            },
        )
    }

    if (uiState.showImportConflictDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Import conflict") },
            text = { Text("How should existing snippets be handled?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onImportConflictResolved(ConflictStrategy.OVERWRITE) }) {
                    Text("Overwrite existing")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onImportConflictResolved(ConflictStrategy.SKIP_EXISTING) }) {
                    Text("Skip existing")
                }
            },
        )
    }

    uiState.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportResult,
            title = { Text("Import complete") },
            text = {
                Text("Imported ${result.snippetsImported} snippets, ${result.foldersImported} folders. Skipped ${result.skipped}.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissImportResult) { Text("OK") }
            },
        )
    }

    SettingsContent(
        uiState = uiState,
        onBack = onBack,
        onOpenAccessibilitySettings = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        },
        onRefreshServiceStatus = viewModel::refreshServiceStatus,
        onThemeModeChange = viewModel::onThemeModeChange,
        onAddToBlacklist = { showAddAppDialog = true },
        onRemoveFromBlacklist = viewModel::removeFromBlacklist,
        onExport = viewModel::onExport,
        onImport = { importLauncher.launch("*/*") },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRefreshServiceStatus: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAddToBlacklist: () -> Unit,
    onRemoveFromBlacklist: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Accessibility Service")

            ListItem(
                headlineContent = { Text("Expansion service") },
                supportingContent = {
                    Text(
                        if (uiState.isAccessibilityServiceEnabled) "Active — text expansion is running"
                        else "Inactive — tap to enable in Settings",
                    )
                },
                leadingContent = {
                    Icon(
                        if (uiState.isAccessibilityServiceEnabled) Icons.Default.CheckCircle
                        else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (uiState.isAccessibilityServiceEnabled)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                },
            )
            Column(Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open Accessibility Settings")
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onRefreshServiceStatus,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Re-check status")
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SectionHeader("Appearance")
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(mode.label) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SectionHeader("Excluded Apps")
            if (uiState.blacklistedPackages.isEmpty()) {
                ListItem(headlineContent = {
                    Text(
                        "No apps excluded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                })
            } else {
                uiState.blacklistedPackages.forEach { pkg ->
                    ListItem(
                        headlineContent = { Text(pkg, style = MaterialTheme.typography.bodyMedium) },
                        trailingContent = {
                            IconButton(onClick = { onRemoveFromBlacklist(pkg) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove $pkg")
                            }
                        },
                    )
                }
            }
            OutlinedButton(
                onClick = onAddToBlacklist,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) { Text("Add excluded app") }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SectionHeader("Backup & Restore")
            ListItem(
                headlineContent = { Text("Export snippets") },
                supportingContent = { Text("Share all snippets as JSON") },
                trailingContent = {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.Share, contentDescription = null)
                    }
                },
                modifier = Modifier.clickable(enabled = !uiState.isExporting) { onExport() },
            )
            ListItem(
                headlineContent = { Text("Import snippets") },
                supportingContent = { Text("Restore from JSON file") },
                trailingContent = {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.FileOpen, contentDescription = null)
                    }
                },
                modifier = Modifier.clickable(enabled = !uiState.isImporting) { onImport() },
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SectionHeader("About")
            ListItem(
                headlineContent = { Text("Snipcraft") },
                supportingContent = { Text("v0.1.0-dev — personal use build") },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    SnipTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(
                isAccessibilityServiceEnabled = true,
                themeMode = ThemeMode.DARK,
                blacklistedPackages = listOf("com.example.blocked"),
            ),
            onBack = {},
            onOpenAccessibilitySettings = {},
            onRefreshServiceStatus = {},
            onThemeModeChange = {},
            onAddToBlacklist = {},
            onRemoveFromBlacklist = {},
            onExport = {},
            onImport = {},
        )
    }
}
