# Snipcraft

System-wide text expansion for Android. Local. Fast. Privacy-first.

Type a shortcut anywhere you can type — Snipcraft expands it instantly using the Android Accessibility Service.

## Features

- **System-wide expansion** — works in Gmail, Chrome, WhatsApp, Slack, Discord, and most text fields
- **ON_DELIMITER trigger** — expands on space, tab, or punctuation; no accidental triggers
- **Built-in variables** — `{{date}}`, `{{date:yyyy-MM-dd}}`, `{{time}}`, `{{clipboard}}`
- **Trie-based matching** — O(L) per keystroke; negligible latency
- **Backspace undo** — 5-deep undo stack per field, 2-second window
- **CRUD library** — search, sort, pin, enable/disable per snippet
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

1. Download `app-release-unsigned.apk` from the [latest release](https://github.com/annabel-lee-x10/snipcraft/releases/latest)
2. On your Android device, enable **Settings → Security → Install unknown apps** for your file manager or browser
3. Open the APK — Android will prompt to install
4. After installing, open Snipcraft and follow the onboarding flow

Alternatively, with ADB (developer options enabled):
```bash
adb install app-release-unsigned.apk
```

## Accessibility setup

1. Open Snipcraft → tap **Open Accessibility Settings**
2. Find **Snipcraft** in the list and enable it
3. Confirm the permission prompt
4. Return to Snipcraft — the service status will show **Active**
5. Recommended: **Settings → Battery → Snipcraft → Unrestricted** to prevent the service being killed

## Building from source

**Prerequisites:** Android Studio (JDK 21 at `C:\Program Files\Android\Android Studio\jbr`), Android SDK 35, Gradle 9.3.1 (wrapper auto-downloads).

```bash
git clone https://github.com/annabel-lee-x10/snipcraft.git
cd snipcraft

# Debug build
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# Release build (unsigned)
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release-unsigned.apk
```

For signing instructions, see `docs/HANDOFF.md`.

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
