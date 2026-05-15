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

| File | Line | Item |
|---|---|---|
| `feature/editor/EditorScreen.kt` | 54 | `EditorEvent.ShowError` is silently swallowed — no snackbar or error UI shown to user when a save fails. Add `SnackbarHostState` to `EditorScreen` and show the error message. |

---

## Phase 2 — Power-user core (v0.2.0)

- **INSTANT + MANUAL trigger modes** — `TriggerMode.INSTANT` and `TriggerMode.MANUAL` are defined in the domain but not wired into `SnipAccessibilityService` or the editor UI.
- **User-defined variables** — `{{var:name}}`, `{{cursor}}` placement marker, `{{ask:label}}` floating input prompt, `{{choice:opt1|opt2}}` picker.
- **Suggestion popup** — floating overlay anchored to cursor position when a partial shortcut is typed.
- **Expansion menus** — one shortcut mapping to multiple body choices.
- **Tags + tag filter chips** — `TagEntity` and `SnippetTagCrossRef` entities are already in the data model but have no UI.
- **Folder nesting** — `FolderEntity.parentId` exists, nesting not exposed in UI.
- **Quick-Add tile** — `feature:quickadd` is a Phase 2 stub.

## Phase 3 — Rich content + automation (v0.3.0)

- Rich text snippets (Markdown body, rendered to HTML/Spanned on paste).
- Image and GIF snippets (URI-based).
- Tasker plugin + public Intent API (`dev.a10101100.snipcraft.action.EXPAND`).
- Share menu integration ("Save as snippet").

## Phase 4 — Sync + per-app rules + analytics (v0.4.0)

- Per-app full rule editor (currently blacklist-only; `CompatibilityRuleEntity` schema supports full rules).
- Community compatibility profile registry (GitHub-hosted JSON).
- Local usage analytics dashboard and trigger heatmap.
- Encrypted vault (SQLCipher + biometric lock).

## Infrastructure

- `core:testing` module is an empty skeleton — no fake repositories implemented. Pass 5+ tests use MockK directly; consolidating fakes here would simplify future tests.
- `core:ui` shared composables module is empty — `EmptyState` lives in `core:designsystem` instead. Worth consolidating when there are 3+ shared composables.
- PlaceholderTest files in most modules: replace with real integration tests as the modules grow.

---

*See `D:\a10101100_labs\PLAN_text_expander.md` §G for full phased roadmap.*
