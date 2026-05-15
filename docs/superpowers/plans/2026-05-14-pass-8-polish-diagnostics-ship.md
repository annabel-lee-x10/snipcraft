# Pass 8 — Polish, Diagnostics, v0.1.0 Ship

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Diagnostics sub-screen (service health, expansion log, export), polish the app (icon, splash screen, strings, theme, accessibility), prep release (R8/ProGuard, CHANGELOG, README, keystore instructions), and ship v0.1.0.

**Architecture:** Working in worktree `D:\a10101100_labs\snipcraft\.claude\worktrees\serene-boyd-f3514f` on branch `claude/serene-boyd-f3514f` (rename to `claude/pass-8-polish-diagnostics-ship` in Task 1). Database migrates v1 → v2 to add `expansion_history` table. Diagnostics is a new fullscreen route (`DiagnosticsRoute`) navigated from the Settings screen (not a bottom-nav tab). AccessibilityService logs each expansion to Room. Release build uses R8 with keep rules committed to `app/proguard-rules.pro`; keystore instructions live in `docs/HANDOFF.md` (keystore itself is gitignored).

**Tech Stack:** Kotlin 2.3.20, AGP 9.1.0, Compose BOM 2026.05.00, Material 3, Hilt 2.59.2, Room 2.8.4 (migration), WorkManager 2.11.2 (flow API), kotlinx.serialization 1.8.1, `androidx.core:core-splashscreen:1.0.1` (new dep).

**Build command everywhere:**
```
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" ./gradlew ...
```
Run from the **worktree directory** `D:\a10101100_labs\snipcraft\.claude\worktrees\serene-boyd-f3514f`.

**Test target:** All 165+ tests green. Current baseline: 157 tests.

---

## File Map

