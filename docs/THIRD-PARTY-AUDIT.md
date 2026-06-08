# Third-Party Dependency Audit — Freedom Suite

Last audited: 2026-06. Run `./gradlew privacyAudit` before every release.

## Policy

- **No Google Play Services, Firebase, or Google Analytics**
- **No crash reporting** (Sentry, Crashlytics, ACRA, Bugsnag, Datadog)
- **No analytics or attribution** (Mixpanel, Amplitude, Adjust, AppsFlyer, Facebook SDK)
- **No network calls except user-configured mail/sync servers** (mailbox.org IMAP/SMTP/CalDAV/WebDAV, S3-compatible storage you choose)
- **AndroidX and JetBrains libraries only** for UI, Room, crypto wrappers — all open source, no telemetry SDKs

## Direct dependencies (production)

| Library | Purpose | Network? | Analytics? |
|---------|---------|----------|------------|
| AndroidX (Compose, Room, Lifecycle, Biometric, Security) | UI, DB, app lock, EncryptedSharedPreferences | No* | No |
| SQLCipher (`net.zetetic`) | Encrypted SQLite | No | No |
| OkHttp (`com.squareup`) | CalDAV + Freedom Sync HTTP | Only our code | No |
| ZXing embedded (`com.journeyapps`) | QR scan in Freedom Auth | No | No |
| ONNX Runtime (`com.microsoft.onnxruntime`) | On-device OCR, object/face detection in Freedom Files | No | No |
| English frequency word list (hermitdave/FrequencyWords, via OpenSubtitles) | Freedom Keyboard local predictions | No | No |
| Sherpa-ONNX (planned) | Freedom Keyboard on-device dictation | No | No |
| Kotlin stdlib / coroutines | Language runtime | No | No |
| Timber (`com.jakewharton`) | **Dev flavor only** — stripped in prod | No | No |

\*AndroidX `emoji2` and `profileinstaller` auto-initializers are **removed** in every app manifest (`tools:node="remove"`). Emoji2 can otherwise download fonts from Google; profileinstaller is unnecessary.

## Transitive dependencies (notable)

| Transitive | Pulled by | Risk | Mitigation |
|------------|-----------|------|------------|
| `com.google.crypto.tink:tink-android` | `androidx.security:security-crypto` | Offline crypto only; no network | Required for EncryptedSharedPreferences |
| `com.google.code.gson:gson` | Tink | JSON parsing only | No network |
| ONNX model weights (`*.onnx` in assets) | Freedom Files ML | Bundled locally; no download at runtime | No |
| `com.google.guava:listenablefuture` | AndroidX concurrent utilities | Stub interface | No network |

**No** `com.google.android.gms`, `com.google.firebase`, or `play-services-*` in the production dependency tree.

## Network call sites (our code only)

| Module | Protocol | When |
|--------|----------|------|
| `protocol/imap` | TCP/TLS IMAP | User syncs mail |
| `protocol/smtp` | TCP/TLS SMTP | User sends mail |
| `protocol/caldav` | HTTPS CalDAV | User syncs calendar |
| `sync/freedom-sync-android` | HTTPS WebDAV / S3 | User runs backup |

All HTTP uses `PrivacyHttpClient` (`core/network`) which:

- Disables system HTTP proxy (`Proxy.NO_PROXY`)
- Blocks known analytics/telemetry host suffixes at runtime

Socket-based IMAP/SMTP already use `Proxy.NO_PROXY`.

## Permissions (merged manifest)

| Permission | Source | Needed? |
|------------|--------|---------|
| `INTERNET` | App manifest | Yes — mail & sync |
| `USE_BIOMETRIC` | App lock | Yes |
| `CAMERA` | Freedom Auth QR scan | Yes |
| `ACCESS_NETWORK_STATE` | Inbox only | Connectivity check |
| `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` | ~~WorkManager~~ | **Removed** — WorkManager dependency dropped |

## Release build guarantees

- `PRIVACY_STRICT=true` — no Timber, silent crash exit
- ProGuard strips `android.util.Log` and Timber
- `android:allowBackup="false"` on all apps
- `network_security_config` — cleartext disabled
- `FLAG_SECURE` on prod UI

## Enforcement

```bash
./gradlew privacyAudit
```

Runs:

1. `DependencyAuditTest` — banned SDK patterns in Gradle files
2. `StorageAuditTest` — unencrypted local storage
3. `:apps:inbox:dependencies` tree scan for forbidden Maven coordinates

## Adding a dependency

1. Check [F-Droid anti-features](https://f-droid.org/docs/Antifeatures/) — no tracking libs
2. Run `./gradlew privacyAudit`
3. Document the library in this file
4. If it adds manifest components, add `tools:node="remove"` in app manifests for any auto-initializer that could phone home

## Known acceptable Google artifacts

| Artifact | Why it's OK |
|----------|-------------|
| `com.google.devtools.ksp` | **Build-time only** — not in APK |
| `com.google.crypto.tink` | Local crypto primitive for Android Keystore wrapper |
| ZXing package `com.google.zxing` | Offline barcode decode — no network in manifest |

These are **not** Google Play Services and do not contact Google at runtime.
