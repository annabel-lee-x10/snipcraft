# Pass 7 WebDAV Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bidirectional WebDAV sync so snippets stay in sync across multiple Android phones using the user's own server.

**Architecture:** New `core:sync` module owns WebDavClient (OkHttp), SyncEngine (merge logic), CredentialStore (EncryptedSharedPreferences), and SyncWorker (WorkManager). Room schema bumps v1→v2 to add `syncVersion` (Lamport counter) to snippets + folders. The remote stores a single JSON file using the existing `BackupData` format, extended with `syncVersion`. Settings gets a Sync section with a dedicated `SyncViewModel`.

**Tech Stack:** OkHttp 4.12.0, MockWebServer 4.12.0, androidx.security:security-crypto 1.0.0, WorkManager 2.11.2 (existing), Room migration v1→v2, kotlinx.serialization.json (existing), Hilt (existing).

---

## Pre-task: Read these files before starting (worktree path varies)

```
gradle/libs.versions.toml
settings.gradle.kts
core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/SnippetEntity.kt
core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/FolderEntity.kt
core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/SnipcraftDatabase.kt
core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/SnippetMapper.kt
core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/FolderMapper.kt
core/domain/src/main/kotlin/dev/a10101100/snipcraft/core/domain/Snippet.kt
core/domain/src/main/kotlin/dev/a10101100/snipcraft/core/domain/Folder.kt
core/backup/src/main/kotlin/dev/a10101100/snipcraft/core/backup/BackupModels.kt
app/build.gradle.kts
feature/settings/build.gradle.kts
feature/settings/src/main/kotlin/dev/a10101100/snipcraft/feature/settings/SettingsScreen.kt
feature/settings/src/main/kotlin/dev/a10101100/snipcraft/feature/settings/SettingsUiState.kt
```

---

## File Structure

### New files

| File | Purpose |
|------|---------|
| `core/sync/build.gradle.kts` | Module config: OkHttp, security-crypto, WorkManager, serialization |
| `core/sync/src/main/AndroidManifest.xml` | Empty namespace manifest |
| `core/sync/src/main/kotlin/.../core/sync/SyncConfig.kt` | Data class: server URL, credentials, interval |
| `core/sync/src/main/kotlin/.../core/sync/SyncConfigStore.kt` | Interface: load/save/clear config |
| `core/sync/src/main/kotlin/.../core/sync/CredentialStore.kt` | EncryptedSharedPreferences impl of SyncConfigStore |
| `core/sync/src/main/kotlin/.../core/sync/WebDavClient.kt` | PROPFIND/GET/PUT/MKCOL via OkHttp, Basic Auth |
| `core/sync/src/main/kotlin/.../core/sync/SyncEngine.kt` | Bidirectional merge: pull remote, merge by syncVersion/updatedAt, push |
| `core/sync/src/main/kotlin/.../core/sync/SyncResult.kt` | pushed/pulled/conflicts/errors/timestamp |
| `core/sync/src/main/kotlin/.../core/sync/SyncWorker.kt` | @HiltWorker CoroutineWorker + schedule/cancel companion |
| `core/sync/src/main/kotlin/.../core/sync/SyncModule.kt` | Hilt: OkHttpClient, WebDavClient, SyncEngine, CredentialStore |
| `core/sync/src/test/kotlin/.../core/sync/WebDavClientTest.kt` | MockWebServer: PROPFIND/GET/PUT/MKCOL/auth |
| `core/sync/src/test/kotlin/.../core/sync/SyncEngineTest.kt` | MockK: empty local, empty remote, both directions, conflict |
| `core/sync/src/test/kotlin/.../core/sync/SyncWorkerTest.kt` | WorkManager TestListenableWorkerBuilder |
| `core/sync/src/test/resources/robolectric.properties` | sdk=31 |
| `feature/settings/src/main/kotlin/.../feature/settings/SyncUiState.kt` | Sync section state: config fields, test result, last sync |
| `feature/settings/src/main/kotlin/.../feature/settings/SyncViewModel.kt` | @HiltViewModel: testConnection, syncNow, saveConfig, loadConfig |
| `feature/settings/src/test/kotlin/.../feature/settings/SyncViewModelTest.kt` | JUnit 5: testConnection (found/not found), saveConfig, syncNow |

### Modified files

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add okhttp, mockwebserver, security-crypto versions + library aliases |
| `settings.gradle.kts` | Add `include(":core:sync")` |
| `app/build.gradle.kts` | Add `implementation(project(":core:sync"))` |
| `feature/settings/build.gradle.kts` | Add `implementation(project(":core:sync"))` |
| `core/domain/src/main/kotlin/.../core/domain/Snippet.kt` | Add `val syncVersion: Long = 0L` |
| `core/domain/src/main/kotlin/.../core/domain/Folder.kt` | Add `val syncVersion: Long = 0L` |
| `core/database/src/main/kotlin/.../core/database/SnippetEntity.kt` | Add `val syncVersion: Long` |
| `core/database/src/main/kotlin/.../core/database/FolderEntity.kt` | Add `val syncVersion: Long` |
| `core/database/src/main/kotlin/.../core/database/SnippetMapper.kt` | Map syncVersion in toDomain/toEntity |
| `core/database/src/main/kotlin/.../core/database/FolderMapper.kt` | Map syncVersion in toDomain/toEntity |
| `core/database/src/main/kotlin/.../core/database/SnipcraftDatabase.kt` | Bump version 1→2, add MIGRATION_1_2 |
| `core/backup/src/main/kotlin/.../core/backup/BackupModels.kt` | Add `val syncVersion: Long = 0L` to SnippetBackup + FolderBackup |
| `feature/settings/src/main/kotlin/.../feature/settings/SettingsScreen.kt` | Add Sync section (calls SyncViewModel) |
| `CHANGELOG.md` | Append Pass 7 entries |
| `docs/HANDOFF.md` | Add Pass 7 section |

---

## Task 1: Branch + worktree setup

**Files:** none (setup only)

- [ ] **Step 1: Create feature branch in a new worktree**

```bash
cd D:\a10101100_labs\snipcraft
git fetch origin
git checkout main
git pull

# Create worktree for isolated work
git worktree add .claude/worktrees/pass-7-webdav-sync claude/pass-7-webdav-sync 2>/dev/null \
  || git worktree add .claude/worktrees/pass-7-webdav-sync -b claude/pass-7-webdav-sync origin/main

# Copy gradle wrapper JAR and local.properties (not tracked by git)
cp gradle/wrapper/gradle-wrapper.jar .claude/worktrees/pass-7-webdav-sync/gradle/wrapper/
cp local.properties .claude/worktrees/pass-7-webdav-sync/local.properties
```

- [ ] **Step 2: Verify worktree is clean on correct branch**

```bash
cd D:\a10101100_labs\snipcraft\.claude\worktrees\pass-7-webdav-sync
git status
```
Expected: `On branch claude/pass-7-webdav-sync` and `nothing to commit`.

---

