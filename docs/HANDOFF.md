# Snipcraft — Pass 8 Handoff

> For a fresh Sonnet session picking up from Pass 7 (completed 2026-05-14).
> Read this file in full before touching any code.

---

## 1. Project Identity

| Field | Value |
|---|---|
| App name | **Snipcraft** |
| Local path | `D:\a10101100_labs\snipcraft` |
| GitHub repo | `https://github.com/annabel-lee-x10/snipcraft` |
| Package | `dev.a10101100.snipcraft` |
| Plan doc | `D:\a10101100_labs\PLAN_text_expander.md` |
| License | MIT |

---

## 2. User's Final Decisions (do not re-litigate)

1. **Name:** Snipcraft
2. **Local path:** `D:\a10101100_labs\snipcraft`
3. **GitHub:** `annabel-lee-x10/snipcraft` (public)
4. **Package:** `dev.a10101100.snipcraft`
5. **License:** MIT (not Apache 2.0 — personal project)
6. **minSdk:** 31 (Android 12 — Pixel 8 Pro and newer only; no legacy compat code)
7. **target + compileSdk:** 35 (Android 15)
8. **Tablet support:** skip MVP
9. **Foreground service notification:** persistent (sideload only, no Play Store to please)
10. **Sync:** WebDAV in MVP (user has multiple Android phones — pass 7)
11. **Personal use only:** skip CI, no Sentry, no SQLCipher, no monetization, no telemetry
12. **App icon:** placeholder text logo for MVP
13. **Branding:** defer to Phase 2+
14. **Crash reporting:** local logs only (Timber), no remote

---

## 3. Tech Stack as Actually Shipped

| Component | Version | Notes |
|---|---|---|
| Gradle | 9.3.1 | Cached at `~/.gradle/wrapper/dists/gradle-9.3.1-bin/` |
| AGP | 9.1.0 | **AGP 9.x no longer requires `kotlin.android` plugin** — omit it |
| Kotlin | 2.3.20 | Cached at `~/.gradle/caches/` |
| KSP | 2.3.7 | **Flat versioning** (not `2.3.20-1.0.x`) — check catalog before updating |
| Compose BOM | 2026.05.00 | Latest at execution time |
| Material 3 | via BOM | Dynamic colors on API 31+ |
| Navigation Compose | 2.9.8 | Type-safe `@Serializable` routes (requires `kotlin.serialization` on `:app`) |
| Hilt | 2.59.2 | `@HiltViewModel`, `@AndroidEntryPoint`, `@HiltWorker` |
| Room | 2.8.4 | `exportSchema = true`; schemas at `core/database/schemas/` |
| WorkManager | 2.11.2 | HiltWorkerFactory; auto-initializer disabled in manifest |
| Coroutines | 1.11.0 | `viewModelScope`, `CoroutineScope(Main + SupervisorJob())` in service |
| Lifecycle | 2.10.0 | `collectAsStateWithLifecycle()` in all screens |
| JDK | 21 (Android Studio JBR) | Path: `C:\Program Files\Android\Android Studio\jbr` |
| JUnit 5 | 5.12.2 | ViewModel unit tests |
| JUnit 4 + Robolectric | 4.14.1 | Android/Compose tests — needs `junit-vintage-engine` |
| Turbine | 1.2.0 | Flow assertions |
| MockK | 1.14.2 | Kotlin-native mocking |

**Build command:**
```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" ./gradlew ...
```
`gradle.properties` sets `org.gradle.java.home` so the daemon finds the JDK. `JAVA_HOME` is NOT in system PATH — must be set in shell.

`local.properties` (gitignored):
```
sdk.dir=C\:\\Users\\User\\AppData\\Local\\Android\\Sdk
```
Note the escaped backslash — that format is required by AGP 9.x SDK path validation.

---

## 4. Module Structure

### Pure-Kotlin JVM (no Android dep — fastest tests, no emulator needed)

