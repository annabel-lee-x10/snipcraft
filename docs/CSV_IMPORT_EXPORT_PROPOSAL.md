# CSV Import / Export Proposal — Snipcraft

_Date: 2026-05-15 | Status: Draft, awaiting user approval before implementation_

---

## 1. Current State (JSON Import / Export)

### What exists today

Snipcraft ships a full JSON backup/restore system (Pass 6, `core:backup`).

| Aspect | Detail |
|---|---|
| **Location** | Settings → "Backup & Restore" section |
| **Export button** | "Export snippets" — opens system share sheet with a `.json` file |
| **Import button** | "Import snippets" — opens Android file picker (`*/*`) |
| **Format** | JSON v1 via `kotlinx.serialization` |
| **Conflict dialog** | Modal before import: user chooses **Skip existing** or **Overwrite existing** |
| **Result dialog** | Post-import summary: "Imported X snippets, Y folders. Skipped Z." |

### JSON v1 schema

```json
{
  "version": 1,
  "exportedAt": "2026-05-15T10:30:45.123Z",
  "snippets": [
    {
      "id": "uuid-string",
      "shortcut": ";email",
      "body": "john@example.com",
      "type": "PLAIN",
      "triggerMode": "ON_DELIMITER",
      "folderId": "uuid-or-null",
      "usageCount": 42,
      "createdAt": 1700000000000,
      "updatedAt": 1700000000000,
      "isPinned": false,
      "isEnabled": true,
      "caseSensitive": false,
      "description": "optional note",
      "syncVersion": 0
    }
  ],
  "folders": [
    {
      "id": "uuid-string",
      "name": "Work",
      "color": -16776961,
      "sortOrder": 0,
      "createdAt": 1700000000000,
      "updatedAt": 1700000000000,
      "syncVersion": 0
    }
  ]
}
```

### What is NOT covered today

- CSV import or export (none)
- Selective export (per-folder or filtered)
- Compatibility rules / app profiles export
- Import validation or schema version migration
- Undo/rollback after import
- Tags (not in the domain model)
- `lastUsedAt` field (in domain model but excluded from backup)

---

## 2. Other Tools' Export Formats

| App | Format | Trigger column | Expansion column | Label/Name | Folder/Group | Header row |
|---|---|---|---|---|---|---|
| **TextExpander** | CSV, `.textexpander` XML | `abbreviation` | `snippet` | `label` (col 3, optional) | Separate XML container | Yes |
| **aText** | CSV | `shortcut` | `phrase` | — | — | Yes |
| **Beeftext** | CSV (headerless), JSON | col 1: `keyword` | col 2: `snippet` | col 3: `name` | `groups[]` in JSON only | **No** |
| **Espanso** | YAML config | `trigger` | `replace` | `label` | By filename | N/A |
| **PhraseExpress** | `.pexdb` SQL; no real CSV | `Description` (3rd-party) | `Content` | `Autotext` | Not in CSV | Unknown |
| **Texpand** | CSV (schema undocumented) | likely `shortcut` | likely `phrase` | unknown | unknown | unknown |
| **AutoTextExpander** | JSON only | unknown | unknown | — | — | N/A |

**Key takeaway:** The trigger field is called `abbreviation`, `shortcut`, `keyword`, or `trigger` depending on the app. The expansion field is called `snippet`, `phrase`, `body`, `expansion`, or `replace`. A good importer must detect these synonyms automatically.

---

## 3. Proposed Snipcraft CSV Schema

### Version marker

The first line of a Snipcraft-native CSV export is a comment:

```
# snipcraft-csv v1
```

This is skipped on import and used for versioning detection. Foreign CSVs without this line are treated as "external format, attempt header detection."

### Header row

```
shortcut,body,folder,description,type,trigger_mode,case_sensitive,is_enabled,is_pinned,usage_count,created_at,updated_at
```

### Column definitions

| Column | Type | Required | Default on import | Notes |
|---|---|---|---|---|
| `shortcut` | string | **Yes** | — | The trigger text, e.g. `;email` |
| `body` | string | **Yes** | — | Expansion text. Multi-line: RFC 4180 quoting (wrap in `"`, escape `"` as `""`) |
| `folder` | string | No | _(no folder)_ | Folder **name** (not ID). Created if it doesn't exist on import. |
| `description` | string | No | `""` | Optional user note |
| `type` | enum | No | `PLAIN` | `PLAIN`, `RICH_HTML`, `IMAGE_URI`, `GIF_URI`, `CLIPBOARD_REF`, `MULTI` |
| `trigger_mode` | enum | No | `ON_DELIMITER` | `ON_DELIMITER`, `IMMEDIATE`, `WORD_BOUNDARY` (match TriggerMode enum) |
| `case_sensitive` | bool | No | `false` | `true` or `false` |
| `is_enabled` | bool | No | `true` | `true` or `false` |
| `is_pinned` | bool | No | `false` | `true` or `false` |
| `usage_count` | integer | No | `0` | Preserved across round-trips; ignored on foreign CSV import |
| `created_at` | ISO 8601 string | No | _import time_ | e.g. `2026-01-15T09:00:00Z` |
| `updated_at` | ISO 8601 string | No | _import time_ | e.g. `2026-01-15T09:00:00Z` |