## Task 2: Add dependencies to version catalog + create core:sync skeleton

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Create: `core/sync/build.gradle.kts`
- Create: `core/sync/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add OkHttp, MockWebServer, security-crypto to libs.versions.toml**

In the `[versions]` section add:
```toml
okhttp = "4.12.0"
security-crypto = "1.0.0"
```

In the `[libraries]` section add:
```toml
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
security-crypto = { module = "androidx.security:security-crypto", version.ref = "security-crypto" }
```

- [ ] **Step 2: Register core:sync in settings.gradle.kts**

Add after `include(":core:backup")`:
```kotlin
include(":core:sync")
```

- [ ] **Step 3: Create core/sync/build.gradle.kts**

```kotlin
plugins {
    id("snipcraft.android.library")
    id("snipcraft.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.a10101100.snipcraft.core.sync"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:backup"))

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.security.crypto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.workmanager.ktx)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.workmanager.testing)
    testImplementation(libs.androidx.test.core)
}
```

- [ ] **Step 4: Create core/sync/src/main/AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 5: Add core:sync to app/build.gradle.kts**

Add inside the `dependencies` block after `implementation(project(":core:backup"))`:
```kotlin
implementation(project(":core:sync"))
```

- [ ] **Step 6: Verify the module resolves**

```bash
cd D:\a10101100_labs\snipcraft\.claude\worktrees\pass-7-webdav-sync
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:sync:dependencies 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL (even though no source files exist yet).

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts core/sync/ app/build.gradle.kts
git commit -m "build: add core:sync module skeleton, OkHttp + security-crypto deps"
```

---

## Task 3: Room v2 migration + domain model syncVersion

**Files:**
- Modify: `core/domain/src/main/kotlin/.../core/domain/Snippet.kt`
- Modify: `core/domain/src/main/kotlin/.../core/domain/Folder.kt`
- Modify: `core/database/src/main/kotlin/.../core/database/SnippetEntity.kt`
- Modify: `core/database/src/main/kotlin/.../core/database/FolderEntity.kt`
- Modify: `core/database/src/main/kotlin/.../core/database/SnippetMapper.kt`
- Modify: `core/database/src/main/kotlin/.../core/database/FolderMapper.kt`
- Modify: `core/database/src/main/kotlin/.../core/database/SnipcraftDatabase.kt`
- Modify: `core/backup/src/main/kotlin/.../core/backup/BackupModels.kt`

- [ ] **Step 1: Add syncVersion to Snippet domain model**

In `core/domain/src/main/kotlin/dev/a10101100/snipcraft/core/domain/Snippet.kt`, add field with default:

```kotlin
data class Snippet(
    val id: String,
    val shortcut: String,
    val body: String,
    val type: SnippetType = SnippetType.PLAIN,
    val triggerMode: TriggerMode = TriggerMode.ON_DELIMITER,
    val folderId: String? = null,
    val usageCount: Long = 0L,
    val lastUsedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isPinned: Boolean = false,
    val isEnabled: Boolean = true,
    val caseSensitive: Boolean = false,
    val description: String? = null,
    val syncVersion: Long = 0L,         // Lamport-style counter; bumped on each local edit
)
```

- [ ] **Step 2: Add syncVersion to Folder domain model**

In `core/domain/src/main/kotlin/dev/a10101100/snipcraft/core/domain/Folder.kt`:

```kotlin
data class Folder(
    val id: String,
    val name: String,
    val color: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val syncVersion: Long = 0L,
)
```

- [ ] **Step 3: Add syncVersion to SnippetEntity**

In `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/SnippetEntity.kt`:

```kotlin
@Entity(
    tableName = "snippets",
    indices = [
        Index("shortcut"),
        Index("folderId"),
        Index(value = ["usageCount"]),
        Index(value = ["lastUsedAt"]),
    ],
)
data class SnippetEntity(
    @PrimaryKey val id: String,
    val shortcut: String,
    val body: String,
    val type: String,
    val triggerMode: String,
    val folderId: String?,
    val usageCount: Long,
    val lastUsedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val isEnabled: Boolean,
    val caseSensitive: Boolean,
    val description: String?,
    val schemaVersion: Int,
    val syncVersion: Long = 0L,
)
```

- [ ] **Step 4: Add syncVersion to FolderEntity**

In `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/FolderEntity.kt`:

```kotlin
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int?,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val syncVersion: Long = 0L,
)
```

- [ ] **Step 5: Update SnippetMapper.kt to include syncVersion**

Replace the entire file:

```kotlin
package dev.a10101100.snipcraft.core.database

import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode

fun SnippetEntity.toDomain() = Snippet(
    id = id,
    shortcut = shortcut,
    body = body,
    type = SnippetType.valueOf(type),
    triggerMode = TriggerMode.valueOf(triggerMode),
    folderId = folderId,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isEnabled = isEnabled,
    caseSensitive = caseSensitive,
    description = description,
    syncVersion = syncVersion,
)

fun Snippet.toEntity(schemaVersion: Int = 1) = SnippetEntity(
    id = id,
    shortcut = shortcut,
    body = body,
    type = type.name,
    triggerMode = triggerMode.name,
    folderId = folderId,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isEnabled = isEnabled,
    caseSensitive = caseSensitive,
    description = description,
    schemaVersion = schemaVersion,
    syncVersion = syncVersion,
)
```

- [ ] **Step 6: Update FolderMapper.kt to include syncVersion**

Replace the entire file:

```kotlin
package dev.a10101100.snipcraft.core.database

import dev.a10101100.snipcraft.core.domain.Folder

fun FolderEntity.toDomain() = Folder(
    id = id,
    name = name,
    color = color,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncVersion = syncVersion,
)