| Module | Description | Pass shipped |
|---|---|---|
| `:core:domain` | Snippet, Folder, TriggerMode, SnippetType, ExpansionStrategy, ExpansionContext, CompatibilityProfile data classes | Pass 2 |
| `:core:engine` | TrieMatcher (suffix trie), OnDelimiterTrigger, UndoStack | Pass 2 |
| `:core:variables` | TemplateParser (`{{token:arg}}`), VariableResolver interface, DateVariableResolver, TimeVariableResolver, ClipboardVariableResolver (closure-based), VariableEngine | Pass 2 |

### Android Libraries

| Module | Description | Pass shipped |
|---|---|---|
| `:core:common` | AppLogger (Timber wrapper: `plantDebugTree()` / `plantReleaseTree()`) | Pass 3 |
| `:core:accessibility` | SnipAccessibilityService (`@AndroidEntryPoint`, full expansion pipeline), AccessibilityEventProcessor (password exclusion), ExpansionExecutor (SET_TEXT + PASTE), SnippetCacheManager, SnipForegroundService (`specialUse`), HealthWatchdogWorker (30-min WorkManager), ServiceHealthChecker, NotificationChannels, VariableEngineModule (Hilt) | Pass 3+4 |
| `:core:compatibility` | CompatibilityResolver (built-in PASTE for Chrome/Discord/Slack/WhatsApp/etc, DISABLED for systemui), CompatibilityModule (Hilt) | Pass 4 |
| `:core:database` | Room v1: SnippetEntity/FolderEntity/CompatibilityRuleEntity (enums as strings), SnippetDao/FolderDao/CompatibilityRuleDao, SnipcraftDatabase, DatabaseModule (Hilt), SnippetMapper/FolderMapper | Pass 4 |
| `:core:data` | SnippetRepository + FolderRepository (interface + impl), DataModule (Hilt), SeedDataPopulator (8 starter snippets seeded on first launch) | Pass 4 |
| `:core:designsystem` | SnipTheme (Material 3, dynamic colors on API 31+, dark-first fallback), SnipTypography, Color tokens, EmptyState composable | Pass 5 |
| `:core:ui` | Skeleton (shared composables — not yet populated) | Pass 1 skeleton |
| `:core:testing` | Skeleton (fake repos, test rules — not yet populated) | Pass 1 skeleton |
| `:core:backup` | Skeleton (JSON import/export — Pass 6) | Pass 1 skeleton |

### Feature Modules (Compose + Hilt)

| Module | Description | Pass shipped |
|---|---|---|
| `:feature:library` | LibraryScreen (search toggle, sort DropdownMenu, pin, LazyColumn, empty state, FAB), LibraryViewModel (StateFlow, observeEnabled, filter+sort derived state), `testTag` on `search_field` + `snippet_list` | Pass 5 |
| `:feature:editor` | EditorScreen (shortcut/body/description OutlinedTextField, enabled Switch, Save TextButton in TopAppBar tagged `save_button`, Delete icon for edit), EditorViewModel (load snippet, canSave, save/delete → NavigateBack events via Channel) | Pass 5 |
| `:feature:settings` | SettingsScreen (service status tile, Open Accessibility Settings, Re-check, theme FilterChips, About), SettingsViewModel (ServiceHealthChecker, ThemeMode) | Pass 5 |
| `:feature:onboarding` | Skeleton — **Pass 6 target** | Pass 1 skeleton |
| `:feature:diagnostics` | Skeleton — Pass 8 | Pass 1 skeleton |
| `:feature:quickadd` | Phase 2 stub — no test file (Compose plugin without runtime would fail) | Pass 1 skeleton |

### App

| Module | Description |
|---|---|
| `:app` | SnipApplication (Hilt + HiltWorkerFactory + seed data), MainActivity, SnipNavHost (type-safe routes: LibraryRoute/SettingsRoute/EditorRoute, bottom nav) |

### Build Logic

| Module | Description |
|---|---|
| `:build-logic` | Convention plugins: `snipcraft.android.application`, `.android.library`, `.android.library.compose`, `.kotlin.jvm`, `.android.hilt` |

---

## 5. What's Been Shipped — Per Pass

