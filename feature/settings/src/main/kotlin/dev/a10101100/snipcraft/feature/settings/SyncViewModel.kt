package dev.a10101100.snipcraft.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.a10101100.snipcraft.core.sync.SyncConfig
import dev.a10101100.snipcraft.core.sync.SyncConfigStore
import dev.a10101100.snipcraft.core.sync.SyncEngine
import dev.a10101100.snipcraft.core.sync.SyncResult
import dev.a10101100.snipcraft.core.sync.SyncWorker
import dev.a10101100.snipcraft.core.sync.WebDavClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val configStore: SyncConfigStore,
    private val syncEngine: SyncEngine,
    private val webDavClient: WebDavClient,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Secondary constructor for tests (avoids Android Context)
    internal constructor(
        configStore: SyncConfigStore,
        syncEngine: SyncEngine,
        webDavClient: WebDavClient,
    ) : this(
        configStore = configStore,
        syncEngine = syncEngine,
        webDavClient = webDavClient,
        context = object : android.content.ContextWrapper(null) {
            override fun getSystemService(name: String): Any? = null
        },
    )

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        configStore.load()?.let { cfg ->
            _uiState.update {
                it.copy(
                    serverUrl = cfg.serverUrl,
                    remotePath = cfg.remotePath,
                    username = cfg.username,
                    password = cfg.password,
                    allowHttp = cfg.allowHttp,
                    intervalHours = cfg.intervalHours,
                )
            }
        }
    }

    fun onServerUrlChange(url: String) = _uiState.update { it.copy(serverUrl = url, testResult = null) }
    fun onRemotePathChange(path: String) = _uiState.update { it.copy(remotePath = path) }
    fun onUsernameChange(u: String) = _uiState.update { it.copy(username = u) }
    fun onPasswordChange(p: String) = _uiState.update { it.copy(password = p) }
    fun onAllowHttpChange(v: Boolean) = _uiState.update { it.copy(allowHttp = v) }
    fun onIntervalChange(hours: Int) = _uiState.update { it.copy(intervalHours = hours) }

    fun onTestConnection() {
        val s = _uiState.value
        if (s.serverUrl.isBlank()) {
            _uiState.update { it.copy(testResult = "Enter a server URL first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            val cfg = currentConfig()
            val creds = WebDavClient.Credentials(cfg.username, cfg.password)
            val result = webDavClient.propFind(cfg.remoteFileUrl(), creds, !cfg.requiresHttps())
            val message = when (result) {
                WebDavClient.PropFindResult.Found    -> "Connected — remote file found"
                WebDavClient.PropFindResult.NotFound -> "Connected — no file yet (will be created on first sync)"
                is WebDavClient.PropFindResult.Error -> "Error: ${result.message}"
            }
            _uiState.update { it.copy(isTesting = false, testResult = message) }
        }
    }

    fun onSyncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = runCatching { syncEngine.sync(currentConfig()) }
                .getOrElse { SyncResult(errors = listOf(it.message ?: "Unknown error")) }
            _uiState.update { it.copy(isSyncing = false, lastSyncResult = result) }
        }
    }

    fun onSaveConfig() {
        val cfg = currentConfig()
        configStore.save(cfg)
        try { SyncWorker.schedule(context, cfg.intervalHours) } catch (_: Exception) {}
    }

    private fun currentConfig() = _uiState.value.let { s ->
        SyncConfig(
            serverUrl = s.serverUrl,
            remotePath = s.remotePath,
            username = s.username,
            password = s.password,
            allowHttp = s.allowHttp,
            intervalHours = s.intervalHours,
        )
    }
}