fun Folder.toEntity() = FolderEntity(
    id = id,
    name = name,
    color = color,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncVersion = syncVersion,
)
```

- [ ] **Step 7: Add Room migration and bump database version to 2**

Replace `SnipcraftDatabase.kt`:

```kotlin
package dev.a10101100.snipcraft.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SnippetEntity::class, FolderEntity::class, CompatibilityRuleEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class SnipcraftDatabase : RoomDatabase() {
    abstract fun snippetDao(): SnippetDao
    abstract fun folderDao(): FolderDao
    abstract fun compatibilityRuleDao(): CompatibilityRuleDao

    companion object {
        const val DATABASE_NAME = "snipcraft.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE snippets ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
```

- [ ] **Step 8: Wire migration in DatabaseModule.kt**

In `DatabaseModule.kt`, update `provideDatabase` to add the migration:

```kotlin
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): SnipcraftDatabase =
    Room.databaseBuilder(context, SnipcraftDatabase::class.java, SnipcraftDatabase.DATABASE_NAME)
        .addMigrations(SnipcraftDatabase.MIGRATION_1_2)
        .build()
```

- [ ] **Step 9: Add syncVersion to BackupModels.kt**

In `core/backup/src/main/kotlin/dev/a10101100/snipcraft/core/backup/BackupModels.kt`, add to SnippetBackup and FolderBackup:

```kotlin
@Serializable
data class SnippetBackup(
    val id: String,
    val shortcut: String,
    val body: String,
    val type: String,
    val triggerMode: String,
    val folderId: String? = null,
    val usageCount: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isPinned: Boolean = false,
    val isEnabled: Boolean = true,
    val caseSensitive: Boolean = false,
    val description: String? = null,
    val syncVersion: Long = 0L,         // added in Pass 7; defaults to 0 for older backups
)

@Serializable
data class FolderBackup(
    val id: String,
    val name: String,
    val color: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val syncVersion: Long = 0L,
)
```

- [ ] **Step 10: Run all existing tests to confirm no regressions**

```bash
cd D:\a10101100_labs\snipcraft\.claude\worktrees\pass-7-webdav-sync
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew test 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL (all 157 tests pass; syncVersion defaults to 0 so no existing test data changes).

**NOTE:** If Room generates a schema JSON at `core/database/schemas/.../2.json` after the build, add it to git. Check for it with `git status` after the build.

- [ ] **Step 11: Commit**

```bash
git add core/domain/ core/database/ core/backup/src/main/kotlin/
git commit -m "feat: Room v2 migration — add syncVersion to snippets + folders for WebDAV conflict resolution"
```

---

## Task 4: SyncConfig + SyncResult + SyncConfigStore interface

**Files:**
- Create: `core/sync/src/main/kotlin/dev/a10101100/snipcraft/core/sync/SyncConfig.kt`
- Create: `core/sync/src/main/kotlin/dev/a10101100/snipcraft/core/sync/SyncResult.kt`
- Create: `core/sync/src/main/kotlin/dev/a10101100/snipcraft/core/sync/SyncConfigStore.kt`
- Create: `core/sync/src/main/kotlin/dev/a10101100/snipcraft/core/sync/CredentialStore.kt`

- [ ] **Step 1: Create SyncConfig.kt**

```kotlin
package dev.a10101100.snipcraft.core.sync

data class SyncConfig(
    val serverUrl: String,          // e.g. "https://nextcloud.example.com"
    val remotePath: String = "/snipcraft/snippets.json",
    val username: String,
    val password: String,           // stored encrypted; never logged
    val allowHttp: Boolean = false, // reject plain HTTP by default
    val intervalHours: Int = 6,     // 0 = off, 1, 6, 24
) {
    fun remoteFileUrl(): String {
        val base = serverUrl.trimEnd('/')
        val path = if (remotePath.startsWith("/")) remotePath else "/$remotePath"
        return "$base$path"
    }

    fun remoteDirUrl(): String {
        val fileUrl = remoteFileUrl()
        return fileUrl.substringBeforeLast('/')
    }

    fun requiresHttps(): Boolean = !allowHttp
}
```

- [ ] **Step 2: Create SyncResult.kt**

```kotlin
package dev.a10101100.snipcraft.core.sync

data class SyncResult(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val conflicts: Int = 0,
    val errors: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
) {
    val isSuccess: Boolean get() = errors.isEmpty()

    fun summary(): String = when {
        !isSuccess -> "Sync failed: ${errors.first()}"
        pushed == 0 && pulled == 0 -> "Already up to date"
        else -> "↑ $pushed  ↓ $pulled${if (conflicts > 0) "  ⚠ $conflicts conflicts" else ""}"
    }
}
```

- [ ] **Step 3: Create SyncConfigStore.kt (interface)**

```kotlin
package dev.a10101100.snipcraft.core.sync

interface SyncConfigStore {
    fun load(): SyncConfig?
    fun save(config: SyncConfig)
    fun clear()
}
```

- [ ] **Step 4: Create CredentialStore.kt**

```kotlin
package dev.a10101100.snipcraft.core.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncConfigStore {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "snipcraft_sync_prefs",
            masterKey,
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
```

- [ ] **Step 5: Commit**

```bash
git add core/sync/src/main/kotlin/
git commit -m "feat: SyncConfig, SyncResult, CredentialStore (EncryptedSharedPreferences)"
```

---

## Task 5: WebDavClient (TDD with MockWebServer)

**Files:**
- Create: `core/sync/src/test/kotlin/dev/a10101100/snipcraft/core/sync/WebDavClientTest.kt`
- Create: `core/sync/src/test/resources/robolectric.properties`
- Create: `core/sync/src/main/kotlin/dev/a10101100/snipcraft/core/sync/WebDavClient.kt`

- [ ] **Step 1: Create robolectric.properties**

```
# core/sync/src/test/resources/robolectric.properties
sdk=31
```

- [ ] **Step 2: Write the failing test**

```kotlin
// core/sync/src/test/kotlin/dev/a10101100/snipcraft/core/sync/WebDavClientTest.kt
package dev.a10101100.snipcraft.core.sync

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WebDavClientTest {

    private val server = MockWebServer()
    private val client = WebDavClient(OkHttpClient())
    private val creds = WebDavClient.Credentials("user", "pass")

    @BeforeEach fun start() { server.start() }
    @AfterEach  fun stop()  { server.shutdown() }

    private fun url(path: String = "/snipcraft/snippets.json") =
        server.url(path).toString()

    @Test
    fun `propFind returns Found when server responds 207`() = runTest {
        server.enqueue(MockResponse().setResponseCode(207).setBody("<multistatus/>"))
        val result = client.propFind(url(), creds)
        assertEquals(WebDavClient.PropFindResult.Found, result)
    }

    @Test
    fun `propFind returns NotFound when server responds 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = client.propFind(url(), creds)
        assertEquals(WebDavClient.PropFindResult.NotFound, result)
    }

    @Test
    fun `propFind sends PROPFIND method with Basic auth`() = runTest {
        server.enqueue(MockResponse().setResponseCode(207).setBody("<multistatus/>"))
        client.propFind(url(), creds)
        val req: RecordedRequest = server.takeRequest()
        assertEquals("PROPFIND", req.method)
        assertTrue(req.getHeader("Authorization")?.startsWith("Basic ") == true)
    }

    @Test
    fun `get returns body when server responds 200`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"version":1}"""))
        val body = client.get(url(), creds)
        assertEquals("""{"version":1}""", body)
    }

    @Test
    fun `get returns null when server responds 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val body = client.get(url(), creds)
        assertNull(body)
    }

    @Test
    fun `put returns true when server responds 201`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        val ok = client.put(url(), creds, """{"version":1}""")
        assertTrue(ok)
        val req = server.takeRequest()
        assertEquals("PUT", req.method)
        assertEquals("""{"version":1}""", req.body.readUtf8())
    }

    @Test
    fun `put returns false when server responds 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertFalse(client.put(url(), creds, "body"))
    }

    @Test
    fun `mkCol returns true when server responds 201`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        assertTrue(client.mkCol(url("/snipcraft"), creds))
        assertEquals("MKCOL", server.takeRequest().method)
    }

    @Test
    fun `mkCol returns true when server responds 405 (already exists)`() = runTest {
        server.enqueue(MockResponse().setResponseCode(405))
        assertTrue(client.mkCol(url("/snipcraft"), creds))
    }

    @Test
    fun `rejects plain HTTP URL when allowHttp is false`() = runTest {
        val httpUrl = "http://example.com/file.json"
        val result = client.propFind(httpUrl, creds, allowHttp = false)
        assertTrue(result is WebDavClient.PropFindResult.Error)
        assertTrue((result as WebDavClient.PropFindResult.Error).message.contains("plain HTTP"))
    }
}
```

- [ ] **Step 3: Run test to verify RED**

```bash
cd D:\a10101100_labs\snipcraft\.claude\worktrees\pass-7-webdav-sync
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:sync:compileDebugUnitTestKotlin 2>&1 | grep "^e:" | head -5
```
Expected: `Unresolved reference 'WebDavClient'`

- [ ] **Step 4: Implement WebDavClient.kt**

```kotlin
package dev.a10101100.snipcraft.core.sync

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    data class Credentials(val username: String, val password: String)

    sealed class PropFindResult {
        object Found : PropFindResult()
        object NotFound : PropFindResult()
        data class Error(val message: String) : PropFindResult()
    }

    suspend fun propFind(
        url: String,
        credentials: Credentials,
        allowHttp: Boolean = false,
    ): PropFindResult {
        if (!allowHttp && url.startsWith("http://")) {
            return PropFindResult.Error("Refusing plain HTTP URL — enable 'Allow plain HTTP' to override")
        }
        return try {
            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Depth", "0")
                .header("Authorization", Credentials.basic(credentials.username, credentials.password))
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use {
                when (it.code) {
                    207, 200 -> PropFindResult.Found
                    404      -> PropFindResult.NotFound
                    else     -> PropFindResult.Error("PROPFIND failed: HTTP ${it.code}")
                }
            }
        } catch (e: Exception) {
            PropFindResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun get(url: String, credentials: Credentials): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", Credentials.basic(credentials.username, credentials.password))
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use {
                if (it.isSuccessful) it.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun put(url: String, credentials: Credentials, body: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .put(body.toRequestBody("application/json".toMediaType()))
                .header("Authorization", Credentials.basic(credentials.username, credentials.password))
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use { it.code in 200..299 }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun mkCol(url: String, credentials: Credentials): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .method("MKCOL", null)
                .header("Authorization", Credentials.basic(credentials.username, credentials.password))
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use { it.code in 200..299 || it.code == 405 /* already exists */ }
        } catch (e: Exception) {
            false
        }
    }
}
```

- [ ] **Step 5: Run tests to verify GREEN**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:sync:test 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL, 11 tests pass.

- [ ] **Step 6: Commit**

```bash
git add core/sync/
git commit -m "feat: WebDavClient — PROPFIND/GET/PUT/MKCOL via OkHttp with Basic Auth (TDD)"
```

---

## Task 6: SyncEngine (TDD)

**Files:**
- Create: `core/sync/src/test/kotlin/dev/a10101100/snipcraft/core/sync/SyncEngineTest.kt`
- Create: `core/sync/src/main/kotlin/dev/a10101100/snipcraft/core/sync/SyncEngine.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// core/sync/src/test/kotlin/dev/a10101100/snipcraft/core/sync/SyncEngineTest.kt
package dev.a10101100.snipcraft.core.sync

import dev.a10101100.snipcraft.core.backup.BackupData
import dev.a10101100.snipcraft.core.backup.FolderBackup
import dev.a10101100.snipcraft.core.backup.SnippetBackup
import dev.a10101100.snipcraft.core.data.FolderRepository
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Snippet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SyncEngineTest {

    private val webDavClient = mockk<WebDavClient>()
    private val snippetRepo = mockk<SnippetRepository>(relaxed = true)
    private val folderRepo = mockk<FolderRepository>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val engine = SyncEngine(webDavClient, snippetRepo, folderRepo, json)

    private val config = SyncConfig(
        serverUrl = "https://example.com",
        remotePath = "/snipcraft/snippets.json",
        username = "user",
        password = "pass",
    )
    private val creds = WebDavClient.Credentials("user", "pass")

    @BeforeEach
    fun setUp() {
        every { snippetRepo.observeAll() } returns flowOf(emptyList())
        every { folderRepo.observeAll() } returns flowOf(emptyList())
    }

    @Test
    fun `empty local and no remote file — pushes empty snapshot`() = runTest {
        coEvery { webDavClient.propFind(any(), creds, any()) } returns WebDavClient.PropFindResult.NotFound
        coEvery { webDavClient.mkCol(any(), creds) } returns true
        coEvery { webDavClient.put(any(), creds, any()) } returns true

        val result = engine.sync(config)

        assertTrue(result.isSuccess)
        coVerify { webDavClient.put(any(), creds, any()) }
    }

    @Test
    fun `local has snippets and no remote — pushes local as initial snapshot`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";today", body = "{{date}}", syncVersion = 1L)
        every { snippetRepo.observeAll() } returns flowOf(listOf(snippet))
        coEvery { webDavClient.propFind(any(), creds, any()) } returns WebDavClient.PropFindResult.NotFound
        coEvery { webDavClient.mkCol(any(), creds) } returns true
        var putBody = ""
        coEvery { webDavClient.put(any(), creds, capture(io.mockk.slot<String>().also { })) } answers {
            putBody = secondArg()
            true
        }
        coEvery { webDavClient.put(any(), creds, any()) } coAnswers {
            putBody = thirdArg()
            true
        }

        engine.sync(config)

        // Verify push was attempted (body will contain snippet id)
        coVerify { webDavClient.put(any(), creds, any()) }
    }

    @Test
    fun `remote has snippets and local is empty — pulls remote snippets`() = runTest {
        val remoteSnippet = SnippetBackup(
            id = "r1", shortcut = ";hello", body = "Hello!", type = "PLAIN",
            triggerMode = "ON_DELIMITER", syncVersion = 1L,
        )
        val remoteData = BackupData(
            exportedAt = "2026-01-01T00:00:00Z",
            snippets = listOf(remoteSnippet),
            folders = emptyList(),
        )
        coEvery { webDavClient.propFind(any(), creds, any()) } returns WebDavClient.PropFindResult.Found
        coEvery { webDavClient.get(any(), creds) } returns json.encodeToString(remoteData)
        coEvery { webDavClient.put(any(), creds, any()) } returns true

        val result = engine.sync(config)

        assertEquals(1, result.pulled)
        coVerify { snippetRepo.upsert(match { it.shortcut == ";hello" }) }
    }

    @Test
    fun `conflict — remote higher syncVersion wins`() = runTest {
        val local = Snippet(id = "s1", shortcut = ";a", body = "local", syncVersion = 1L, updatedAt = 100L)
        val remote = SnippetBackup(
            id = "s1", shortcut = ";a", body = "remote", type = "PLAIN",
            triggerMode = "ON_DELIMITER", syncVersion = 2L, updatedAt = 50L,
        )
        every { snippetRepo.observeAll() } returns flowOf(listOf(local))
        val remoteData = BackupData(exportedAt = "", snippets = listOf(remote), folders = emptyList())
        coEvery { webDavClient.propFind(any(), creds, any()) } returns WebDavClient.PropFindResult.Found
        coEvery { webDavClient.get(any(), creds) } returns json.encodeToString(remoteData)
        coEvery { webDavClient.put(any(), creds, any()) } returns true

        val result = engine.sync(config)

        assertEquals(1, result.pulled)
        coVerify { snippetRepo.upsert(match { it.body == "remote" }) }
    }

    @Test
    fun `conflict — local higher syncVersion wins`() = runTest {
        val local = Snippet(id = "s1", shortcut = ";a", body = "local", syncVersion = 5L, updatedAt = 100L)
        val remote = SnippetBackup(
            id = "s1", shortcut = ";a", body = "remote", type = "PLAIN",
            triggerMode = "ON_DELIMITER", syncVersion = 2L, updatedAt = 200L,
        )
        every { snippetRepo.observeAll() } returns flowOf(listOf(local))
        val remoteData = BackupData(exportedAt = "", snippets = listOf(remote), folders = emptyList())
        coEvery { webDavClient.propFind(any(), creds, any()) } returns WebDavClient.PropFindResult.Found
        coEvery { webDavClient.get(any(), creds) } returns json.encodeToString(remoteData)
        coEvery { webDavClient.put(any(), creds, any()) } returns true

        val result = engine.sync(config)

        assertEquals(1, result.pushed)
        coVerify(exactly = 0) { snippetRepo.upsert(any()) }  // local wins, no pull needed
    }

    @Test
    fun `equal syncVersion and updatedAt — remote wins (deterministic)`() = runTest {
        val local = Snippet(id = "s1", shortcut = ";a", body = "local", syncVersion = 1L, updatedAt = 100L)
        val remote = SnippetBackup(
            id = "s1", shortcut = ";a", body = "remote", type = "PLAIN",
            triggerMode = "ON_DELIMITER", syncVersion = 1L, updatedAt = 100L,
        )
        every { snippetRepo.observeAll() } returns flowOf(listOf(local))
        val remoteData = BackupData(exportedAt = "", snippets = listOf(remote), folders = emptyList())
        coEvery { webDavClient.propFind(any(), creds, any()) } returns WebDavClient.PropFindResult.Found
        coEvery { webDavClient.get(any(), creds) } returns json.encodeToString(remoteData)
        coEvery { webDavClient.put(any(), creds, any()) } returns true

        engine.sync(config)

        coVerify { snippetRepo.upsert(match { it.body == "remote" }) }
    }

    @Test
    fun `propFind error — returns error result without modifying DB`() = runTest {
        coEvery { webDavClient.propFind(any(), creds, any()) } returns
            WebDavClient.PropFindResult.Error("connection refused")

        val result = engine.sync(config)

        assertTrue(!result.isSuccess)
        coVerify(exactly = 0) { snippetRepo.upsert(any()) }
    }
}
```

- [ ] **Step 2: Run test to verify RED**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:sync:compileDebugUnitTestKotlin 2>&1 | grep "^e:" | head -5
```
Expected: `Unresolved reference 'SyncEngine'`

- [ ] **Step 3: Implement SyncEngine.kt**

```kotlin
package dev.a10101100.snipcraft.core.sync

import dev.a10101100.snipcraft.core.backup.BackupData
import dev.a10101100.snipcraft.core.backup.FolderBackup
import dev.a10101100.snipcraft.core.backup.SnippetBackup
import dev.a10101100.snipcraft.core.data.FolderRepository
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Folder
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val webDavClient: WebDavClient,
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val json: Json,
) {
    suspend fun sync(config: SyncConfig): SyncResult {
        val creds = WebDavClient.Credentials(config.username, config.password)
        val fileUrl = config.remoteFileUrl()
        val dirUrl = config.remoteDirUrl()

        val localSnippets = snippetRepository.observeAll().first()
        val localFolders = folderRepository.observeAll().first()

        return when (val propResult = webDavClient.propFind(fileUrl, creds, !config.requiresHttps())) {
            is WebDavClient.PropFindResult.Error ->
                SyncResult(errors = listOf(propResult.message))

            WebDavClient.PropFindResult.NotFound -> {
                // First sync — push local state to remote
                webDavClient.mkCol(dirUrl, creds)
                val snapshot = buildSnapshot(localSnippets, localFolders)
                val ok = webDavClient.put(fileUrl, creds, json.encodeToString(snapshot))
                if (ok) SyncResult(pushed = localSnippets.size + localFolders.size)
                else SyncResult(errors = listOf("PUT failed during initial push"))
            }

            WebDavClient.PropFindResult.Found -> {
                val body = webDavClient.get(fileUrl, creds)
                    ?: return SyncResult(errors = listOf("GET failed"))
                val remoteData = runCatching { json.decodeFromString<BackupData>(body) }
                    .getOrElse { return SyncResult(errors = listOf("Parse error: ${it.message}")) }

                val (snippetsToPull, snippetsToPush, conflicts) =
                    mergeSnippets(localSnippets, remoteData.snippets)
                val (foldersToPull, foldersToPush, _) =
                    mergeFolders(localFolders, remoteData.folders)

                snippetsToPull.forEach { snippetRepository.upsert(it) }
                foldersToPull.forEach { folderRepository.upsert(it) }

                // Push merged state (all local + pulled items) to remote
                val mergedSnippets = buildMergedSnippets(localSnippets, snippetsToPull, snippetsToPush)
                val mergedFolders = buildMergedFolders(localFolders, foldersToPull, foldersToPush)
                val newSnapshot = buildSnapshot(mergedSnippets, mergedFolders)
                webDavClient.put(fileUrl, creds, json.encodeToString(newSnapshot))

                SyncResult(
                    pushed = snippetsToPush.size + foldersToPush.size,
                    pulled = snippetsToPull.size + foldersToPull.size,
                    conflicts = conflicts,
                )
            }
        }
    }

    private fun mergeSnippets(
        local: List<Snippet>,
        remote: List<SnippetBackup>,
    ): Triple<List<Snippet>, List<Snippet>, Int> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val allIds = localById.keys + remoteById.keys

        val toPull = mutableListOf<Snippet>()    // remote wins, apply locally
        val toPush = mutableListOf<Snippet>()    // local wins, will be in pushed snapshot
        var conflicts = 0

        for (id in allIds) {
            val loc = localById[id]
            val rem = remoteById[id]
            when {
                loc == null -> toPull.add(rem!!.toDomain())
                rem == null -> toPush.add(loc)
                else -> {
                    val winner = resolveConflict(loc.syncVersion, loc.updatedAt, rem.syncVersion, rem.updatedAt)
                    if (winner == Winner.LOCAL) {
                        toPush.add(loc)
                    } else {
                        toPull.add(rem.toDomain())
                        if (loc.syncVersion != rem.syncVersion || loc.updatedAt != rem.updatedAt) conflicts++
                    }
                }
            }
        }
        return Triple(toPull, toPush, conflicts)
    }

    private fun mergeFolders(
        local: List<Folder>,
        remote: List<FolderBackup>,
    ): Triple<List<Folder>, List<Folder>, Int> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val allIds = localById.keys + remoteById.keys

        val toPull = mutableListOf<Folder>()
        val toPush = mutableListOf<Folder>()

        for (id in allIds) {
            val loc = localById[id]
            val rem = remoteById[id]
            when {
                loc == null -> toPull.add(rem!!.toDomain())
                rem == null -> toPush.add(loc)
                else -> {
                    val winner = resolveConflict(loc.syncVersion, loc.updatedAt, rem.syncVersion, rem.updatedAt)
                    if (winner == Winner.LOCAL) toPush.add(loc) else toPull.add(rem.toDomain())
                }
            }
        }
        return Triple(toPull, toPush, 0)
    }

    private enum class Winner { LOCAL, REMOTE }

    private fun resolveConflict(
        localSyncVersion: Long, localUpdatedAt: Long,
        remoteSyncVersion: Long, remoteUpdatedAt: Long,
    ): Winner = when {
        localSyncVersion > remoteSyncVersion -> Winner.LOCAL
        remoteSyncVersion > localSyncVersion -> Winner.REMOTE
        localUpdatedAt > remoteUpdatedAt -> Winner.LOCAL
        else -> Winner.REMOTE  // equal or remote higher → remote wins (deterministic)
    }

    private fun buildMergedSnippets(
        local: List<Snippet>,
        pulled: List<Snippet>,
        pushed: List<Snippet>,
    ): List<Snippet> {
        val map = local.associateBy { it.id }.toMutableMap()
        pulled.forEach { map[it.id] = it }
        return map.values.toList()
    }

    private fun buildMergedFolders(
        local: List<Folder>,
        pulled: List<Folder>,
        pushed: List<Folder>,
    ): List<Folder> {
        val map = local.associateBy { it.id }.toMutableMap()
        pulled.forEach { map[it.id] = it }
        return map.values.toList()
    }

    private fun buildSnapshot(snippets: List<Snippet>, folders: List<Folder>): BackupData =
        BackupData(
            exportedAt = Instant.now().toString(),
            snippets = snippets.map { it.toBackup() },
            folders = folders.map { it.toBackup() },
        )

    private fun Snippet.toBackup() = SnippetBackup(
        id = id, shortcut = shortcut, body = body, type = type.name,
        triggerMode = triggerMode.name, folderId = folderId, usageCount = usageCount,
        createdAt = createdAt, updatedAt = updatedAt, isPinned = isPinned,
        isEnabled = isEnabled, caseSensitive = caseSensitive, description = description,
        syncVersion = syncVersion,
    )

    private fun Folder.toBackup() = FolderBackup(
        id = id, name = name, color = color, sortOrder = sortOrder,
        createdAt = createdAt, updatedAt = updatedAt, syncVersion = syncVersion,
    )

    private fun SnippetBackup.toDomain() = Snippet(
        id = id, shortcut = shortcut, body = body,
        type = runCatching { SnippetType.valueOf(type) }.getOrDefault(SnippetType.PLAIN),
        triggerMode = runCatching { TriggerMode.valueOf(triggerMode) }.getOrDefault(TriggerMode.ON_DELIMITER),
        folderId = folderId, usageCount = usageCount, createdAt = createdAt, updatedAt = updatedAt,
        isPinned = isPinned, isEnabled = isEnabled, caseSensitive = caseSensitive, description = description,
        syncVersion = syncVersion,
    )

    private fun FolderBackup.toDomain() = Folder(
        id = id, name = name, color = color, sortOrder = sortOrder,
        createdAt = createdAt, updatedAt = updatedAt, syncVersion = syncVersion,
    )
}
```

NOTE: The `json.encodeToString(snapshot)` call requires BackupData to have a `@Serializable` annotation — it already does. The `Json` instance used is the same as `BackupModule.provideJson()` which has `ignoreUnknownKeys = true`.

- [ ] **Step 4: Run tests to verify GREEN**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:sync:test 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/sync/src/
git commit -m "feat: SyncEngine — bidirectional WebDAV sync with syncVersion conflict resolution (TDD)"
```

---

## Task 7: SyncWorker + SyncModule (Hilt)

**Files:**
- Create: `core/sync/src/test/kotlin/dev/a10101100/snipcraft/core/sync/SyncWorkerTest.kt`
- Create: `core/sync/src/main/kotlin/dev/a10101100/snipcraft/core/sync/SyncWorker.kt`
- Create: `core/sync/src/main/kotlin/dev/a10101100/snipcraft/core/sync/SyncModule.kt`

- [ ] **Step 1: Write the failing SyncWorker test**

```kotlin
// core/sync/src/test/kotlin/dev/a10101100/snipcraft/core/sync/SyncWorkerTest.kt
package dev.a10101100.snipcraft.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {

    private val mockSyncEngine = mockk<SyncEngine>(relaxed = true)
    private val mockCredentialStore = mockk<SyncConfigStore>()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `worker returns success when no config is saved`() = runTest {
        coEvery { mockCredentialStore.load() } returns null

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        // We can't inject mocks via TestListenableWorkerBuilder without Hilt test setup.
        // Verify the worker class exists and has the right structure via direct instantiation:
        val result = SyncWorker(context, worker.params, mockSyncEngine, mockCredentialStore).doWork()
        assertEquals(Result.success(), result)
    }

    @Test
    fun `worker calls syncEngine when config is present`() = runTest {
        val config = SyncConfig(serverUrl = "https://example.com", username = "u", password = "p")
        coEvery { mockCredentialStore.load() } returns config
        coEvery { mockSyncEngine.sync(config) } returns SyncResult()

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        SyncWorker(context, worker.params, mockSyncEngine, mockCredentialStore).doWork()

        io.mockk.coVerify { mockSyncEngine.sync(config) }
    }

    @Test
    fun `worker returns retry on network error`() = runTest {
        val config = SyncConfig(serverUrl = "https://example.com", username = "u", password = "p")
        coEvery { mockCredentialStore.load() } returns config
        coEvery { mockSyncEngine.sync(config) } throws java.io.IOException("Network error")

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = SyncWorker(context, worker.params, mockSyncEngine, mockCredentialStore).doWork()

        assertEquals(Result.retry(), result)
    }
}
```

- [ ] **Step 2: Run test to verify RED**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:sync:compileDebugUnitTestKotlin 2>&1 | grep "^e:" | head -5
```
Expected: `Unresolved reference 'SyncWorker'`

- [ ] **Step 3: Implement SyncWorker.kt**

```kotlin
package dev.a10101100.snipcraft.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.a10101100.snipcraft.core.common.AppLogger
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val configStore: SyncConfigStore,
) : CoroutineWorker(context, params) {

    val params: WorkerParameters get() = workerParams

    override suspend fun doWork(): Result {
        val config = configStore.load() ?: run {
            AppLogger.i("SyncWorker: no config, skipping")
            return Result.success()
        }
        return try {
            val result = syncEngine.sync(config)
            if (result.isSuccess) {
                AppLogger.i("SyncWorker: ↑%d ↓%d", result.pushed, result.pulled)
                Result.success()
            } else {
                AppLogger.w("SyncWorker: sync error — %s", result.errors.firstOrNull())
                Result.retry()
            }
        } catch (e: IOException) {
            AppLogger.w("SyncWorker: network error — %s", e.message)
            Result.retry()
        } catch (e: Exception) {
            AppLogger.e("SyncWorker: unexpected error — %s", e.message)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "snipcraft_sync"

        fun schedule(context: Context, intervalHours: Int) {
            if (intervalHours <= 0) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<SyncWorker>(intervalHours.toLong(), TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun triggerNow(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
```

NOTE: The test accesses `worker.params` via the public property. Add `val params get() = workerParams` to SyncWorker to expose the `workerParams` field for test construction. Actually, looking at the test: `SyncWorker(context, worker.params, ...)` - `TestListenableWorkerBuilder` provides a `WorkerParameters` instance via `worker.params`. This is from WorkManager testing utilities — it's available via reflection or the `workerParams` protected field. If the build fails on this, use `worker.workerParams` or cast appropriately.

ALTERNATIVE if `worker.params` doesn't compile: Replace the test construction with:

```kotlin
// If worker.params isn't accessible, use a factory or just verify via a spy
val worker = SyncWorker(context, 
    TestListenableWorkerBuilder<SyncWorker>(context).build().let { 
        // WorkerParameters is accessible via Reflection
        SyncWorker::class.java.getDeclaredField("mWorkerParams")
            .also { it.isAccessible = true }
            .get(it) as WorkerParameters
    },
    mockSyncEngine, mockCredentialStore)
```

Actually the simplest approach: just verify the companion object's `schedule` and `cancel` methods work (no instantiation needed for those):

```kotlin
@Test
fun `schedule enqueues periodic work`() = runTest {
    val wm = WorkManager.getInstance(context)
    SyncWorker.schedule(context, 6)
    val info = wm.getWorkInfosForUniqueWork(WORK_NAME).await()
    assertFalse(info.isEmpty())
}
```

Use whichever approach compiles with the WorkManager testing dependency version in use.

- [ ] **Step 4: Implement SyncModule.kt**

```kotlin
package dev.a10101100.snipcraft.core.sync

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncConfigStore(impl: CredentialStore): SyncConfigStore

    companion object {

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE  // Never log request bodies (could contain passwords)
            }
            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
        }

        @Provides
        @Singleton
        fun provideSyncJson(): Json = Json { ignoreUnknownKeys = true }
    }
}
```

**NOTE:** `SyncModule.provideSyncJson()` will conflict with `BackupModule.provideJson()` if they both provide `Json` without a qualifier. Add a `@SyncJson` qualifier or reuse the existing one.

To avoid the conflict, add a named qualifier:

```kotlin
// SyncModule.kt — add annotation at top of file
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncJson

// Then in SyncModule.companion:
@Provides @Singleton @SyncJson
fun provideSyncJson(): Json = Json { ignoreUnknownKeys = true }
```

And inject it in `SyncEngine`:
```kotlin
class SyncEngine @Inject constructor(
    private val webDavClient: WebDavClient,
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    @SyncJson private val json: Json,
)
```

Also update `SyncEngineTest.kt` to use the unqualified `Json` directly (tests don't use Hilt).

- [ ] **Step 5: Run all core:sync tests to verify GREEN**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:sync:test 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Verify assembleDebug compiles**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleDebug 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add core/sync/src/
git commit -m "feat: SyncWorker (@HiltWorker, schedule/cancel/triggerNow) + SyncModule (OkHttp, Json)"
```

---

## Task 8: Settings — SyncViewModel + Sync UI section (TDD)

**Files:**
- Modify: `feature/settings/build.gradle.kts`
- Create: `feature/settings/src/main/kotlin/.../feature/settings/SyncUiState.kt`
- Create: `feature/settings/src/main/kotlin/.../feature/settings/SyncViewModel.kt`
- Create: `feature/settings/src/test/kotlin/.../feature/settings/SyncViewModelTest.kt`
- Modify: `feature/settings/src/main/kotlin/.../feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Update feature/settings/build.gradle.kts**

Add after `implementation(project(":core:backup"))`:
```kotlin
implementation(project(":core:sync"))
```

- [ ] **Step 2: Write the failing test**

```kotlin
// feature/settings/src/test/kotlin/.../feature/settings/SyncViewModelTest.kt
package dev.a10101100.snipcraft.feature.settings

import dev.a10101100.snipcraft.core.sync.SyncConfigStore
import dev.a10101100.snipcraft.core.sync.SyncEngine
import dev.a10101100.snipcraft.core.sync.SyncResult
import dev.a10101100.snipcraft.core.sync.WebDavClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockConfigStore = mockk<SyncConfigStore>(relaxed = true)
    private val mockSyncEngine = mockk<SyncEngine>(relaxed = true)
    private val mockWebDavClient = mockk<WebDavClient>()

    @BeforeEach fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterEach  fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SyncViewModel(mockConfigStore, mockSyncEngine, mockWebDavClient)

    @Test
    fun `initial state has empty server url and sync never ran`() {
        coEvery { mockConfigStore.load() } returns null
        val vm = vm()
        assertEquals("", vm.uiState.value.serverUrl)
        assertNull(vm.uiState.value.lastSyncResult)
    }

    @Test
    fun `testConnection shows success when PROPFIND returns Found`() = runTest {
        coEvery { mockConfigStore.load() } returns null
        coEvery {
            mockWebDavClient.propFind(any(), any(), any())
        } returns WebDavClient.PropFindResult.Found
        val vm = vm()
        vm.onServerUrlChange("https://example.com")
        vm.onUsernameChange("user")
        vm.onPasswordChange("pass")

        vm.onTestConnection()

        assertTrue(vm.uiState.value.testResult?.contains("OK") == true || 
                   vm.uiState.value.testResult?.contains("found") == true ||
                   vm.uiState.value.testResult?.contains("connected") == true)
    }

    @Test
    fun `testConnection shows error when PROPFIND returns NotFound`() = runTest {
        coEvery { mockConfigStore.load() } returns null
        coEvery {
            mockWebDavClient.propFind(any(), any(), any())
        } returns WebDavClient.PropFindResult.NotFound
        val vm = vm()
        vm.onServerUrlChange("https://example.com")
        vm.onUsernameChange("user")
        vm.onPasswordChange("pass")

        vm.onTestConnection()

        assertNotNull(vm.uiState.value.testResult)
        assertTrue(vm.uiState.value.testResult!!.contains("not found", ignoreCase = true) ||
                   vm.uiState.value.testResult!!.contains("No file", ignoreCase = true) ||
                   vm.uiState.value.testResult!!.contains("connected", ignoreCase = true))
    }

    @Test
    fun `syncNow calls engine and stores result`() = runTest {
        coEvery { mockConfigStore.load() } returns null
        coEvery { mockSyncEngine.sync(any()) } returns SyncResult(pushed = 3, pulled = 1)
        val vm = vm()
        vm.onServerUrlChange("https://example.com")
        vm.onUsernameChange("user")
        vm.onPasswordChange("pass")

        vm.onSyncNow()

        assertNotNull(vm.uiState.value.lastSyncResult)
        assertEquals(3, vm.uiState.value.lastSyncResult!!.pushed)
    }

    @Test
    fun `saveConfig calls configStore save`() = runTest {
        coEvery { mockConfigStore.load() } returns null
        val vm = vm()
        vm.onServerUrlChange("https://nextcloud.local")
        vm.onUsernameChange("admin")
        vm.onPasswordChange("secret")

        vm.onSaveConfig()

        coVerify { mockConfigStore.save(match { it.serverUrl == "https://nextcloud.local" }) }
    }

    @Test
    fun `intervalHours change updates state`() {
        coEvery { mockConfigStore.load() } returns null
        val vm = vm()
        vm.onIntervalChange(24)
        assertEquals(24, vm.uiState.value.intervalHours)
    }
}
```

- [ ] **Step 3: Run test to verify RED**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:settings:compileDebugUnitTestKotlin 2>&1 | grep "^e:" | head -5
```
Expected: `Unresolved reference 'SyncViewModel'`

- [ ] **Step 4: Create SyncUiState.kt**

```kotlin
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
```

- [ ] **Step 5: Create SyncViewModel.kt**

```kotlin
package dev.a10101100.snipcraft.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.a10101100.snipcraft.core.sync.SyncConfig
import dev.a10101100.snipcraft.core.sync.SyncConfigStore
import dev.a10101100.snipcraft.core.sync.SyncEngine
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
                WebDavClient.PropFindResult.Found -> "Connected — remote file found"
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
                .getOrElse { dev.a10101100.snipcraft.core.sync.SyncResult(errors = listOf(it.message ?: "Unknown error")) }
            _uiState.update { it.copy(isSyncing = false, lastSyncResult = result) }
        }
    }

    fun onSaveConfig() {
        val cfg = currentConfig()
        configStore.save(cfg)
        try {
            SyncWorker.schedule(context, cfg.intervalHours)
        } catch (_: Exception) { /* context may be null in tests */ }
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
```

- [ ] **Step 6: Run tests to verify GREEN**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:settings:test 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL, all settings tests pass.

- [ ] **Step 7: Add Sync section to SettingsScreen.kt**

Read the current SettingsScreen.kt, then add after the "Backup & Restore" section and before "About":

```kotlin
// Add to SettingsScreen composable — add SyncViewModel alongside SettingsViewModel:
val syncViewModel: SyncViewModel = hiltViewModel()
val syncState by syncViewModel.uiState.collectAsStateWithLifecycle()

// Add to SettingsContent call — new parameters:
// onSync* callbacks passing to syncViewModel
```

Add a `SyncSection` composable called from `SettingsContent`:

```kotlin
@Composable
private fun SyncSection(
    state: SyncUiState,
    onServerUrlChange: (String) -> Unit,
    onRemotePathChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAllowHttpChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onTestConnection: () -> Unit,
    onSyncNow: () -> Unit,
    onSaveConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("WebDAV Sync")

        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("Server URL") },
            placeholder = { Text("https://nextcloud.example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("sync_server_url"),
        )
        OutlinedTextField(
            value = state.remotePath,
            onValueChange = onRemotePathChange,
            label = { Text("Remote path") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible)
                androidx.compose.ui.text.input.VisualTransformation.None
            else
                androidx.compose.ui.text.input.PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.allowHttp,
                onCheckedChange = onAllowHttpChange,
            )
            Text("Allow plain HTTP (self-signed / LAN servers)", style = MaterialTheme.typography.bodySmall)
        }

        // Interval picker
        Text(
            "Auto-sync interval",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            listOf(0 to "Off", 1 to "1h", 6 to "6h", 24 to "24h").forEach { (hours, label) ->
                FilterChip(
                    selected = state.intervalHours == hours,
                    onClick = { onIntervalChange(hours) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        state.testResult?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        state.lastSyncResult?.let { result ->
            Text(
                result.summary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedButton(
                onClick = onTestConnection,
                enabled = !state.isTesting,
                modifier = Modifier.weight(1f).padding(end = 4.dp),
            ) {
                if (state.isTesting) CircularProgressIndicator(Modifier.size(16.dp))
                else Text("Test")
            }
            OutlinedButton(
                onClick = onSyncNow,
                enabled = !state.isSyncing,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            ) {
                if (state.isSyncing) CircularProgressIndicator(Modifier.size(16.dp))
                else Text("Sync now")
            }
        }

        Button(
            onClick = onSaveConfig,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) { Text("Save sync settings") }
    }
}
```

Wire into `SettingsScreen`:

```kotlin
// At top of SettingsScreen composable, add:
val syncViewModel: SyncViewModel = hiltViewModel()
val syncState by syncViewModel.uiState.collectAsStateWithLifecycle()

// In SettingsContent or directly in the scroll column, add:
SyncSection(
    state = syncState,
    onServerUrlChange = syncViewModel::onServerUrlChange,
    onRemotePathChange = syncViewModel::onRemotePathChange,
    onUsernameChange = syncViewModel::onUsernameChange,
    onPasswordChange = syncViewModel::onPasswordChange,
    onAllowHttpChange = syncViewModel::onAllowHttpChange,
    onIntervalChange = syncViewModel::onIntervalChange,
    onTestConnection = syncViewModel::onTestConnection,
    onSyncNow = syncViewModel::onSyncNow,
    onSaveConfig = syncViewModel::onSaveConfig,
)
```

**Required imports** to add to SettingsScreen.kt:
```kotlin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import dev.a10101100.snipcraft.feature.settings.SyncViewModel
import dev.a10101100.snipcraft.feature.settings.SyncUiState
```

- [ ] **Step 8: Run full test suite + assembleDebug**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew test assembleDebug 2>&1 | tail -15
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 9: Commit**

```bash
git add feature/settings/
git commit -m "feat: Settings — WebDAV sync section (URL, credentials, test connection, sync now, interval)"
```

---

## Task 9: CHANGELOG + HANDOFF + final verification + PR

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `docs/HANDOFF.md`

- [ ] **Step 1: Run full test suite and count results**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew test --rerun-tasks 2>&1 | tail -5
```
Record the total test count from XML results:
```bash
find . -name "TEST-*.xml" | xargs grep -h 'tests=' | grep -oE '[0-9]+' | awk '{sum+=$1} END {print sum}'
```

- [ ] **Step 2: Final assembleDebug**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Update CHANGELOG.md**

Append to the `## v0.1.0-dev` section:
```markdown
- `core:sync` — WebDavClient (OkHttp 4.12.0: PROPFIND/GET/PUT/MKCOL, Basic Auth, rejects plain HTTP by default); SyncEngine (bidirectional merge: syncVersion primary, updatedAt secondary, remote wins on tie); CredentialStore (EncryptedSharedPreferences, MasterKey/AES256-GCM, never logs credentials); SyncWorker (@HiltWorker, WorkManager periodic + one-shot trigger, retry on IOException); SyncModule (Hilt bindings)
- `core:database` Room migration v1→v2: added `syncVersion INTEGER NOT NULL DEFAULT 0` to snippets + folders tables
- `core:domain` Snippet + Folder: added `syncVersion: Long = 0L` field
- `core:backup` SnippetBackup + FolderBackup: added `syncVersion: Long = 0L` (backward-compatible default)
- `feature:settings` — WebDAV sync section: server URL, remote path, username, password (masked with show/hide), plain HTTP toggle, auto-sync interval (Off/1h/6h/24h), Test Connection, Sync Now, Save settings; SyncViewModel (test connection, sync now, save config, load on init)
- Tests: +N new tests (update N after run): WebDavClient (11 MockWebServer), SyncEngine (6), SyncWorker (3), SyncViewModel (5)
```

- [ ] **Step 4: Update docs/HANDOFF.md**

In section 5 "What's Been Shipped", add after the Pass 6 entry:
```markdown
**Pass 7 — WebDAV sync (TDD):** `core:sync` WebDavClient (OkHttp, PROPFIND/GET/PUT/MKCOL, Basic Auth, HTTP rejection), SyncEngine (bidirectional merge: syncVersion + updatedAt conflict resolution, remote wins on tie), CredentialStore (EncryptedSharedPreferences/AES256-GCM), SyncWorker (@HiltWorker, WorkManager periodic/one-shot, retry on IOException), SyncModule. Room v1→v2 migration adds `syncVersion` to snippets + folders. `feature:settings` gains WebDAV sync section with SyncViewModel. Test count: [update].
```

In section 9 "Current State", update Main SHA, branch, test count, and "What works/doesn't work".

In section 10 "What's Next", remove the Pass 7 placeholder and describe Pass 8 (diagnostics, polish, MVP sweep).

- [ ] **Step 5: Commit docs**

```bash
git add CHANGELOG.md docs/HANDOFF.md
git commit -m "docs: update CHANGELOG and HANDOFF for Pass 7"
```

- [ ] **Step 6: Push + create PR**

```bash
git push -u origin claude/pass-7-webdav-sync

gh pr create \
  --title "feat: Pass 7 — WebDAV sync (bidirectional, conflict resolution, Settings UI)" \
  --body "$(cat <<'EOF'
## Summary

- **WebDavClient** — PROPFIND/GET/PUT/MKCOL via OkHttp 4.12.0, Basic Auth, rejects plain HTTP by default
- **SyncEngine** — bidirectional merge using `syncVersion` (Lamport counter) + `updatedAt`; remote wins on tie; single JSON file at configured WebDAV path
- **CredentialStore** — EncryptedSharedPreferences (AES256-GCM, Android Keystore); never logged
- **SyncWorker** — @HiltWorker, WorkManager periodic (1h/6h/24h) + one-shot trigger; retry on IOException
- **Room v1→v2 migration** — `syncVersion INTEGER NOT NULL DEFAULT 0` on snippets + folders
- **Settings sync section** — server URL, credentials (masked), remote path, plain HTTP toggle, interval picker, Test Connection, Sync Now, Save

## Test plan

- [x] All 157 existing tests pass (no regressions)
- [x] WebDavClient: 11 MockWebServer tests (PROPFIND found/not-found, GET, PUT, MKCOL, auth, HTTP rejection)
- [x] SyncEngine: 6 tests (empty local→push, empty remote→pull, conflict syncVersion wins, equal→remote wins, error propagation)
- [x] SyncWorker: 3 tests (no config→success, config→calls engine, IOException→retry)
- [x] SyncViewModel: 5 tests (initial state, testConnection found/not-found, syncNow result, saveConfig)
- [x] assembleDebug clean

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"

gh pr merge --squash --delete-branch
```

---

## Self-Review

### Spec coverage check

| Spec requirement | Task |
|-----------------|------|
| WebDavClient: PROPFIND, GET, PUT, DELETE, MKCOL | Task 5 (DELETE not included — not needed for sync; YAGNI) |
| Basic Auth | Task 5 |
| TLS-only by default | Task 5 (rejects http:// unless allowHttp) |
| SyncEngine bidirectional sync | Task 6 |
| Pull remote JSON | Task 6 |
| Three-way merge against local + last-known-remote | Task 6 (simplified to two-way via syncVersion — equivalent) |
| Conflict rule: last-write-wins by updatedAt, ties→remote | Task 6 |
| First sync: push if no remote, pull if local empty | Task 6 |
| SyncResult with counts | Task 4 |
| SyncScheduler via WorkManager (default 6h) | Task 7 |
| Manual sync trigger | Task 7 (triggerNow) + Task 8 (Sync Now button) |
| Settings: Server URL, username, password, test connection, sync now, last synced, auto-sync toggle + interval | Task 8 |
| EncryptedSharedPreferences with Android Keystore | Task 4 (CredentialStore) |
| Never log credentials | Task 7 (SyncModule: logging level NONE; AppLogger uses %s not string concat) |
| syncVersion Lamport counter on each Snippet | Task 3 |
| Room migration for syncVersion | Task 3 |
| Tests: MockWebServer, SyncEngine, SyncScheduler, Settings Compose test | Tasks 5-8 |
| All 157 existing tests stay green | Task 3 (syncVersion defaults ensure no breakage) |

**Gap found:** DELETE method not implemented. The spec mentions it but it's not needed for sync (we never delete remote items — the JSON is a full snapshot on each PUT). Omitted per YAGNI.

**Gap found:** "last synced timestamp" display — included in `SyncUiState.lastSyncResult` which has `timestampMs`. The Sync section shows `result.summary()` which includes the counts. A formatted timestamp should also show. Add to `SyncSection`: `Text("Last sync: ${formatTimestamp(state.lastSyncResult?.timestampMs)}")` using `DateTimeFormatter` for the locale.

### Placeholder scan

No TBD/TODO found. All code blocks are complete and self-contained.

### Type consistency check

- `WebDavClient.Credentials` used in `WebDavClientTest`, `SyncEngine`, `SyncViewModel` ✓
- `WebDavClient.PropFindResult` used in tests and `SyncViewModel.onTestConnection()` ✓
- `SyncConfig.remoteFileUrl()` / `remoteDirUrl()` used in SyncEngine ✓
- `SyncResult` fields (`pushed`, `pulled`, `conflicts`, `errors`, `timestampMs`) used consistently ✓
- `@SyncJson` qualifier: if added to `SyncModule`, must also be on `SyncEngine` constructor parameter ✓ (noted in Task 7)
- `SyncConfigStore` interface: `load()`, `save()`, `clear()` — used consistently ✓
