# Third-Party Dependency Audit — Freedom Suite

**Last audited:** 2026-06-08 (full re-audit). Run `./gradlew privacyAudit` before every release.

## Freedom principle

**Freedom means freedom from the machine** — no private-company telemetry, no spy SDKs, no silent exfiltration. Every third-party library must justify its presence; network egress is centralized and host-blocked.

## Verdict: PASS (with documented acceptances)

| Check | Result |
|-------|--------|
| Firebase / Play Services / GMS | **None** in prod dependency trees (all 7 apps) |
| Analytics / crash / attribution SDKs | **None** declared or imported |
| Offline apps without `INTERNET` | **Keyboard, Search** |
| HTTP egress gateway | **All OkHttp via `PrivacyHttpClient`** |
| Encrypted local storage | **Enforced** (`StorageAuditTest`) |
| Startup font/profile downloaders | **Removed** (emoji2, profileinstaller, WorkManager) |
| Release log stripping | **R8 + `PRIVACY_STRICT`** |

## Policy (non-negotiable)

- **No** Google Play Services, Firebase, ML Kit, or Play Integrity
- **No** crash reporting (Sentry, Crashlytics, ACRA, Bugsnag, Datadog, Instabug)
- **No** analytics or attribution (Mixpanel, Amplitude, Adjust, AppsFlyer, Facebook SDK, Segment, PostHog, Countly)
- **No** ads / billing / Huawei HMS SDKs
- **No** WorkManager (background workers that could pull in network stack)
- **Network only** to servers the user configures (mail, CalDAV, user-chosen sync storage)
- **No** `INTERNET` on keyboard or unified search hub

## Direct dependencies (production APK)

| Library | Module | Purpose | Network at runtime? | Telemetry? |
|---------|--------|---------|---------------------|------------|
| AndroidX (Compose, Room, Lifecycle, Biometric, Fragment, Navigation) | All apps | UI, DB, lifecycle | No* | No |
| `androidx.security:security-crypto` | core/crypto | EncryptedSharedPreferences (Tink) | No | No |
| SQLCipher `net.zetetic:sqlcipher-android` | core/storage | Encrypted SQLite | No | No |
| OkHttp `com.squareup.okhttp3` | core/network, caldav, sync | User-configured HTTP only | **Yes — gated** | No |
| ZXing embedded `com.journeyapps:zxing-android-embedded` | apps/auth | Offline QR scan | No | No |
| ONNX Runtime `com.microsoft.onnxruntime` | core/ml | On-device vision/OCR | No | No |
| Kotlin stdlib / coroutines | All | Language runtime | No | No |
| Timber `com.jakewharton:timber` | **dev flavor only** | Redacted dev logs | No | No |

\* `emoji2`, `profileinstaller`, and `WorkManager` auto-initializers are **stripped** from every app manifest (`tools:node="remove"`). Without this, emoji2 can download fonts from Google CDN.

## Transitive dependencies (reviewed)

| Transitive | Pulled by | Risk | Verdict |
|------------|-----------|------|---------|
| `com.google.crypto.tink:tink-android` | security-crypto | Local AEAD for Keystore wrapper | **Accept** — no network |
| `com.google.code.gson:gson` | Tink | JSON parse offline | **Accept** |
| `com.google.guava:listenablefuture` | AndroidX concurrent | Stub interface | **Accept** |
| `androidx.lifecycle.ProcessLifecycleInitializer` | AndroidX startup | In-process lifecycle | **Accept** — no network |
| ONNX weights (`*.onnx` in assets) | Freedom Files, Chat | Bundled at build; ~18 MB (SFace INT8 + YOLO + OCR) — see [ML-MODELS.md](ML-MODELS.md) | **Accept** — no runtime download |
| `com.google.zxing:core` (package namespace) | ZXing | Offline decode | **Accept** — not Play Services |

**Confirmed absent** from `prodReleaseRuntimeClasspath` on all shippable apps: `firebase`, `gms`, `play-services`, `sentry`, `mlkit`, `facebook`, `work-runtime`.

## Network architecture

All HTTP **must** use `PrivacyHttpClient.create()`:

```kotlin
// protocol/caldav, sync/freedom-sync-android — defaults to PrivacyHttpClient
```

Properties:

- `Proxy.NO_PROXY` — no system proxy hijack
- **Host blocklist** for known telemetry domains (Google Analytics, Firebase, Sentry, Mixpanel, Facebook, Adjust, etc.)
- TLS only via `network_security_config` (`cleartextTrafficPermitted="false"`)

