# MVP deployment audit — Freedom Suite

**Goal:** Define a **minimum viable product** that is honest to ship on GrapheneOS / F-Droid — complete enough for daily use, with no embarrassing CRUD holes or wasted on-device ML.

**Competitor baselines** (privacy-aligned, not Google ecosystem):

| App | We compare to | Not trying to match |
|-----|---------------|---------------------|
| Inbox | FairEmail, K-9 | Gmail, Outlook |
| Calendar | Etar, Simple Calendar | Google Calendar sync |
| Messages | Local notes / Session (future) | Signal, WhatsApp |
| Auth | Aegis | 1Password |
| Files | Simple Gallery + Cryptomator | Google Photos, Immich cloud |
| Search | Sesame (local slice) | Google app search |
| Keyboard | OpenBoard, FlorisBoard | Gboard |
| Chat | Ollama / local LLM apps | ChatGPT app |

**Audit date:** 2026-06-07 · **Updated:** 2026-06-08 (P0 MVP complete for GitHub Release) · **Method:** source review of all `apps/*`, `core/ml`, cross-app bridges.

---

## Executive summary

| Verdict | Detail |
|---------|--------|
| **Shippable today?** | **7-app GitHub Release bundle** — Auth, Inbox, Calendar, Files, Messages, Search, Keyboard |
| **Not in release bundle** | Chat (dev/GitHub + model push), Camera (planned) |
| **Suite-wide P0** | ✅ Search deep links wired in all target apps |
| **CRUD holes closed** | Calendar edit + date/time pickers; Files folder delete + export + backup index; Messages channel delete; Inbox sign-out + reply |
| **ML underused** | Strong indexing in Files; zero ML in Inbox/Calendar/Search UI; Chat digests vision but has no local LLM |

**Recommended MVP:** 7 F-Droid apps with **one cross-app fix** (search deep links) + **per-app P0 table below**. Defer Chat, Camera, video, real-time messaging, CalDAV.

---

## Per-app scorecard

| App | Core loop works? | CRUD complete? | ML used well? | MVP ready? |
|-----|------------------|----------------|--------------|------------|
| **Auth** | ✅ TOTP + backup | C/R/D — no edit | N/A | ✅ Yes |
| **Files** | ✅ import/encrypt/search | Folder delete + export; backup includes ML index | ✅ Strong | ✅ Yes |
| **Inbox** | ✅ IMAP + spam filter | Reply, sign-out, settings | ❌ None | ✅ Yes |
| **Calendar** | ✅ agenda + reminders | Create/edit with dates, all-day, reminders | ❌ None | ✅ Yes |
| **Messages** | ✅ encrypted notes | Channel delete UI | ❌ None | ✅ Yes (honest copy) |
| **Search** | ✅ query + openHit | N/A | ⚠️ Indirect only | ✅ Yes |
| **Keyboard** | ✅ type + suggest | No clear-learned-words | ❌ ASR stub | ⚠️ Shift/layers P1 |
| **Chat** | ⚠️ LLM service wired | No persist yet | ✅ On-device GenAI + vision | ⚠️ Needs model push |

---

## CRUD audit (the “holes”)

### Calendar — events

| Operation | Status | Gap |
|-----------|--------|-----|
| Create | ✅ | Date/time pickers, all-day, reminder chips |
| Read | ✅ | Agenda grouped by Today/Tomorrow/date; Upcoming/Past filter |
| Update | ✅ | Edit screen with same fields as create |
| Delete | ✅ | Detail screen delete button |
| Reminders | ✅ | Local notifications via AlarmManager; re-scheduled on boot |

### Files — folders & files

| Operation | Status | Gap |
|-----------|--------|-----|
| Create folder | ✅ | |
| Read | ✅ | |
| **Update** | ❌ | **No rename folder/file, no move** |
| Delete file | ✅ | Long-press + detail |
| **Delete folder** | ⚠️ | `deleteFolder()` exists — **no UI** |
| Export/share | ❌ | No decrypt-to-SAF export |

### Messages — channels & messages

| Operation | Status | Gap |
|-----------|--------|-----|
| Create channel | ✅ | GROUP type is cosmetic |
| Read | ✅ | |
| Update | ❌ | No rename channel, no edit message |
| **Delete channel** | ⚠️ | ViewModel method — **no UI** |
| Delete message | ❌ | |

### Inbox — mail

| Operation | Status | Gap |
|-----------|--------|-----|
| Read/sync | ✅ | Manual refresh; 50-msg window |
| Send | ✅ | New mail only |
| Archive | ✅ | Swipe |
| Delete/trash | ❌ | |
| Reply/forward | ❌ | |
| **Sign out** | ⚠️ | `signOut()` — **no UI** |

### Auth — TOTP accounts

| Operation | Status | Gap |
|-----------|--------|-----|
| CRUD | C/R/D | No edit account (acceptable for MVP) |

