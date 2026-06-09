# F-Droid pipeline and CI audit

## Current state (after automation)

| Layer | Status |
|-------|--------|
| **CI on push/PR** | `.github/workflows/ci.yml` → `fdroidVerify` |
| **Publish APKs** | `./scripts/publish.sh` → `.github/workflows/publish.yml` → GitHub Releases |
| **Local gate** | `./scripts/ci-verify.sh` |
| **F-Droid metadata** | `fdroid/metadata/org.freedomsuite.*.yml` (7 apps) |
| **Integration tests** | `./gradlew integrationTest` (8 Robolectric + 4 JVM audit tests) |
| **Privacy audit** | `./gradlew privacyAudit` (all F-Droid apps) |

## One-command verification

```bash
./scripts/ci-verify.sh
```

Runs: asset prebuild → integration tests → privacy/storage audits → `assembleProdRelease` for all shippable apps.

Gradle equivalent:

```bash
./gradlew fdroidVerify
```

## F-Droid apps

| App ID | Gradle task | Tag pattern | Notes |
|--------|-------------|-------------|-------|
| `org.freedomsuite.inbox` | `:apps:inbox:assembleProdRelease` | `inbox-v1.0.0` | |
| `org.freedomsuite.calendar` | `:apps:calendar:assembleProdRelease` | `calendar-v1.0.0` | |
| `org.freedomsuite.messages` | `:apps:messages:assembleProdRelease` | `messages-v1.0.0` | |
| `org.freedomsuite.auth` | `:apps:auth:assembleProdRelease` | `auth-v1.0.0` | |
| `org.freedomsuite.files` | `:apps:files:assembleProdRelease` | `files-v1.0.0` | `prebuild`: fetch ML models |
| `org.freedomsuite.keyboard` | `:apps:keyboard:assembleProdRelease` | `keyboard-v0.1.0` | No `INTERNET` |
| `org.freedomsuite.search` | `:apps:search:assembleProdRelease` | `search-v0.1.0` | Suite search hub |

**Not shipped:** `org.freedomsuite.chat` (placeholder UI only).

## Submitting to official F-Droid

F-Droid builds from source on **their** infrastructure and signs with **their** key. You do not upload APKs manually.

### Automated release flow (recommended)

1. Merge to `main` — CI runs `fdroidVerify` automatically.
2. Bump `versionCode` / `versionName` in the app’s `build.gradle.kts`.
3. Update the matching `fdroid/metadata/org.freedomsuite.<app>.yml` build block.
4. Tag per app: `git tag inbox-v1.0.1 && git push origin inbox-v1.0.1`
5. Copy metadata to your **fdroiddata** fork:

   ```bash
   ./scripts/fdroid-sync-metadata.sh /path/to/fdroiddata
   ```

6. Open a merge request on [gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).

7. F-Droid’s GitLab CI builds the app from your tag. First inclusion requires human review.

### First-time checklist

- [ ] Replace `SourceCode` / `Repo` URLs in metadata with your real Git remote
- [ ] Add screenshots to fdroiddata (`metadata/org.freedomsuite.inbox/en-US/…`)
- [ ] Confirm ONNX models fetch in F-Droid `prebuild` (Freedom Files)
- [ ] Run `fdroid lint org.freedomsuite.inbox` locally if you have [fdroidserver](https://gitlab.com/fdroid/fdroidserver) installed
- [ ] Optional: `fdroid build org.freedomsuite.inbox` in Docker to mirror F-Droid builders

## Integration test audit

### What runs today

| Test | Module | Coverage |
|------|--------|----------|
| `MockImapProtocolTest` | mock-server | IMAP wire protocol |
| `MockServerSmokeTest` | mock-server | SMTP/HTTP mocks start |
| `DependencyAuditTest` | mock-server | Banned SDKs in Gradle + source |
| `StorageAuditTest` | mock-server | SQLCipher, no plain prefs |
| `ImapIntegrationTest` | integration | IMAP client vs mock |
| `SmtpIntegrationTest` | integration | SMTP send |
| `CalDavIntegrationTest` | integration | CalDAV CRUD |
| `FreedomSyncIntegrationTest` | integration | WebDAV/S3/local backup |
| `TotpIntegrationTest` | integration | TOTP generation |
| `MailboxOrgEndToEndTest` | integration | Full mail+calendar flow |
| `FileEncryptionTest` | integration | AES-GCM round-trip |

All mocks bind `127.0.0.1` — no real network.

### Gaps (not yet covered)

| Gap | Risk | Suggested test |
|-----|------|----------------|
| No app-level UI/instrumentation tests | Regressions in Compose screens | Robolectric smoke per app `MainActivity` |
| Search ContentProviders | IPC contract breaks | JVM test with `MockContentProvider` or androidTest |
| ML indexing pipeline | Files search empty | Robolectric test with tiny fixture image |
| Keyboard IME | Input latency/regression | androidTest `InputConnection` harness |
| Prod `assembleProdRelease` in CI only | Dev/prod manifest drift | Already in `fdroidVerify` |
| Per-app dependency audit was inbox-only | **Fixed** — `privacyAudit` now checks all apps |

## Audit findings (pre-pipeline)

### F-Droid readiness

| Issue | Severity | Resolution |
|-------|----------|------------|
| No CI | High | **Fixed** — GitHub Actions |
| Metadata templates only | High | **Fixed** — 7 YAML files |
| `privacyAudit` scanned inbox only | Medium | **Fixed** — all F-Droid apps |
| ML models not guaranteed in clean checkout | High | **Fixed** — `fdroid-prebuild.sh` + Files `prebuild` |
| No release tagging strategy | Medium | **Documented** — per-app tags |
| Placeholder chat in app table | Low | Excluded from `fdroidVerify` |
| GitHub URLs are placeholders | Medium | Update before fdroiddata MR |
| No screenshots | Medium | Required for fdroiddata MR quality |
| Keyboard IME review | Low | F-Droid may ask extra questions — no network permission helps |

### CI / build

| Issue | Severity | Notes |
|-------|----------|-------|
| `fdroidVerify` builds 7 release APKs (~15–25 min CI) | Medium | Acceptable; parallelize later with matrix job |
| No code signing in CI | OK | F-Droid signs when building from source |
| ONNX ~18 MB (SFace INT8 + YOLO + OCR) in CI | Medium | `fetch-ml-models.sh` + `quantize-ml-models.sh`; see [ML-MODELS.md](ML-MODELS.md) |
| Rust `freedom-sync-core` | OK | Pure Kotlin Android wrapper today — no cargo in CI |

## GitHub Actions secrets

None required for CI verify/build. Optional:

| Secret | Purpose |
|--------|---------|
| `GITLAB_TOKEN` | Future: auto-open fdroiddata MR |
| `FDROID_REPO_SSH_KEY` | Future: self-hosted F-Droid repo publish |

## Self-hosted F-Droid repo (optional)

If you want **your own** repo instead of waiting on fdroiddata review:

1. Install [fdroidserver](https://gitlab.com/fdroid/fdroidserver)
2. Point `config.yml` at a git mirror of your APKs or source builds
3. Extend `release.yml` to rsync/scp `repo/` to your web server

Official inclusion and self-hosted repo are independent — many projects do both.

## Related docs

- [THIRD-PARTY-AUDIT.md](THIRD-PARTY-AUDIT.md) — dependency policy
- [PRIVACY.md](PRIVACY.md) — release build guarantees
- [testing/README.md](../testing/README.md) — integration test details