**Pass 1 — Project skeleton:** Gradle 9.3.1 + AGP 9.1.0 + Kotlin 2.3.20, all 20 module directories wired into `settings.gradle.kts`, `build-logic` convention plugins, JUnit 5 + JUnit 4 + Robolectric test infrastructure, minimal `MainActivity` with Compose placeholder, `SnipApplication` with Hilt. 18 placeholder tests, `assembleDebug` clean.

**Pass 2 — Pure-Kotlin engine (TDD):** `core:domain` data classes (Snippet, Folder, TriggerMode, SnippetType, ExpansionStrategy, ExpansionContext, CompatibilityProfile). `core:engine` trie-based suffix matcher, ON_DELIMITER trigger, 5-deep undo stack. `core:variables` template parser (`{{token:arg}}`), three built-in resolvers (date/time/clipboard), pluggable `VariableResolver` interface, `VariableEngine` single-pass dispatcher. 50 unit tests, all JUnit 5.

**Pass 3 — AccessibilityService skeleton (TDD):** `core:common` `AppLogger`. `core:accessibility` `SnipAccessibilityService` (stub — no expansion yet), `AccessibilityEventProcessor` (password detection via two-layer check), `SnipForegroundService` (`specialUse` type, `IMPORTANCE_MIN`, `START_STICKY`), `HealthWatchdogWorker` (30-min WorkManager periodic), `ServiceHealthChecker`, `NotificationChannels`, `accessibility_service_config.xml`. App wired with `HiltWorkerFactory`, WorkManager auto-initializer disabled. 11 Robolectric tests.

**Pass 4 — Room database + repositories + live expansion (TDD):** `core:database` Room v1 (3 entities, 3 DAOs, schema exported to `schemas/`). `core:data` SnippetRepository + FolderRepository, SeedDataPopulator (8 snippets seeded when DB empty). `core:compatibility` CompatibilityResolver. `core:accessibility` upgraded: `@AndroidEntryPoint`, `ExpansionExecutor` (SET_TEXT + PASTE strategies), `SnippetCacheManager` (Flow-driven live trie rebuild), `VariableEngineModule` (ClipboardManager wired). `SnipAccessibilityService` fully wired with injection. 23 new tests, total 108.

**Pass 5 — Compose CRUD UI (TDD):** `core:designsystem` SnipTheme + typography + EmptyState. `feature:library` LibraryScreen + LibraryViewModel. `feature:editor` EditorScreen + EditorViewModel. `feature:settings` SettingsScreen + SettingsViewModel. `SnipNavHost` with type-safe `@Serializable` routes and bottom NavigationBar. `MainActivity` now renders `SnipNavHost`. 21 new tests (ViewModel + Compose), total 129.

**Pass 6 — Onboarding + Backup + Blacklist (TDD):** `CompatibilityRuleRepository` in `core:data` (observeBlacklistedPackages/addToBlacklist/removeFromBlacklist). `CompatibilityResolver.setUserBlacklist()` updated live from `SnipAccessibilityService` Flow subscription. `ServiceHealthChecker` now `@Singleton @Inject constructor` (Hilt-injectable). `core:backup` BackupManager (export → versioned JSON v1, import with SKIP_EXISTING/OVERWRITE conflict). `feature:onboarding` HorizontalPager (4 pages: Accessibility → Notification → Battery → Sandbox expansion using real TrieMatcher + VariableEngine). `SnipNavHost` shows OnboardingRoute when service not enabled. `feature:settings` Excluded Apps + Backup & Restore sections. 28 new tests, total 157.

**Pass 7 — WebDAV sync (TDD):** `core:sync` new module — WebDavClient (OkHttp 4.12.0, PROPFIND/GET/PUT/MKCOL, Basic Auth, `MasterKeys.AES256_GCM_SPEC` credential storage, HTTP rejection), SyncEngine (bidirectional merge: syncVersion primary, updatedAt secondary, remote wins on tie), CredentialStore (EncryptedSharedPreferences/AES256-GCM), SyncWorker (@HiltWorker, WorkManager periodic/one-shot), SyncModule (@SyncJson qualifier). Room v1→v2 migration: `syncVersion INTEGER NOT NULL DEFAULT 0` on snippets + folders. `feature:settings` WebDAV sync section with SyncViewModel. Test count: see CHANGELOG.

