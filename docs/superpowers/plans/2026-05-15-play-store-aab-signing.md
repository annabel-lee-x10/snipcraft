# Play Store AAB Signing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire release signing config into the Gradle build, bump the version for Play Store, and produce the supporting docs (RELEASE_KEYSTORE.md, PRIVACY.md, PLAY_CONSOLE_CHECKLIST.md) so Snipcraft can be uploaded to Play Store internal testing.

**Architecture:** No keytool run — the user generates the keystore themselves. The Gradle build is wired so `assembleDebug` always works, `bundleRelease` fails with a clear human-readable error when `keystore.properties` is absent, and produces a signed AAB when it is present. A dedicated `validateReleaseKeystore` task is the failure point, not a cryptic null-pointer in the signing pipeline.

**Tech Stack:** Gradle 9.3.1, AGP 9.1.0, Kotlin 2.3.20, Android Studio JBR (JDK 21 at `C:\Program Files\Android\Android Studio\jbr`), PowerShell on Windows 11.

**Working directory:** `D:\a10101100_labs\snipcraft\.claude\worktrees\sad-shamir-52cb55`  
**Build command prefix (all Gradle invocations):** PowerShell — `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew <task>`

---

## File Map

| File | Action | What changes |
|---|---|---|
| `app/build.gradle.kts` | Modify | Add `validateReleaseKeystore` task, conditional `signingConfigs`, wire release buildType, bump `versionCode`/`versionName` |
| `docs/RELEASE_KEYSTORE.md` | Create | keytool command, keystore.properties template, backup warnings, TODO block |
| `docs/BACKLOG.md` | Modify | Prepend keystore TODO at top |
| `CHANGELOG.md` | Modify | Prepend v0.1.1 entry |
| `docs/PRIVACY.md` | Create | ~300-word privacy policy for Play Console |
| `docs/PLAY_CONSOLE_CHECKLIST.md` | Create | Step-by-step Play Console UI checklist with copy suggestions |

---

## Task 1: Rename branch

**Files:** git branch rename only

- [ ] **Step 1: Rename the worktree branch**

```powershell
git branch -m claude/sad-shamir-52cb55 claude/play-store-aab-signing
```

Expected output: (none — silent success)

- [ ] **Step 2: Verify**

```powershell
git branch
```

Expected: `* claude/play-store-aab-signing` shown as current branch.

---

## Task 2: Update `app/build.gradle.kts` — signing config + version bump

**Files:**
- Modify: `app/build.gradle.kts`

The current file has `versionCode = 1`, `versionName = "0.1.0"`, a bare `release` buildType with ProGuard, and no `signingConfigs` block.

The new approach:
- Add `import java.util.Properties` at the top
- Load `keystore.properties` lazily from project root (no crash if absent)
- Register a `validateReleaseKeystore` task that throws a clear `GradleException` when the file is missing
- Wire `bundleRelease` and `assembleRelease` to depend on that task
- Conditionally create `signingConfigs.create("release")` only when `keystore.properties` exists
- Set `signingConfig` on the release buildType accordingly
- Bump `versionCode` to `2`, `versionName` to `"0.1.1"`

- [ ] **Step 1: Replace `app/build.gradle.kts` with the updated content**

Write the file at `app/build.gradle.kts` (full replacement — show complete file):

```kotlin
import java.util.Properties

plugins {
    id("snipcraft.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("snipcraft.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

tasks.register("validateReleaseKeystore") {
    doFirst {
        if (!keystorePropsFile.exists()) {
            throw GradleException(
                "\n\n" +
                "  ❌  keystore.properties not found at project root.\n\n" +
                "  Before running bundleRelease or assembleRelease:\n" +
                "    1. Generate the release keystore (see docs/RELEASE_KEYSTORE.md)\n" +
                "    2. Create keystore.properties at project root with storeFile, storePassword,\n" +
                "       keyAlias, and keyPassword filled in.\n\n" +
                "  This keystore CANNOT be regenerated once the app is live on Play Store.\n" +
                "  Back up the .keystore file and passwords off-machine immediately.\n"
            )
        }
    }
}

tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    dependsOn("validateReleaseKeystore")
}

android {
    namespace = "dev.a10101100.snipcraft"

    defaultConfig {
        applicationId = "dev.a10101100.snipcraft"
        versionCode = 2
        versionName = "0.1.1"
    }

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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePropsFile.exists())
                signingConfigs.getByName("release")
            else
                null
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:accessibility"))
    implementation(project(":core:compatibility"))
    implementation(project(":core:database"))
    implementation(project(":core:sync"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:library"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:diagnostics"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.activity)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    api(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.workmanager.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
}
```

