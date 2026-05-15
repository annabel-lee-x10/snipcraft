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
