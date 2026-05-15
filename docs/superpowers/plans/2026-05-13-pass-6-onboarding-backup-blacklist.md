# Pass 6 Implementation Plan — Onboarding, Backup, Blacklist

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship Pass 6: 3-card onboarding permission carousel with sandbox expansion, JSON export/import, and per-app blacklist UI.

**Architecture:** `feature:onboarding` uses HorizontalPager with 4 pages (3 permission cards + sandbox); `core:backup` BackupManager serializes domain objects via kotlinx.serialization; blacklist managed via new `CompatibilityRuleRepository` in `core:data`, surfaced in `feature:settings`. CompatibilityResolver gains live user-blacklist updates subscribed from `SnipAccessibilityService`.

**Tech Stack:** Kotlin 2.3.20, AGP 9.1.0, Compose BOM 2026.05.00, Room 2.8.4, Hilt 2.59.2, kotlinx.serialization 1.8.1, JUnit 5.12.2, MockK 1.14.2, Robolectric 4.14.1

---

## Pre-task: Read these files before starting

Before any task, read these files to confirm current state:
- `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/CompatibilityRuleDao.kt`
- `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/SnippetRepository.kt`
- `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/DataModule.kt`
- `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/SnippetRepositoryImpl.kt`
- `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/FolderRepository.kt` (check if observeAll() exists)
- `core/compatibility/src/main/kotlin/dev/a10101100/snipcraft/core/compatibility/CompatibilityResolver.kt`
- `core/accessibility/src/main/kotlin/dev/a10101100/snipcraft/core/accessibility/SnipAccessibilityService.kt`
- `feature/onboarding/build.gradle.kts`
- `core/backup/build.gradle.kts`
- `feature/settings/build.gradle.kts`
- `feature/settings/src/main/kotlin/dev/a10101100/snipcraft/feature/settings/SettingsUiState.kt`
- `feature/settings/src/main/kotlin/dev/a10101100/snipcraft/feature/settings/SettingsViewModel.kt`
- `feature/settings/src/main/kotlin/dev/a10101100/snipcraft/feature/settings/SettingsScreen.kt`
- `app/src/main/kotlin/dev/a10101100/snipcraft/navigation/SnipNavHost.kt`
- `gradle/libs.versions.toml`

---

## File Structure

### New files
| File | Purpose |
|------|---------|
| `core/data/src/main/kotlin/.../core/data/CompatibilityRuleRepository.kt` | Interface for user blacklist CRUD |
| `core/data/src/main/kotlin/.../core/data/CompatibilityRuleRepositoryImpl.kt` | Room-backed implementation |
| `core/data/src/test/kotlin/.../core/data/CompatibilityRuleRepositoryTest.kt` | JUnit 5 + MockK tests |
| `core/backup/src/main/kotlin/.../core/backup/BackupModels.kt` | `@Serializable` data classes for JSON schema |
| `core/backup/src/main/kotlin/.../core/backup/BackupManager.kt` | export() + import() logic |
| `core/backup/src/main/kotlin/.../core/backup/BackupModule.kt` | Hilt module providing BackupManager + Json |
| `core/backup/src/test/kotlin/.../core/backup/BackupManagerTest.kt` | JUnit 5 + MockK round-trip tests |
| `feature/onboarding/src/main/kotlin/.../feature/onboarding/OnboardingUiState.kt` | State + event types |
| `feature/onboarding/src/main/kotlin/.../feature/onboarding/OnboardingViewModel.kt` | Permission detection, sandbox engine |
| `feature/onboarding/src/main/kotlin/.../feature/onboarding/OnboardingScreen.kt` | HorizontalPager, 4 pages, permission launchers |
| `feature/onboarding/src/test/kotlin/.../feature/onboarding/OnboardingViewModelTest.kt` | JUnit 5 ViewModel tests |

### Modified files
| File | Change |
|------|--------|
| `core/database/src/main/kotlin/.../core/database/CompatibilityRuleDao.kt` | Add `observeUserBlacklist()`, `deleteByPackage()` |
| `core/data/src/main/kotlin/.../core/data/SnippetRepository.kt` | Add `observeAll(): Flow<List<Snippet>>` |
| `core/data/src/main/kotlin/.../core/data/SnippetRepositoryImpl.kt` | Implement `observeAll()` |
| `core/data/src/main/kotlin/.../core/data/FolderRepository.kt` | Add `observeAll()` if missing |
| `core/data/src/main/kotlin/.../core/data/FolderRepositoryImpl.kt` | Implement `observeAll()` if missing |
| `core/data/src/main/kotlin/.../core/data/DataModule.kt` | Bind CompatibilityRuleRepository |
| `core/compatibility/src/main/kotlin/.../core/compatibility/CompatibilityResolver.kt` | Add `setUserBlacklist(Set<String>)` |
| `core/accessibility/src/main/kotlin/.../core/accessibility/SnipAccessibilityService.kt` | Subscribe to user blacklist flow, update resolver |
| `feature/onboarding/build.gradle.kts` | Add core:engine, core:variables, core:accessibility deps |
| `core/backup/build.gradle.kts` | Add core:data dep + kotlin.serialization plugin |
| `feature/settings/build.gradle.kts` | Add core:backup + core:compatibility deps |
| `feature/settings/src/main/kotlin/.../feature/settings/SettingsUiState.kt` | Add blacklist + backup state |
| `feature/settings/src/main/kotlin/.../feature/settings/SettingsViewModel.kt` | Add blacklist + backup actions |
| `feature/settings/src/main/kotlin/.../feature/settings/SettingsScreen.kt` | Add blacklist + backup UI sections |
| `app/src/main/kotlin/.../navigation/SnipNavHost.kt` | Add OnboardingRoute + start-destination logic |
| `CHANGELOG.md` | Append Pass 6 entries |
| `docs/HANDOFF.md` | Add Pass 6 section |

---

## Task 1: Branch setup

**Files:** none

- [ ] **Step 1: Create feature branch**

```bash
cd D:\a10101100_labs\snipcraft
git checkout main
git pull
git checkout -b claude/pass-6-onboarding-import-export-blacklist
```

- [ ] **Step 2: Verify clean state**

```bash
git status
```
Expected: `nothing to commit, working tree clean`

---

## Task 2: CompatibilityRuleDao — add observeUserBlacklist() + deleteByPackage()

**Files:**
- Modify: `core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/CompatibilityRuleDao.kt`

- [ ] **Step 1: Write the failing test**