---

## 6. Architecture Decisions Made During Execution

These were discovered the hard way — don't re-discover them.

### Accessibility / Android Service

**Password detection is a two-layer check:**
```kotlin
// AccessibilityEventProcessor.kt
val isPasswordNode = node.isPassword || isPasswordInputType(inputType)
// isPasswordInputType checks TYPE_TEXT_VARIATION_PASSWORD, TYPE_TEXT_VARIATION_WEB_PASSWORD,
// TYPE_NUMBER_VARIATION_PASSWORD, TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
```
`node.isPassword` alone is insufficient — WebView-injected fields return `false` for `isPassword` but set the inputType variation correctly.

**Foreground service `specialUse` requires `<property>` tag:**
```xml
<service android:foregroundServiceType="specialUse" ...>
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="accessibility-companion" />
</service>
```
AGP lints on missing property even for sideloads. Already committed in `core/accessibility/src/main/AndroidManifest.xml`.

**WorkManager + Hilt requires disabling the auto-initializer:**
```xml
<!-- app/src/main/AndroidManifest.xml -->
<provider android:name="androidx.startup.InitializationProvider" ...>
    <meta-data android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup" tools:node="remove" />
</provider>
```
`SnipApplication implements Configuration.Provider` and provides `HiltWorkerFactory`. Already wired.

**AccessibilityService scope:**
```kotlin
// SnipAccessibilityService.kt
private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
// Cancel in onUnbind() — not in onDestroy() which may not be called
override fun onUnbind(intent: Intent?): Boolean {
    serviceScope.cancel()
    return super.onUnbind(intent)
}
```

**Trie match uses backward scan from last delimiter:**
The `SnipAccessibilityService.onAccessibilityEvent()` finds the last delimiter in the field text, then calls `trieMatcher.matchSuffix(textBeforeDelim)`. Don't use naive `lastIndexOf(shortcut)` — it doesn't handle edge cases with repeated text.

### Room / Database

**Enums stored as STRING names — no TypeConverter needed:**
```kotlin
// SnippetEntity.kt
val type: String,        // SnippetType.name
val triggerMode: String, // TriggerMode.name
// Mapper converts: SnippetType.valueOf(entity.type)
```
Simpler, debuggable in SQLite Browser, trivially migrated vs serialized enum ordinals. Room plugin (`androidx.room`) hooks into KSP that's already applied by `snipcraft.android.hilt`. **Do NOT add `alias(libs.plugins.ksp)` again** to the database module — it will fail.

### Testing

**Robolectric setup (required for every Android module with Robolectric tests):**
```kotlin
// build.gradle.kts
testOptions { unitTests { isIncludeAndroidResources = true } }
// test dependencies:
testImplementation(libs.junit4)               // provides @RunWith, @Before, @Test (JUnit 4)
testRuntimeOnly(libs.junit.vintage.engine)    // bridges JUnit 4 → JUnit Platform
testRuntimeOnly(libs.junit.platform.launcher) // required by Gradle 9 for useJUnitPlatform()
testImplementation(libs.robolectric)
// test/resources/robolectric.properties:  sdk=31
```
Tests use `@RunWith(RobolectricTestRunner::class)` with JUnit 4 annotations (`@Before`, `@Test` from `org.junit`).

**Compose tests: save buttons in TopAppBar, not scrolled content:**
`performScrollTo()` from `compose-ui-test-junit4` is not available in library module unit tests. Any composable that needs to be clicked in a Compose test must be above the fold. The `save_button` testTag is on the TopAppBar `TextButton`, not the body-area `Button`.

**Unique testTags:** Having two nodes with the same `testTag` causes unpredictable `onNodeWithTag()` behavior. Each tagged node must be unique within a composition.

