# Cryptography — Freedom Suite

## Design goals

1. Stolen remote storage → useless ciphertext
2. Stolen local database → useless without passphrase + device keys
3. Post-quantum safe key establishment between devices
4. Crypto-agility — version byte on every blob for future algorithm rotation

## Algorithm profile (v1)

| Purpose | Algorithm | NIST reference |
|---------|-----------|----------------|
| Bulk encryption (DB, files, blobs) | AES-256-GCM | FIPS 197, SP 800-38D |
| Alternative symmetric | XChaCha20-Poly1305 | RFC 8439 |
| Passphrase → key | Argon2id + HKDF-SHA3-512 | RFC 9106 |
| Classical key exchange | X25519 | RFC 7748 |
| Post-quantum KEM | ML-KEM-768 | FIPS 203 |
| Hybrid device pairing | X25519 + ML-KEM-768 | Hybrid per NIST IR 8547 |
| Signatures | Ed25519 | RFC 8032 |
| Android key wrap | AES-256-GCM (Keystore) | — |

## Post-quantum notes

- **AES-256 for data at rest is already quantum-resistant** (NIST Security Category 5). Grover's algorithm does not make AES-256 trivial to break.
- **Public-key algorithms** (X25519, RSA) need hybrid ML-KEM for long-term security — used in Freedom Sync device pairing.
- ML-KEM-1024 may be used for Messages master key wrap (maximum margin).

## Key hierarchy

```
Passphrase ──Argon2id──► Master Secret Key (MSK)
                              │
                              ├── HKDF ──► Data Encryption Key (DEK)
                              ├── HKDF ──► Manifest Key
                              └── Wrapped in Android Keystore (biometric/PIN gate)
```

## Implementation

- **Rust:** `sync/freedom-sync-core/` — crypto primitives, blob format, manifest
- **Android:** `core/crypto/` — Keystore integration, SecurePreferences, FileEncryption
- **Storage:** `core/storage/` — SQLCipher for all Room databases, EncryptedFileStore
- **Audit:** `./gradlew storageAudit` — static enforcement (see [STORAGE-AUDIT.md](STORAGE-AUDIT.md))

## What we do not do

- Custom ciphers
- Proprietary crypto SDKs
- Keys on remote servers (zero-knowledge sync)