Socket protocols (IMAP/SMTP) use `Proxy.NO_PROXY` in `ImapConnection` / `JavaSmtpClient`.

### Call sites (our code only)

| Module | Protocol | When |
|--------|----------|------|
| `protocol/imap` | TLS IMAP | User syncs mail |
| `protocol/smtp` | TLS SMTP | User sends mail |
| `protocol/caldav` | HTTPS CalDAV | User syncs calendar |
| `sync/freedom-sync-android` | HTTPS WebDAV / S3 | User runs backup |

No Freedom-operated backend exists.

## Permissions by app (source manifest)

| App | INTERNET | Other sensitive | Notes |
|-----|----------|-----------------|-------|
| Inbox | Yes | — | Mail only |
| Calendar | Yes | — | CalDAV only |
| Messages | Yes | Biometric | Sync optional |
| Auth | Yes | Camera, Biometric | Camera = QR only; INTERNET = sync backup |
| Files | Yes | Biometric | Sync + ML is on-device |
| Keyboard | **No** | RECORD_AUDIO, VIBRATE | Audio for local dictation (planned); no network |
| Search | **No** | — | Queries signed local providers only |
| Chat (placeholder) | **No** | — | Not distributed |

Removed in this audit: unused `ACCESS_NETWORK_STATE` from Inbox; premature `INTERNET` from Chat placeholder.

## Exported components

| Component | Protection |
|-----------|------------|
| Search ContentProviders | `org.freedomsuite.permission.SEARCH` (signature) |
| Calendar event bridge | `org.freedomsuite.permission.CALENDAR_BRIDGE` (signature) |
| Keyboard IME | `BIND_INPUT_METHOD` |
| Speech recognition | `BIND_SPEECH_RECOGNITION_SERVICE` |

## Release build guarantees

- `PRIVACY_STRICT=true` on `prod` flavor
- No Timber on prod classpath (`devImplementation` only)
- `FreedomUncaughtExceptionHandler` — silent exit, memory wipe; **no crash upload**
- ProGuard strips `android.util.Log` and Timber
- `FLAG_SECURE` on prod UI (screenshot deterrence)
- `android:allowBackup="false"` on all apps

## Automated enforcement

```bash
./gradlew privacyAudit
```

Runs JVM tests in `:testing:mock-server`:

| Test | What it catches |
|------|-----------------|
| `DependencyAuditTest` | Banned Maven coords + analytics imports in source |
| `StorageAuditTest` | Plain SharedPreferences, unencrypted Room/files |
| `ManifestPrivacyAuditTest` | Forbidden permissions, offline apps with INTERNET, missing network security config |
| `NetworkEgressAuditTest` | OkHttp built outside `PrivacyHttpClient`, missing host blocklist |

Also builds all prod release APKs (validates merged manifests compile).

## Acceptable Google artifacts (not spyware)

| Artifact | Why OK |
|----------|--------|
| `com.google.devtools.ksp` | Build-time only — not in APK |
| `com.google.crypto.tink` | Offline crypto primitive |
| `com.google.zxing` | Offline barcode math — no network permission added |
| Package `com.google.*` in ProGuard `-dontwarn` | Strips accidental telemetry if ever linked |

These are **not** Google Play Services and do not contact Google at runtime.

## Adding a dependency

1. Read [F-Droid anti-features](https://f-droid.org/docs/Antifeatures/)
2. Confirm: no network unless user-initiated; no analytics; open source preferred
3. Run `./gradlew privacyAudit`
4. Document here with network/telemetry columns
5. If manifest adds auto-initializers → `tools:node="remove"` in app + `core/privacy`

## Residual risks (monitored)

| Risk | Mitigation |
|------|------------|
| Future AndroidX adds network initializer | `core/privacy` manifest merge + audit |
| User points sync at hostile server | Expected — user choice; payload encrypted |
| ONNX / Sherpa models from CDN at **build** time | Build scripts only; models bundled in APK, not downloaded at runtime |
| Keyboard `RECORD_AUDIO` | Local dictation only; no INTERNET permission |
| Microsoft ONNX Runtime binary | Runs inference locally; no outbound calls in our integration |

## Microsoft ONNX Runtime note

`onnxruntime-android` is a native inference engine. We load models from **assets** only. Microsoft's desktop telemetry does not apply to our Android embedding; we do not call ORT training/telemetry APIs. If upstream ever adds network code, pin version and re-audit.
