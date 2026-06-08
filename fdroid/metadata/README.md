# F-Droid metadata

Ready-to-submit build metadata for each Freedom Suite app. Official submission copies these into [fdroiddata](https://gitlab.com/fdroid/fdroiddata).

## Verify locally before submitting

```bash
./scripts/ci-verify.sh
```

## Sync to fdroiddata fork

```bash
git clone https://gitlab.com/fdroid/fdroiddata.git
./scripts/fdroid-sync-metadata.sh ../fdroiddata
cd ../fdroiddata
fdroid lint org.freedomsuite.inbox   # requires fdroidserver
```

## Apps

| Metadata file | App ID | Status |
|---------------|--------|--------|
| `org.freedomsuite.inbox.yml` | Inbox | Ready |
| `org.freedomsuite.calendar.yml` | Calendar | Ready |
| `org.freedomsuite.messages.yml` | Messages | Ready |
| `org.freedomsuite.auth.yml` | Auth | Ready |
| `org.freedomsuite.files.yml` | Files | Ready (ML prebuild) |
| `org.freedomsuite.keyboard.yml` | Keyboard | Ready |
| `org.freedomsuite.search.yml` | Search | Ready |
| — | Chat | **Not submitted** (placeholder) |

## Tag convention

Per-app tags so F-Droid `UpdateCheckMode` works in a monorepo:

```
inbox-v1.0.0
files-v1.0.0
keyboard-v0.1.0
```

Update `commit:` and version fields in the YAML when releasing.

## Build command (CI / local)

```bash
./gradlew fdroidVerify
# or single app:
./gradlew :apps:inbox:assembleProdRelease
```

See [docs/FDROID-PIPELINE.md](../../docs/FDROID-PIPELINE.md) for the full pipeline.