| Action | File |
|---|---|
| Rename | branch `claude/serene-boyd-f3514f` → `claude/pass-8-polish-diagnostics-ship` |
| **DB layer** | |
| Create | `core/database/src/main/kotlin/.../database/ExpansionHistoryEntity.kt` |
| Create | `core/database/src/main/kotlin/.../database/ExpansionHistoryDao.kt` |
| Modify | `core/database/src/main/kotlin/.../database/SnipcraftDatabase.kt` (add entity, version=2, migration) |
| Modify | `core/database/src/main/kotlin/.../database/DatabaseModule.kt` (provide ExpansionHistoryDao, add migration) |
| Create | `core/database/src/test/kotlin/.../database/ExpansionHistoryDaoTest.kt` |
| **Domain** | |
| Modify | `core/domain/src/main/kotlin/.../domain/ExpansionEvent.kt` (new data class) |
| **Repository** | |
| Modify | `core/data/src/main/kotlin/.../data/ExpansionHistoryRepository.kt` (new interface + impl) |
| Modify | `core/data/src/main/kotlin/.../data/DataModule.kt` (bind ExpansionHistoryRepositoryImpl) |
| Create | `core/data/src/test/kotlin/.../data/ExpansionHistoryRepositoryTest.kt` |
| **Accessibility** | |
| Modify | `core/accessibility/src/main/kotlin/.../accessibility/SnipAccessibilityService.kt` (inject repo, log) |
| **Diagnostics feature** | |
| Create | `feature/diagnostics/src/main/kotlin/.../feature/diagnostics/DiagnosticsUiState.kt` |
| Create | `feature/diagnostics/src/main/kotlin/.../feature/diagnostics/DiagnosticsViewModel.kt` |
| Create | `feature/diagnostics/src/main/kotlin/.../feature/diagnostics/DiagnosticsScreen.kt` |
| Create | `feature/diagnostics/src/main/kotlin/.../feature/diagnostics/DiagnosticsExporter.kt` |
| Modify | `feature/diagnostics/build.gradle.kts` (add core:accessibility, core:database, workmanager deps) |
| Create | `feature/diagnostics/src/test/kotlin/.../feature/diagnostics/DiagnosticsViewModelTest.kt` |
| **Navigation** | |
| Modify | `app/src/main/kotlin/.../navigation/SnipNavHost.kt` (add DiagnosticsRoute composable) |
| Modify | `feature/settings/src/main/kotlin/.../feature/settings/SettingsScreen.kt` (add Diagnostics row + onDiagnostics callback) |
| **App icon** | |
| Modify | `app/src/main/res/drawable/ic_launcher_foreground.xml` (paper-snippet glyph) |
| **Splash screen** | |
| Modify | `gradle/libs.versions.toml` (add splashscreen version + library alias) |
| Modify | `app/build.gradle.kts` (add splashscreen dep + R8 release buildType) |
| Modify | `app/src/main/res/values/themes.xml` (add Theme.Snipcraft.Starting) |
| Modify | `app/src/main/AndroidManifest.xml` (MainActivity theme → Starting) |
| Modify | `app/src/main/kotlin/.../MainActivity.kt` (installSplashScreen()) |
| Create | `app/proguard-rules.pro` |
| **Polish** | |
| Modify | `app/src/main/res/values/strings.xml` (externalize all UI strings) |
| Modify | All feature screen files (use string resources, add contentDescription to all Icon calls) |
| **Release prep** | |
| Modify | `CHANGELOG.md` (roll [Unreleased] → ## v0.1.0 — 2026-05-14) |
| Modify | `README.md` (full project description, install steps, features, license) |
| Modify | `.gitignore` (add *.jks, *.keystore, keystore.properties) |
| Modify | `docs/HANDOFF.md` (Pass 8 section + keystore instructions + Phase 2+ pointer) |

**Package prefix for all source files:** `dev.a10101100.snipcraft`

---

## Task 1: Rename feature branch

**Files:** (git operation only)

- [ ] **Step 1: Rename the branch**

```bash
cd "D:\a10101100_labs\snipcraft\.claude\worktrees\serene-boyd-f3514f"
git branch -m claude/serene-boyd-f3514f claude/pass-8-polish-diagnostics-ship
```

Expected: no output, `git branch` now shows `claude/pass-8-polish-diagnostics-ship`.

- [ ] **Step 2: Verify**

```bash
git status
```

Expected: `On branch claude/pass-8-polish-diagnostics-ship`, nothing to commit.

---

## Task 2: ExpansionHistoryEntity + Room migration

**Files:**
- Create: `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/ExpansionHistoryEntity.kt`
- Create: `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/Migrations.kt`

- [ ] **Step 1: Create ExpansionHistoryEntity**

```kotlin
// core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/ExpansionHistoryEntity.kt
package dev.a10101100.snipcraft.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expansion_history")
data class ExpansionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shortcut: String,
    val packageName: String,
    val timestamp: Long,
    val success: Boolean,
    val errorReason: String?,
)
```

- [ ] **Step 2: Create Migrations.kt**

```kotlin
// core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/Migrations.kt
package dev.a10101100.snipcraft.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expansion_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shortcut` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `success` INTEGER NOT NULL,
                `errorReason` TEXT
            )
            """.trimIndent()
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/ExpansionHistoryEntity.kt
git add core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/Migrations.kt
git commit -m "feat: add ExpansionHistoryEntity + Room v1→v2 migration"
```

---

## Task 3: ExpansionHistoryDao + SnipcraftDatabase v2

**Files:**
- Create: `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/ExpansionHistoryDao.kt`
- Modify: `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/SnipcraftDatabase.kt`
- Modify: `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/DatabaseModule.kt`

- [ ] **Step 1: Write failing test (JUnit 5, no Robolectric needed — Room has in-memory builder)**

Create `core/database/src/test/kotlin/dev/a10101100/snipcraft/core/database/ExpansionHistoryDaoTest.kt`:

```kotlin
package dev.a10101100.snipcraft.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ExpansionHistoryDaoTest {

    private lateinit var db: SnipcraftDatabase
    private lateinit var dao: ExpansionHistoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SnipcraftDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.expansionHistoryDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `insert and observe last 50`() = runTest {
        val entity = ExpansionHistoryEntity(
            shortcut = ";today",
            packageName = "com.gmail.android",
            timestamp = 1000L,
            success = true,
            errorReason = null,
        )
        dao.insert(entity)
        val results = dao.observeLast50().first()
        assertEquals(1, results.size)
        assertEquals(";today", results[0].shortcut)
    }

    @Test
    fun `observeLast50 returns at most 50 rows ordered by timestamp desc`() = runTest {
        repeat(60) { i ->
            dao.insert(
                ExpansionHistoryEntity(
                    shortcut = ";s$i",
                    packageName = "pkg",
                    timestamp = i.toLong(),
                    success = true,
                    errorReason = null,
                )
            )
        }
        val results = dao.observeLast50().first()
        assertEquals(50, results.size)
        // Most recent first
        assertTrue(results[0].timestamp > results[1].timestamp)
    }

    @Test
    fun `insert failed expansion stores errorReason`() = runTest {
        dao.insert(
            ExpansionHistoryEntity(
                shortcut = ";test",
                packageName = "com.foo",
                timestamp = 999L,
                success = false,
                errorReason = "executor_failed",
            )
        )
        val results = dao.observeLast50().first()
        assertEquals("executor_failed", results[0].errorReason)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:database:test --tests "*.ExpansionHistoryDaoTest" 2>&1 | tail -20
```

Expected: FAIL — `ExpansionHistoryDao` not found / `expansionHistoryDao()` not found.

- [ ] **Step 3: Create ExpansionHistoryDao**

```kotlin
// core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/ExpansionHistoryDao.kt
package dev.a10101100.snipcraft.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpansionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpansionHistoryEntity)

    @Query("SELECT * FROM expansion_history ORDER BY timestamp DESC LIMIT 50")
    fun observeLast50(): Flow<List<ExpansionHistoryEntity>>
}
```

- [ ] **Step 4: Update SnipcraftDatabase to version 2**

Replace the entire `SnipcraftDatabase.kt`:

```kotlin
// core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/SnipcraftDatabase.kt
package dev.a10101100.snipcraft.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SnippetEntity::class,
        FolderEntity::class,
        CompatibilityRuleEntity::class,
        ExpansionHistoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SnipcraftDatabase : RoomDatabase() {
    abstract fun snippetDao(): SnippetDao
    abstract fun folderDao(): FolderDao
    abstract fun compatibilityRuleDao(): CompatibilityRuleDao
    abstract fun expansionHistoryDao(): ExpansionHistoryDao

    companion object {
        const val DATABASE_NAME = "snipcraft.db"
    }
}
```

- [ ] **Step 5: Update DatabaseModule to add migration + new DAO**

Replace the entire `DatabaseModule.kt`:

```kotlin
// core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/DatabaseModule.kt
package dev.a10101100.snipcraft.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SnipcraftDatabase =
        Room.databaseBuilder(context, SnipcraftDatabase::class.java, SnipcraftDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun provideSnippetDao(db: SnipcraftDatabase): SnippetDao = db.snippetDao()

    @Provides fun provideFolderDao(db: SnipcraftDatabase): FolderDao = db.folderDao()

    @Provides fun provideCompatibilityRuleDao(db: SnipcraftDatabase): CompatibilityRuleDao = db.compatibilityRuleDao()

    @Provides fun provideExpansionHistoryDao(db: SnipcraftDatabase): ExpansionHistoryDao = db.expansionHistoryDao()
}
```

- [ ] **Step 6: Add Robolectric test infrastructure to core:database build.gradle.kts**

Check `core/database/build.gradle.kts` — if it does NOT already have Robolectric dependencies, add:
```kotlin
testOptions { unitTests { isIncludeAndroidResources = true } }
// In dependencies:
testImplementation(libs.junit4)
testRuntimeOnly(libs.junit.vintage.engine)
testRuntimeOnly(libs.junit.platform.launcher)
testImplementation(libs.robolectric)
testImplementation(libs.androidx.test.core)
testImplementation(libs.kotlinx.coroutines.test)
```

Also create `core/database/src/test/resources/robolectric.properties` with content:
```
sdk=31
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:database:test --tests "*.ExpansionHistoryDaoTest" 2>&1 | tail -20
```

Expected: 3 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add core/database/
git commit -m "feat: ExpansionHistoryDao + SnipcraftDatabase v2 + 3 DAO tests"
```

---

## Task 4: ExpansionEvent domain model + ExpansionHistoryRepository

**Files:**
- Create: `core/domain/src/main/kotlin/dev/a10101100/snipcraft/core/domain/ExpansionEvent.kt`
- Create: `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/ExpansionHistoryRepository.kt`
- Modify: `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/DataModule.kt`

- [ ] **Step 1: Create ExpansionEvent domain model**

```kotlin
// core/domain/src/main/kotlin/dev/a10101100/snipcraft/core/domain/ExpansionEvent.kt
package dev.a10101100.snipcraft.core.domain

data class ExpansionEvent(
    val id: Long,
    val shortcut: String,
    val packageName: String,
    val timestamp: Long,
    val success: Boolean,
    val errorReason: String?,
)
```

- [ ] **Step 2: Write failing repository tests**

Create `core/data/src/test/kotlin/dev/a10101100/snipcraft/core/data/ExpansionHistoryRepositoryTest.kt`:

```kotlin
package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.ExpansionHistoryDao
import dev.a10101100.snipcraft.core.database.ExpansionHistoryEntity
import dev.a10101100.snipcraft.core.domain.ExpansionEvent
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExpansionHistoryRepositoryTest {

    private val dao = mockk<ExpansionHistoryDao>(relaxed = true)
    private lateinit var repo: ExpansionHistoryRepository

    @BeforeEach
    fun setUp() {
        repo = ExpansionHistoryRepositoryImpl(dao)
    }

    @Test
    fun `logExpansion inserts entity via dao`() = runTest {
        repo.logExpansion(";today", "com.gmail.android", 1000L, true, null)
        coVerify {
            dao.insert(
                match {
                    it.shortcut == ";today" &&
                    it.packageName == "com.gmail.android" &&
                    it.timestamp == 1000L &&
                    it.success &&
                    it.errorReason == null
                }
            )
        }
    }

    @Test
    fun `observeRecent maps entity to domain model`() = runTest {
        val entity = ExpansionHistoryEntity(
            id = 1L,
            shortcut = ";sig",
            packageName = "com.slack",
            timestamp = 2000L,
            success = true,
            errorReason = null,
        )
        every { dao.observeLast50() } returns flowOf(listOf(entity))

        val results = repo.observeRecent().first()
        assertEquals(1, results.size)
        assertEquals(ExpansionEvent(1L, ";sig", "com.slack", 2000L, true, null), results[0])
    }

    @Test
    fun `logExpansion with failure stores errorReason`() = runTest {
        repo.logExpansion(";bad", "pkg", 3000L, false, "executor_failed")
        coVerify {
            dao.insert(match { !it.success && it.errorReason == "executor_failed" })
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:data:test --tests "*.ExpansionHistoryRepositoryTest" 2>&1 | tail -20
```

Expected: FAIL — `ExpansionHistoryRepository` not found.

- [ ] **Step 4: Create the repository interface and implementation**

```kotlin
// core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/ExpansionHistoryRepository.kt
package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.ExpansionHistoryDao
import dev.a10101100.snipcraft.core.database.ExpansionHistoryEntity
import dev.a10101100.snipcraft.core.domain.ExpansionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ExpansionHistoryRepository {
    suspend fun logExpansion(
        shortcut: String,
        packageName: String,
        timestamp: Long,
        success: Boolean,
        errorReason: String?,
    )
    fun observeRecent(): Flow<List<ExpansionEvent>>
}

class ExpansionHistoryRepositoryImpl @Inject constructor(
    private val dao: ExpansionHistoryDao,
) : ExpansionHistoryRepository {

    override suspend fun logExpansion(
        shortcut: String,
        packageName: String,
        timestamp: Long,
        success: Boolean,
        errorReason: String?,
    ) {
        dao.insert(
            ExpansionHistoryEntity(
                shortcut = shortcut,
                packageName = packageName,
                timestamp = timestamp,
                success = success,
                errorReason = errorReason,
            )
        )
    }

    override fun observeRecent(): Flow<List<ExpansionEvent>> =
        dao.observeLast50().map { entities ->
            entities.map { e ->
                ExpansionEvent(
                    id = e.id,
                    shortcut = e.shortcut,
                    packageName = e.packageName,
                    timestamp = e.timestamp,
                    success = e.success,
                    errorReason = e.errorReason,
                )
            }
        }
}
```

- [ ] **Step 5: Bind in DataModule**

Open `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/DataModule.kt` and add:

```kotlin
@Binds
@Singleton
abstract fun bindExpansionHistoryRepository(
    impl: ExpansionHistoryRepositoryImpl
): ExpansionHistoryRepository
```

(Add this abstract function inside the existing `@Module @InstallIn(SingletonComponent::class) abstract class DataModule` — check the existing pattern in that file and follow it. If DataModule is an `object`, convert the new binding to a separate `@Module abstract class` companion or add a separate module.)

> **Note on DataModule pattern:** Examine the existing `DataModule.kt` — if it is an `abstract class` with `@Binds`, add the new function directly. If it is an `object` with `@Provides`, add a separate abstract class `DataBindsModule` in the same file to host the new `@Binds` function.

- [ ] **Step 6: Run tests to verify pass**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:data:test --tests "*.ExpansionHistoryRepositoryTest" 2>&1 | tail -20
```

Expected: 3 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add core/domain/ core/data/
git commit -m "feat: ExpansionEvent domain model + ExpansionHistoryRepository (3 tests)"
```

---

## Task 5: Wire expansion logging in SnipAccessibilityService

**Files:**
- Modify: `core/accessibility/src/main/kotlin/dev/a10101100/snipcraft/core/accessibility/SnipAccessibilityService.kt`

The service already injects `snippetRepository`. Add `expansionHistoryRepository` the same way.

- [ ] **Step 1: Add injection and logging**

In `SnipAccessibilityService.kt`, add the new inject field after the existing ones:

```kotlin
@Inject lateinit var expansionHistoryRepository: ExpansionHistoryRepository
```

And in the `onAccessibilityEvent` coroutine, after the `val ok = executor.execute(...)` line, replace the current log block:

```kotlin
// Replace this:
if (ok) {
    snippetRepository.incrementUsage(snippet.id, System.currentTimeMillis())
    AppLogger.i("Expanded '%s' → '%s' in %s", shortcut, expandedText, snapshot.packageName)
}

// With this:
val now = System.currentTimeMillis()
if (ok) {
    snippetRepository.incrementUsage(snippet.id, now)
    AppLogger.i("Expanded '%s' in %s", shortcut, snapshot.packageName)
}
serviceScope.launch {
    expansionHistoryRepository.logExpansion(
        shortcut = shortcut,
        packageName = snapshot.packageName,
        timestamp = now,
        success = ok,
        errorReason = if (ok) null else "executor_failed",
    )
}
```

Also add the import at the top of the file:
```kotlin
import dev.a10101100.snipcraft.core.data.ExpansionHistoryRepository
```

- [ ] **Step 2: Verify the full test suite still compiles and passes**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:accessibility:test 2>&1 | tail -20
```

Expected: All accessibility module tests pass (no new tests added here — the existing tests use mocked service).

- [ ] **Step 3: Commit**

```bash
git add core/accessibility/src/main/kotlin/dev/a10101100/snipcraft/core/accessibility/SnipAccessibilityService.kt
git commit -m "feat: log each expansion attempt to ExpansionHistoryRepository"
```

---

## Task 6: DiagnosticsViewModel (TDD)

**Files:**
- Create: `feature/diagnostics/src/main/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsUiState.kt`
- Create: `feature/diagnostics/src/main/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsViewModel.kt`
- Create: `feature/diagnostics/src/test/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsViewModelTest.kt`

First update `feature/diagnostics/build.gradle.kts` to add the needed dependencies.

- [ ] **Step 1: Update feature:diagnostics build.gradle.kts**

Replace the existing file content with:

```kotlin
plugins {
    id("snipcraft.android.library.compose")
    id("snipcraft.android.hilt")
}

android {
    namespace = "dev.a10101100.snipcraft.feature.diagnostics"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:accessibility"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.workmanager.ktx)
    implementation(libs.kotlinx.serialization.json)
    api(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
```

- [ ] **Step 2: Create DiagnosticsUiState**

```kotlin
// feature/diagnostics/src/main/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsUiState.kt
package dev.a10101100.snipcraft.feature.diagnostics

import dev.a10101100.snipcraft.core.domain.ExpansionEvent

data class DiagnosticsUiState(
    val accessibilityEnabled: Boolean = false,
    val foregroundServiceRunning: Boolean = false,
    val watchdogScheduled: Boolean = false,
    val batteryExemptionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val currentImePackage: String = "",
    val recentExpansions: List<ExpansionEvent> = emptyList(),
    val isExportingDiagnostics: Boolean = false,
)

sealed class DiagnosticsEvent {
    data class ShareDiagnostics(val json: String) : DiagnosticsEvent()
}
```

- [ ] **Step 3: Write failing ViewModel tests**

```kotlin
// feature/diagnostics/src/test/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsViewModelTest.kt
package dev.a10101100.snipcraft.feature.diagnostics

import app.cash.turbine.test
import dev.a10101100.snipcraft.core.data.ExpansionHistoryRepository
import dev.a10101100.snipcraft.core.domain.ExpansionEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    private val historyRepo = mockk<ExpansionHistoryRepository>(relaxed = true)

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun buildVm(
        accessibilityEnabled: Boolean = false,
        foregroundRunning: Boolean = false,
        watchdogScheduled: Boolean = false,
        batteryExempt: Boolean = false,
        notificationsEnabled: Boolean = true,
        imePackage: String = "",
    ) = DiagnosticsViewModel(
        historyRepository = historyRepo,
        isAccessibilityEnabled = { accessibilityEnabled },
        isForegroundRunning = { foregroundRunning },
        isWatchdogScheduled = { watchdogScheduled },
        isBatteryExempt = { batteryExempt },
        areNotificationsEnabled = { notificationsEnabled },
        currentImePackage = { imePackage },
    )

    @Test
    fun `initial state reflects service checks`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = buildVm(accessibilityEnabled = true, batteryExempt = true)
        vm.uiState.test {
            val state = awaitItem()
            assert(state.accessibilityEnabled)
            assert(state.batteryExemptionGranted)
            assertFalse(state.foregroundServiceRunning)
        }
    }

    @Test
    fun `recent expansions are surfaced in state`() = runTest {
        val events = listOf(
            ExpansionEvent(1L, ";today", "com.gmail.android", 1000L, true, null),
        )
        every { historyRepo.observeRecent() } returns flowOf(events)
        val vm = buildVm()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.recentExpansions.size)
            assertEquals(";today", state.recentExpansions[0].shortcut)
        }
    }

    @Test
    fun `ime package is surfaced in state`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = buildVm(imePackage = "com.google.android.inputmethod.latin")
        vm.uiState.test {
            assertEquals("com.google.android.inputmethod.latin", awaitItem().currentImePackage)
        }
    }

    @Test
    fun `exportDiagnostics emits ShareDiagnostics event`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = buildVm()
        vm.events.test {
            vm.exportDiagnostics()
            val event = awaitItem()
            assert(event is DiagnosticsEvent.ShareDiagnostics)
            assert((event as DiagnosticsEvent.ShareDiagnostics).json.contains("diagnostics"))
        }
    }

    @Test
    fun `triggerWatchdog delegates to watchdog scheduler without crashing`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        var triggered = false
        val vm = DiagnosticsViewModel(
            historyRepository = historyRepo,
            isAccessibilityEnabled = { false },
            isForegroundRunning = { false },
            isWatchdogScheduled = { false },
            isBatteryExempt = { false },
            areNotificationsEnabled = { true },
            currentImePackage = { "" },
            onTriggerWatchdog = { triggered = true },
        )
        vm.triggerWatchdog()
        assert(triggered)
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:diagnostics:test --tests "*.DiagnosticsViewModelTest" 2>&1 | tail -20
```

Expected: FAIL — `DiagnosticsViewModel` not found.

- [ ] **Step 5: Implement DiagnosticsViewModel**

```kotlin
// feature/diagnostics/src/main/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsViewModel.kt
package dev.a10101100.snipcraft.feature.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.POWER_SERVICE
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.a10101100.snipcraft.core.accessibility.HealthWatchdogWorker
import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import dev.a10101100.snipcraft.core.accessibility.SnipForegroundService
import dev.a10101100.snipcraft.core.data.ExpansionHistoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyRepository: ExpansionHistoryRepository,
) : ViewModel() {

    // Lambdas extracted for testability — overridable in tests via secondary constructor
    internal var isAccessibilityEnabled: () -> Boolean = {
        ServiceHealthChecker(context).isServiceEnabled()
    }
    internal var isForegroundRunning: () -> Boolean = {
        @Suppress("DEPRECATION")
        (context.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(100)
            .any { it.service.className == SnipForegroundService::class.java.name }
    }
    internal var isWatchdogScheduled: () -> Boolean = {
        try {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork("snipcraft_health_watchdog")
                .get()
                .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        } catch (e: Exception) { false }
    }
    internal var isBatteryExempt: () -> Boolean = {
        (context.getSystemService(POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)
    }
    internal var areNotificationsEnabled: () -> Boolean = {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    internal var currentImePackage: () -> String = {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore("/") ?: ""
    }
    internal var onTriggerWatchdog: () -> Unit = {
        HealthWatchdogWorker.schedule(context)
    }

    private val _events = Channel<DiagnosticsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState = combine(
        historyRepository.observeRecent(),
    ) { (expansions) ->
        DiagnosticsUiState(
            accessibilityEnabled = isAccessibilityEnabled(),
            foregroundServiceRunning = isForegroundRunning(),
            watchdogScheduled = isWatchdogScheduled(),
            batteryExemptionGranted = isBatteryExempt(),
            notificationPermissionGranted = areNotificationsEnabled(),
            currentImePackage = currentImePackage(),
            recentExpansions = expansions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiagnosticsUiState(
            accessibilityEnabled = isAccessibilityEnabled(),
            foregroundServiceRunning = isForegroundRunning(),
            watchdogScheduled = isWatchdogScheduled(),
            batteryExemptionGranted = isBatteryExempt(),
            notificationPermissionGranted = areNotificationsEnabled(),
            currentImePackage = currentImePackage(),
        ),
    )

    fun triggerWatchdog() {
        onTriggerWatchdog()
    }

    fun exportDiagnostics() {
        viewModelScope.launch {
            val state = uiState.value
            val json = Json { prettyPrint = true }.encodeToString(buildDiagnosticsBundle(state))
            _events.send(DiagnosticsEvent.ShareDiagnostics(json))
        }
    }

    private fun buildDiagnosticsBundle(state: DiagnosticsUiState): String {
        val obj = buildJsonObject {
            put("exportedAt", System.currentTimeMillis())
            put("appVersion", "0.1.0")
            put("deviceModel", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            put("androidVersion", android.os.Build.VERSION.SDK_INT)
            putJsonObject("diagnostics") {
                put("accessibilityEnabled", state.accessibilityEnabled)
                put("foregroundServiceRunning", state.foregroundServiceRunning)
                put("watchdogScheduled", state.watchdogScheduled)
                put("batteryExemptionGranted", state.batteryExemptionGranted)
                put("notificationPermissionGranted", state.notificationPermissionGranted)
                put("currentIme", state.currentImePackage)
            }
            putJsonArray("recentExpansions") {
                state.recentExpansions.take(50).forEach { event ->
                    add(buildJsonObject {
                        put("shortcut", event.shortcut)
                        put("packageName", event.packageName)
                        put("timestamp", event.timestamp)
                        put("success", event.success)
                        event.errorReason?.let { put("errorReason", it) }
                    })
                }
            }
        }
        return Json { prettyPrint = true }.encodeToString(obj)
    }
}

// Secondary constructor for tests — bypasses Android context dependencies
internal fun DiagnosticsViewModel(
    historyRepository: ExpansionHistoryRepository,
    isAccessibilityEnabled: () -> Boolean,
    isForegroundRunning: () -> Boolean,
    isWatchdogScheduled: () -> Boolean,
    isBatteryExempt: () -> Boolean,
    areNotificationsEnabled: () -> Boolean,
    currentImePackage: () -> String,
    onTriggerWatchdog: () -> Unit = {},
): DiagnosticsViewModel {
    // We need a minimal Context stub — use a reflective approach
    // Actually for pure-JVM tests, we create a subclass with the lambdas already wired:
    throw UnsupportedOperationException("Use the test-specific constructor approach below")
}
```

> **Note on test constructor:** The test needs to avoid Android context. The cleanest approach is to make the ViewModel's Android-specific dependencies into injectable lambdas that are overridable. Since the production `@HiltViewModel` constructor takes `@ApplicationContext context`, the tests set the lambdas via property injection AFTER construction — but that requires an actual ViewModel instance. Instead, restructure the ViewModel slightly:
>
> **Revised approach:** Extract all Android checks into a `DiagnosticsChecker` interface injected into the ViewModel, with a production `AndroidDiagnosticsChecker` Hilt impl and a test `FakeDiagnosticsChecker`. This is cleaner for testing.

Restructure as follows:

```kotlin
// DiagnosticsChecker.kt — new file in feature:diagnostics
package dev.a10101100.snipcraft.feature.diagnostics

interface DiagnosticsChecker {
    fun isAccessibilityEnabled(): Boolean
    fun isForegroundRunning(): Boolean
    fun isWatchdogScheduled(): Boolean
    fun isBatteryExempt(): Boolean
    fun areNotificationsEnabled(): Boolean
    fun currentImePackage(): String
    fun triggerWatchdog()
}
```

```kotlin
// AndroidDiagnosticsChecker.kt — production impl, @Inject constructor
package dev.a10101100.snipcraft.feature.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.a10101100.snipcraft.core.accessibility.HealthWatchdogWorker
import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import dev.a10101100.snipcraft.core.accessibility.SnipForegroundService
import javax.inject.Inject

class AndroidDiagnosticsChecker @Inject constructor(
    private val context: Context,
) : DiagnosticsChecker {

    override fun isAccessibilityEnabled() =
        ServiceHealthChecker(context).isServiceEnabled()

    override fun isForegroundRunning(): Boolean {
        @Suppress("DEPRECATION")
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(100)
            .any { it.service.className == SnipForegroundService::class.java.name }
    }

    override fun isWatchdogScheduled(): Boolean = try {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("snipcraft_health_watchdog")
            .get()
            .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    } catch (e: Exception) { false }

    override fun isBatteryExempt(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)

    override fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun currentImePackage(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore("/") ?: ""

    override fun triggerWatchdog() = HealthWatchdogWorker.schedule(context)
}
```

Add a `DiagnosticsModule.kt` to bind the checker:

```kotlin
// feature/diagnostics/src/main/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsModule.kt
package dev.a10101100.snipcraft.feature.diagnostics

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsModule {
    @Provides
    @Singleton
    fun provideChecker(@ApplicationContext context: Context): DiagnosticsChecker =
        AndroidDiagnosticsChecker(context)
}
```

Now rewrite `DiagnosticsViewModel.kt` to use the checker:

```kotlin
// feature/diagnostics/src/main/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsViewModel.kt
package dev.a10101100.snipcraft.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.a10101100.snipcraft.core.data.ExpansionHistoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val checker: DiagnosticsChecker,
    private val historyRepository: ExpansionHistoryRepository,
) : ViewModel() {

    private val _events = Channel<DiagnosticsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState = combine(
        historyRepository.observeRecent(),
    ) { (expansions) ->
        DiagnosticsUiState(
            accessibilityEnabled = checker.isAccessibilityEnabled(),
            foregroundServiceRunning = checker.isForegroundRunning(),
            watchdogScheduled = checker.isWatchdogScheduled(),
            batteryExemptionGranted = checker.isBatteryExempt(),
            notificationPermissionGranted = checker.areNotificationsEnabled(),
            currentImePackage = checker.currentImePackage(),
            recentExpansions = expansions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiagnosticsUiState(
            accessibilityEnabled = checker.isAccessibilityEnabled(),
            foregroundServiceRunning = checker.isForegroundRunning(),
            watchdogScheduled = checker.isWatchdogScheduled(),
            batteryExemptionGranted = checker.isBatteryExempt(),
            notificationPermissionGranted = checker.areNotificationsEnabled(),
            currentImePackage = checker.currentImePackage(),
        ),
    )

    fun triggerWatchdog() {
        checker.triggerWatchdog()
    }

    fun exportDiagnostics() {
        viewModelScope.launch {
            val state = uiState.value
            val json = buildDiagnosticsJson(state)
            _events.send(DiagnosticsEvent.ShareDiagnostics(json))
        }
    }

    private fun buildDiagnosticsJson(state: DiagnosticsUiState): String {
        val obj = buildJsonObject {
            put("exportedAt", System.currentTimeMillis())
            put("appVersion", "0.1.0")
            put("deviceModel", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            put("androidVersion", android.os.Build.VERSION.SDK_INT)
            putJsonObject("diagnostics") {
                put("accessibilityEnabled", state.accessibilityEnabled)
                put("foregroundServiceRunning", state.foregroundServiceRunning)
                put("watchdogScheduled", state.watchdogScheduled)
                put("batteryExemptionGranted", state.batteryExemptionGranted)
                put("notificationPermissionGranted", state.notificationPermissionGranted)
                put("currentIme", state.currentImePackage)
            }
            putJsonArray("recentExpansions") {
                state.recentExpansions.take(50).forEach { event ->
                    add(buildJsonObject {
                        put("shortcut", event.shortcut)
                        put("packageName", event.packageName)
                        put("timestamp", event.timestamp)
                        put("success", event.success)
                        event.errorReason?.let { put("errorReason", it) }
                    })
                }
            }
        }
        return Json { prettyPrint = true }.encodeToString(obj)
    }
}
```

And rewrite the tests to use a `FakeDiagnosticsChecker`:

```kotlin
// DiagnosticsViewModelTest.kt (final version)
package dev.a10101100.snipcraft.feature.diagnostics

import app.cash.turbine.test
import dev.a10101100.snipcraft.core.data.ExpansionHistoryRepository
import dev.a10101100.snipcraft.core.domain.ExpansionEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    private val historyRepo = mockk<ExpansionHistoryRepository>(relaxed = true)

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun fakeChecker(
        accessibilityEnabled: Boolean = false,
        foregroundRunning: Boolean = false,
        watchdogScheduled: Boolean = false,
        batteryExempt: Boolean = false,
        notificationsEnabled: Boolean = true,
        imePackage: String = "",
        onWatchdogTriggered: () -> Unit = {},
    ) = object : DiagnosticsChecker {
        override fun isAccessibilityEnabled() = accessibilityEnabled
        override fun isForegroundRunning() = foregroundRunning
        override fun isWatchdogScheduled() = watchdogScheduled
        override fun isBatteryExempt() = batteryExempt
        override fun areNotificationsEnabled() = notificationsEnabled
        override fun currentImePackage() = imePackage
        override fun triggerWatchdog() = onWatchdogTriggered()
    }

    @Test
    fun `initial state reflects service checks`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = DiagnosticsViewModel(
            checker = fakeChecker(accessibilityEnabled = true, batteryExempt = true),
            historyRepository = historyRepo,
        )
        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.accessibilityEnabled)
            assertTrue(state.batteryExemptionGranted)
            assertFalse(state.foregroundServiceRunning)
        }
    }

    @Test
    fun `recent expansions are surfaced in state`() = runTest {
        val events = listOf(ExpansionEvent(1L, ";today", "com.gmail.android", 1000L, true, null))
        every { historyRepo.observeRecent() } returns flowOf(events)
        val vm = DiagnosticsViewModel(checker = fakeChecker(), historyRepository = historyRepo)
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.recentExpansions.size)
            assertEquals(";today", state.recentExpansions[0].shortcut)
        }
    }

    @Test
    fun `ime package is surfaced in state`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = DiagnosticsViewModel(
            checker = fakeChecker(imePackage = "com.google.android.inputmethod.latin"),
            historyRepository = historyRepo,
        )
        vm.uiState.test {
            assertEquals("com.google.android.inputmethod.latin", awaitItem().currentImePackage)
        }
    }

    @Test
    fun `exportDiagnostics emits ShareDiagnostics event with json`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = DiagnosticsViewModel(checker = fakeChecker(), historyRepository = historyRepo)
        vm.events.test {
            vm.exportDiagnostics()
            val event = awaitItem() as DiagnosticsEvent.ShareDiagnostics
            assertTrue(event.json.contains("diagnostics"))
            assertTrue(event.json.contains("appVersion"))
        }
    }

    @Test
    fun `triggerWatchdog delegates to checker`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        var triggered = false
        val vm = DiagnosticsViewModel(
            checker = fakeChecker(onWatchdogTriggered = { triggered = true }),
            historyRepository = historyRepo,
        )
        vm.triggerWatchdog()
        assertTrue(triggered)
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:diagnostics:test --tests "*.DiagnosticsViewModelTest" 2>&1 | tail -20
```

Expected: 5 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add feature/diagnostics/
git commit -m "feat: DiagnosticsViewModel + DiagnosticsChecker interface (5 tests)"
```

---

## Task 7: DiagnosticsScreen

**Files:**
- Create: `feature/diagnostics/src/main/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsScreen.kt`

- [ ] **Step 1: Create DiagnosticsScreen**

```kotlin
// feature/diagnostics/src/main/kotlin/dev/a10101100/snipcraft/feature/diagnostics/DiagnosticsScreen.kt
package dev.a10101100.snipcraft.feature.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
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
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
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
            Text(if (ok) "Active" else "Inactive", color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
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
                contentDescription = if (event.success) "Expansion succeeded" else "Expansion failed: ${event.errorReason}",
                tint = if (event.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}
```

- [ ] **Step 2: Verify module compiles**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:diagnostics:assembleDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add feature/diagnostics/src/main/kotlin/
git commit -m "feat: DiagnosticsScreen with status pills, IME display, test field, expansion log"
```

---

## Task 8: Navigation — DiagnosticsRoute + Settings row

**Files:**
- Modify: `app/src/main/kotlin/dev/a10101100/snipcraft/navigation/SnipNavHost.kt`
- Modify: `feature/settings/src/main/kotlin/dev/a10101100/snipcraft/feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Add DiagnosticsRoute to SnipNavHost**

In `SnipNavHost.kt`, add the new route object near the other `@Serializable` objects:

```kotlin
@Serializable
object DiagnosticsRoute
```

Add the composable inside the `NavHost { ... }` block (after the `SettingsRoute` composable):

```kotlin
composable<DiagnosticsRoute> {
    DiagnosticsScreen(onBack = { navController.popBackStack() })
}
```

Add the import at the top:
```kotlin
import dev.a10101100.snipcraft.feature.diagnostics.DiagnosticsScreen
```

- [ ] **Step 2: Add "Diagnostics" nav row to SettingsScreen**

In `SettingsScreen.kt`, add `onDiagnostics: () -> Unit` parameter to both `SettingsScreen` and `SettingsContent` composables.

Inside `SettingsContent`, add a new section BEFORE the "About" section:

```kotlin
Spacer(Modifier.height(8.dp))
HorizontalDivider()

SectionHeader("Diagnostics")
ListItem(
    headlineContent = { Text("Compatibility & diagnostics") },
    supportingContent = { Text("Service health, expansion log, debug export") },
    trailingContent = {
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open diagnostics screen",
        )
    },
    modifier = Modifier.clickable { onDiagnostics() },
)
```

Add the import:
```kotlin
import androidx.compose.material.icons.automirrored.filled.ArrowForward
```

In `SnipNavHost.kt`, update the `SettingsScreen` call to pass the new callback:

```kotlin
composable<SettingsRoute> {
    SettingsScreen(
        onBack = { navController.popBackStack() },
        onDiagnostics = { navController.navigate(DiagnosticsRoute) },
    )
}
```

Also update `SettingsPreview` in `SettingsScreen.kt` to pass `onDiagnostics = {}`.

- [ ] **Step 3: Verify app compiles**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :app:assembleDebug 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all tests**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew test 2>&1 | tail -30
```

Expected: All tests pass. If SettingsViewModel tests reference `SettingsContent` signature, update test stubs.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/ feature/settings/
git commit -m "feat: wire DiagnosticsRoute into NavHost, add Diagnostics row in Settings"
```

---

## Task 9: App icon — paper-snippet glyph

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`

The current foreground is a plain white circle. Replace with a paper/snippet document icon.

- [ ] **Step 1: Replace ic_launcher_foreground.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Paper body with clipped top-right corner -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M27,22 L68,22 L81,35 L81,86 L27,86 Z" />
    <!-- Folded corner triangle -->
    <path
        android:fillColor="#D0B4E8"
        android:pathData="M68,22 L68,35 L81,35 Z" />
    <!-- Text line 1 -->
    <path
        android:strokeColor="#6750A4"
        android:strokeWidth="4"
        android:strokeLineCap="round"
        android:pathData="M36,48 L72,48" />
    <!-- Text line 2 -->
    <path
        android:strokeColor="#6750A4"
        android:strokeWidth="4"
        android:strokeLineCap="round"
        android:pathData="M36,58 L72,58" />
    <!-- Text line 3 (shorter, indicates end of text) -->
    <path
        android:strokeColor="#6750A4"
        android:strokeWidth="4"
        android:strokeLineCap="round"
        android:pathData="M36,68 L56,68" />
</vector>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/drawable/ic_launcher_foreground.xml
git commit -m "polish: paper-snippet adaptive icon foreground"
```

---

## Task 10: Splash screen

**Files:**
- Modify: `gradle/libs.versions.toml` (add splashscreen)
- Modify: `app/build.gradle.kts` (add splashscreen dep + R8 release config)
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/AndroidManifest.xml` (MainActivity theme)
- Modify: `app/src/main/kotlin/dev/a10101100/snipcraft/MainActivity.kt`

- [ ] **Step 1: Add splashscreen to version catalog**

In `gradle/libs.versions.toml`, add in the `[versions]` section:
```toml
splashscreen = "1.0.1"
```

And in `[libraries]`:
```toml
androidx-core-splashscreen = { module = "androidx.core:core-splashscreen", version.ref = "splashscreen" }
```

- [ ] **Step 2: Add splashscreen dep + release buildType to app/build.gradle.kts**

In `app/build.gradle.kts`, add inside the `android { }` block:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

And add in `dependencies`:
```kotlin
implementation(libs.androidx.core.splashscreen)
```

- [ ] **Step 3: Update themes.xml**

Replace the entire file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Base application theme — Compose handles Material You theming at runtime -->
    <style name="Theme.Snipcraft" parent="android:Theme.Material.Light.NoActionBar" />

    <!-- Splash screen theme — used only for the instant startup window -->
    <style name="Theme.Snipcraft.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">#1C1B1F</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
        <item name="windowSplashScreenIconBackgroundColor">#6750A4</item>
        <item name="postSplashScreenTheme">@style/Theme.Snipcraft</item>
    </style>
</resources>
```

- [ ] **Step 4: Update AndroidManifest.xml — MainActivity theme**

In `app/src/main/AndroidManifest.xml`, change the `<activity>` element:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.Snipcraft.Starting"
    android:windowSoftInputMode="adjustResize">
```

- [ ] **Step 5: Call installSplashScreen() in MainActivity**

In `MainActivity.kt`, add `installSplashScreen()` before `super.onCreate()` and add the import:

```kotlin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    startForegroundCompanion()
    setContent {
        SnipTheme {
            SnipNavHost()
        }
    }
}
```

- [ ] **Step 6: Verify assembleDebug**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :app:assembleDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/res/values/themes.xml app/src/main/AndroidManifest.xml app/src/main/kotlin/dev/a10101100/snipcraft/MainActivity.kt
git commit -m "polish: splash screen via androidx.core.splashscreen"
```

---

## Task 11: ProGuard/R8 keep rules

**Files:**
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: Create proguard-rules.pro**

```
# Room — entity field names map to DB column names; obfuscating them breaks the DB.
-keep class dev.a10101100.snipcraft.core.database.** { *; }

# kotlinx.serialization — @Serializable classes used for JSON backup and diagnostics export.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * { *; }

# WorkManager — worker class names are stored in WorkRequest and must survive R8.
-keep class dev.a10101100.snipcraft.core.accessibility.HealthWatchdogWorker { *; }

# Hilt — generated component and factory class names must be preserved.
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class **_HiltComponents* { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }

# Timber
-dontwarn org.jetbrains.annotations.**
```

- [ ] **Step 2: Verify assembleRelease compiles**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :app:assembleRelease 2>&1 | tail -40
```

Expected: BUILD SUCCESSFUL (will produce an unsigned APK at `app/build/outputs/apk/release/app-release-unsigned.apk`).

If R8 errors appear about missing rules, add appropriate `-dontwarn` or `-keep` lines to `proguard-rules.pro`.

- [ ] **Step 3: Commit**

```bash
git add app/proguard-rules.pro
git commit -m "release: ProGuard/R8 keep rules for Room, Hilt, serialization, WorkManager"
```

---

## Task 12: Strings to strings.xml + contentDescription pass

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: Settings, Library, Editor, Onboarding, Diagnostics screen files

The goal is to:
1. Externalize all user-visible string literals to `strings.xml`
2. Ensure every `Icon(...)` call has a non-null `contentDescription`

- [ ] **Step 1: Expand strings.xml**

Replace `app/src/main/res/values/strings.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Snipcraft</string>

    <!-- Common -->
    <string name="cd_back">Navigate back</string>
    <string name="cd_check_circle">Active</string>
    <string name="cd_error">Inactive</string>
    <string name="action_ok">OK</string>
    <string name="action_cancel">Cancel</string>

    <!-- Library -->
    <string name="library_title">Library</string>
    <string name="library_search_placeholder">Search snippets…</string>
    <string name="library_empty_title">No snippets yet</string>
    <string name="library_empty_body">Tap + to create your first snippet</string>
    <string name="library_fab_create">Create snippet</string>
    <string name="library_sort_frequency">Frequency</string>
    <string name="library_sort_recent">Recent</string>
    <string name="library_sort_alpha">A–Z</string>
    <string name="cd_sort_menu">Sort options</string>
    <string name="cd_search">Search</string>
    <string name="cd_pin">Pin snippet</string>
    <string name="cd_delete">Delete snippet</string>

    <!-- Editor -->
    <string name="editor_title_create">New snippet</string>
    <string name="editor_title_edit">Edit snippet</string>
    <string name="editor_shortcut_label">Shortcut</string>
    <string name="editor_shortcut_placeholder">;today</string>
    <string name="editor_body_label">Body</string>
    <string name="editor_description_label">Description (optional)</string>
    <string name="editor_enabled_label">Enabled</string>
    <string name="editor_save">Save</string>
    <string name="cd_delete_snippet">Delete this snippet</string>

    <!-- Settings -->
    <string name="settings_title">Settings</string>
    <string name="settings_section_service">Accessibility Service</string>
    <string name="settings_service_active">Active — text expansion is running</string>
    <string name="settings_service_inactive">Inactive — tap to enable in Settings</string>
    <string name="settings_open_accessibility">Open Accessibility Settings</string>
    <string name="settings_recheck">Re-check status</string>
    <string name="settings_section_appearance">Appearance</string>
    <string name="settings_section_excluded">Excluded Apps</string>
    <string name="settings_excluded_empty">No apps excluded</string>
    <string name="settings_add_excluded">Add excluded app</string>
    <string name="settings_add_excluded_label">Package name</string>
    <string name="settings_add_excluded_placeholder">com.example.app</string>
    <string name="settings_add_excluded_confirm">Add</string>
    <string name="settings_import_conflict_title">Import conflict</string>
    <string name="settings_import_conflict_body">How should existing snippets be handled?</string>
    <string name="settings_import_overwrite">Overwrite existing</string>
    <string name="settings_import_skip">Skip existing</string>
    <string name="settings_import_done_title">Import complete</string>
    <string name="settings_section_backup">Backup &amp; Restore</string>
    <string name="settings_export_title">Export snippets</string>
    <string name="settings_export_body">Share all snippets as JSON</string>
    <string name="settings_import_title">Import snippets</string>
    <string name="settings_import_body">Restore from JSON file</string>
    <string name="settings_section_diagnostics">Diagnostics</string>
    <string name="settings_diagnostics_title">Compatibility &amp; diagnostics</string>
    <string name="settings_diagnostics_body">Service health, expansion log, debug export</string>
    <string name="settings_section_about">About</string>
    <string name="settings_about_version">v0.1.0 — personal use build</string>
    <string name="cd_export">Export</string>
    <string name="cd_import">Import</string>
    <string name="cd_remove_app">Remove %1$s from excluded apps</string>
    <string name="cd_open_diagnostics">Open diagnostics screen</string>

    <!-- Diagnostics -->
    <string name="diagnostics_title">Compatibility &amp; Diagnostics</string>
    <string name="diagnostics_section_status">Service Status</string>
    <string name="diagnostics_accessibility">Accessibility Service</string>
    <string name="diagnostics_foreground">Foreground Service</string>
    <string name="diagnostics_watchdog">Health Watchdog (WorkManager)</string>
    <string name="diagnostics_battery">Battery Exemption</string>
    <string name="diagnostics_notification">Notification Permission</string>
    <string name="diagnostics_section_keyboard">Active Keyboard</string>
    <string name="diagnostics_keyboard_unknown">Unknown</string>
    <string name="diagnostics_keyboard_label">Default input method package</string>
    <string name="diagnostics_section_testfield">Test Field</string>
    <string name="diagnostics_testfield_label">Type a shortcut + space</string>
    <string name="diagnostics_testfield_placeholder">;today </string>
    <string name="diagnostics_testfield_support">The accessibility service will expand it if active</string>
    <string name="diagnostics_section_log">Recent Expansions</string>
    <string name="diagnostics_log_empty">No expansions yet</string>
    <string name="diagnostics_section_actions">Actions</string>
    <string name="diagnostics_trigger_watchdog">Trigger Watchdog Now</string>
    <string name="diagnostics_export">Export Diagnostics Bundle</string>
    <string name="diagnostics_export_subject">Snipcraft diagnostics bundle</string>
    <string name="diagnostics_export_chooser">Export diagnostics</string>

    <!-- Onboarding -->
    <string name="onboarding_get_started">Get Started</string>
    <string name="onboarding_skip">Skip</string>
    <string name="onboarding_next">Next</string>
    <string name="onboarding_sandbox_label">Try it here</string>
    <string name="onboarding_sandbox_hint">Type ;today + space</string>
</resources>
```

- [ ] **Step 2: Audit and fix contentDescription in feature screens**

Open each of the following files and ensure every `Icon(...)` that communicates meaning has a descriptive (non-null) `contentDescription`. Icons that are purely decorative (e.g., trailingContent on a row that already has a text label) may use `contentDescription = null`.

Files to audit:
- `feature/library/src/main/kotlin/.../LibraryScreen.kt`
- `feature/editor/src/main/kotlin/.../EditorScreen.kt`
- `feature/settings/src/main/kotlin/.../SettingsScreen.kt`
- `feature/diagnostics/src/main/kotlin/.../DiagnosticsScreen.kt`
- `feature/onboarding/src/main/kotlin/.../OnboardingScreen.kt`

For each Icon that is interactive (inside a Button or IconButton) or conveys information (status icons), make `contentDescription` a non-empty string. Example fixes:

In `SettingsScreen.kt`:
```kotlin
// BEFORE:
Icon(..., contentDescription = null, ...)

// AFTER (for status icon):
Icon(
    if (uiState.isAccessibilityServiceEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
    contentDescription = if (uiState.isAccessibilityServiceEnabled)
        "Accessibility service is active"
    else
        "Accessibility service is inactive",
    ...
)
```

- [ ] **Step 3: Run all tests**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew test 2>&1 | tail -20
```

Expected: All tests pass. The strings.xml change doesn't affect unit tests.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml feature/
git commit -m "polish: expand strings.xml, fix contentDescription on all Icons"
```

---

## Task 13: CHANGELOG v0.1.0 + README + .gitignore + keystore instructions

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `README.md`
- Modify: `.gitignore`
- Modify: `docs/HANDOFF.md`

- [ ] **Step 1: Roll CHANGELOG [Unreleased] → v0.1.0**

Replace the `## [Unreleased] — v0.1.0-dev` heading and everything below it with:

```markdown
## [v0.1.0] — 2026-05-14

### Added
- Project skeleton: 20 Gradle modules, version catalog, convention plugins
- `core:domain` — Snippet, Folder, TriggerMode, ExpansionStrategy, SnippetType data classes
- `core:engine` — Trie-based suffix matcher + ON_DELIMITER trigger logic (TDD)
- `core:variables` — Built-in `{{date}}`, `{{time}}`, `{{clipboard}}` resolvers (TDD)
- `core:common` — AppLogger (Timber wrapper, debug/release trees)
- `core:accessibility` — SnipAccessibilityService, SnipForegroundService (specialUse, START_STICKY, IMPORTANCE_MIN), HealthWatchdogWorker (30-min periodic WorkManager), AccessibilityEventProcessor (password field hard-exclusion), ServiceHealthChecker, NotificationChannels
- `core:database` — Room v1→v2: SnippetEntity, FolderEntity, CompatibilityRuleEntity, ExpansionHistoryEntity; all DAOs; schema exported
- `core:data` — SnippetRepository, FolderRepository, CompatibilityRuleRepository, ExpansionHistoryRepository; SeedDataPopulator (8 starter snippets)
- `core:compatibility` — CompatibilityResolver with PASTE profiles (Chrome, Firefox, Discord, Slack, WhatsApp) and DISABLED profiles (systemui)
- `core:backup` — BackupManager: JSON v1 export/import with SKIP_EXISTING/OVERWRITE conflict handling
- `core:designsystem` — SnipTheme (Material 3, dynamic colors API 31+), typography, EmptyState composable
- `feature:library` — LibraryScreen: search, sort (frequency/recent/alpha), pin, FAB, empty state
- `feature:editor` — EditorScreen: shortcut + body + description fields, enabled toggle, save/delete
- `feature:settings` — SettingsScreen: accessibility status, theme chips, excluded apps, backup/restore, diagnostics entry point
- `feature:onboarding` — HorizontalPager (Accessibility → Notification → Battery → Sandbox); auto-advances on accessibility grant; real snippet expansion in sandbox
- `feature:diagnostics` — DiagnosticsScreen: 5 service status pills, IME detection, test field, last-50 expansion log, diagnostics JSON export, watchdog trigger
- `SnipNavHost` — type-safe Navigation Compose routes (Library, Settings, Editor, Onboarding, Diagnostics)
- Adaptive app icon: paper-snippet glyph (foreground vector + dark background)
- Splash screen via `androidx.core.splashscreen`
- R8/ProGuard enabled for release builds with keep rules for Room, Hilt, kotlinx.serialization, WorkManager
- 165+ unit tests, all green (JUnit 5 + JUnit 4/Robolectric + MockK + Turbine)

### Changed
- Version promoted from `0.1.0-dev` to `0.1.0`

[v0.1.0]: https://github.com/annabel-lee-x10/snipcraft/releases/tag/v0.1.0
```

- [ ] **Step 2: Update README.md**

Replace the entire `README.md`:

```markdown
# Snipcraft

System-wide text expansion for Android. Local. Fast. Privacy-first.

Type a shortcut anywhere you can type — Snipcraft expands it instantly using the Android Accessibility Service.

## Features

- **System-wide expansion** — works in Gmail, Chrome, WhatsApp, Slack, Discord, and most text fields
- **ON_DELIMITER trigger** — expand on space, tab, or punctuation; no accidental triggers
- **Built-in variables** — `{{date}}`, `{{date:yyyy-MM-dd}}`, `{{time}}`, `{{clipboard}}`
- **Trie-based matching** — O(L) per keystroke; no noticeable latency
- **Backspace undo** — 5-deep undo stack per field, 2-second window
- **CRUD library** — search, sort, pin, folder, enable/disable per snippet
- **JSON backup/restore** — export all snippets to a file, import with conflict handling
- **Per-app blacklist** — exclude specific apps from expansion
- **Compatibility layer** — PASTE strategy for WebView/Chrome, DISABLED for password fields (non-overridable)
- **Diagnostics screen** — service health pills, expansion log, debug bundle export
- **First-run onboarding** — guided Accessibility permission flow with sandbox test
- **No telemetry** — fully local, no data leaves the device
- **MIT licensed** — personal use, open source

## Requirements

- Android 12+ (API 31)
- Accessibility Service permission (granted in Settings)
- Battery optimization exemption (recommended to prevent service kill)

## Install (sideload)

1. Download `app-release.apk` from the [latest release](https://github.com/annabel-lee-x10/snipcraft/releases/latest)
2. On your Android device, enable **Settings → Security → Install unknown apps** for your file manager or browser
3. Open the APK — Android will prompt to install
4. After installing, open Snipcraft and follow the onboarding flow

## Accessibility Setup

1. Open Snipcraft → tap "Open Accessibility Settings"
2. Find **Snipcraft** in the list and enable it
3. Confirm the permission prompt
4. Return to Snipcraft — the service status will show **Active**
5. Recommended: grant **Battery → Unrestricted** to prevent the service being killed in the background

## Building from source

**Prerequisites:** Android Studio Meerkat (or JDK 21 at `C:\Program Files\Android\Android Studio\jbr`), Android SDK 35, Gradle 9.3.1 (wrapper auto-downloads).

```bash
git clone https://github.com/annabel-lee-x10/snipcraft.git
cd snipcraft

# Debug build
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleDebug

# APK: app/build/outputs/apk/debug/app-debug.apk

# Release build (unsigned — see docs/HANDOFF.md for signing)
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleRelease
```

## Running tests

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew test
```

## Module structure

| Module | Description |
|---|---|
| `:app` | MainActivity, SnipNavHost, app-level Hilt wiring |
| `:core:engine` | Trie matcher, ON_DELIMITER trigger, undo stack (pure Kotlin) |
| `:core:variables` | Template parser, built-in resolvers (pure Kotlin) |
| `:core:domain` | Domain data classes (pure Kotlin) |
| `:core:accessibility` | SnipAccessibilityService + foreground service + health watchdog |
| `:core:compatibility` | Per-app strategy resolver |
| `:core:database` | Room DB v2 + all DAOs + migrations |
| `:core:data` | Repositories (DB ↔ domain) |
| `:core:backup` | JSON export/import |
| `:core:designsystem` | Material 3 theme + design tokens |
| `:feature:library` | Snippet list + search UI |
| `:feature:editor` | Snippet create/edit UI |
| `:feature:settings` | Settings screen |
| `:feature:onboarding` | First-run permission flow |
| `:feature:diagnostics` | Health checks + expansion log + debug export |

## License

MIT — see [LICENSE](LICENSE).
```

- [ ] **Step 3: Update .gitignore for keystore files**

Open `.gitignore` and add (at the end, or in an appropriate section):

```
# Signing keystores — never commit
*.jks
*.keystore
keystore.properties
release-keystore.jks
```

- [ ] **Step 4: Add keystore instructions to docs/HANDOFF.md**

Append a new Pass 8 section to `docs/HANDOFF.md` (before the final `---` or at the end):

````markdown
## 11. Pass 8 — Polish + Diagnostics + v0.1.0 (2026-05-14)

### What shipped
- `feature:diagnostics` — full screen: 5 service status pills, IME detection, test field, last-50 expansion log, diagnostics JSON export, watchdog trigger button
- Room DB migrated v1 → v2 (`expansion_history` table); AccessibilityService logs each expansion attempt
- Adaptive app icon: paper-snippet glyph (foreground vector drawable)
- Splash screen via `androidx.core.splashscreen`
- R8/ProGuard enabled for release builds (`app/proguard-rules.pro`)
- CHANGELOG rolled to `v0.1.0`; README expanded with install steps, module table, build instructions
- Final test count: 165+ unit tests, all green

### Keystore — how to sign a release APK for sideloading

The keystore is NOT in the repo (gitignored). Generate it once and keep it safe:

```bash
# Run from project root. Replace values in <angle brackets>.
keytool -genkeypair \
  -keystore release-keystore.jks \
  -alias snipcraft \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass <your-store-password> \
  -keypass <your-key-password> \
  -dname "CN=Snipcraft, OU=Personal, O=a10101100, L=Unknown, S=Unknown, C=US"
```

Then create `keystore.properties` (also gitignored) at the project root:

```properties
storeFile=../../release-keystore.jks
storePassword=<your-store-password>
keyAlias=snipcraft
keyPassword=<your-key-password>
```

To wire the signing config into `app/build.gradle.kts` (do this before the next release):

```kotlin
// Load keystore.properties at the top of app/build.gradle.kts:
import java.util.Properties
val keystoreProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) props.load(f.inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... rest of release config
        }
    }
}
```

Until the signing config is wired, `assembleRelease` produces an **unsigned** APK at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

You can still sideload the unsigned APK using `adb install` on a device with developer options enabled:
```bash
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

### Phase 2+ roadmap pointer

Planned features deferred post v0.1.0:
- INSTANT + MANUAL trigger modes
- User-defined variables (`{{var:name}}`, `{{cursor}}`, `{{ask:label}}`)
- Suggestion popup (floating overlay anchored to cursor)
- Expansion menus (one shortcut → multiple choices)
- Tags + tag filter chips
- Folder nesting
- Rich text / image / GIF snippets
- WebDAV sync (multiple devices)
- Tasker plugin + Intent API
- Quick-Add tile (notification shade)
- Per-app full rule editor (beyond blacklist)
- Encrypted vault (SQLCipher + biometric)

See `D:\a10101100_labs\PLAN_text_expander.md` §G for full phased roadmap.
````

- [ ] **Step 5: Commit all documentation changes**

```bash
git add CHANGELOG.md README.md .gitignore docs/HANDOFF.md
git commit -m "docs: roll CHANGELOG to v0.1.0, expand README, keystore instructions, Phase 2+ pointer"
```

---

## Task 14: Full build verification

**Goal:** Confirm all tests pass and both debug + release APKs build clean.

- [ ] **Step 1: Run all tests**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew test 2>&1 | tail -40
```

Expected: All tests PASS. Count should be 165+. No failures, no errors.

If any tests fail: diagnose and fix before proceeding. Do NOT skip failures.

- [ ] **Step 2: assembleDebug**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: assembleRelease**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleRelease 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL. Unsigned APK at `app/build/outputs/apk/release/app-release-unsigned.apk`.

If R8 failures occur, diagnose the missing keep rule, add it to `proguard-rules.pro`, and re-run.

- [ ] **Step 4: Commit verification result**

If everything is green, commit a final summary commit:

```bash
git add -A
git status  # confirm only expected files, nothing sensitive
git commit -m "chore: Pass 8 complete — 165+ tests green, debug + release builds clean"
```

---

## Task 15: PR + merge + git tag v0.1.0 + GitHub release

- [ ] **Step 1: Push the feature branch**

```bash
git push -u origin claude/pass-8-polish-diagnostics-ship
```

- [ ] **Step 2: Create the PR**

```bash
gh pr create \
  --title "feat: Pass 8 — Diagnostics, Polish, v0.1.0 ship" \
  --body "$(cat <<'EOF'
## Summary

- **Diagnostics screen** (`feature:diagnostics`): 5 service status pills, IME detection, sandbox test field, last-50 expansion log, diagnostics JSON export, watchdog trigger button
- **Room DB v1→v2 migration**: `expansion_history` table; `SnipAccessibilityService` logs each expansion attempt
- **App icon**: paper-snippet vector glyph replacing placeholder circle
- **Splash screen**: `androidx.core.splashscreen` with dark background + icon
- **R8/ProGuard**: release builds now minified with keep rules for Room, Hilt, serialization, WorkManager
- **Polish**: `strings.xml` fully populated, all `Icon()` calls have `contentDescription`
- **CHANGELOG**: `[Unreleased]` rolled to `v0.1.0 — 2026-05-14`
- **README**: full install guide, sideload steps, module table, build instructions
- **Keystore instructions**: added to `docs/HANDOFF.md` (keystore itself gitignored)
- **Tests**: 165+ unit tests, all green

## Test plan

- [ ] `./gradlew test` — all tests pass, count ≥ 165
- [ ] `./gradlew assembleDebug` — BUILD SUCCESSFUL
- [ ] `./gradlew assembleRelease` — BUILD SUCCESSFUL (unsigned APK produced)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 3: Confirm tests pass in CI (or locally) then merge**

```bash
gh pr checks  # wait for any CI checks
gh pr merge --squash --delete-branch
```

- [ ] **Step 4: Tag v0.1.0 on the merge commit**

After merge, fetch and tag:

```bash
git fetch origin main
git tag v0.1.0 origin/main
git push origin v0.1.0
```

- [ ] **Step 5: Create the GitHub release**

First write the release notes to a temp file:

```bash
cat > /tmp/release-notes.md << 'EOF'
## Snipcraft v0.1.0 — First MVP release

System-wide text expansion for Android. Local. Fast. Privacy-first.

### What's included

- **ON_DELIMITER expansion** — type a shortcut + space anywhere and it expands instantly
- **Built-in variables** — `{{date}}`, `{{time}}`, `{{clipboard}}`
- **CRUD library** — create, edit, delete, search, pin, sort snippets
- **Per-app blacklist** — exclude specific apps
- **JSON backup/restore** — full round-trip export/import
- **Onboarding** — guided accessibility permission flow with sandbox test
- **Diagnostics screen** — service health, expansion log, debug export
- **165+ unit tests**, all green

### Install

1. Download `app-release-unsigned.apk` below
2. Enable **Install unknown apps** for your file manager
3. Install the APK and follow onboarding

> **Note:** This is an unsigned sideload APK. On a device with ADB enabled you can also run:
> `adb install app-release-unsigned.apk`

### Requirements

- Android 12+ (API 31)
- Accessibility Service permission
EOF
```

Then create the release and upload the APK:

```bash
gh release create v0.1.0 \
  --title "Snipcraft v0.1.0" \
  --notes-file /tmp/release-notes.md \
  "app/build/outputs/apk/release/app-release-unsigned.apk#app-release-unsigned.apk"
```

- [ ] **Step 6: Verify the release URL**

```bash
gh release view v0.1.0 --web
```

Report back: PR URL, merge SHA, release tag URL, release APK asset URL, final test count.

---

## Self-Review

### Spec coverage check

| Requirement | Task |
|---|---|
| Diagnostics — 5 status pills | Task 6, 7 |
| Diagnostics — IME detection | Task 6, 7 |
| Diagnostics — test field | Task 7 |
| Diagnostics — last-50 expansion log | Task 2–6 (DB), Task 7 (UI) |
| Diagnostics — export JSON bundle | Task 6, 7 |
| Diagnostics — trigger watchdog button | Task 6, 7 |
| App icon — paper-snippet glyph | Task 9 |
| Splash screen | Task 10 |
| Strings to strings.xml | Task 12 |
| contentDescription on icons | Task 12 |
| ProGuard/R8 release | Task 10, 11 |
| versionCode=1, versionName="0.1.0" | Already in app/build.gradle.kts — verify unchanged |
| CHANGELOG v0.1.0 | Task 13 |
| README | Task 13 |
| .gitignore keystore | Task 13 |
| Keystore instructions in HANDOFF | Task 13 |
| `./gradlew test` clean | Task 14 |
| `assembleDebug` clean | Task 14 |
| `assembleRelease` clean | Task 14 |
| git tag v0.1.0 | Task 15 |
| gh release create | Task 15 |
| Upload release APK | Task 15 |
| HANDOFF.md Pass 8 section | Task 13 |
| Phase 2+ pointer | Task 13 |

All requirements covered. ✓

### Placeholder scan

No TBD, TODO, or placeholder patterns in this plan. All code blocks are complete. ✓

### Type consistency

- `DiagnosticsUiState` defined in Task 6, used in Task 7 ✓
- `DiagnosticsEvent.ShareDiagnostics(json: String)` defined in Task 6, used in Task 7 ✓
- `ExpansionHistoryRepository.logExpansion(shortcut, packageName, timestamp, success, errorReason)` defined in Task 4, called in Task 5 ✓
- `DiagnosticsChecker` interface defined in Task 6, production impl `AndroidDiagnosticsChecker` in Task 6, fake in tests ✓
- `ExpansionEvent` domain model defined in Task 4, used in `DiagnosticsUiState.recentExpansions` in Task 6 ✓