### Example rows

```csv
# snipcraft-csv v1
shortcut,body,folder,description,type,trigger_mode,case_sensitive,is_enabled,is_pinned,usage_count,created_at,updated_at
;email,john@example.com,,,PLAIN,ON_DELIMITER,false,true,false,0,2026-01-15T09:00:00Z,2026-01-15T09:00:00Z
;sig,"Best regards,
John",Work,Email signature,PLAIN,ON_DELIMITER,false,true,false,42,2026-01-15T09:00:00Z,2026-05-15T09:00:00Z
;address,"123 Main St, Suite 400",Work,,PLAIN,ON_DELIMITER,false,true,false,5,2026-02-01T00:00:00Z,2026-02-01T00:00:00Z
```

Note `;sig` has an embedded newline inside the quoted `body` field — valid RFC 4180.

---

## 4. Ingestion Strategy for Foreign CSVs

### Header detection heuristics

On import, if the file lacks `# snipcraft-csv v1`, scan row 1 for known column names:

**Trigger column synonyms** (case-insensitive):
`shortcut`, `abbreviation`, `trigger`, `keyword`, `command`, `shorthand`, `abbr`

**Expansion column synonyms** (case-insensitive):
`body`, `expansion`, `snippet`, `phrase`, `text`, `replace`, `content`, `value`, `template`, `plaintext`, `plain_text`

**Label/description synonyms** (case-insensitive):
`description`, `label`, `name`, `note`, `title`

**Folder/group synonyms** (case-insensitive):
`folder`, `group`, `category`, `collection`, `set`

**Detection algorithm:**
1. Read row 1 as candidate header. Check if any cell matches a known synonym.
2. If ≥ 1 synonym matched: treat as headered CSV, map columns.
3. If 0 matches but row 1 looks like data (trigger starts with a non-alpha character like `;`, `:`, `/`): treat as **headerless CSV**, map positionally: col 1 = shortcut, col 2 = body, col 3 = description (Beeftext convention).
4. If detection is ambiguous: fall back to UI column-mapping dialog (Phase 2 feature — see §5).

### Edge cases to handle

| Case | Handling |
|---|---|
| BOM (`﻿` at byte 0) | Strip before parsing — Excel UTF-8 CSV always emits a BOM |
| Encoding | Default UTF-8. If UTF-8 decode fails, attempt Windows-1252 (common for PhraseExpress exports) |
| Trailing whitespace in `shortcut` | Trim before insert |
| Empty rows | Skip silently |
| Rows with `shortcut` only (no `body`) | Skip with warning count |
| Embedded commas in unquoted field | Fail gracefully — log as parse error, skip row, continue |
| Embedded newlines | Supported per RFC 4180 via quoted strings |
| `folder` name collision | If folder with that name already exists, use it. Create otherwise. |
| `type` enum unknown value | Fall back to `PLAIN` (same behavior as JSON import) |
| `trigger_mode` enum unknown value | Fall back to `ON_DELIMITER` |
| Timestamp parse failure | Use import time instead |
| Duplicate `shortcut` within same CSV | Last row wins (consistent with OVERWRITE; first row wins for SKIP_EXISTING) |

### Conflict resolution

Same SKIP_EXISTING / OVERWRITE dialog used for JSON import — no new UI needed.  
Result dialog shows: "Imported X snippets (Y folders created). Skipped Z. Parse errors: W."

### Phase 2: Manual column mapping UI

If header detection fails entirely, show a column-mapping sheet:
- Display first 3 rows of the CSV as a preview table.
- User assigns each detected column to one of: Shortcut / Body / Description / Folder / (ignore).
- Save mapping for the session (not persisted — each import is one-shot).

---

## 5. Suggested Implementation Order

Assuming user approves this proposal:

1. **CSV Export** — serialize snippets + folders to the proposed schema; add "Export as CSV" option alongside existing JSON export in Settings. Low risk, no import logic needed.
2. **CSV Import — Snipcraft-native** — parse `# snipcraft-csv v1` files only. Re-use existing conflict dialog and result dialog.
3. **CSV Import — foreign, headered** — add synonym detection for headered CSVs (TextExpander, aText).
4. **CSV Import — headerless** — add positional fallback for Beeftext-style CSVs.
5. **Phase 2: Column-mapping UI** — for ambiguous or unknown formats.

Each step is independently shippable and testable.

---

## 6. Open Questions for User

1. **Which tool is your CSV from?** Knowing the source tells us whether we need to implement synonym detection immediately or if native Snipcraft CSV format is sufficient for your use case.

2. **Multi-line bodies:** Does your CSV already use RFC 4180 quoting for multi-line snippets, or are newlines represented as `\n` literal escape sequences? Both are common.

3. **Folder support:** Do you use folders in Snipcraft? If so, does your source CSV have a group/category column we should map?

4. **Round-trip priority vs. interop priority:** Should the initial CSV export optimize for faithful Snipcraft round-trips (all 12 columns) or for maximum compatibility with other tools (just 2 columns: shortcut + body)?

5. **Export format choice:** Should CSV export be a separate button, or replace/augment the existing "Export snippets" button with a format picker (JSON / CSV)?
