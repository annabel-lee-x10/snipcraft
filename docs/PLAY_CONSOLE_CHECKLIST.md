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
• WebDAV sync. Bring your own server (Nextcloud, Synology, self-hosted dav). Credentials stored with AES-256 encryption on-device.
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
