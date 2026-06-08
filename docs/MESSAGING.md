# Messaging — Freedom Messages

Private messaging lives in **Freedom Messages** (`org.freedomsuite.messages`) as encrypted **channels**.

## Channels

| Type | Description |
|------|-------------|
| **Personal** | Just you — broadcast secrets, notes, and photos to yourself (like a one-person Telegram channel) |
| **Group** | Multi-person channels — member invites and E2EE sync coming in a future release |

All messages are stored in an **encrypted SQLCipher database** on device. Freedom Sync replicates ciphertext blobs across your devices.

## App lock

Every Freedom Suite app requires **PIN or biometric** unlock on launch, with a configurable **grace period** (default 20 seconds) before re-locking when you switch away.

See `core/applock/` and Messages settings.