There is currently no test for the new DAO methods. We'll verify the new methods compile and work in Task 3's repository test. For now, update the DAO directly (it's a Room interface — the actual behavior is tested via the repository).

- [ ] **Step 2: Update CompatibilityRuleDao**

Replace the file content:

```kotlin
package dev.a10101100.snipcraft.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompatibilityRuleDao {

    @Query("SELECT * FROM compatibility_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): CompatibilityRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CompatibilityRuleEntity)

    @Query("SELECT * FROM compatibility_rules WHERE isBuiltIn = 0 ORDER BY packageName ASC")
    fun observeUserBlacklist(): Flow<List<CompatibilityRuleEntity>>

    @Query("DELETE FROM compatibility_rules WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
```

- [ ] **Step 3: Commit**

```bash
git add core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/CompatibilityRuleDao.kt
git commit -m "feat: add observeUserBlacklist + deleteByPackage to CompatibilityRuleDao"
```

---

## Task 3: CompatibilityRuleRepository in core:data (TDD)

**Files:**
- Create: `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepository.kt`
- Create: `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepositoryImpl.kt`
- Create: `core/data/src/test/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepositoryTest.kt`
- Modify: `core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/DataModule.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// core/data/src/test/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepositoryTest.kt
package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.CompatibilityRuleDao
import dev.a10101100.snipcraft.core.database.CompatibilityRuleEntity
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompatibilityRuleRepositoryTest {

    private val dao = mockk<CompatibilityRuleDao>(relaxed = true)
    private val repo = CompatibilityRuleRepositoryImpl(dao)

    @Test
    fun `observeBlacklistedPackages returns package names from dao`() = runTest {
        val entity = CompatibilityRuleEntity(
            id = "1",
            packageName = "com.example.app",
            strategy = "DISABLED",
            triggerOverride = null,
            delayMs = 0,
            notes = null,
            isBuiltIn = false,
            updatedAt = 0L,
        )
        every { dao.observeUserBlacklist() } returns flowOf(listOf(entity))

        val packages = repo.observeBlacklistedPackages().first()

        assertEquals(listOf("com.example.app"), packages)
    }

    @Test
    fun `addToBlacklist upserts record with DISABLED strategy`() = runTest {
        val captured = slot<CompatibilityRuleEntity>()
        coEvery { dao.upsert(capture(captured)) } returns Unit

        repo.addToBlacklist("com.example.new")

        assertEquals("com.example.new", captured.captured.packageName)
        assertEquals("DISABLED", captured.captured.strategy)
        assertEquals(false, captured.captured.isBuiltIn)
    }

    @Test
    fun `removeFromBlacklist calls deleteByPackage`() = runTest {
        repo.removeFromBlacklist("com.example.app")

        coVerify { dao.deleteByPackage("com.example.app") }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:data:test --tests "dev.a10101100.snipcraft.core.data.CompatibilityRuleRepositoryTest" 2>&1 | tail -20
```
Expected: FAIL — `CompatibilityRuleRepositoryImpl` not found.

- [ ] **Step 3: Create the interface**

```kotlin
// core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepository.kt
package dev.a10101100.snipcraft.core.data

import kotlinx.coroutines.flow.Flow

interface CompatibilityRuleRepository {
    fun observeBlacklistedPackages(): Flow<List<String>>
    suspend fun addToBlacklist(packageName: String)
    suspend fun removeFromBlacklist(packageName: String)
}
```

- [ ] **Step 4: Create the implementation**

```kotlin
// core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepositoryImpl.kt
package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.CompatibilityRuleDao
import dev.a10101100.snipcraft.core.database.CompatibilityRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatibilityRuleRepositoryImpl @Inject constructor(
    private val dao: CompatibilityRuleDao,
) : CompatibilityRuleRepository {

    override fun observeBlacklistedPackages(): Flow<List<String>> =
        dao.observeUserBlacklist().map { entities -> entities.map { it.packageName } }

    override suspend fun addToBlacklist(packageName: String) {
        dao.upsert(
            CompatibilityRuleEntity(
                id = UUID.randomUUID().toString(),
                packageName = packageName,
                strategy = "DISABLED",
                triggerOverride = null,
                delayMs = 0,
                notes = null,
                isBuiltIn = false,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun removeFromBlacklist(packageName: String) {
        dao.deleteByPackage(packageName)
    }
}
```

- [ ] **Step 5: Bind in DataModule**

Add to `DataModule.kt` alongside the existing `@Binds` methods:

```kotlin
@Binds @Singleton
abstract fun bindCompatibilityRuleRepository(
    impl: CompatibilityRuleRepositoryImpl,
): CompatibilityRuleRepository
```

- [ ] **Step 6: Run test to verify it passes**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:data:test --tests "dev.a10101100.snipcraft.core.data.CompatibilityRuleRepositoryTest" 2>&1 | tail -20
```
Expected: 3 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepository.kt \
        core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepositoryImpl.kt \
        core/data/src/test/kotlin/dev/a10101100/snipcraft/core/data/CompatibilityRuleRepositoryTest.kt \
        core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/DataModule.kt
git commit -m "feat: add CompatibilityRuleRepository for user blacklist management"
```

---

## Task 4: SnippetRepository.observeAll() + FolderRepository.observeAll()

**Files:**
- Modify: `core/database/src/main/kotlin/.../core/database/SnippetDao.kt`
- Modify: `core/data/src/main/kotlin/.../core/data/SnippetRepository.kt`
- Modify: `core/data/src/main/kotlin/.../core/data/SnippetRepositoryImpl.kt`
- Modify: `core/data/src/main/kotlin/.../core/data/FolderRepository.kt` (add if missing)
- Modify: `core/data/src/main/kotlin/.../core/data/FolderRepositoryImpl.kt` (add if missing)
- Modify: `core/database/src/main/kotlin/.../core/database/FolderDao.kt` (add if missing)

NOTE: Read each file first. If `observeAll()` already exists, skip that file.

- [ ] **Step 1: Add observeAll() to SnippetDao**

Add to `SnippetDao.kt`:
```kotlin
@Query("SELECT * FROM snippets ORDER BY createdAt ASC")
fun observeAll(): Flow<List<SnippetEntity>>
```

- [ ] **Step 2: Add observeAll() to SnippetRepository interface**

Add to `SnippetRepository.kt`:
```kotlin
fun observeAll(): Flow<List<Snippet>>
```

- [ ] **Step 3: Implement in SnippetRepositoryImpl**

Add to `SnippetRepositoryImpl.kt`:
```kotlin
override fun observeAll(): Flow<List<Snippet>> =
    dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }
```

- [ ] **Step 4: Check FolderDao, FolderRepository, FolderRepositoryImpl for observeAll()**

If `FolderDao.kt` lacks `observeAll()`, add:
```kotlin
@Query("SELECT * FROM folders ORDER BY sortOrder ASC")
fun observeAll(): Flow<List<FolderEntity>>
```

If `FolderRepository.kt` lacks `observeAll()`, add:
```kotlin
fun observeAll(): Flow<List<Folder>>
```

If `FolderRepositoryImpl.kt` lacks `observeAll()`, add:
```kotlin
override fun observeAll(): Flow<List<Folder>> =
    dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }
```

- [ ] **Step 5: Run all data + database tests to confirm nothing broke**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:data:test :core:database:test 2>&1 | tail -20
```
Expected: all PASS.

- [ ] **Step 6: Commit**

```bash
git add core/database/src/main/kotlin/dev/a10101100/snipcraft/core/database/ \
        core/data/src/main/kotlin/dev/a10101100/snipcraft/core/data/
git commit -m "feat: add observeAll() to SnippetRepository and FolderRepository for backup"
```

---

## Task 5: CompatibilityResolver — user blacklist support + SnipAccessibilityService wiring

**Files:**
- Modify: `core/compatibility/src/main/kotlin/.../core/compatibility/CompatibilityResolver.kt`
- Modify: `core/accessibility/src/main/kotlin/.../core/accessibility/SnipAccessibilityService.kt`

- [ ] **Step 1: Write failing test for resolver**

Find `CompatibilityResolverTest.kt` in `core/compatibility/src/test/` and add:

```kotlin
@Test
fun `strategyFor returns DISABLED for user-blacklisted package`() {
    val resolver = CompatibilityResolver()
    resolver.setUserBlacklist(setOf("com.user.blacklisted"))

    val result = resolver.strategyFor("com.user.blacklisted")

    assertEquals(ExpansionStrategy.DISABLED, result)
}

@Test
fun `strategyFor uses built-in profile when package not in user blacklist`() {
    val resolver = CompatibilityResolver()
    resolver.setUserBlacklist(emptySet())

    val result = resolver.strategyFor("com.android.chrome")

    assertEquals(ExpansionStrategy.PASTE, result)
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:compatibility:test 2>&1 | tail -20
```
Expected: FAIL — `setUserBlacklist` not found.

- [ ] **Step 3: Update CompatibilityResolver**

Add a thread-safe user blacklist to the existing class:

```kotlin
// add field to CompatibilityResolver:
@Volatile private var userBlacklist: Set<String> = emptySet()

fun setUserBlacklist(packages: Set<String>) {
    userBlacklist = packages
}

// update strategyFor() to check userBlacklist first:
fun strategyFor(packageName: String): ExpansionStrategy {
    if (packageName in userBlacklist) return ExpansionStrategy.DISABLED
    return defaultStrategyFor(packageName)
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:compatibility:test 2>&1 | tail -20
```
Expected: all PASS.

- [ ] **Step 5: Wire into SnipAccessibilityService**

In `SnipAccessibilityService.kt`, inject `CompatibilityRuleRepository` and subscribe to the user blacklist. Find the `onServiceConnected()` method and add after existing setup code:

```kotlin
@Inject lateinit var compatibilityRuleRepository: CompatibilityRuleRepository

// In onServiceConnected(), after existing serviceScope.launch blocks:
serviceScope.launch {
    compatibilityRuleRepository.observeBlacklistedPackages().collect { packages ->
        compatibilityResolver.setUserBlacklist(packages.toSet())
    }
}
```

**IMPORTANT:** `SnipAccessibilityService` is in `core:accessibility`, which depends on `core:data` already (via SnippetCacheManager). Verify `core:data` is already in `core/accessibility/build.gradle.kts` before adding this injection. If not, add `implementation(project(":core:data"))` to that build file.

- [ ] **Step 6: Run accessibility module tests**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:accessibility:test 2>&1 | tail -20
```
Expected: all PASS.

- [ ] **Step 7: Commit**

```bash
git add core/compatibility/src/main/kotlin/ \
        core/compatibility/src/test/kotlin/ \
        core/accessibility/src/main/kotlin/ \
        core/accessibility/build.gradle.kts
git commit -m "feat: CompatibilityResolver supports live user blacklist from Room"
```

---

## Task 6: core:backup — BackupModels + BackupManager (TDD)

**Files:**
- Modify: `core/backup/build.gradle.kts`
- Create: `core/backup/src/main/kotlin/.../core/backup/BackupModels.kt`
- Create: `core/backup/src/main/kotlin/.../core/backup/BackupManager.kt`
- Create: `core/backup/src/main/kotlin/.../core/backup/BackupModule.kt`
- Create: `core/backup/src/test/kotlin/.../core/backup/BackupManagerTest.kt`

- [ ] **Step 1: Update core/backup/build.gradle.kts**

Replace file content:

```kotlin
plugins {
    id("snipcraft.android.library")
    id("snipcraft.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.a10101100.snipcraft.core.backup"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
// core/backup/src/test/kotlin/dev/a10101100/snipcraft/core/backup/BackupManagerTest.kt
package dev.a10101100.snipcraft.core.backup

import dev.a10101100.snipcraft.core.data.FolderRepository
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Folder
import dev.a10101100.snipcraft.core.domain.Snippet
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupManagerTest {

    private val snippetRepo = mockk<SnippetRepository>(relaxed = true)
    private val folderRepo = mockk<FolderRepository>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val manager = BackupManager(snippetRepo, folderRepo, json)

    @Test
    fun `export produces JSON with version 1`() = runTest {
        every { snippetRepo.observeAll() } returns flowOf(emptyList())
        every { folderRepo.observeAll() } returns flowOf(emptyList())

        val result = manager.export()
        val parsed = json.decodeFromString<BackupData>(result)

        assertEquals(1, parsed.version)
        assertTrue(parsed.exportedAt.isNotBlank())
    }

    @Test
    fun `export serializes snippet fields correctly`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";today", body = "{{date:yyyy-MM-dd}}")
        every { snippetRepo.observeAll() } returns flowOf(listOf(snippet))
        every { folderRepo.observeAll() } returns flowOf(emptyList())

        val result = manager.export()
        val parsed = json.decodeFromString<BackupData>(result)

        assertEquals(1, parsed.snippets.size)
        assertEquals(";today", parsed.snippets[0].shortcut)
        assertEquals("{{date:yyyy-MM-dd}}", parsed.snippets[0].body)
    }

    @Test
    fun `import SKIP_EXISTING skips snippet with same shortcut`() = runTest {
        val existing = Snippet(id = "s1", shortcut = ";today", body = "old body")
        coEvery { snippetRepo.getByShortcut(";today") } returns existing
        every { snippetRepo.observeAll() } returns flowOf(listOf(existing))
        every { folderRepo.observeAll() } returns flowOf(emptyList())

        val exportJson = manager.export()
        val result = manager.import(exportJson, ConflictStrategy.SKIP_EXISTING)

        assertEquals(0, result.snippetsImported)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `import OVERWRITE upserts snippet regardless of existing`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";today", body = "{{date:yyyy-MM-dd}}")
        val existing = Snippet(id = "s1", shortcut = ";today", body = "old")
        coEvery { snippetRepo.getByShortcut(";today") } returns existing
        every { snippetRepo.observeAll() } returns flowOf(listOf(snippet))
        every { folderRepo.observeAll() } returns flowOf(emptyList())

        val exportJson = manager.export()
        manager.import(exportJson, ConflictStrategy.OVERWRITE)

        coVerify { snippetRepo.upsert(any()) }
    }

    @Test
    fun `round-trip export then import restores snippet`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";sig", body = "Best regards")
        every { snippetRepo.observeAll() } returns flowOf(listOf(snippet))
        every { folderRepo.observeAll() } returns flowOf(emptyList())
        coEvery { snippetRepo.getByShortcut(";sig") } returns null

        val exportJson = manager.export()
        val result = manager.import(exportJson, ConflictStrategy.SKIP_EXISTING)

        assertEquals(1, result.snippetsImported)
        assertEquals(0, result.skipped)
        coVerify { snippetRepo.upsert(match { it.shortcut == ";sig" }) }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:backup:test --tests "dev.a10101100.snipcraft.core.backup.BackupManagerTest" 2>&1 | tail -20
```
Expected: FAIL — `BackupManager`, `BackupData`, `ConflictStrategy` not found.

- [ ] **Step 4: Create BackupModels.kt**

```kotlin
// core/backup/src/main/kotlin/dev/a10101100/snipcraft/core/backup/BackupModels.kt
package dev.a10101100.snipcraft.core.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String,
    val snippets: List<SnippetBackup>,
    val folders: List<FolderBackup>,
)

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
)

@Serializable
data class FolderBackup(
    val id: String,
    val name: String,
    val color: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class ConflictStrategy { SKIP_EXISTING, OVERWRITE }

data class ImportResult(
    val snippetsImported: Int,
    val foldersImported: Int,
    val skipped: Int,
)
```

- [ ] **Step 5: Create BackupManager.kt**

```kotlin
// core/backup/src/main/kotlin/dev/a10101100/snipcraft/core/backup/BackupManager.kt
package dev.a10101100.snipcraft.core.backup

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
class BackupManager @Inject constructor(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val json: Json,
) {
    suspend fun export(): String {
        val snippets = snippetRepository.observeAll().first()
        val folders = folderRepository.observeAll().first()
        val data = BackupData(
            exportedAt = Instant.now().toString(),
            snippets = snippets.map { it.toBackup() },
            folders = folders.map { it.toBackup() },
        )
        return json.encodeToString(BackupData.serializer(), data)
    }

    suspend fun import(jsonString: String, conflictStrategy: ConflictStrategy): ImportResult {
        val data = json.decodeFromString(BackupData.serializer(), jsonString)
        var imported = 0
        var skipped = 0

        data.folders.forEach { folderBackup ->
            folderRepository.upsert(folderBackup.toDomain())
            imported++
        }

        data.snippets.forEach { snippetBackup ->
            val existing = snippetRepository.getByShortcut(snippetBackup.shortcut)
            if (existing != null && conflictStrategy == ConflictStrategy.SKIP_EXISTING) {
                skipped++
            } else {
                snippetRepository.upsert(snippetBackup.toDomain())
                imported++
            }
        }

        return ImportResult(
            snippetsImported = imported - data.folders.size,
            foldersImported = data.folders.size,
            skipped = skipped,
        )
    }

    private fun Snippet.toBackup() = SnippetBackup(
        id = id,
        shortcut = shortcut,
        body = body,
        type = type.name,
        triggerMode = triggerMode.name,
        folderId = folderId,
        usageCount = usageCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isEnabled = isEnabled,
        caseSensitive = caseSensitive,
        description = description,
    )

    private fun Folder.toBackup() = FolderBackup(
        id = id,
        name = name,
        color = color,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun SnippetBackup.toDomain() = Snippet(
        id = id,
        shortcut = shortcut,
        body = body,
        type = runCatching { SnippetType.valueOf(type) }.getOrDefault(SnippetType.PLAIN),
        triggerMode = runCatching { TriggerMode.valueOf(triggerMode) }.getOrDefault(TriggerMode.ON_DELIMITER),
        folderId = folderId,
        usageCount = usageCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isEnabled = isEnabled,
        caseSensitive = caseSensitive,
        description = description,
    )

    private fun FolderBackup.toDomain() = Folder(
        id = id,
        name = name,
        color = color,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
```

- [ ] **Step 6: Create BackupModule.kt**

```kotlin
// core/backup/src/main/kotlin/dev/a10101100/snipcraft/core/backup/BackupModule.kt
package dev.a10101100.snipcraft.core.backup

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }
}
```

- [ ] **Step 7: Run test to verify it passes**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :core:backup:test --tests "dev.a10101100.snipcraft.core.backup.BackupManagerTest" 2>&1 | tail -30
```
Expected: 5 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add core/backup/
git commit -m "feat: core:backup — BackupManager with export/import + versioned JSON schema"
```

---

## Task 7: feature:onboarding build.gradle.kts — add dependencies

**Files:**
- Modify: `feature/onboarding/build.gradle.kts`

- [ ] **Step 1: Update build file**

Replace the file with (preserving existing structure, adding new deps):

```kotlin
plugins {
    id("snipcraft.android.library.compose")
    id("snipcraft.android.hilt")
}

android {
    namespace = "dev.a10101100.snipcraft.feature.onboarding"

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
    implementation(project(":core:engine"))
    implementation(project(":core:variables"))
    implementation(project(":core:accessibility"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    api(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
```

Note: Check `libs.versions.toml` for exact alias names for `compose.ui.test.junit4`, `junit4`, `junit.vintage.engine`, `junit.platform.launcher`. Use the same aliases as `core:accessibility` which already uses Robolectric.

- [ ] **Step 2: Commit**

```bash
git add feature/onboarding/build.gradle.kts
git commit -m "build: feature:onboarding — add engine, variables, accessibility, test deps"
```

---

## Task 8: OnboardingUiState + OnboardingViewModel (TDD)

**Files:**
- Create: `feature/onboarding/src/main/kotlin/dev/a10101100/snipcraft/feature/onboarding/OnboardingUiState.kt`
- Create: `feature/onboarding/src/main/kotlin/dev/a10101100/snipcraft/feature/onboarding/OnboardingViewModel.kt`
- Create: `feature/onboarding/src/test/kotlin/dev/a10101100/snipcraft/feature/onboarding/OnboardingViewModelTest.kt`
- Also create: `feature/onboarding/src/test/resources/robolectric.properties` with content `sdk=31`

- [ ] **Step 1: Write the failing test**

```kotlin
// feature/onboarding/src/test/kotlin/dev/a10101100/snipcraft/feature/onboarding/OnboardingViewModelTest.kt
package dev.a10101100.snipcraft.feature.onboarding

import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.variables.DateVariableResolver
import dev.a10101100.snipcraft.core.variables.TimeVariableResolver
import dev.a10101100.snipcraft.core.variables.VariableEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OnboardingViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val mockHealthChecker = mockk<ServiceHealthChecker>()
    private val mockSnippetRepo = mockk<SnippetRepository>()
    private val variableEngine = VariableEngine(listOf(DateVariableResolver(), TimeVariableResolver()))

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { mockSnippetRepo.observeEnabled() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(isServiceEnabled: Boolean = false): OnboardingViewModel {
        every { mockHealthChecker.isServiceEnabled() } returns isServiceEnabled
        return OnboardingViewModel(mockHealthChecker, mockSnippetRepo, variableEngine)
    }

    @Test
    fun `initial card is 0 and accessibility not granted when service disabled`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)

        assertEquals(0, vm.uiState.value.currentCard)
        assertFalse(vm.uiState.value.accessibilityGranted)
    }

    @Test
    fun `initial state marks accessibilityGranted when service already enabled`() = runTest {
        val vm = makeViewModel(isServiceEnabled = true)

        assertTrue(vm.uiState.value.accessibilityGranted)
    }

    @Test
    fun `checkPermissionsOnResume sets accessibilityGranted and advances from card 0`() = runTest {
        every { mockHealthChecker.isServiceEnabled() } returns false
        val vm = makeViewModel(isServiceEnabled = false)
        assertEquals(0, vm.uiState.value.currentCard)

        every { mockHealthChecker.isServiceEnabled() } returns true
        vm.checkPermissionsOnResume()

        assertTrue(vm.uiState.value.accessibilityGranted)
        assertEquals(1, vm.uiState.value.currentCard)
    }

    @Test
    fun `checkPermissionsOnResume does not advance if not on card 0`() = runTest {
        val vm = makeViewModel(isServiceEnabled = true)
        vm.onSkip() // advance to card 1

        vm.checkPermissionsOnResume()

        assertEquals(1, vm.uiState.value.currentCard)
    }

    @Test
    fun `onSkip from card 0 advances to card 1`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        vm.onSkip()

        assertEquals(1, vm.uiState.value.currentCard)
    }

    @Test
    fun `onSkip from card 2 advances to card 3 (sandbox)`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        vm.onSkip() // → 1
        vm.onSkip() // → 2
        vm.onSkip() // → 3

        assertEquals(3, vm.uiState.value.currentCard)
    }

    @Test
    fun `onNotificationPermissionResult granted sets notificationGranted and advances from card 1`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        vm.onSkip() // → card 1

        vm.onNotificationPermissionResult(granted = true)

        assertTrue(vm.uiState.value.notificationGranted)
        assertEquals(2, vm.uiState.value.currentCard)
    }

    @Test
    fun `onSandboxTextChanged with ;today + space expands to today date`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";today", body = "{{date:yyyy-MM-dd}}")
        every { mockSnippetRepo.observeEnabled() } returns flowOf(listOf(snippet))
        val vm = makeViewModel(isServiceEnabled = false)

        vm.onSandboxTextChanged(";today ")

        val sandboxText = vm.uiState.value.sandboxText
        // Should match yyyy-MM-dd pattern
        assertTrue(sandboxText.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
            "Expected date pattern but got: $sandboxText")
    }

    @Test
    fun `onSandboxTextChanged with non-matching text does not expand`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)

        vm.onSandboxTextChanged("hello")

        assertEquals("hello", vm.uiState.value.sandboxText)
    }

    @Test
    fun `onComplete emits NavigateToLibrary event`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        val events = mutableListOf<OnboardingEvent>()
        val job = kotlinx.coroutines.launch(dispatcher) {
            vm.events.collect { events.add(it) }
        }

        vm.onComplete()
        job.cancel()

        assertTrue(events.any { it is OnboardingEvent.NavigateToLibrary })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:onboarding:test --tests "dev.a10101100.snipcraft.feature.onboarding.OnboardingViewModelTest" 2>&1 | tail -20
```
Expected: FAIL — classes not found.

- [ ] **Step 3: Create OnboardingUiState.kt**

```kotlin
// feature/onboarding/src/main/kotlin/dev/a10101100/snipcraft/feature/onboarding/OnboardingUiState.kt
package dev.a10101100.snipcraft.feature.onboarding

data class OnboardingUiState(
    val currentCard: Int = 0,
    val accessibilityGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryExemptGranted: Boolean = false,
    val sandboxText: String = "",
    val sandboxExpanded: Boolean = false,
)

sealed interface OnboardingEvent {
    data object NavigateToLibrary : OnboardingEvent
}

val TOTAL_CARDS = 4  // 3 permission cards + 1 sandbox card
```

- [ ] **Step 4: Create OnboardingViewModel.kt**

```kotlin
// feature/onboarding/src/main/kotlin/dev/a10101100/snipcraft/feature/onboarding/OnboardingViewModel.kt
package dev.a10101100.snipcraft.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.ExpansionContext
import dev.a10101100.snipcraft.core.engine.TrieMatcher
import dev.a10101100.snipcraft.core.variables.VariableEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val serviceHealthChecker: ServiceHealthChecker,
    private val snippetRepository: SnippetRepository,
    private val variableEngine: VariableEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val sandboxTrie = TrieMatcher()
    private var shortcutToBody: Map<String, String> = emptyMap()

    init {
        val serviceEnabled = serviceHealthChecker.isServiceEnabled()
        _uiState.update { it.copy(accessibilityGranted = serviceEnabled) }

        viewModelScope.launch {
            val snippets = snippetRepository.observeEnabled().first()
            shortcutToBody = snippets.associate { it.shortcut to it.body }
            sandboxTrie.rebuild(snippets.map { it.shortcut })
        }
    }

    fun checkPermissionsOnResume() {
        val enabled = serviceHealthChecker.isServiceEnabled()
        if (enabled && !_uiState.value.accessibilityGranted) {
            _uiState.update { it.copy(accessibilityGranted = true) }
            if (_uiState.value.currentCard == 0) {
                _uiState.update { it.copy(currentCard = 1) }
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(notificationGranted = granted) }
        if (granted && _uiState.value.currentCard == 1) {
            _uiState.update { it.copy(currentCard = 2) }
        }
    }

    fun onBatteryExemptGranted(granted: Boolean) {
        _uiState.update { it.copy(batteryExemptGranted = granted) }
    }

    fun onSkip() {
        val current = _uiState.value.currentCard
        if (current < TOTAL_CARDS - 1) {
            _uiState.update { it.copy(currentCard = current + 1) }
        }
    }

    fun onSandboxTextChanged(newText: String) {
        _uiState.update { it.copy(sandboxText = newText, sandboxExpanded = false) }
        if (newText.isEmpty() || newText.last() !in DELIMITERS) return

        val textBeforeDelim = newText.dropLast(1)
        val matchedShortcut = sandboxTrie.matchSuffix(textBeforeDelim) ?: return
        val body = shortcutToBody[matchedShortcut] ?: return

        viewModelScope.launch {
            val ctx = ExpansionContext(
                packageName = "dev.a10101100.snipcraft",
                fieldText = textBeforeDelim,
                cursorPosition = textBeforeDelim.length,
            )
            val expanded = variableEngine.resolve(body, ctx)
            _uiState.update { it.copy(sandboxText = expanded, sandboxExpanded = true) }
        }
    }

    fun onComplete() {
        viewModelScope.launch { _events.send(OnboardingEvent.NavigateToLibrary) }
    }

    companion object {
        private val DELIMITERS = setOf(' ', '\t', '\n', '.', ',', '!', '?', ';', ':', ')', ']', '}', '\'', '"')
    }
}
```

**NOTE:** `VariableEngine` is a pure-Kotlin class in `core:variables`. It is NOT a Hilt-provided singleton by default. Check `core:variables/src/main/...` for a Hilt module. If none exists, the feature:onboarding module needs to provide its own `VariableEngine` via a Hilt module, OR the ViewModel must create it directly.

If `VariableEngine` is not Hilt-bound in a way accessible from `feature:onboarding`, create a local `OnboardingModule.kt`:

```kotlin
// feature/onboarding/src/main/kotlin/.../feature/onboarding/OnboardingModule.kt
package dev.a10101100.snipcraft.feature.onboarding

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.a10101100.snipcraft.core.variables.DateVariableResolver
import dev.a10101100.snipcraft.core.variables.TimeVariableResolver
import dev.a10101100.snipcraft.core.variables.VariableEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule {
    @Provides @Singleton
    fun provideVariableEngine(): VariableEngine =
        VariableEngine(listOf(DateVariableResolver(), TimeVariableResolver()))
}
```

Check `core/accessibility/src/main/kotlin/.../core/accessibility/VariableEngineModule.kt` — if `VariableEngine` is already provided there at `SingletonComponent`, you don't need a duplicate. If it's provided there, just depend on the singleton in the constructor.

- [ ] **Step 5: Create robolectric.properties**

```
# feature/onboarding/src/test/resources/robolectric.properties
sdk=31
```

- [ ] **Step 6: Run test to verify it passes**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:onboarding:test --tests "dev.a10101100.snipcraft.feature.onboarding.OnboardingViewModelTest" 2>&1 | tail -30
```
Expected: all PASS.

- [ ] **Step 7: Commit**

```bash
git add feature/onboarding/src/
git commit -m "feat: OnboardingViewModel + OnboardingUiState with permission detection and sandbox engine"
```

---

## Task 9: OnboardingScreen — 3-card HorizontalPager + sandbox

**Files:**
- Create: `feature/onboarding/src/main/kotlin/.../feature/onboarding/OnboardingScreen.kt`

- [ ] **Step 1: Create OnboardingScreen.kt**

```kotlin
// feature/onboarding/src/main/kotlin/dev/a10101100/snipcraft/feature/onboarding/OnboardingScreen.kt
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Collect navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.NavigateToLibrary -> onComplete()
            }
        }
    }

    // Check accessibility on resume (user may have granted while in Settings)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.checkPermissionsOnResume()
        }
    }

    // Notification permission launcher (API 33+)
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onNotificationPermissionResult(granted) }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentCard,
        pageCount = { TOTAL_CARDS },
    )

    // Sync pager to ViewModel's currentCard
    LaunchedEffect(uiState.currentCard) {
        if (pagerState.currentPage != uiState.currentCard) {
            pagerState.animateScrollToPage(uiState.currentCard)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> AccessibilityCard(
                        granted = uiState.accessibilityGranted,
                        onGrant = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
                            val already = pm.isIgnoringBatteryOptimizations(context.packageName)
                            if (already) {
                                viewModel.onBatteryExemptGranted(true)
                            } else {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                } catch (_: Exception) { /* OEM may not support */ }
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
        description = "Some devices aggressively kill background apps. Exempting Snipcraft from battery optimisation ensures snippets expand reliably, even hours after you last used the app.",
        granted = granted,
        grantButtonLabel = "Exempt from Battery Optimisation",
        grantButtonTag = "battery_grant_button",
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
            onClick = if (granted) onSkip else onSkip,
            modifier = Modifier.fillMaxWidth().testTag("skip_button_$grantButtonTag"),
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
```

- [ ] **Step 2: Run the full onboarding module test suite**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:onboarding:test 2>&1 | tail -30
```
Expected: all PASS (ViewModel tests pass; screen has no unit tests yet — that's OK, the ViewModel tests cover behaviour).

- [ ] **Step 3: Commit**

```bash
git add feature/onboarding/src/main/kotlin/
git commit -m "feat: OnboardingScreen — 3-card HorizontalPager with permission flows and sandbox expansion"
```

---

## Task 10: Wire OnboardingRoute into SnipNavHost

**Files:**
- Modify: `app/src/main/kotlin/dev/a10101100/snipcraft/navigation/SnipNavHost.kt`

- [ ] **Step 1: Update SnipNavHost.kt**

Add `OnboardingRoute` and start-destination logic. Read the current file first, then apply changes:

```kotlin
// Add to the @Serializable routes at the top:
@Serializable object OnboardingRoute

// In SnipNavHost composable body, determine start destination:
val context = LocalContext.current
val startDestination: Any = remember {
    val checker = ServiceHealthChecker(context)
    if (checker.isServiceEnabled()) LibraryRoute else OnboardingRoute
}

// Change NavHost to use startDestination:
NavHost(navController = navController, startDestination = startDestination) {
    // existing composables unchanged...

    // Add:
    composable<OnboardingRoute> {
        OnboardingScreen(
            onComplete = {
                navController.navigate(LibraryRoute) {
                    popUpTo<OnboardingRoute> { inclusive = true }
                }
            }
        )
    }
}
```

**NOTE:** The bottom NavigationBar visibility logic in the current NavHost likely checks if the current destination is a top-level route. Make sure `OnboardingRoute` is excluded from the bottom nav by checking:
```kotlin
val showBottomNav = currentDestination?.hierarchy?.any {
    it.hasRoute(LibraryRoute::class) || it.hasRoute(SettingsRoute::class)
} == true
```
(Do NOT include OnboardingRoute in showBottomNav check.)

Also add the import: `import dev.a10101100.snipcraft.feature.onboarding.OnboardingScreen`

- [ ] **Step 2: Build the app to verify wiring**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :app:assembleDebug 2>&1 | tail -30
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/dev/a10101100/snipcraft/navigation/SnipNavHost.kt
git commit -m "feat: wire OnboardingRoute into SnipNavHost — shows on first launch when service not enabled"
```

---

## Task 11: Settings — Blacklist UI (TDD)

**Files:**
- Modify: `feature/settings/build.gradle.kts`
- Modify: `feature/settings/src/main/kotlin/.../feature/settings/SettingsUiState.kt`
- Modify: `feature/settings/src/main/kotlin/.../feature/settings/SettingsViewModel.kt`
- Modify: `feature/settings/src/main/kotlin/.../feature/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/test/kotlin/.../feature/settings/SettingsViewModelTest.kt` (add new tests)

- [ ] **Step 1: Update feature/settings/build.gradle.kts**

Add to dependencies:
```kotlin
implementation(project(":core:backup"))
implementation(project(":core:compatibility"))
```

Also add for blacklist — accessing PackageManager is plain Android, no extra dep needed.

- [ ] **Step 2: Write failing ViewModel tests**

Find `SettingsViewModelTest.kt` and add:

```kotlin
// Additional imports needed:
import dev.a10101100.snipcraft.core.data.CompatibilityRuleRepository
import dev.a10101100.snipcraft.core.backup.BackupManager
import dev.a10101100.snipcraft.core.backup.ConflictStrategy
import dev.a10101100.snipcraft.core.backup.ImportResult
import kotlinx.coroutines.flow.flowOf

// Add to the test class, with mockk fields:
private val mockCompatibilityRepo = mockk<CompatibilityRuleRepository>(relaxed = true)
private val mockBackupManager = mockk<BackupManager>(relaxed = true)

@Test
fun `blacklisted packages loaded from repository on init`() = runTest {
    every { mockCompatibilityRepo.observeBlacklistedPackages() } returns flowOf(listOf("com.example.app"))
    val vm = SettingsViewModel(/* inject mockCompatibilityRepo, mockBackupManager */)

    advanceUntilIdle()
    assertEquals(listOf("com.example.app"), vm.uiState.value.blacklistedPackages)
}

@Test
fun `removeFromBlacklist calls repository`() = runTest {
    val vm = SettingsViewModel(/* ... */)

    vm.removeFromBlacklist("com.example.app")

    coVerify { mockCompatibilityRepo.removeFromBlacklist("com.example.app") }
}

@Test
fun `export calls backupManager and emits ShareExport event`() = runTest {
    coEvery { mockBackupManager.export() } returns """{"version":1,"exportedAt":"","snippets":[],"folders":[]}"""
    val vm = SettingsViewModel(/* ... */)
    val events = mutableListOf<SettingsEvent>()
    val job = launch { vm.events.collect { events.add(it) } }

    vm.onExport()
    advanceUntilIdle()
    job.cancel()

    assertTrue(events.any { it is SettingsEvent.ShareExport })
}

@Test
fun `import with json shows conflict dialog`() = runTest {
    val vm = SettingsViewModel(/* ... */)

    vm.onImportJsonReceived("""{"version":1,"exportedAt":"","snippets":[],"folders":[]}""")

    assertTrue(vm.uiState.value.showImportConflictDialog)
}
```

NOTE: `SettingsViewModel` currently uses a secondary constructor for tests. Extend that pattern to accept the new dependencies. Read the current SettingsViewModel carefully before writing tests.

- [ ] **Step 3: Run tests to verify they fail**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:settings:test 2>&1 | tail -20
```
Expected: FAIL — new fields not found.

- [ ] **Step 4: Update SettingsUiState.kt**

Add to `SettingsUiState`:

```kotlin
data class SettingsUiState(
    val isAccessibilityServiceEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "",
    // Pass 6 additions:
    val blacklistedPackages: List<String> = emptyList(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val showImportConflictDialog: Boolean = false,
    val pendingImportJson: String? = null,
    val importResult: ImportResult? = null,
)
```

Add a `SettingsEvent` sealed interface (or Channel events):

```kotlin
sealed interface SettingsEvent {
    data class ShareExport(val json: String) : SettingsEvent
    data class ShowImportResult(val result: ImportResult) : SettingsEvent
}
```

- [ ] **Step 5: Update SettingsViewModel.kt**

Inject `CompatibilityRuleRepository` and `BackupManager`. Add:

```kotlin
@Inject lateinit var compatibilityRuleRepository: CompatibilityRuleRepository
@Inject lateinit var backupManager: BackupManager

private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()

// In init:
viewModelScope.launch {
    compatibilityRuleRepository.observeBlacklistedPackages().collect { packages ->
        _uiState.update { it.copy(blacklistedPackages = packages) }
    }
}

fun addToBlacklist(packageName: String) {
    viewModelScope.launch { compatibilityRuleRepository.addToBlacklist(packageName) }
}

fun removeFromBlacklist(packageName: String) {
    viewModelScope.launch { compatibilityRuleRepository.removeFromBlacklist(packageName) }
}

fun onExport() {
    viewModelScope.launch {
        _uiState.update { it.copy(isExporting = true) }
        runCatching { backupManager.export() }
            .onSuccess { json -> _events.send(SettingsEvent.ShareExport(json)) }
        _uiState.update { it.copy(isExporting = false) }
    }
}

fun onImportJsonReceived(json: String) {
    _uiState.update { it.copy(showImportConflictDialog = true, pendingImportJson = json) }
}

fun onImportConflictResolved(strategy: ConflictStrategy) {
    val json = _uiState.value.pendingImportJson ?: return
    _uiState.update { it.copy(showImportConflictDialog = false, pendingImportJson = null, isImporting = true) }
    viewModelScope.launch {
        val result = runCatching { backupManager.import(json, strategy) }.getOrNull()
        _uiState.update { it.copy(isImporting = false, importResult = result) }
        result?.let { _events.send(SettingsEvent.ShowImportResult(it)) }
    }
}

fun dismissImportResult() {
    _uiState.update { it.copy(importResult = null) }
}
```

- [ ] **Step 6: Update SettingsScreen.kt**

Add two new sections to `SettingsContent()`. After the "Appearance" section and before "About":

```kotlin
// --- Excluded Apps section ---
SectionHeader("Excluded Apps")
uiState.blacklistedPackages.forEach { pkg ->
    ListItem(
        headlineContent = { Text(pkg, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            IconButton(onClick = { onRemoveFromBlacklist(pkg) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove $pkg")
            }
        }
    )
}
if (uiState.blacklistedPackages.isEmpty()) {
    ListItem(headlineContent = {
        Text("No apps excluded", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    })
}
OutlinedButton(
    onClick = onAddToBlacklist,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
) { Text("Add excluded app") }
HorizontalDivider()

// --- Backup section ---
SectionHeader("Backup & Restore")
ListItem(
    headlineContent = { Text("Export snippets") },
    supportingContent = { Text("Share as JSON file") },
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
HorizontalDivider()
```

The `SettingsContent()` composable gains new parameters:
```kotlin
onRemoveFromBlacklist: (String) -> Unit,
onAddToBlacklist: () -> Unit,
onExport: () -> Unit,
onImport: () -> Unit,
```

In `SettingsScreen()`, add the "add app" dialog state and the file import launcher. Add to the composable body:

```kotlin
// Add app dialog state
var showAddAppDialog by remember { mutableStateOf(false) }
var addAppText by remember { mutableStateOf("") }

// File picker for import
val importLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
) { uri ->
    uri ?: return@rememberLauncherForActivityResult
    val content = context.contentResolver.openInputStream(uri)
        ?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
    viewModel.onImportJsonReceived(content)
}

// Collect share events
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
            is SettingsEvent.ShowImportResult -> { /* show snackbar — use snackbarHostState */ }
        }
    }
}

// Conflict resolution dialog
if (uiState.showImportConflictDialog) {
    AlertDialog(
        onDismissRequest = { /* dismiss */ },
        title = { Text("Import conflict") },
        text = { Text("How should existing snippets be handled?") },
        confirmButton = {
            TextButton(onClick = { viewModel.onImportConflictResolved(ConflictStrategy.OVERWRITE) }) {
                Text("Overwrite")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.onImportConflictResolved(ConflictStrategy.SKIP_EXISTING) }) {
                Text("Skip existing")
            }
        }
    )
}

// Add app dialog
if (showAddAppDialog) {
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
        }
    )
}
```

Wire callbacks in the `SettingsContent()` call inside `SettingsScreen()`:
```kotlin
onRemoveFromBlacklist = { pkg -> viewModel.removeFromBlacklist(pkg) },
onAddToBlacklist = { showAddAppDialog = true },
onExport = { viewModel.onExport() },
onImport = { importLauncher.launch("*/*") },
```

- [ ] **Step 7: Run all settings tests**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew :feature:settings:test 2>&1 | tail -30
```
Expected: all PASS.

- [ ] **Step 8: Full assembleDebug**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleDebug 2>&1 | tail -30
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add feature/settings/
git commit -m "feat: Settings — blacklist UI (add/remove excluded apps) and JSON export/import"
```

---

## Task 12: Run full test suite + verify no regressions

- [ ] **Step 1: Run all tests**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew test 2>&1 | tail -40
```
Expected: all tests PASS. Count should be ≥ 129 (prior) + new tests.

- [ ] **Step 2: Final assembleDebug**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleDebug 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL.

---

## Task 13: CHANGELOG + HANDOFF update + PR

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `docs/HANDOFF.md`

- [ ] **Step 1: Update CHANGELOG.md**

Append to the `## v0.1.0-dev` section:

```markdown
### Pass 6 — Onboarding + Backup + Blacklist (2026-05-13)

- `feature:onboarding` — 3-card HorizontalPager permission carousel: Accessibility, Notifications (POST_NOTIFICATIONS on API 33+), Battery exemption; auto-advance on accessibility grant detected on resume; Skip button on each card; sandbox test field wired to real expansion engine (TrieMatcher + VariableEngine); "Get Started" navigates to Library
- `core:backup` — BackupManager: `export()` serializes all snippets + folders to versioned JSON (`version: 1`), `import()` with SKIP_EXISTING / OVERWRITE conflict resolution; kotlinx.serialization
- `feature:settings` — Backup & Restore section: Export button (system share sheet), Import button (file picker + conflict dialog); Excluded Apps section: add/remove package blacklist
- `CompatibilityRuleRepository` in `core:data` — CRUD for user-defined blacklisted packages; wired to CompatibilityResolver via live Flow subscription in SnipAccessibilityService
- `SnipNavHost` — OnboardingRoute added; start destination is Library if service enabled, Onboarding otherwise
- Tests: +N new tests across core:data, core:backup, feature:onboarding, feature:settings (update N after run)
```

- [ ] **Step 2: Update docs/HANDOFF.md**

Add a Pass 6 section at the bottom of section 5 "What's Been Shipped", update section 9 "Current State" (test count, what works), and update section 10 "What's Next" to remove Pass 6 from the list and describe Pass 7 as next.

Key additions to HANDOFF.md:

**Section 5 addition:**
```markdown
**Pass 6 — Onboarding + Backup + Blacklist (TDD):** `feature:onboarding` HorizontalPager (4 pages: Accessibility/Notification/Battery permission cards + sandbox expansion field). `core:backup` BackupManager (JSON export/import, ConflictStrategy). `CompatibilityRuleRepository` in `core:data` for user blacklist (observeBlacklistedPackages, addToBlacklist, removeFromBlacklist). `CompatibilityResolver.setUserBlacklist()` updated live from `SnipAccessibilityService` subscription. `feature:settings` gains Backup & Restore section + Excluded Apps section. `SnipNavHost` shows OnboardingRoute as start destination when service not enabled. Test count: [update after run].
```

**Section 9 update:**
```markdown
| Field | Value |
| Main SHA | [merge SHA] |
| Branch | `main` (clean, all merged) |
| Test count | **[new count] unit tests**, all green |
| `assembleDebug` | CLEAN |
```

**Section 10 update:** Remove "Pass 6 — Onboarding + Export/Import + Blacklist (target this dispatch)" block. Pass 7 (WebDAV) is next.

- [ ] **Step 3: Final commit**

```bash
git add CHANGELOG.md docs/HANDOFF.md
git commit -m "docs: update CHANGELOG and HANDOFF for Pass 6"
```

- [ ] **Step 4: Push branch and create PR**

```bash
git push -u origin claude/pass-6-onboarding-import-export-blacklist
gh pr create \
  --title "feat: Pass 6 — Onboarding, JSON backup, per-app blacklist UI" \
  --body "## Summary
- 3-card permission carousel (Accessibility, Notification, Battery) with sandbox expansion field
- JSON export/import in Settings (versioned schema, SKIP/OVERWRITE conflict resolution)
- Per-app blacklist UI in Settings (add by package name, live Room-backed)
- CompatibilityResolver updated to use live user blacklist from DB
- OnboardingRoute wired as start destination when service not enabled

## Test plan
- [ ] All existing 129 tests pass (no regressions)
- [ ] New onboarding ViewModel tests cover permission detection + sandbox expansion
- [ ] BackupManager round-trip test: export → import restores all snippets
- [ ] CompatibilityRuleRepository tests: add/remove/observe
- [ ] assembleDebug clean

🤖 Generated with Claude Code"
```

- [ ] **Step 5: Verify CI / wait for tests then merge**

```bash
gh pr checks --watch
gh pr merge --squash
```

---

## Key invariants to preserve

1. **Password field exclusion** — CompatibilityResolver's `defaultStrategyFor()` must still return `DISABLED` for password fields regardless of user blacklist. User blacklist is an additional blacklist, not a replacement.

2. **Hilt graph** — if `VariableEngine` is already provided as `@Singleton` in `VariableEngineModule` (core:accessibility), don't duplicate the binding in `OnboardingModule`. Check for duplicate bindings.

3. **Test tags must be unique** — each `testTag` in the onboarding screen must be unique across the full composition.

4. **No hardcoded test counts** — after running tests, update HANDOFF.md with the actual count from Gradle output.

5. **Robolectric properties** — every Android module test directory needs `src/test/resources/robolectric.properties` with `sdk=31`. Check before adding to avoid duplicate.