- [ ] **Step 2: Verify `assembleDebug` still passes (all 168 tests must stay green)**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug test --continue
```

Expected: `BUILD SUCCESSFUL`. All 168 tests green. No compilation errors.

- [ ] **Step 3: Verify `bundleRelease` fails gracefully (keystore.properties absent)**

Confirm `keystore.properties` does NOT exist at project root:
```powershell
Test-Path "keystore.properties"
```
Expected: `False`

Then run:
```powershell
.\gradlew bundleRelease
```

Expected: `BUILD FAILED` with the message containing `❌  keystore.properties not found at project root` and the instructions pointing to `docs/RELEASE_KEYSTORE.md`. There must be no stack trace / NullPointerException. If a stack trace appears, the validation task is not wired correctly — go back and fix.

- [ ] **Step 4: Commit**

```powershell
git add app/build.gradle.kts
git commit -m "build: wire release signing config + bump versionCode 2 / versionName 0.1.1"
```

---

## Task 3: Write `docs/RELEASE_KEYSTORE.md`

**Files:**
- Create: `docs/RELEASE_KEYSTORE.md`

- [ ] **Step 1: Create the file**

Write `docs/RELEASE_KEYSTORE.md` with this exact content:

```markdown
# Release Keystore — Snipcraft

> **TODO (user): generate release keystore.**
> Run the keytool command below, fill in `keystore.properties` at project root,
> back up both off-machine. Required before `./gradlew bundleRelease` can produce
> a signed AAB for Play Store upload.

---

## ⚠️  Read this first

The release keystore is the cryptographic identity that proves every future Snipcraft
update is from you. **If you lose the keystore file or forget the passwords, you can
never update the app on Play Store.** The existing install becomes orphaned and you
would have to publish a new app under a new package name.

Back up immediately after generating:
- Copy `app/keystore/snipcraft-release.keystore` to an offline location (USB drive,
  encrypted password manager file attachment, or similar).
- Store `storePassword` and `keyPassword` in your password manager.

---

## Step 1: Create the keystore directory

```bash
mkdir -p app/keystore
```

The directory is gitignored via `*.keystore` in `.gitignore` — the file inside it
will never be committed.

## Step 2: Generate the keystore

Run from the project root:

```bash
keytool -genkeypair \
  -keystore app/keystore/snipcraft-release.keystore \
  -alias snipcraft \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Snipcraft, OU=Personal, O=a10101100, L=Unknown, S=Unknown, C=US" \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

Replace `YOUR_STORE_PASSWORD` and `YOUR_KEY_PASSWORD` with strong passwords you will
store in your password manager. They can be the same value if you prefer.

`-validity 10000` is ~27 years — long enough that expiry is not a concern for a
personal project.

## Step 3: Create `keystore.properties`

Create a file called `keystore.properties` **at the project root** (it is gitignored):

```properties
storeFile=app/keystore/snipcraft-release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=snipcraft
keyPassword=YOUR_KEY_PASSWORD
```

Fill in the same passwords you used in the keytool command above.
`storeFile` is a path relative to the project root.

## Step 4: Get the SHA-1 certificate fingerprint (for Play Console)

```bash
keytool -list -v \
  -keystore app/keystore/snipcraft-release.keystore \
  -alias snipcraft \
  -storepass YOUR_STORE_PASSWORD
```

Copy the `SHA1:` fingerprint from the output. Play Console may ask for it in the
App Signing settings — paste it there.

## Step 5: Build the signed AAB

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
  PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew bundleRelease
```

Output file: `app/build/outputs/bundle/release/app-release.aab`

Upload this file to Play Console → Internal Testing → Create new release.

## Step 6: Back up NOW

Before doing anything else:

1. Copy `app/keystore/snipcraft-release.keystore` to at least two offline locations.
2. Confirm your password manager has `storePassword` and `keyPassword` saved.
3. Never commit the keystore or `keystore.properties` to git. The `.gitignore` already
   prevents this, but double-check with `git status` if in doubt.