**ViewModel tests use `UnconfinedTestDispatcher`:**
```kotlin
@BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
@AfterEach fun tearDown() { Dispatchers.resetMain() }
```
This makes coroutines in `viewModelScope` run immediately, so the ViewModel state is updated before the first assertion.

### Compose / Navigation

**`@Serializable` routes need `kotlin.serialization` plugin only on `:app`:**
Routes are defined in `app/src/main/kotlin/.../navigation/SnipNavHost.kt`. Feature modules don't need the plugin.

**`ClipboardVariableResolver` uses a closure to keep `core:variables` pure Kotlin:**
```kotlin
// core:accessibility / VariableEngineModule.kt
val clipboardResolver = ClipboardVariableResolver {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
}
```
`core:variables` is pure Kotlin JVM — no Android import. The Android-side provider is injected in `core:accessibility`.

---

## 7. Plan Reference

Full plan: `D:\a10101100_labs\PLAN_text_expander.md`

Key sections for Pass 6:
- **J1** — First-run onboarding UX flow (7 steps: Welcome → Why → Grant Accessibility → Verify → Seed → Try It → Done)
- **H6** — Permission UX (deep-link to `Settings.ACTION_ACCESSIBILITY_SETTINGS`, post-grant animation, sandbox field)
- **F** — MVP scope items still outstanding: JSON export/import, compatibility diagnostics screen, per-app blacklist UI

---

## 8. Hard Rules (carry forward from all passes)

- **TDD throughout:** write failing tests first, watch red, then implement green.
- **Feature branches + PRs only** — never commit implementation directly to `main`. Branch name pattern: `claude/pass-N-description`.
- **Auto-merge on green tests + assembleDebug.** No manual merge confirmation needed.
- **Frequent commits** — one logical unit per commit, descriptive messages.
- **No telemetry, no analytics, no remote calls except WebDAV sync** (Pass 7).
- **Local-first, privacy-conscious** — no snippet bodies in logs, no clipboard content in crash reports.
- **`assembleDebug` must pass before any PR merge.**
- **All prior tests must stay green** — no regressions.

---

## 9. Current State

| Field | Value |
|---|---|
| Main SHA | `7253045` (Pass 6 merge) |
| Branch | `claude/pass-7-webdav-sync` (PR open / pending merge) |
| Test count | **see CHANGELOG — 157 + Pass 7 new tests**, all green |
| `assembleDebug` | CLEAN |
| APK location | `app/build/outputs/apk/debug/app-debug.apk` (after build) |

**What works end-to-end today (sideload to Pixel 8 Pro):**
- Fresh install → Onboarding carousel (Accessibility → Notification → Battery → Sandbox)
- Sandbox test field: type `;today ` → expands to today's date inline in onboarding
- Grant accessibility → auto-advances to next card
- Complete onboarding → Library screen with 8 seed snippets
- Tap + FAB → create a snippet → saved to Room, appears in list
- Tap existing snippet → edit or delete
- Enable Accessibility Service → `SnipAccessibilityService` is live, `SnipForegroundService` starts
- Type `;today ` in any text field → expands to today's date
- Type `;sig ` → expands to placeholder signature
- HealthWatchdogWorker pings every 30 min; notifies if service is killed
- Settings → Excluded Apps: add/remove package blacklist (applied live in AccessibilityService)
- Settings → Backup & Restore: Export snippets (share JSON) / Import (file picker + conflict dialog)
- Settings → WebDAV Sync: configure server/credentials, Test Connection, Sync Now, auto-interval

**What doesn't work yet:**
- No compatibility diagnostics screen (Pass 8)
- WebDAV sync UI wired but no actual data flowing until user configures a server

---

## 10. What's Next

### Pass 6 — DONE (merged 2026-05-13)

Onboarding carousel, JSON backup, per-app blacklist. See CHANGELOG for full detail.

### Pass 7 — DONE (merged 2026-05-14)

WebDAV sync: WebDavClient, SyncEngine, CredentialStore, SyncWorker, SyncModule, SyncViewModel, Settings sync section. Room v1→v2 migration. See CHANGELOG for full detail.

### Pass 8 — Compatibility diagnostics + polish + MVP sweep (next target)

