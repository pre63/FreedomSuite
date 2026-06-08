# Freedom Suite

Parasite-free Android applications for F-Droid. No tracking. No crash telemetry.
All local data encrypted. Open protocols. Your servers, your keys.

## Applications

| App | Package | Status |
|-----|---------|--------|
| **Freedom Inbox** | `org.freedomsuite.inbox` | IMAP/SMTP · swipe to archive |
| **Freedom Calendar** | `org.freedomsuite.calendar` | CalDAV · event CRUD |
| **Freedom Messages** | `org.freedomsuite.messages` | Channels · photos · Freedom Sync |
| **Freedom Auth** | `org.freedomsuite.auth` | TOTP codes · encrypted backup |
| **Freedom Files** | `org.freedomsuite.files` | Encrypted photos & files · Freedom Sync |
| **Freedom Chat** | `org.freedomsuite.chat` | Placeholder |
| **Freedom Keyboard** | `org.freedomsuite.keyboard` | Private IME · local predictions · on-device dictation |
| **Freedom Search** | `org.freedomsuite.search` | Unified local search · mail · photos · calendar · messages |

## Build & publish

```bash
./scripts/github-setup.sh       # once: public GitHub repo + push main
./scripts/publish.sh            # prod APKs → GitHub Releases (automated)
./scripts/publish.sh inbox      # single app
./scripts/publish.sh --local    # build here, upload with gh (no CI wait)
./scripts/ci-verify.sh          # local verify: audits + tests + prod APKs
./gradlew assembleDevDebug      # dev builds with logging
```

CI verifies every push to `main`. Releases are APK files on GitHub — see [docs/GITHUB-RELEASES.md](docs/GITHUB-RELEASES.md). F-Droid metadata: [docs/FDROID-PIPELINE.md](docs/FDROID-PIPELINE.md).

## Project structure

```
FreedomSuite/
├── apps/           # Application modules
├── core/           # Shared libraries (crypto, UI, storage, search-api, ml, …)
├── protocol/       # IMAP, SMTP, CalDAV adapters
├── sync/           # Freedom Sync (Rust core + Android)
├── docs/           # Privacy, crypto, threat model
└── fdroid/         # F-Droid metadata
```

## Principles

- **Release builds**: zero logcat, no crash reporting, R8 log stripping
- **Encryption**: SQLCipher local DBs, AES-256-GCM files, Keystore-wrapped keys
- **Post-quantum**: hybrid ML-KEM for device pairing and sync (see `docs/CRYPTO.md`)
- **Email/calendar**: mailbox.org via IMAP/SMTP/CalDAV
- **Everything else**: Freedom Sync to encrypted blob storage (WebDAV or S3)

See [docs/PRIVACY.md](docs/PRIVACY.md) and [docs/CRYPTO.md](docs/CRYPTO.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
