# Freedom Auth — TOTP authenticator

Freedom Auth stores time-based one-time passwords (TOTP, RFC 6238) locally in an encrypted SQLCipher database.

## Features

- Scan QR codes or import `otpauth://` URIs
- Manual secret entry (Base32)
- Live codes with countdown timer
- Tap to copy, long-press to delete
- PIN / biometric app lock (shared `core/applock`)

## Encrypted backup (Authy-style)

Unlike cloud-hosted authenticators, Freedom Auth backs up **only ciphertext** to a destination you control:

| Backend | Description |
|---------|-------------|
| **Local** | Encrypted file on device |
| **mailbox.org WebDAV** | Your dav.mailbox.org storage |
| **S3-compatible** | Any S3 API endpoint |

You choose a **backup passphrase** (min 8 characters). The passphrase derives the encryption key via PBKDF2 — the same passphrase restores accounts on a new device. Freedom Suite never sees your secrets or passphrase.

## Security notes

- `FLAG_SECURE` in production builds (no screenshots)
- No analytics, no crash reporting
- Secrets never logged
