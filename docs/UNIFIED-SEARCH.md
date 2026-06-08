# Unified Search

Freedom Search queries Mail, Photos, Calendar, and Messages through a shared on-device API. All indexing and matching happen locally — no network calls.

## Architecture

```
apps/search          Freedom Search UI (tile results by source)
       │
       ▼
core/search-api      UnifiedSearchClient + SearchQueryExpander
       │
       ├── content://org.freedomsuite.inbox.search/results?q=…
       ├── content://org.freedomsuite.files.search/results?q=…
       ├── content://org.freedomsuite.calendar.search/results?q=…
       └── content://org.freedomsuite.messages.search/results?q=…
```

Each Freedom app exposes a `ContentProvider` that returns a `MatrixCursor` of `SearchHit` rows. The Search app (or any signed Freedom app holding `org.freedomsuite.permission.SEARCH`) calls `UnifiedSearchClient.search()` to fan out across installed sources.

## Permission

`org.freedomsuite.permission.SEARCH` is declared in **Freedom Search** with `protectionLevel="signature"`. Every provider and client must be signed with the same release key (same model as `CALENDAR_BRIDGE`).

## Query expansion

`SearchQueryExpander` maps natural terms to indexed synonyms — e.g. `car` → `vehicle`, `automobile`; `id` → `license`, `passport`. Photo search in Freedom Files and unified search share this logic.

## Photo intelligence

Freedom Files indexes images with ONNX models in `core/ml` (YOLOv5n object labels, PP-OCR text). Results land in `image_analysis.searchBlob`. The Files search provider reads that index — no re-inference at query time.

## Reuse in other apps

Per-app search can call the same client with a source filter:

```kotlin
val client = UnifiedSearchClient(context)
val photos = client.search("car", sources = setOf(SearchBridge.Source.PHOTO))
```

Freedom Files keeps a fast in-process DAO path for its photo grid; the provider path is available for cross-app unified search.

## Opening results

`UnifiedSearchClient.openHit()` launches the owning app with extras:

| Extra | Used by |
|-------|---------|
| `freedom_search_hit_id` | All |
| `freedom_search_hit_source` | All |
| `freedom_search_mail_folder` | Mail |
| `freedom_search_mail_uid` | Mail |

## Build

```bash
./gradlew :apps:search:assembleDevDebug
```

Install Freedom Search plus the apps you want to query (Inbox, Files, Calendar, Messages). All must share the same signing key.
