# Privacy Policy — Freedom Suite

Freedom Suite applications are **parasite-free**: they do not track you, profile you, or report crashes to us or any third party.

## What we do NOT collect

- No analytics (no Firebase, no Matomo, no custom telemetry)
- No crash reporting (no Sentry, Crashlytics, ACRA, or similar)
- No advertising IDs
- No device fingerprinting
- No log upload in release builds

## Release builds (F-Droid)

- **Zero logcat output** — logging calls are stripped at compile time
- **No automatic crash reports** — fatal errors exit silently; no stack traces sent anywhere
- **`android:allowBackup="false"`** — Android cloud backup disabled by default
- **TLS only** — no cleartext network traffic

## Dev builds

Development builds (`dev` flavor) may write **redacted** logs to logcat on your device only. Email addresses and secrets are redacted. Dev builds are not distributed via F-Droid.

## Third-party libraries

No Firebase, Play Services, crash reporters, or analytics SDKs. See [THIRD-PARTY-AUDIT.md](THIRD-PARTY-AUDIT.md). Run `./gradlew privacyAudit` before release.

## Network traffic

Apps connect only to servers **you configure**:

| App | Default servers |
|-----|-----------------|
| Inbox | Your IMAP/SMTP provider (mailbox.org presets included) |
| Calendar | Your CalDAV server (mailbox.org presets included) |
| Messages / Sync | Optional: mailbox.org WebDAV, S3-compatible storage, or local-only |
| Keyboard | **No network** — predictions and dictionary stay on device |
| Search | **No network** — queries other Freedom apps via signed local bridge |
| Chat | Placeholder — no network permission until implemented |

We operate no central Freedom Suite server.

## Local data

All app data is stored **encrypted on your device** (SQLCipher, AES-256-GCM). See [CRYPTO.md](CRYPTO.md).

## Contact

Report privacy concerns via the project issue tracker (when published).
