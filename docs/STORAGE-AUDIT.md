# Local Storage Audit — Freedom Suite

All user data at rest must be encrypted. This document records the audit and enforcement rules.

## Enforcement

```bash
./gradlew storageAudit
```

Runs static checks that fail CI if production code introduces:

- Plain `SharedPreferences` or `DataStore`
- Room databases without SQLCipher
- `File.writeBytes()` outside approved wrappers

## Approved storage APIs

| Layer | API | Module |
|-------|-----|--------|
| Key-value secrets | `SecurePreferences` | `core/crypto` |
| SQL databases | `EncryptedRoom.build()` | `core/storage` |
| Files | `EncryptedFileStore` | `core/storage` |
| Sync backup blobs | `EncryptedFileStore.writeCiphertext()` | `core/storage` |

## Audit results (2026-06)

### Databases — encrypted (SQLCipher)

| App | Database | Key prefs file |
|-----|----------|----------------|
| Auth | `freedom_auth.db` | `freedom_auth_keys` |
| Messages | `freedom_messages.db` | `freedom_messages_keys` |
| Inbox | `freedom_inbox.db` | `freedom_inbox_keys` |
| Calendar | `freedom_calendar.db` | `freedom_calendar_keys` |
| Files | `freedom_files.db` | `freedom_files_keys` |
| Chat | — | No persistence yet |

Passphrases are 256-bit random values in Keystore-backed `SecurePreferences`.

### Preferences — encrypted

All prefs use `SecurePreferences` → `EncryptedSharedPreferences` + Android Keystore `MasterKey`.

| Prefs file | Contents |
|------------|----------|
| `freedom_mail_account` | Mail account + password |
| `freedom_applock` | PIN hash (PBKDF2), salt, settings |
| `freedom_auth_sync` / `freedom_messages_sync` | Sync backend credentials |
| `freedom_sync.{namespace}` | Device sync keys |

### Files — encrypted

| Path | Encryption |
|------|------------|
| `files/messages_attachments/*.bin` | AES-256-GCM via `EncryptedFileStore` (separate attachment DEK) |
| `files/files_content/*.bin` | AES-256-GCM via `EncryptedFileStore` (`files_content_dek`) |
| `files/freedom-sync/{ns}/*.bin` | Pre-encrypted backup ciphertext |

Blob format includes a crypto version byte per `CryptoAlgorithms.VERSION` (legacy blobs without version byte are still readable).

### Android backup

All apps set `android:allowBackup="false"`.

## Key separation

| Key | Purpose |
|-----|---------|
| `messages_db_passphrase` | SQLCipher database |
| `messages_attachment_dek` | Photo attachment files (separate from DB key) |
| `freedom_sync.*.sync_key` | Device-bound sync encryption |
| User backup passphrase | PBKDF2-derived key for portable auth backups |

## Remaining gaps

| Item | Status | Notes |
|------|--------|-------|
| Messages sync passphrase | Open | Uses device key only; auth has user passphrase |
| Messages backup attachments | Open | Snapshot stores paths, not attachment bytes |
| Backup KDF | Partial | PBKDF2 implemented; docs target Argon2id |
| Chat persistence | N/A | Not implemented yet — must use `EncryptedRoom` |

## Adding new storage

1. Use an approved API from the table above.
2. Run `./gradlew storageAudit` before merging.
3. Update this document if a new storage location is introduced.
