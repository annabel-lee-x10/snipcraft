package dev.a10101100.snipcraft.feature.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.a10101100.snipcraft.core.domain.ExpansionEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DiagnosticsEvent.ShareDiagnostics -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, event.json)
                        putExtra(Intent.EXTRA_SUBJECT, "Snipcraft diagnostics bundle")
                    }
                    context.startActivity(Intent.createChooser(intent, "Export diagnostics"))
                }
            }
        }
    }

    DiagnosticsContent(
        uiState = uiState,
        onBack = onBack,
        onTriggerWatchdog = viewModel::triggerWatchdog,
        onExportDiagnostics = viewModel::exportDiagnostics,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsContent(
    uiState: DiagnosticsUiState,
    onBack: () -> Unit,
    onTriggerWatchdog: () -> Unit,
    onExportDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Compatibility & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
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
            DiagSectionHeader("Service Status")
            StatusPill(label = "Accessibility Service", ok = uiState.accessibilityEnabled)
            StatusPill(label = "Foreground Service", ok = uiState.foregroundServiceRunning)
            StatusPill(label = "Health Watchdog (WorkManager)", ok = uiState.watchdogScheduled)
            StatusPill(label = "Battery Exemption", ok = uiState.batteryExemptionGranted)
            StatusPill(label = "Notification Permission", ok = uiState.notificationPermissionGranted)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            DiagSectionHeader("Active Keyboard")
            ListItem(
                headlineContent = {
                    Text(uiState.currentImePackage.ifBlank { "Unknown" })
                },
                supportingContent = { Text("Default input method package") },
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            DiagSectionHeader("Test Field")
            var testText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                label = { Text("Type a shortcut + space") },
                placeholder = { Text(";today ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                supportingText = { Text("The accessibility service will expand it if active") },
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            DiagSectionHeader("Recent Expansions (last ${uiState.recentExpansions.size})")
            if (uiState.recentExpansions.isEmpty()) {
                ListItem(
                    headlineContent = {
                        Text(
                            "No expansions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            } else {
                uiState.recentExpansions.forEach { event ->
                    ExpansionEventRow(event)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            DiagSectionHeader("Actions")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Button(
                    onClick = onTriggerWatchdog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Trigger Watchdog Now")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onExportDiagnostics,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExportingDiagnostics,
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Export Diagnostics Bundle")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiagSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun StatusPill(label: String, ok: Boolean) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Icon(
                imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = if (ok) "$label is active" else "$label is inactive",
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
        },
        supportingContent = {
            Text(
                if (ok) "Active" else "Inactive",
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        },
    )
}

@Composable
private fun ExpansionEventRow(event: ExpansionEvent) {
    val dateStr = remember(event.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
    }
    ListItem(
        headlineContent = { Text(event.shortcut) },
        supportingContent = { Text("${event.packageName} · $dateStr") },
        trailingContent = {
            Icon(
                imageVector = if (event.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = if (event.success) "Expansion succeeded"
                else "Expansion failed: ${event.errorReason}",
                tint = if (event.success) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}