```

- [ ] **Step 2: Commit**

```powershell
git add docs/RELEASE_KEYSTORE.md
git commit -m "docs: add RELEASE_KEYSTORE.md with keytool command and keystore.properties template"
```

---

## Task 4: Update `docs/BACKLOG.md`

**Files:**
- Modify: `docs/BACKLOG.md`

- [ ] **Step 1: Prepend the keystore TODO at the top, before the Phase 2 section**

Open `docs/BACKLOG.md`. After the `# Snipcraft — Backlog` heading and the intro line, insert a new section before `## Code-level TODOs`:

```markdown
## 🔑  Action required — release signing

> **TODO (user): generate release keystore.**
> Run the keytool command in `docs/RELEASE_KEYSTORE.md`, fill in `keystore.properties`
> at project root, back up both off-machine. Required before `./gradlew bundleRelease`
> can produce a signed AAB for Play Store upload.

---
```

The final top of the file should look like:

```markdown
# Snipcraft — Backlog

Items deferred post v0.1.0. Ordered roughly by impact.

---

## 🔑  Action required — release signing

> **TODO (user): generate release keystore.**
> Run the keytool command in `docs/RELEASE_KEYSTORE.md`, fill in `keystore.properties`
> at project root, back up both off-machine. Required before `./gradlew bundleRelease`
> can produce a signed AAB for Play Store upload.

---

## Code-level TODOs
...
```

- [ ] **Step 2: Commit**

```powershell
git add docs/BACKLOG.md
git commit -m "docs: prepend release keystore TODO to BACKLOG.md"
```

---

## Task 5: Update `CHANGELOG.md`

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Prepend the v0.1.1 entry above the existing `[v0.1.0]` block**

The current file starts with the `# Changelog` header, then `## [v0.1.0] — 2026-05-14`. Insert this block between the header block and the `[v0.1.0]` section:

```markdown
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
```

- [ ] **Step 2: Commit**

```powershell
git add CHANGELOG.md
git commit -m "docs: add CHANGELOG entry for v0.1.1 (Play Store signing setup)"
```

---

## Task 6: Write `docs/PRIVACY.md`

**Files:**
- Create: `docs/PRIVACY.md`

This will be hosted somewhere publicly reachable (GitHub Pages or raw GitHub URL) and linked in Play Console. It must be plain English, ~300 words, no legalese.

- [ ] **Step 1: Create the file**

Write `docs/PRIVACY.md` with this exact content:

```markdown
# Privacy Policy — Snipcraft

**Last updated:** 2026-05-15

Snipcraft is a local-first Android text expander. This policy describes what data
the app handles and where it goes.

---

## What data Snipcraft processes

**Snippets and folders.** Text you store as snippets (shortcuts, bodies, descriptions)
and the folders you create. This data lives in a Room database on your device.
Nothing is sent anywhere unless you configure WebDAV sync.

**App blacklist.** Package names of apps you choose to exclude from text expansion.
Stored locally.

**WebDAV credentials.** If you configure WebDAV sync, the server URL and your
credentials are stored in Android's EncryptedSharedPreferences on-device, encrypted
with AES-256-GCM. Credentials are never transmitted except to the WebDAV server you
configured, over HTTPS.

**Expansion events.** A local log of the last 50 text expansions (shortcut matched,
package name of the target app, outcome) stored in the local database for diagnostics.
This data never leaves the device.

---

## What Snipcraft does NOT collect

- No analytics or usage tracking of any kind.
- No crash reports sent to any third party. Logs are written to the Android logcat
  only — they stay on your device.
- No accounts, no sign-in, no user profiles.
- No advertising identifiers.
- No content of password fields — Snipcraft hard-excludes password inputs at two
  detection layers and never reads or stores their values.

---

## Where data is transmitted

The only outbound network traffic Snipcraft makes is to the WebDAV server **you**
configured. No traffic goes to Snipcraft, Anthropic, or any third party. If you do
not configure WebDAV sync, the app makes no network requests at all.

WebDAV sync is always encrypted in transit (HTTPS). The server is under your control.

---

## Contact

Privacy questions: anaken.x.ai@gmail.com
```

- [ ] **Step 2: Commit**

```powershell
git add docs/PRIVACY.md
git commit -m "docs: add PRIVACY.md for Play Console privacy policy URL"
```

---

## Task 7: Write `docs/PLAY_CONSOLE_CHECKLIST.md`

**Files:**
- Create: `docs/PLAY_CONSOLE_CHECKLIST.md`

- [ ] **Step 1: Create the file**

Write `docs/PLAY_CONSOLE_CHECKLIST.md` with this exact content:

