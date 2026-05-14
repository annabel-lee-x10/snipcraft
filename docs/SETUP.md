# Snipcraft — Setup Guide

**Snipcraft v0.1.0 · Android 12+ (API 31) · Pixel 8 Pro**

---

## Path A — Sideload the prebuilt APK (recommended)

### 1. Download the APK

Open this URL on your phone or copy the file over USB:

```
https://github.com/annabel-lee-x10/snipcraft/releases/download/v0.1.0/app-release-unsigned.apk
```

Or navigate to: **GitHub → annabel-lee-x10/snipcraft → Releases → v0.1.0 → app-release-unsigned.apk**

### 2. Allow installation from unknown sources

On Pixel 8 Pro with Android 14/15:

1. Go to **Settings → Apps → Special app access → Install unknown apps**
2. Tap your browser (e.g., Chrome) or file manager
3. Toggle **Allow from this source** ON

[screenshot: Settings → Apps → Special app access → Install unknown apps → Chrome → toggle]

### 3. Sign the APK

The release APK is unsigned. Android refuses to install unsigned APKs. You need `apksigner` from Android SDK Build-Tools (already on your machine if Android Studio is installed).

On Windows (Git Bash):

```bash
# Set this to your Build-Tools version
BTVER="35.0.0"
BTPATH="$LOCALAPPDATA/Android/Sdk/build-tools/$BTVER"

"$BTPATH/apksigner" sign \
  --ks "$USERPROFILE/.android/debug.keystore" \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out snipcraft.apk \
  app-release-unsigned.apk
```

This signs with the Android debug keystore (always present after Android Studio installation). The resulting `snipcraft.apk` is ready to install.

**Tip:** If you have ADB, skip the "Install unknown apps" dance entirely — see Path B's `adb install` one-liner with the debug APK instead.

### 4. Install the APK

Transfer `snipcraft.apk` to your Pixel 8 Pro (USB, Bluetooth share, or cloud). Open it with your file manager. Android will show a warning ("Install unknown app?") — tap **Install**. The app will appear in your launcher as **Snipcraft**.

---

### 5. Onboarding walkthrough

Open Snipcraft. You'll see a 4-step onboarding flow.

#### Step 1 (card 1) — Grant Accessibility permission

Tap **Open Accessibility Settings**. You'll land in:

> **Settings → Accessibility → Downloaded apps → Snipcraft**

Tap **Snipcraft**, then toggle **Use Snipcraft** ON.

Android will show a warning:
> *"This app can read all text on screen, including passwords..."*

Tap **Allow**. This warning is standard for all accessibility services — Snipcraft only triggers on your configured shortcuts and does not log keystrokes or send data anywhere.

After granting, Snipcraft detects the permission and advances automatically.

[screenshot: Accessibility settings → Downloaded apps → Snipcraft → toggle]

#### Step 2 (card 2) — Notification permission

Tap **Allow notifications**. This enables the health watchdog alert (appears if the service gets killed in the background). Tap **Allow** in the system dialog.

[screenshot: Notification permission dialog]

#### Step 3 (card 3) — Battery exemption (important for background survival)

Tap **Grant battery exemption**. You'll land in:

> **Settings → Apps → Snipcraft → Battery → Unrestricted**

Select **Unrestricted**. Without this, Android may kill the accessibility service after a few hours of screen-off time.

[screenshot: Apps → Snipcraft → Battery → Unrestricted]

#### Step 4 (card 4) — Test sandbox

The onboarding ends with a sandbox text field. Try typing `;today ` (semicolon, the word today, then a space). Snipcraft should replace it with today's date inline.

Tap **Get Started** to open the snippet library.

---

### 6. Create or edit snippets

The **Library** screen shows 8 pre-loaded starter snippets (`;sig`, `;email`, `;today`, etc.).

To create a new snippet:
1. Tap the **+** button (bottom-right)
2. Enter a **Shortcut** (e.g., `;addr`)
3. Enter the **Body** (the text it expands to)
4. Tap **Save** in the top bar

To edit: tap any snippet in the list.

**Built-in variables you can use in the body:**

| Variable | Expands to |
|---|---|
| `{{date}}` | Today's date (e.g., `2026-05-14`) |
| `{{date:dd/MM/yyyy}}` | Date in custom format |
| `{{time}}` | Current time (e.g., `14:32`) |
| `{{time:hh:mm a}}` | Time with AM/PM |
| `{{clipboard}}` | Current clipboard contents |

---

### 7. WebDAV sync setup (optional — for multiple devices)

Go to **Settings → WebDAV Sync**.

1. **Server URL** — full URL to the JSON file on your WebDAV server, e.g.:
   `https://your-nas.local:5006/snipcraft/snippets.json`
   (must end in `.json`)

2. **Remote path** — the directory part, e.g.: `/snipcraft/`

3. **Username / Password** — your WebDAV credentials

4. **Allow plain HTTP** — leave OFF unless your server doesn't have SSL

5. Tap **Test Connection** — should show ✓ Found or ✓ Created

6. Tap **Sync Now** to do an immediate sync

7. Set **Auto-sync interval** (Off / 1h / 6h / 24h) for background sync

[screenshot: Settings → WebDAV Sync section]

---

## Path B — Build from source

### Prerequisites