- `feature:diagnostics` — health pill, 5 test targets (EditText / multi-line / password / search / WebView), debug bundle export
- OEM-specific guides (Xiaomi, Huawei, Samsung) via `Build.MANUFACTURER`
- Acceptance criteria sweep (plan Appendix): 10 criteria on Pixel 6 API 35 + Samsung S22 API 34
- Fix any test gaps found during real-device testing

### Former Pass 7 scope (for reference)

Per user decision: sync from Day 1 (multiple phones). See plan section G Phase 4 for protocol:
- Server URL + credentials in local-encrypted DataStore prefs
- Bidirectional with conflict handling (last-write-wins per `updatedAt`)
- Manual sync button + auto-periodic (configurable interval, default 1h)
- `core:sync` new module (or in `core:backup`)

### Pass 8 — Polish + diagnostics + MVP sweep

- `feature:diagnostics` — health pill, 5 test targets (EditText / multi-line / password / search / WebView), debug bundle export
- OEM-specific guides (Xiaomi, Huawei, Samsung) via `Build.MANUFACTURER`
- Acceptance criteria sweep (plan Appendix): 10 criteria on Pixel 6 API 35 + Samsung S22 API 34

### Phases 2–5

Defer until MVP ships: INSTANT/MANUAL triggers, user-defined variables, `{{cursor}}`, suggestion popup, folder nesting, tags, rich text, WebDAV/Drive sync, Tasker plugin, desktop companion. See `PLAN_text_expander.md` §G.

---

## 11. Pass 8 — Polish + Diagnostics + v0.1.0 (2026-05-14)

### What shipped

- Room DB migrated v1 → v2 (`expansion_history` table); `ExpansionHistoryRepository` added; `SnipAccessibilityService` logs each expansion attempt
- `feature:diagnostics` — full screen: 5 service status pills (accessibility, foreground service, WorkManager watchdog, battery exemption, notification), IME detection, test field, last-50 expansion log, diagnostics JSON export, watchdog trigger button
- `DiagnosticsChecker` interface + `AndroidDiagnosticsChecker` production impl; testable with fake
- Adaptive app icon: paper-snippet glyph (document + 3 text lines vector drawable)
- Splash screen via `androidx.core.splashscreen` (dark background #1C1B1F + icon + purple icon bg)
- R8/ProGuard enabled for release builds (`app/proguard-rules.pro`)
- `strings.xml` expanded with UI string resources
- All `Icon()` calls have non-null `contentDescription` where semantically meaningful
- CHANGELOG rolled to `v0.1.0 — 2026-05-14`; README expanded with install guide, module table, build instructions
- Final test count: 165+ unit tests, all green

### Keystore — how to sign a release APK for sideloading

The keystore is NOT in the repo (gitignored). Generate it once and keep it safe:

```bash
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

Create `keystore.properties` (gitignored) at project root:

```properties
storeFile=../../release-keystore.jks
storePassword=<your-store-password>
keyAlias=snipcraft
keyPassword=<your-key-password>
```

Wire signing in `app/build.gradle.kts` (before next release):

```kotlin
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
        release { signingConfig = signingConfigs.getByName("release") }
    }
}
```

Until signing is wired, sideload unsigned APK:
```bash
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

### Phase 2+ roadmap pointer

Planned features deferred post v0.1.0:
- INSTANT + MANUAL trigger modes
- User-defined variables (`{{var:name}}`, `{{cursor}}`, `{{ask:label}}`)
- Suggestion popup (floating overlay anchored to cursor)
- Expansion menus (one shortcut → multiple choices)
- Tags + tag filter chips; folder nesting
- Rich text / image / GIF snippets
- WebDAV sync (multiple devices)
- Tasker plugin + Intent API
- Quick-Add tile (notification shade)
- Per-app full rule editor (beyond blacklist)
- Encrypted vault (SQLCipher + biometric)

See `D:\a10101100_labs\PLAN_text_expander.md` §G for full phased roadmap.

---

*Updated 2026-05-14 — v0.1.0 shipped.*