---

## Suite-wide gaps (fix once, helps everyone)

### P0 — Search deep links (broken end-to-end)

`UnifiedSearchClient.openHit()` sets `freedom_search_hit_id`, `freedom_search_hit_source`, and mail folder/uid — **no app reads these in `MainActivity`**.

**MVP fix:** Shared `SearchDeepLinkHandler` in `core/search-api`; each app navigates to the correct screen on cold start.

| Source | Should open |
|--------|-------------|
| Mail | `message/{uid}` with correct folder |
| Photo | `file/{fileId}` |
| Calendar | `event/{uid}` |
| Message | `channel/{id}` + scroll to message |

### P0 — Backup restore drops ML index (Files)

`exportSnapshot()` omits `image_analysis`. After restore, photo search and similar faces are empty until user re-runs “Index all photos”.

**MVP fix:** Include analysis rows in backup JSON **or** auto `indexAllImages()` after restore.

### P1 — S3 sync UI missing (Auth, Files, Messages, Calendar)

Backend enum supports S3; only WebDAV fields shown. Either hide S3 chip or add credential form.

### P1 — Docs vs code

README still says Calendar = CalDAV, Chat = placeholder, Messages = channels — align [README.md](../README.md) and [PRIVACY.md](PRIVACY.md) with [CALENDAR-PROTOCOL.md](CALENDAR-PROTOCOL.md).

---

## On-device ML — what we have vs what we use

### Bundled today (~18 MB, `core/ml`)

| Model | Capability |
|-------|------------|
| YOLOv5n | 80 COCO object labels |
| YuNet + SFace | Faces + 128-D embeddings |
| PP-OCR | English text in images |

### Current usage

| App | Uses ML? | How |
|-----|----------|-----|
| **Files** | ✅ | Index, photo search, similar faces, on-demand OCR |
| **Chat** | ⚠️ | Vision → text summary for prompt; no local LLM |
| **Search** | ⚠️ | Reads Files index only |
| Inbox, Calendar, Messages, Auth, Keyboard | ❌ | |

### ML integration opportunities (high value, low scope)

These use **existing models** — no new weights for MVP.

| Feature | App | Effort | User value |
|---------|-----|--------|------------|
| **“Find in photos” from Search** | Search + Files | P0 with deep links | Core suite story |
| **Photo hit thumbnail** | Search | Small | Shows ML isn’t vaporware |
| **Mail attachment OCR** | Inbox | Medium | Receipts, PDF scans as images |
| **“Copy text from image” in detail** | Files | ✅ Already exists | Promote in UI |
| **Invite location → calendar hint** | Inbox → Calendar | Small | Parse ICS location into event |
| **Smart album: “People” stub** | Files | Medium | Cluster SFace embeddings (no new model) |
| **Chat local replies** | Chat | Large | Needs on-device LLM — **out of MVP** |
| **Keyboard next-word** | Keyboard | Done | Classical, not neural — OK |

**MVP ML rule:** Ship **Search → Files photo results** and **face/similar UI discoverability**. Defer Inbox attachment OCR to P1 unless quick.

---

## Competitor gap summary (what MVP must cover)

### Freedom Inbox

**Have:** IMAP sync, read, compose, archive, invite cards, local + server search.  
**Need for MVP:** Reply, sign-out/settings, stable sync (don’t wipe folder), search deep link, optional plain attachment list.  
**Defer:** HTML mail, push, multi-account, PGP.

### Freedom Calendar

**Have:** List, create (broken times), delete, email invite import.  
**Need for MVP:** **Edit event**, **date/time picker**, search deep link.  
**Defer:** Month view, recurrence, reminders, CalDAV.

### Freedom Messages

**Have:** Encrypted local channels, text + photo posts, sync backup.  
**Need for MVP:** Honest naming (“encrypted notes” not “group chat”), channel delete UI, fix attachment restore in sync.  
**Defer:** Real E2E messaging, Session/SimpleX.

### Freedom Auth

**Have:** TOTP, QR, backup/restore, app lock.  
**Need for MVP:** Nothing blocking — optional account edit P2.  
**Defer:** HOTP, icons, autofill.

### Freedom Files

**Have:** Encrypted import, folders, delete files, ML search, faces, backup.  
**Need for MVP:** Folder delete UI, export single file, backup includes index, search deep link.  
**Defer:** Rename/move, video, editor.

### Freedom Search

**Have:** Cross-app query, grouped results.  
**Need for MVP:** **Working openHit**, photo thumbnail on tile.  
**Defer:** History, widgets, filters.

### Freedom Keyboard

**Have:** QWERTY, suggestions, personal dictionary learning.  
**Need for MVP:** Shift key, numbers/symbols layer.  
**Defer:** Sherpa-ONNX voice, themes, emoji.

### Freedom Chat