```markdown
# Play Console Setup Checklist — Snipcraft v0.1.1

For uploading to the **Internal Testing** track. Work through these sections in order
in the Play Console UI. Mark each item as you complete it.

---

## ⚠️  Before you start

- [ ] You have run the keytool command in `docs/RELEASE_KEYSTORE.md` and have
      `keystore.properties` at project root.
- [ ] You have run `./gradlew bundleRelease` and the AAB exists at
      `app/build/outputs/bundle/release/app-release.aab`.
- [ ] Your privacy policy is hosted at a **publicly reachable URL** (not localhost,
      not a private GitHub repo, not a file on your desktop).
      **This is the #1 stall for first-timers.** Play Console will not let you proceed
      to the store presence or content rating steps until a live URL is entered.
      Quick option: push `docs/PRIVACY.md` to GitHub and enable GitHub Pages, or use
      the raw GitHub URL: `https://raw.githubusercontent.com/annabel-lee-x10/snipcraft/main/docs/PRIVACY.md`
      (raw GitHub URLs are publicly reachable without Pages setup).

---

## 1. Create the app in Play Console

- [ ] Sign in at [play.google.com/console](https://play.google.com/console).
- [ ] Click **Create app**.
- [ ] App name: `Snipcraft`
- [ ] Default language: English (United States)
- [ ] App or game: **App**
- [ ] Free or paid: **Free**
- [ ] Accept the declarations and click **Create app**.

---

## 2. Store presence — Main store listing

Navigate to: **Grow → Store presence → Main store listing**

### App name
```
Snipcraft
```

### Short description (max 80 chars — pick one)
Option A (57 chars — recommended):
```
Keyboard shortcuts that expand in any Android text field.
```
Option B (55 chars):
```
Type shortcuts, get full text — in any app, privately.
```
Option C (63 chars):
```
Text expander for Android. Expand shortcuts anywhere, no cloud.
```

### Full description (max 4000 chars)
Paste and expand as needed:
```
Snipcraft is a text expander for Android.

Type ;sig, ;today, or any shortcut you define in any app — email, browser, chat, terminal — and it replaces it inline. The whole thing runs locally on your phone, with optional WebDAV sync to a server you own. No accounts, no cloud, no telemetry — just the keys you press, expanded.

Key features:
• Expand anywhere. Type your shortcut in any Android text field and watch it become the text you meant.
• Inline, not interruptive. No popups, no overlays.
• Variables. {{date}}, {{time:HH:mm a}}, {{clipboard}} — composed in a single pass.
• Undo five deep. An overeager expansion is one tap from un-happening.
• WebDAV sync. Bring your own server (Nextcloud, Synology, self-hosted). Credentials stored with AES-256 encryption on-device.
• No backend, ever. Snippets live in a local database. The only network call is to your WebDAV server.
• Password-field safe. Two-layer detection hard-excludes password fields from the expansion engine.
• JSON backup. Export and import with conflict handling.
• Diagnostics. Five service health indicators, expansion history, debug bundle export.
• Material You. Dynamic colors on Android 12+.
```

### Screenshots
Capture and upload at least 2 phone screenshots. Suggested captures:
- [ ] Library screen with 2–3 snippets visible (shows the core UI)
- [ ] Editor screen with a snippet open (shows shortcut + body fields)
- [ ] Settings screen → WebDAV section (shows the privacy-conscious sync story)
- [ ] Diagnostics screen with all 5 pills green (shows the "tells the truth" story)
- [ ] Onboarding sandbox card with an expansion mid-type (most visually interesting)

Minimum: 2 screenshots. Play Console requires at least 2.

### App icon
- [ ] Upload the adaptive icon (512×512 PNG, already in the project as the launcher icon).
      Export from Android Studio: right-click `res/mipmap-xxxhdpi/ic_launcher.webp` → Export.
      Or use a 512×512 PNG render of the adaptive icon foreground on the brand background (#1C1B1F).

### Feature graphic (optional but recommended)
1024×500 PNG. See `docs/BRAND.md` section B for a hero visual prompt if you want to generate one.
Can be skipped for internal testing.

---

## 3. Store presence — Privacy policy

Navigate to: **App content → Privacy policy**

- [ ] Enter the URL to your hosted `docs/PRIVACY.md`.
      Example raw GitHub URL:
      `https://raw.githubusercontent.com/annabel-lee-x10/snipcraft/main/docs/PRIVACY.md`
- [ ] Click **Save**.

> **Stall warning:** If the URL returns a 404 or is not publicly reachable, Play
> Console will block progression to later steps. Test the URL in an incognito window
> before entering it.

---

## 4. App content — Ratings

Navigate to: **App content → App content rating**

- [ ] Click **Start questionnaire**.
- [ ] Category: **Utilities**
- [ ] Answer all questions. For Snipcraft:
  - Violence: **No**
  - Sexual content: **No**
  - Profanity: **No**
  - Controlled substances: **No**
  - User-generated content: **No** (snippets are private, never shared through the app)
  - Personal/sensitive data: **No tracking, no sharing**
- [ ] Submit and apply the rating.

Expected rating: **Everyone**

---

## 5. App content — Target audience

Navigate to: **App content → Target audience and content**

- [ ] Target age group: **18 and over** (or "All ages" — both are accurate; 18+ avoids
      the extra child-safety review steps and is honest for a power-user tool).
- [ ] Save.

---

## 6. App content — Ads

Navigate to: **App content → Ads**

- [ ] Select: **No, my app does not contain ads**.
- [ ] Save.

---

## 7. App content — Data safety

Navigate to: **App content → Data safety**

- [ ] Does your app collect or share user data? **No** (for a local-only install with no WebDAV configured)
      If you want to be precise about WebDAV: **Yes** → data type: "Other app activity" (sync metadata) → shared with: nobody → encrypted in transit: yes → user can delete: yes.
- [ ] Save and submit.

---

## 8. Internal testing track — Upload the AAB

Navigate to: **Testing → Internal testing → Create new release**

- [ ] Click **Upload** and select `app/build/outputs/bundle/release/app-release.aab`.
- [ ] Release name: `0.1.1 (2)`  *(version name + version code)*
- [ ] Release notes (optional): `First Play Store build. Feature-complete MVP: text expansion, WebDAV sync, JSON backup, diagnostics.`
- [ ] Click **Save** then **Review release**.
- [ ] If any errors appear in the review panel, address them before continuing.
- [ ] Click **Start rollout to Internal testing**.

---

## 9. Testers — Add yourself

Navigate to: **Testing → Internal testing → Testers**

- [ ] Click **Create email list** (or add to an existing list).
- [ ] Add your Google account email(s).
- [ ] Save.

---

## 10. Opt in on your OPPO device

- [ ] Copy the **opt-in URL** shown in the Testers panel.
- [ ] Open that URL on your OPPO device (signed in with the tester Google account).
- [ ] Tap **Become a tester** → **Download [app name] on Google Play**.
- [ ] Uninstall the sideloaded APK first if still installed.
- [ ] Install from the Play Store listing.

---

## Common first-timer stalls

| Stall | Fix |
|---|---|
| Privacy policy URL not publicly reachable | Host the file and verify with incognito window before entering the URL |
| "You need to complete the content rating questionnaire" | Complete section 4 above before uploading the AAB |
| "APK/AAB is not signed" | You uploaded before running keytool + keystore.properties setup — see `docs/RELEASE_KEYSTORE.md` |
| "Version code already used" | Bump `versionCode` in `app/build.gradle.kts` and rebuild |
| "Package name already exists" | Your package `dev.a10101100.snipcraft` is already registered under your account — proceed with that app listing |
| Grey/unavailable "Publish" button | The internal testing track doesn't have a "Publish" button — use the opt-in URL from the Testers panel instead |
| OPPO device shows "App not available in your country" | Internal testing bypasses geo restrictions — use the opt-in URL, not the Play Store search |
```

- [ ] **Step 2: Commit**

```powershell
git add docs/PLAY_CONSOLE_CHECKLIST.md
git commit -m "docs: add PLAY_CONSOLE_CHECKLIST.md for Play Store internal testing setup"
```

---

## Task 8: Final verification pass

**Files:** No changes — verification only.

- [ ] **Step 1: Run full test suite + assembleDebug one final time**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug test --continue
```

Expected: `BUILD SUCCESSFUL`. All 168 tests green. Zero new warnings about signing.

- [ ] **Step 2: Confirm bundleRelease still fails gracefully**

```powershell
.\gradlew bundleRelease 2>&1 | Select-String -Pattern "keystore|RELEASE_KEYSTORE|BUILD FAILED"
```

Expected output lines include:
- `keystore.properties not found at project root`
- `docs/RELEASE_KEYSTORE.md`
- `BUILD FAILED`

No line should contain `NullPointerException`, `ClassCastException`, or `Exception in thread`.

- [ ] **Step 3: Confirm all committed docs exist**

```powershell
@("docs/RELEASE_KEYSTORE.md", "docs/PRIVACY.md", "docs/PLAY_CONSOLE_CHECKLIST.md", "docs/BACKLOG.md", "CHANGELOG.md") | ForEach-Object { Test-Path $_ }
```

Expected: `True` for each path (5 × `True`).

---

## Task 9: Push and open PR

- [ ] **Step 1: Push the branch**

```powershell
git push -u origin claude/play-store-aab-signing
```

- [ ] **Step 2: Open PR**

```powershell
gh pr create `
  --title "build: Play Store signing config + docs for v0.1.1 internal testing" `
  --body "$(cat <<'EOF'
## Summary
- Wires release signing config into `app/build.gradle.kts` — reads from `keystore.properties` (gitignored), fails gracefully with actionable instructions when absent
- Bumps `versionCode` 1→2, `versionName` 0.1.0→0.1.1 for first Play Store upload
- Adds `docs/RELEASE_KEYSTORE.md` — keytool command, keystore.properties template, backup warnings
- Adds `docs/PRIVACY.md` — plain-English privacy policy ready to host for Play Console
- Adds `docs/PLAY_CONSOLE_CHECKLIST.md` — step-by-step Play Console setup guide

## What the user does after merge
1. Run `keytool` command in `docs/RELEASE_KEYSTORE.md` with their own passwords
2. Create `keystore.properties` at project root
3. Back up keystore + passwords off-machine immediately
4. Run `./gradlew bundleRelease` → get signed AAB at `app/build/outputs/bundle/release/app-release.aab`
5. Follow `docs/PLAY_CONSOLE_CHECKLIST.md` to upload to internal testing track

## Test plan
- [x] `assembleDebug` passes, all 168 tests green
- [x] `bundleRelease` without `keystore.properties` fails with clear human-readable error (no stack trace)
- [x] All new docs exist and contain complete content

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 3: Note the PR URL from the output**

- [ ] **Step 4: Tag `v0.1.1` after PR is merged to main**

Wait for auto-merge (green `assembleDebug` + tests), then:

```powershell
git checkout main
git pull origin main
git tag -a v0.1.1 -m "v0.1.1 — Play Store signing config + internal testing docs"
git push origin v0.1.1
```

---

## Self-review against spec

**Spec coverage check:**

| Spec requirement | Task that covers it |
|---|---|
| Check current state of release signing (HANDOFF.md Pass 8) | Context explored pre-plan |
| No keystore generation — user does it | Task 2 wires config; keytool command only in Task 3 docs |
| `keystore.properties` template in docs | Task 3 |
| `keystore.properties` gitignore | Already in `.gitignore` — no action needed |
| Wire signing config in `app/build.gradle.kts` | Task 2 |
| Graceful fail when keystore.properties absent | Task 2 (`validateReleaseKeystore` task) + Task 8 verification |
| `assembleDebug` unaffected | Task 2 step 2 + Task 8 step 1 |
| versionCode 1→2, versionName 0.1.0→0.1.1 | Task 2 |
| CHANGELOG v0.1.1 entry | Task 5 |
| `docs/RELEASE_KEYSTORE.md` with exact keytool command | Task 3 |
| Backup warnings prominent | Task 3 (⚠️ section + Step 6) |
| TODO in RELEASE_KEYSTORE.md | Task 3 (top of file) |
| TODO prepended in BACKLOG.md | Task 4 |
| `docs/PRIVACY.md` ~300 words plain English | Task 6 |
| Privacy policy covers: snippets, blacklist, WebDAV creds, no analytics | Task 6 |
| `docs/PLAY_CONSOLE_CHECKLIST.md` | Task 7 |
| Short description candidates (80 chars) | Task 7 (3 options) |
| Privacy policy URL stall warning | Task 7 (⚠️ Before you start + section 3) |
| Content rating guidance | Task 7 (section 4) |
| Screenshot list with suggestions | Task 7 (section 2) |
| Feature branch `claude/play-store-aab-signing` | Task 1 |
| PR opened | Task 9 |
| Tag `v0.1.1` after merge | Task 9 step 4 |
| Report: AAB path, keystore SHA-1, docs links | Task 9 PR description covers what user does next; SHA-1 obtained by user after keytool run |

**Gaps found:** None. All spec requirements mapped.

**Placeholder scan:** No TBD/TODO left in plan steps. All code blocks are complete. All commands have expected outputs.

**Type consistency:** No custom types introduced — this is build config + docs only.
