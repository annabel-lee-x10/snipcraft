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
