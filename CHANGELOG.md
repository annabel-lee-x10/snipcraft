# Changelog

All notable changes to Snipcraft will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [v0.1.1] — 2026-05-15

### Changed
- `versionCode` bumped to 2, `versionName` to `0.1.1` for first Play Store upload.
- Release signing config wired in `app/build.gradle.kts` — reads credentials from
  `keystore.properties` at project root (gitignored). Absent file produces a clear
  build error with setup instructions; debug builds are unaffected.
- `bundleRelease` now has a `validateReleaseKeystore` preflight task that fails early
  with actionable instructions rather than a cryptic signing exception.

### Added
- `docs/RELEASE_KEYSTORE.md` — keytool command, `keystore.properties` template, and
  backup guidance for the release signing key.
- `docs/PRIVACY.md` — plain-English privacy policy for Play Console.
- `docs/PLAY_CONSOLE_CHECKLIST.md` — step-by-step Play Console setup checklist.

[v0.1.1]: https://github.com/annabel-lee-x10/snipcraft/releases/tag/v0.1.1

## [v0.1.0] — 2026-05-14

### Added
- Project skeleton: 20 Gradle modules, version catalog, convention plugins
- `core:domain` — Snippet, Folder, TriggerMode, ExpansionStrategy, SnippetType data classes
- `core:engine` — Trie-based suffix matcher + ON_DELIMITER trigger logic (TDD)
- `core:variables` — Built-in `{{date}}`, `{{time}}`, `{{clipboard}}` resolvers (TDD)
- `core:common` — AppLogger (Timber wrapper, debug/release trees)
- `core:accessibility` — SnipAccessibilityService, SnipForegroundService (specialUse, START_STICKY, IMPORTANCE_MIN), HealthWatchdogWorker (30-min periodic WorkManager), AccessibilityEventProcessor (password field hard-exclusion), ServiceHealthChecker, NotificationChannels
- `core:database` — Room v3: SnippetEntity, FolderEntity, CompatibilityRuleEntity, ExpansionHistoryEntity; all DAOs; schema exported; migrations 1→2 (syncVersion) and 2→3 (expansion_history)
- `core:data` — SnippetRepository, FolderRepository, CompatibilityRuleRepository, ExpansionHistoryRepository; SeedDataPopulator (8 starter snippets on first launch)
- `core:compatibility` — CompatibilityResolver with PASTE profiles (Chrome, Firefox, Discord, Slack, WhatsApp) and DISABLED profiles (systemui); user blacklist support
- `core:backup` — BackupManager: JSON v1 export/import with SKIP_EXISTING/OVERWRITE conflict handling
- `core:sync` — WebDavClient (OkHttp 4.12.0: PROPFIND/GET/PUT/MKCOL, Basic Auth; 11 MockWebServer tests); SyncEngine (bidirectional merge, syncVersion primary; 6 tests); CredentialStore (EncryptedSharedPreferences AES256-GCM); SyncWorker (@HiltWorker, WorkManager periodic + one-shot); SyncModule (Hilt)
- `core:designsystem` — SnipTheme (Material 3, dynamic colors API 31+, dark-first), SnipTypography, Color tokens, EmptyState composable
- `feature:library` — LibraryScreen: search, sort (frequency/recent/alpha), pin, FAB, empty state
- `feature:editor` — EditorScreen: shortcut + body + description fields, enabled toggle, save/delete
- `feature:settings` — SettingsScreen: accessibility status, theme chips, excluded apps, backup/restore, WebDAV sync section, diagnostics entry point; SyncViewModel (WebDAV config + test connection + sync now)
- `feature:onboarding` — HorizontalPager (Accessibility → Notification → Battery → Sandbox); auto-advances on accessibility grant; real snippet expansion in sandbox
- `feature:diagnostics` — DiagnosticsScreen: 5 service status pills, IME detection, test field, last-50 expansion log, diagnostics JSON export, watchdog trigger button
- `SnipNavHost` — type-safe Navigation Compose routes (Library, Settings, Editor, Onboarding, Diagnostics)
- `SnipAccessibilityService` — logs each expansion attempt to Room `expansion_history` table
- Adaptive app icon: paper-snippet glyph (document + text lines vector foreground, dark background)
- Splash screen via `androidx.core.splashscreen` (dark background + icon + purple icon bg)
- R8/ProGuard enabled for release builds with keep rules for Room, Hilt, kotlinx.serialization, WorkManager
- `strings.xml` populated; all `Icon()` calls have non-null `contentDescription`

### Changed
- Version promoted from `0.1.0-dev` to `0.1.0`
- About section in Settings: version string updated to `v0.1.0`

### Tests
- 168 unit tests, all green (JUnit 5 + JUnit 4/Robolectric + MockK + Turbine)

[v0.1.0]: https://github.com/annabel-lee-x10/snipcraft/releases/tag/v0.1.0
