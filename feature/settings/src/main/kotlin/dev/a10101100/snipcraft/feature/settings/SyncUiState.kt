package dev.a10101100.snipcraft.feature.settings

import dev.a10101100.snipcraft.core.sync.SyncResult

data class SyncUiState(
    val serverUrl: String = "",
    val remotePath: String = "/snipcraft/snippets.json",
    val username: String = "",
    val password: String = "",
    val allowHttp: Boolean = false,
    val intervalHours: Int = 6,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncResult: SyncResult? = null,
)
