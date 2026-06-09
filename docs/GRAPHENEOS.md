# GrapheneOS Compatibility Audit

**Target OS:** [GrapheneOS](https://grapheneos.org/) on supported Pixel hardware (arm64).

**Verdict:** Freedom Suite is **compatible with GrapheneOS as the primary target platform**. There are no hard blockers (no Google Play Services, no SafetyNet, no proprietary push/analytics). A few gaps should be closed before calling the suite “GrapheneOS-ready” for all features.

**Audit date:** 2026-06-07 · **Scope:** all `apps/*`, shared `core/*`, native ML/SQLCipher, manifests, privacy audits.

---

## Executive summary

| Area | Status | Notes |
|------|--------|-------|
| No GMS / Firebase / Play Integrity | ✅ Pass | Enforced by `DependencyAuditTest`, `privacyAudit` |
| F-Droid / sideload install | ✅ Pass | No Play Store dependency |
| arm64 native libs (ONNX, SQLCipher) | ✅ Pass | `abiFilters`: `arm64-v8a`, `armeabi-v7a` |
| TLS-only networking | ✅ Pass | `network_security_config` — cleartext disabled |
| Encrypted local storage | ✅ Pass | SQLCipher + Keystore-backed prefs |
| Background execution model | ✅ Pass | Manual sync; no boot receivers / wake locks |
| Per-app network (GOS feature) | ⚠️ Verify UX | User must grant network per app in Settings |
| Suite cross-app bridges | ⚠️ Install discipline | Same release signing key required |
| Auth QR scanner (camera) | ⚠️ Verify | `CAMERA` declared; ZXing handles runtime prompt |
| Freedom Chat screen capture | ⚠️ Gap | Missing `FLAG_SECURE` on prod |
| Keyboard voice input | 🔜 Future | `RECORD_AUDIO` declared; ASR still stub |
| Real-device GOS smoke test | ❌ Not run | Recommended before 1.0 |

---

## Why GrapheneOS fits

GrapheneOS users typically want:

- Apps that work **without Google Play Services**
- **Minimal background tracking** and no vendor telemetry
- **Per-app network control**
- **Strong device encryption** and hardware-backed keys
- **F-Droid or direct APK** installs

Freedom Suite is built for exactly this profile: open source, F-Droid-oriented, no analytics, TLS-only, encrypted local data, and optional user-chosen servers (IMAP, WebDAV, S3).

---

## Platform baseline

From `FreedomApplicationConventionPlugin.kt`:

| Setting | Value | GrapheneOS impact |
|---------|-------|-------------------|
| `minSdk` | 26 | Supported on all GOS Pixel builds |
| `targetSdk` | 35 | Modern permission / storage behavior |
| `compileSdk` | 35 | Current AOSP APIs |
| ABIs | `arm64-v8a`, `armeabi-v7a` | GOS devices are **arm64**; x86 emulator APKs are intentionally excluded |
| `allowBackup` | `false` (all apps) | No cloud/adb backup of app data by default |
| Release logging | Stripped (`PRIVACY_STRICT`) | No logcat leakage |

---

## Per-app compatibility

| App | Network on GOS | Sensitive permissions | GOS notes |
|-----|----------------|----------------------|-----------|
| **Inbox** | Grant in Settings → Network | `INTERNET` | Manual folder sync (pull/refresh) — survives battery restrictions |
| **Calendar** | Grant if using Freedom Sync backup | `INTERNET` | Local-first; no CalDAV wire traffic |
| **Messages** | Grant for sync backend | `INTERNET`, `USE_BIOMETRIC` | No SMS/telephony permissions |
| **Auth** | Grant for encrypted backup export | `INTERNET`, `USE_BIOMETRIC`, `CAMERA` | QR via ZXing `ScanContract` |
| **Files** | Grant for Freedom Sync | `INTERNET`, `USE_BIOMETRIC` | Photos via SAF import — no `READ_MEDIA_*` |
| **Keyboard** | **Offline** (no `INTERNET`) | `RECORD_AUDIO`, `VIBRATE` | Enable IME + optional speech provider in System settings |
| **Search** | **Offline** | Signature `SEARCH` only | Fan-out to other signed Freedom apps |
| **Chat** (non–F-Droid) | Grant if using cloud LLM | `INTERNET`, `RECORD_AUDIO` | Local vision via bundled ONNX |

---

## GrapheneOS-specific behaviors

### 1. Per-app network permission

GrapheneOS lets users **deny network access per app** (Settings → Network → App connectivity). Apps that declare `INTERNET` still need the user to allow connectivity.

**Affected:** Inbox, Calendar, Messages, Auth (backup), Files, Chat.

**Recommendation:** Surface clear errors when sync/IMAP fails due to connectivity (distinguish “network denied” vs “server error” where possible). Keyboard and Search work fully offline.

### 2. Suite must share one signing key

Cross-app features use **signature-level** permissions:

- `org.freedomsuite.permission.SEARCH` — unified search providers
- `org.freedomsuite.permission.CALENDAR_BRIDGE` — Inbox → Calendar invites

All participating APKs must be signed with the **same release key**. On GrapheneOS, install every app from the **same channel** (F-Droid official build, or the same GitHub Release batch). Mixing a debug build with a release build will break search and calendar bridge silently.

### 3. Third-party keyboard and speech

Freedom Keyboard is a standard `InputMethodService`:

1. Settings → System → Keyboard → On-screen keyboard → enable **Freedom Keyboard**
2. Switch IME in any text field

`FreedomSpeechRecognitionService` is a `RecognitionService` stub today. When on-device ASR ships, users will also choose it under voice-input settings. No Google speech services are required.

### 4. Biometrics and app lock

`BiometricHelper` uses `BIOMETRIC_STRONG` only (no weak/class-2 biometrics). GrapheneOS exposes hardware-backed Keystore the same way as stock Android for Pixel devices. Device PIN fallback is offered via the negative button.

### 5. Storage and scoped access

Files app imports via **Storage Access Framework** (`OpenDocument`, `GetContent`). Data lives in app-private encrypted storage. No broad storage or media permissions — aligned with GOS storage isolation.

### 6. Native ML (Files, Chat)

~18 MB ONNX models ship in APK assets (`core/ml`). Inference uses ONNX Runtime native libs on **arm64-v8a**. This is standard JNI on AOSP; no GMS ML Kit. First-run indexing is CPU-bound and on-device only.

### 7. Optional Sandboxed Google Play

If the user installs Sandboxed Google Play, Freedom Suite **does not call into it**. Compatibility is unchanged.

### 8. Private DNS and user CAs

`network_security_config` trusts **system CAs only** (`cleartextTrafficPermitted="false"`). This is correct for mailbox.org and public providers. Self-hosted mail/sync with a **private TLS CA** will fail unless the user installs the CA as a system trust anchor (GOS documents this workflow). Certificate pinning is not implemented yet (see [THREAT-MODEL.md](THREAT-MODEL.md)).

---

## What we deliberately avoid (GrapheneOS-friendly)

Verified by `ManifestPrivacyAuditTest` and `DependencyAuditTest`:

| Forbidden on GOS / privacy grounds | Present? |
|-----------------------------------|----------|
| Google Play Services / Firebase | ❌ No |
| SafetyNet / Play Integrity | ❌ No |
| `RECEIVE_BOOT_COMPLETED` | ❌ No |
| `WAKE_LOCK` | ❌ No |
| Location / phone state / ad ID | ❌ No |
| `GET_ACCOUNTS` | ❌ No |
| WorkManager / ProfileInstaller / EmojiCompat auto-init | Removed in manifests |
| Cleartext HTTP | ❌ No |

Inbox sync is **user-initiated** (no periodic background worker), which matches GrapheneOS battery and execution limits without needing foreground services or exemptions.

---

## Gaps and recommendations

### P1 — before claiming full GOS readiness

| Issue | Impact | Action |
|-------|--------|--------|
| **Chat missing `FLAG_SECURE`** | Screen capture / recents thumbnail on sensitive UI | Match other apps’ `MainActivity` pattern |
| **No on-device GOS smoke test** | Unknown regressions on real GOS build | Run checklist below on Pixel + GOS |
| **Per-app network denial UX** | User thinks app is broken | Improve error copy when offline/denied |

### P2 — polish

| Issue | Impact | Action |
|-------|--------|--------|
| **Keyboard `RECORD_AUDIO`** | Needed when Sherpa-ONNX voice ships | Add runtime permission flow before enabling mic |
| **Chat `RECORD_AUDIO`** | Same for future voice messages | Defer or request at use-time |
| **README still says “CalDAV”** | User expectation on GOS | Align docs with local-first calendar ([CALENDAR-PROTOCOL.md](CALENDAR-PROTOCOL.md)) |
| **Unified search deep links** | Search opens hits but targets ignore intents | Finish cross-app navigation |

### P3 — optional

| Issue | Notes |
|-------|-------|
| x86 APKs excluded | Cannot install on x86 AVD without changing `abiFilters`; use arm64 hardware or Cuttlefish |
| Certificate pinning | Threat model mentions it; not implemented |
| GrapheneOS “exploit protection” | Standard native libs; no known incompatibility |

---

## Installation guide (GrapheneOS)

1. **Preferred:** Install from F-Droid (when listed) or [GitHub Releases](GITHUB-RELEASES.md) / Obtainium — same signing key for all apps.
2. **Network:** For each online app, Settings → Network → App connectivity → allow **Freedom Inbox**, **Files**, etc.
3. **Keyboard:** Enable Freedom Keyboard in system keyboard settings.
4. **Search:** Install Search plus at least one provider app (Inbox, Files, Calendar, Messages) from the **same release**.
5. **Biometric lock:** Optional per-app lock uses device biometrics/PIN already configured on GOS.

---

## Smoke-test checklist (GrapheneOS device)

Run on a supported Pixel with current GrapheneOS:

- [ ] Install full suite from one release channel
- [ ] Grant per-app network; Inbox IMAP sync succeeds
- [ ] Calendar invite from Inbox → event appears (bridge)
- [ ] Freedom Search returns hits from Inbox + Files
- [ ] Files: import photo via SAF, run “Index photos”, local search works
- [ ] Auth: scan TOTP QR, biometric unlock
- [ ] Keyboard: enable IME, type with local predictions
- [ ] Deny network for Inbox → confirm understandable error
- [ ] Confirm no unexpected outbound connections (GOS network monitor or mock-server audits locally)

---

## CI alignment

Existing audits support GrapheneOS goals; no GOS-specific Gradle task is required:

```bash
./gradlew privacyAudit          # manifests + dependency blocklist
./gradlew integrationTest       # includes manifest + network egress audits
./gradlew fdroidVerify          # reproducible F-Droid builds
./gradlew mlIntegrationTest     # ONNX models on arm64-class CI host
```

---

## Conclusion

**Freedom Suite should target GrapheneOS.** The architecture (no GMS, encrypted local-first, manual sync, F-Droid distribution, minimal permissions) matches how GrapheneOS is meant to be used. No code changes are required for basic compatibility today; close the P1 gaps (especially Chat `FLAG_SECURE` and a real-device smoke test) before marketing the suite as “GrapheneOS-ready.”
