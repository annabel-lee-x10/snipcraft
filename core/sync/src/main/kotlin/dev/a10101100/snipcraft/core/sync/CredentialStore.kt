package dev.a10101100.snipcraft.core.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncConfigStore {

    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "snipcraft_sync_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun load(): SyncConfig? {
        val url = prefs.getString(KEY_SERVER_URL, null) ?: return null
        return SyncConfig(
            serverUrl = url,
            remotePath = prefs.getString(KEY_REMOTE_PATH, null) ?: "/snipcraft/snippets.json",
            username = prefs.getString(KEY_USERNAME, null) ?: "",
            password = prefs.getString(KEY_PASSWORD, null) ?: "",
            allowHttp = prefs.getBoolean(KEY_ALLOW_HTTP, false),
            intervalHours = prefs.getInt(KEY_INTERVAL_HOURS, 6),
        )
    }

    override fun save(config: SyncConfig) {
        prefs.edit()
            .putString(KEY_SERVER_URL, config.serverUrl)
            .putString(KEY_REMOTE_PATH, config.remotePath)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .putBoolean(KEY_ALLOW_HTTP, config.allowHttp)
            .putInt(KEY_INTERVAL_HOURS, config.intervalHours)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_SERVER_URL = "server_url"
        const val KEY_REMOTE_PATH = "remote_path"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_ALLOW_HTTP = "allow_http"
        const val KEY_INTERVAL_HOURS = "interval_hours"
    }
}
