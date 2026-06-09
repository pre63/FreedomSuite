# Freedom Suite

Parasite-free Android applications for F-Droid and [GrapheneOS](docs/GRAPHENEOS.md). No tracking. No crash telemetry.
All local data encrypted. Open protocols. Your servers, your keys.

## Applications

| App | Package | Status |
|-----|---------|--------|
| **Inbox** | `org.freedomsuite.inbox` | IMAP/SMTP · spam filter · reply |
| **Calendar** | `org.freedomsuite.calendar` | Local events · edit · invite import |
| **Messages** | `org.freedomsuite.messages` | Encrypted private notes & photos |
| **Auth** | `org.freedomsuite.auth` | TOTP codes · encrypted backup |
| **Files** | `org.freedomsuite.files` | Encrypted photos & files · on-device ML search |
| **Camera** *(planned)* | `org.freedomsuite.camera` | Encrypted capture · on-device effects — see [docs/CAMERA-APP.md](docs/CAMERA-APP.md) |
| **Chat** | `org.freedomsuite.chat` | On-device LLM + optional remote — [docs/LLM-SERVICE.md](docs/LLM-SERVICE.md) (GitHub/dev) |
| **Keyboard** | `org.freedomsuite.keyboard` | Private IME · local predictions |
| **Search** | `org.freedomsuite.search` | Unified local search · deep links to hits |

## Build & publish

```bash
make help                       # all developer commands
make sim                        # start emulator + install all dev apps
make dev-mail-server            # local IMAP/SMTP for Inbox testing — docs/DEV-MAIL-SERVER.md
make install-dev                # install dev apps on connected device/emulator
make build-dev                  # assemble devDebug APKs (logging enabled)
make build-prod                 # assemble F-Droid release APKs
make ci-verify                  # audits + tests + prod APKs
./scripts/publish.sh            # prod APKs → GitHub Releases
```

**Install on device/emulator:** apps use **product flavors** — run `make install-dev` or `./gradlew :apps:auth:installDevDebug` (not `installDebug`). Dev packages are `org.freedomsuite.*.dev`. For x86 emulators, copy `local.properties.example` → `local.properties` and set `freedom.includeEmulatorAbis=true`.

CI verifies every push to `main`. Releases are APK files on GitHub — see [docs/GITHUB-RELEASES.md](docs/GITHUB-RELEASES.md). F-Droid metadata: [docs/FDROID-PIPELINE.md](docs/FDROID-PIPELINE.md).

## Project structure

```
FreedomSuite/
├── apps/           # Application modules
├── core/           # Shared libraries (crypto, UI, storage, search-api, ml, llm, …)
├── protocol/       # IMAP, SMTP, CalDAV adapters
├── sync/           # Freedom Sync (Rust core + Android)
├── docs/           # Privacy, crypto, threat model
└── fdroid/         # F-Droid metadata
```

## Principles

- **Release builds**: zero logcat, no crash reporting, R8 log stripping
- **Encryption**: SQLCipher local DBs, AES-256-GCM files, Keystore-wrapped keys
- **Post-quantum**: hybrid ML-KEM for device pairing and sync (see `docs/CRYPTO.md`)
- **Email**: your IMAP/SMTP provider (discovery + on-device spam filter)
- **Calendar**: local on-device events; email invite import from Inbox
- **Everything else**: Freedom Sync to encrypted blob storage (WebDAV or S3)

See [docs/PRIVACY.md](docs/PRIVACY.md), [docs/CRYPTO.md](docs/CRYPTO.md), [docs/LLM-SERVICE.md](docs/LLM-SERVICE.md), [docs/SPAM-FILTER.md](docs/SPAM-FILTER.md), and [docs/MVP-AUDIT.md](docs/MVP-AUDIT.md) for deployment scope.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
