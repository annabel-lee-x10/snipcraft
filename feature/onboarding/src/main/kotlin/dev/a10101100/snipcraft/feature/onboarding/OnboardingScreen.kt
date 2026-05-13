package dev.a10101100.snipcraft.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.NavigateToLibrary -> onComplete()
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.checkPermissionsOnResume()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onNotificationPermissionResult(granted) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { TOTAL_CARDS },
    )

    LaunchedEffect(uiState.currentCard) {
        if (pagerState.currentPage != uiState.currentCard) {
            pagerState.animateScrollToPage(uiState.currentCard)
        }
    }

    Scaffold(modifier = modifier) { padding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { page ->
            when (page) {
                0 -> AccessibilityCard(
                    granted = uiState.accessibilityGranted,
                    onGrant = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    },
                    onSkip = { viewModel.onSkip() },
                )
                1 -> NotificationCard(
                    granted = uiState.notificationGranted,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onNotificationPermissionResult(true)
                        }
                    },
                    onSkip = { viewModel.onSkip() },
                )
                2 -> BatteryCard(
                    granted = uiState.batteryExemptGranted,
                    onGrant = {
                        val pm = context.getSystemService(PowerManager::class.java)
                        if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
                            viewModel.onBatteryExemptGranted(true)
                        } else {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}"),
                                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                )
                            }
                        }
                    },
                    onSkip = { viewModel.onSkip() },
                )
                3 -> SandboxCard(
                    sandboxText = uiState.sandboxText,
                    sandboxExpanded = uiState.sandboxExpanded,
                    onTextChange = { viewModel.onSandboxTextChanged(it) },
                    onGetStarted = { viewModel.onComplete() },
                )
            }
        }
    }
}

@Composable
private fun AccessibilityCard(
    granted: Boolean,
    onGrant: () -> Unit,
    onSkip: () -> Unit,
) {
    PermissionCard(
        title = "Enable Accessibility",
        description = "Snipcraft uses Android's Accessibility Service to detect when you type a shortcut in any app and replace it with your snippet.\n\nWe do not log keystrokes. Only your shortcuts trigger Snipcraft.",
        granted = granted,
        grantButtonLabel = "Open Accessibility Settings",
        grantButtonTag = "accessibility_grant_button",
        skipTag = "accessibility_skip_button",
        onGrant = onGrant,
        onSkip = onSkip,
    )
}

@Composable
private fun NotificationCard(
    granted: Boolean,
    onGrant: () -> Unit,
    onSkip: () -> Unit,
) {
    PermissionCard(
        title = "Allow Notifications",
        description = "Snipcraft shows a persistent notification so Android keeps it alive in the background. Without it, the service may be killed after a few minutes.",
        granted = granted,
        grantButtonLabel = "Allow Notifications",
        grantButtonTag = "notification_grant_button",
        skipTag = "notification_skip_button",
        onGrant = onGrant,
        onSkip = onSkip,
    )
}

@Composable
private fun BatteryCard(
    granted: Boolean,
    onGrant: () -> Unit,
    onSkip: () -> Unit,
) {
    PermissionCard(
        title = "Disable Battery Optimisation",
        description = "Some devices aggressively kill background apps. Exempting Snipcraft ensures snippets expand reliably, even hours after you last used the app.",
        granted = granted,
        grantButtonLabel = "Exempt from Battery Optimisation",
        grantButtonTag = "battery_grant_button",
        skipTag = "battery_skip_button",
        onGrant = onGrant,
        onSkip = onSkip,
    )
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    grantButtonLabel: String,
    grantButtonTag: String,
    skipTag: String,
    onGrant: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (granted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Granted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        if (!granted) {
            Button(
                onClick = onGrant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(grantButtonTag),
            ) { Text(grantButtonLabel) }
            Spacer(modifier = Modifier.height(8.dp))
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(skipTag),
        ) { Text(if (granted) "Continue →" else "Skip for now") }
    }
}

@Composable
private fun SandboxCard(
    sandboxText: String,
    sandboxExpanded: Boolean,
    onTextChange: (String) -> Unit,
    onGetStarted: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Try It", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Type  ;today  then a space to see expansion in action.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(16.dp)) {
                BasicTextField(
                    value = sandboxText,
                    onValueChange = onTextChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (sandboxExpanded)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandbox_field"),
                    decorationBox = { inner ->
                        if (sandboxText.isEmpty()) {
                            Text(
                                ";today ",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }
                        inner()
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("get_started_button"),
        ) { Text("Get Started") }
    }
}
