# Changelog

All notable changes to Snipcraft will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] — v0.1.0-dev

### Added
- Project skeleton: 20 Gradle modules, version catalog, convention plugins
- `core:domain` — Snippet, Folder, TriggerMode, ExpansionStrategy, SnippetType data classes
- `core:engine` — Trie-based suffix matcher + ON_DELIMITER trigger logic (TDD)
- `core:variables` — Built-in `{{date}}`, `{{time}}`, `{{clipboard}}` resolvers (TDD)
- `core:common` — AppLogger (Timber wrapper, debug/release trees)
- `core:accessibility` — SnipAccessibilityService, SnipForegroundService (specialUse, START_STICKY, IMPORTANCE_MIN), HealthWatchdogWorker (30-min periodic WorkManager), AccessibilityEventProcessor (password field hard-exclusion), ServiceHealthChecker, NotificationChannels
- `accessibility_service_config.xml` — typeViewTextChanged|typeViewFocused|typeWindowStateChanged, isAccessibilityTool=true, canRetrieveWindowContent=true
- Minimal `MainActivity` with Compose "Snipcraft" placeholder + foreground service startup
- Hilt + HiltWorkerFactory wired into SnipApplication
- 85 unit tests, all passing (JUnit 5 + JUnit 4/Robolectric)
- `core:database` — Room v1: SnippetEntity, FolderEntity, CompatibilityRuleEntity; SnippetDao (observeEnabled, getByShortcut, incrementUsage), FolderDao, CompatibilityRuleDao; SnipcraftDatabase; DatabaseModule (Hilt); SnippetMapper, FolderMapper
- `core:data` — SnippetRepository interface + SnippetRepositoryImpl, FolderRepository, FolderRepositoryImpl, DataModule (Hilt); SeedDataPopulator (8 starter snippets on first launch)
- `core:compatibility` — CompatibilityResolver with built-in PASTE profiles (Chrome, Firefox, Discord, Slack, WhatsApp, Instagram, Twitter) and DISABLED profiles (systemui, settings); CompatibilityModule (Hilt)
- `core:accessibility` — ExpansionExecutor (SET_TEXT via ACTION_SET_TEXT; PASTE via clipboard swap + ACTION_PASTE with 600ms restore); SnippetCacheManager (observes repo, keeps TrieMatcher live); VariableEngineModule (Hilt: wires ClipboardVariableResolver to real ClipboardManager); SnipAccessibilityService upgraded to @AndroidEntryPoint with full expansion pipeline
- 108 total unit tests, all passing
- `core:designsystem` — SnipTheme (Material 3, dynamic colors on Android 12+, dark-first fallback), SnipTypography, Color tokens, EmptyState composable
- `feature:library` — LibraryScreen (search, sort by frequency/recent/alpha, pin, FAB, empty state); LibraryViewModel (StateFlow, search filter, sort, pin toggle, delete); 6 ViewModel tests + 2 Compose screen tests (empty + populated state)
- `feature:editor` — EditorScreen (shortcut + body + description fields, enabled toggle, save in TopAppBar + body, delete for existing); EditorViewModel (load existing snippet, canSave validation, save/delete → NavigateBack events); 7 ViewModel tests + 4 Compose screen tests (disabled save, enabled save, click save, delete icon for edit)
- `feature:settings` — SettingsScreen (accessibility service status + Open Settings CTA + Re-check, theme mode chips, about section); SettingsViewModel (ServiceHealthChecker, ThemeMode toggle); 2 ViewModel tests
- `SnipNavHost` — type-safe Navigation Compose routes (LibraryRoute, EditorRoute, SettingsRoute), bottom nav bar (Library / Settings), Editor as full-screen overlay
- `MainActivity` — replaced Compose placeholder with SnipNavHost + SnipTheme
- 129 total unit tests, all passing
- `core:database` — CompatibilityRuleDao: added `observeUserBlacklist()` (Flow, user rules only) and `deleteByPackage()`
- `core:data` — CompatibilityRuleRepository interface + CompatibilityRuleRepositoryImpl (Hilt @Singleton); SnippetRepository: added `observeAll()` for backup (SnippetDao already had it)
- `core:compatibility` — CompatibilityResolver: added `setUserBlacklist(Set<String>)` and `@Volatile private var userBlacklist`; user blacklist checked before built-in profiles
- `core:accessibility` — ServiceHealthChecker: now `@Singleton @Inject constructor(@ApplicationContext)` (Hilt-injectable); SnipAccessibilityService: subscribes to `CompatibilityRuleRepository.observeBlacklistedPackages()` and calls `compatibilityResolver.setUserBlacklist()` live
- `core:backup` — BackupManager (`export(): String` → versioned JSON v1, `import(json, ConflictStrategy) → ImportResult`); BackupData/SnippetBackup/FolderBackup @Serializable classes; BackupModule (provides `kotlinx.serialization.json.Json` singleton); 5 unit tests (export fields, folder, skip/overwrite, round-trip)
- `feature:onboarding` — OnboardingScreen (HorizontalPager, 4 pages: Accessibility card → Notification card → Battery exemption card → Sandbox field + "Get Started"); OnboardingViewModel (permission detection via ServiceHealthChecker, lifecycle-resume polling for accessibility grant, auto-advance on grant, sandbox expansion using real TrieMatcher + VariableEngine against seed snippets); 11 ViewModel tests
- `SnipNavHost` — OnboardingRoute added; start destination is OnboardingRoute when `ServiceHealthChecker.isServiceEnabled() == false`, LibraryRoute otherwise; OnboardingRoute removed from back stack on completion
- `feature:settings` — Excluded Apps section: list of user-blacklisted packages with per-row delete button + "Add excluded app" dialog (package name text input); Backup & Restore section: Export (share sheet via Intent.ACTION_SEND) + Import (GetContent file picker + conflict dialog: Overwrite / Skip existing) + import result dialog; SettingsViewModel: addToBlacklist, removeFromBlacklist, onExport, onImportJsonReceived, onImportConflictResolved, events Channel (ShareExport, ShowImportResult); 7 new ViewModel tests
- 157 total unit tests, all passing
- `core:sync` — new module; WebDavClient (OkHttp 4.12.0: PROPFIND/GET/PUT/MKCOL, Basic Auth, rejects plain HTTP by default; 11 MockWebServer tests); SyncEngine (bidirectional merge: syncVersion primary, updatedAt secondary, remote wins on tie; 6 unit tests); CredentialStore (EncryptedSharedPreferences/MasterKeys AES256-GCM, never logs credentials); SyncWorker (@HiltWorker CoroutineWorker, WorkManager periodic + one-shot, retry on IOException; 3 WorkManager tests); SyncModule (Hilt: OkHttpClient, @SyncJson qualifier avoids conflict with BackupModule Json)
- `core:database` — Room v1→v2 migration: `ALTER TABLE snippets ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0` + same for folders; schema v2 JSON generated
- `core:domain` — Snippet + Folder: added `syncVersion: Long = 0L` (backward-compatible default)
- `core:backup` — SnippetBackup + FolderBackup: added `syncVersion: Long = 0L` (backward-compatible; used as sync snapshot format)
- `feature:settings` — WebDAV sync section: server URL, remote path, username, password (masked, show/hide toggle), plain HTTP toggle, auto-sync interval (Off/1h/6h/24h), Test Connection (PROPFIND inline result), Sync Now, Save settings; SyncViewModel (@HiltViewModel, secondary test constructor); SyncUiState; 6 unit tests