| Tool | Required version | Notes |
|---|---|---|
| JDK 21 | exactly 21 | Android Studio ships this at `C:\Program Files\Android\Android Studio\jbr` |
| Android SDK | Platform 35, Build-Tools 35.0.0 | Install via Android Studio SDK Manager |
| Git | any | |
| Android Studio | optional | Needed for adb / emulator |

Gradle 9.3.1 downloads automatically via the wrapper.

### Clone and configure

```bash
git clone https://github.com/annabel-lee-x10/snipcraft.git
cd snipcraft
```

Create `local.properties` at the repo root (gitignored):

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

Note the escaped backslashes — AGP 9.x requires this format on Windows.

### Build debug APK

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
PATH="$JAVA_HOME/bin:$PATH" \
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Install on a connected device with ADB:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Build release APK

The release build requires a signing keystore. Generate one (run once, keep the `.jks` file somewhere safe — **never commit it**):

```bash
keytool -genkeypair \
  -keystore release-keystore.jks \
  -alias snipcraft \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass YOUR_STORE_PASS \
  -keypass YOUR_KEY_PASS \
  -dname "CN=Snipcraft, O=Personal, C=US"
```

Create `keystore.properties` at the repo root (gitignored):

```properties
storeFile=../../release-keystore.jks
storePassword=YOUR_STORE_PASS
keyAlias=snipcraft
keyPassword=YOUR_KEY_PASS
```

Wire it in `app/build.gradle.kts` (see `docs/HANDOFF.md` for the exact snippet). Then:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
PATH="$JAVA_HOME/bin:$PATH" \
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release-unsigned.apk` (unsigned) or `app-release.apk` (signed, if keystore is wired).

The release APK is unsigned and cannot be installed directly via `adb install`. Either wire signing (see `docs/HANDOFF.md` signing section) or sign with the debug keystore before installing:

```bash
# Quick sign with debug keystore (development use only)
BTVER="35.0.0"
"$LOCALAPPDATA/Android/Sdk/build-tools/$BTVER/apksigner" sign \
  --ks "$USERPROFILE/.android/debug.keystore" \
  --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

adb install app-release-signed.apk
```

### Run tests

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
PATH="$JAVA_HOME/bin:$PATH" \
./gradlew test
```

Expected: 196 tests, 0 failures.

---

## Path C — Troubleshooting

### Accessibility service got killed (OEM battery management)

**Symptom:** Shortcuts stopped expanding after leaving the phone idle overnight.

**Cause:** OEM battery management (Pixel does this less aggressively than Xiaomi/Samsung but it still happens if battery exemption wasn't granted).

**Fix:**
1. Go to **Settings → Accessibility → Downloaded apps → Snipcraft** — re-enable if it was turned off
2. Go to **Settings → Apps → Snipcraft → Battery → Unrestricted** — grant if not already
3. Open Snipcraft → **Settings → Re-check status** — confirms the service is running

Snipcraft will also post a notification ("Snipcraft needs attention") if the watchdog worker detects the service is down. Tap it to jump to Accessibility Settings.

---

### Battery exemption was rescinded

**Symptom:** Service keeps getting killed even though it was working before.

**Cause:** A system update or Android's "adaptive battery" revoked the exemption.

**Fix:** Go to **Settings → Apps → Snipcraft → Battery** and set back to **Unrestricted**.

---

### Snippets not expanding in a specific app

**Check 1 — Is that app excluded?**
Open Snipcraft → **Settings → Excluded Apps**. If the app's package name is in the list, remove it.

**Check 2 — Which keyboard is active?**
Open Snipcraft → **Settings → Diagnostics**. Under "Active Keyboard", check the IME package. Some keyboards (especially Samsung Keyboard with aggressive autocorrect) can interfere. Try Gboard as a test.

**Check 3 — Is the app a WebView / browser?**
Snipcraft uses the `PASTE` strategy for Chrome/Firefox/Discord/WhatsApp. This replaces clipboard contents temporarily. If clipboard restore is slow, reduce the 600ms post-expansion delay by swapping to a manual trigger or checking `ExpansionExecutor.kt`.

**Check 4 — Run the diagnostics test field**
Open Snipcraft → **Settings → Diagnostics** → scroll to "Test Field". Type `;today ` there. If it expands, the service is working and the problem is app-specific.

---

### WebDAV sync not pulling new snippets

1. Tap **Test Connection** in Settings → WebDAV Sync. A `Found` result means the file exists and credentials are correct.
2. Check the URL format — it must point to the JSON file directly, not a directory.
3. If using a self-signed certificate, your WebDAV client may reject it. Test with a proper cert or temporarily enable **Allow plain HTTP** on an HTTP server.
4. Check that the server's clock is reasonably accurate — sync uses `updatedAt` timestamps for conflict resolution.

---

### Diagnostics export

If you hit a reproducible bug and want to inspect what's happening:

1. Open Snipcraft → **Settings → Diagnostics**
2. Scroll to "Actions" → tap **Export Diagnostics Bundle**
3. Share the JSON via email or save to disk

The bundle includes service health flags, the last 50 expansion events (shortcut + package + success/fail), device model, and Android version. It does **not** include snippet bodies, clipboard contents, or credentials.

---

*For the full feature roadmap and architecture notes, see `docs/HANDOFF.md` and `D:\a10101100_labs\PLAN_text_expander.md`.*