**Chat MVP** — `core/llm` + ONNX GenAI on-device; remote Grok with on-device fallback. Run `make fetch-llm && make push-llm-model`. Persistence still P1. Not on F-Droid until model distribution is documented.

---

## MVP feature set (deployment target)

### Tier P0 — must ship (blocks “complete enough”)

| # | Item | App(s) | Est. |
|---|------|--------|------|
| 1 | Search deep link handler | core + all 4 providers | 2–3 d |
| 2 | Calendar **edit event** + date/time pickers | Calendar | 2–3 d |
| 3 | Inbox **settings**: sign out, account info | Inbox | 1 d |
| 4 | Inbox **reply** (prefill To/Subject/Re:) | Inbox | 1–2 d |
| 5 | Files **folder delete** UI | Files | 0.5 d |
| 6 | Files **export/share** one file (decrypt → SAF) | Files | 1–2 d |
| 7 | Backup **includes** `image_analysis` or auto re-index | Files | 1 d |
| 8 | Fix sync: stop `clearFolder` wiping state / raise limit | Inbox | 1 d |
| 9 | Messages: **delete channel** UI; remove fake GROUP promise | Messages | 1 d |
| 10 | README/PRIVACY doc accuracy | docs | 0.5 d |

**~12–15 dev days** for a coherent suite MVP.

### Tier P1 — should ship soon after P0

| Item | App(s) |
|------|--------|
| Keyboard shift + `?123` symbol layer | Keyboard |
| Files rename folder/file | Files |
| Search result photo thumbnail | Search |
| Inbox mail deep link passes folder (fix `openMessage`) | Inbox |
| Calendar restore UI (mirror Auth) | Calendar |
| Chat `FLAG_SECURE` | Chat |
| S3 settings or hide chip | Auth, Files, Messages |

### Tier P2 — post-MVP

| Item | Notes |
|------|-------|
| Freedom Camera MVP | [CAMERA-APP.md](CAMERA-APP.md) |
| Inbox attachment OCR | Uses existing PP-OCR |
| Files “People” face clusters | SFace only |
| Messages real protocol | [MESSAGING.md](MESSAGING.md) |
| Local LLM for Chat | Ollama localhost / small GGUF |
| Sherpa-ONNX keyboard dictation | [KEYBOARD docs] |
| Month/week calendar views | |
| HTML email viewer | |

### Explicitly out of MVP scope

- Google Play / Play Services
- Push notifications / background IMAP IDLE
- CalDAV server sync
- Signal/WhatsApp replacement
- Cloud photo library (Immich server)
- Real-time group chat
- On-device generative Chat
- Video, RAW, photo editing

---

## Recommended deployment bundle

### F-Droid MVP (7 apps)

```
org.freedomsuite.auth      ✅ ship
org.freedomsuite.files     ✅ ship after P0 #5–7
org.freedomsuite.inbox     ✅ ship after P0 #3–4, #8
org.freedomsuite.calendar  ✅ ship after P0 #2
org.freedomsuite.messages  ✅ ship after P0 #9 (rebrand copy)
org.freedomsuite.search    ✅ ship after P0 #1
org.freedomsuite.keyboard  ✅ ship (P1 shift/layers fast follow)
```

### Not in MVP store listing

```
org.freedomsuite.chat      — dev/GitHub only until local LLM
org.freedomsuite.camera    — not started
```

### User-facing positioning (honest)

> **Freedom Suite** — encrypted local apps for mail, calendar, photos, TOTP, notes, and search. No analytics. Your mailbox.org mail; your calendar stays on device; photos are encrypted and searchable on-device. Not a WhatsApp or Google Photos replacement.

---

## AI / ML MVP checklist

| Check | Action |
|-------|--------|
| Photo search works from Search app | P0 deep link + optional thumbnail |
| User discovers ML in Files | Onboarding line: “Import photos → Settings → Index” |
| Similar faces visible | ✅ detail screen — add empty-state hint |
| OCR copy | ✅ — mention in file detail for images |
| Don’t claim “AI chat” | Chat off F-Droid until LLM wired |
| Don’t bundle ML in mail/calendar | No value until attachment OCR (P1) |

---

## Risk register

| Risk | Mitigation |
|------|------------|
| User installs one app, search empty | Search app explains “install Files/Inbox…” |
| Mixed debug/release signing breaks bridge | F-Droid + docs: same release channel |
| 50-mail sync loses history | P0 #8 |
| Calendar create wrong time | P0 #2 |
| “Group channel” trust violation | P0 #9 rename/hide GROUP |

---

## Next step

Implement **P0 #1 (search deep links)** first — highest leverage across the suite — then **Calendar edit** and **Inbox reply/settings** in parallel.

See also: [GRAPHENEOS.md](GRAPHENEOS.md), [ML-MODELS.md](ML-MODELS.md), [UNIFIED-SEARCH.md](UNIFIED-SEARCH.md).
