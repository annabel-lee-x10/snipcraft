# Snipcraft — Play Store Internal Testing: Signing + Docs Design

**Date:** 2026-05-15
**Branch:** `claude/play-store-aab-signing`
**Status:** Approved by user

---

## Context

Snipcraft v0.1.0 was sideload-distributed as a GitHub Releases APK. The user's OCBC banking app blocks sideloaded installs, requiring a Play Store distribution channel. The immediate goal is to get onto the Play Store **internal testing** track (not public) so the app can be installed alongside banking apps on an OPPO device.

The v0.1.0 APK was signed with a release keystore that is NOT in the repo (gitignored). That keystore has not yet been formally wired into the Gradle build — the `app/build.gradle.kts` currently has no `signingConfigs` block.

---

## Decisions Made (not re-litigated)

- **No keytool run by Claude.** User will generate the keystore themselves with their own passwords. Claude wires the Gradle config, writes docs, writes PRIVACY.md and PLAY_CONSOLE_CHECKLIST.md.
- **Keystore location:** `app/keystore/snipcraft-release.keystore` (caught by existing `*.keystore` gitignore glob).
- **`keystore.properties`** at project root (already gitignored).
- **Graceful skip:** if `keystore.properties` is absent, `bundleRelease` fails with a clear error (not a cryptic null-pointer); debug builds are unaffected.
- **Version bump:** `versionCode` 1→2, `versionName` "0.1.0"→"0.1.1" (v0.1.0 APK already shipped to GitHub Releases; Play Store needs a distinct versionCode).
- **CHANGELOG:** append v0.1.1 entry above v0.1.0.
- **Tag:** `v0.1.1` applied after PR merge to main.

---

## Architecture

### 1. Signing config (`app/build.gradle.kts`)

```kotlin
import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

android {
    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            signingConfig = if (keystorePropsFile.exists())
                signingConfigs.getByName("release") else null
            // ProGuard already wired
        }
    }
}
```

`signingConfig = null` causes Gradle to emit an unsigned AAB (with a clear warning) rather than a cryptic exception. The user's first signed build is a deliberate action after generating the keystore.

### 2. Files produced / modified

| File | Action |
|---|---|
| `app/build.gradle.kts` | Add signingConfigs block + wire release buildType |
| `app/build.gradle.kts` | Bump `versionCode` 1→2, `versionName` "0.1.0"→"0.1.1" |
| `docs/RELEASE_KEYSTORE.md` | New — keytool command, keystore.properties template, backup warnings, TODO |
| `docs/BACKLOG.md` | Prepend keystore TODO |
| `docs/PRIVACY.md` | New — ~300-word privacy policy for Play Console |
| `docs/PLAY_CONSOLE_CHECKLIST.md` | New — step-by-step Play Console UI checklist |
| `CHANGELOG.md` | Prepend v0.1.1 entry |

### 3. `RELEASE_KEYSTORE.md` critical content

- `keytool -genkeypair` command with all flags (RSA 2048, validity 10000 days, alias snipcraft, `-dname` matching the project)
- `keystore.properties` template (placeholder values the user fills in)
- Backup warning in bold: keystore + passwords must be backed up off-machine immediately — losing either makes app updates impossible forever
- TODO block at top of file
- Note that `./gradlew bundleRelease` will produce `app/build/outputs/bundle/release/app-release.aab` after setup

### 4. `PRIVACY.md` scope

Covers: snippets/blacklist/WebDAV credentials stored locally on-device; WebDAV sync sends data only to user's own server, encrypted in transit; zero analytics, telemetry, crash reporting, or accounts; contact email for privacy questions. Plain English, no legalese, ~300 words. Designed to be hosted as a GitHub Pages page or raw GitHub URL.

### 5. `PLAY_CONSOLE_CHECKLIST.md` scope

Step-by-step UI checklist for Play Console. Includes:
- App name + long description (seeded from BRAND.md elevator pitch)
- Short description candidates (80 chars, 3 options from BRAND.md taglines)
- Screenshot list with suggested captures
- Privacy policy URL slot + warning that URL must be publicly reachable
- Content rating guidance (text expander, no UGC, no objectionable content)
- Target audience declaration
- Internal testing track setup + tester opt-in link distribution
- Common first-timer stalls (privacy policy URL the #1 stall)

---

## What Claude does NOT do

- Does not run `keytool` (user picks their own passwords)
- Does not run `./gradlew bundleRelease` to completion (keystore absent — build verifies graceful failure only)
- Does not push/upload the AAB anywhere
- Does not merge the PR (auto-merge on green tests + assembleDebug per HANDOFF rules)

---

## Success criteria

- `./gradlew assembleDebug` passes (no regressions)
- `./gradlew bundleRelease` without `keystore.properties` exits with a clear Gradle warning, not an exception
- All 168 existing tests pass
- `docs/RELEASE_KEYSTORE.md`, `docs/PRIVACY.md`, `docs/PLAY_CONSOLE_CHECKLIST.md` exist and are complete
- PR opened from `claude/play-store-aab-signing` to `main`
- `versionCode = 2`, `versionName = "0.1.1"` in `app/build.gradle.kts`
