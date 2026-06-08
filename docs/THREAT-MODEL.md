# Threat Model — Freedom Suite

## In scope

| Threat | Mitigation |
|--------|------------|
| App vendor tracking | No analytics; open source; F-Droid builds from source; see [THIRD-PARTY-AUDIT.md](THIRD-PARTY-AUDIT.md) |
| Mail provider reads mail | Expected — use mailbox.org + optional OpenPGP |
| Stolen phone (locked) | Keystore + app lock |
| Stolen phone (unlocked) | Local data exposed — same as any app |
| Network interception | TLS 1.2+; optional certificate pinning |
| Stolen S3/WebDAV bucket | Client-side E2EE — ciphertext only |
| Stolen encrypted DB file | Useless without passphrase |
| Plaintext local cache | All Room DBs use SQLCipher; prefs use EncryptedSharedPreferences; files use AES-GCM (see [STORAGE-AUDIT.md](STORAGE-AUDIT.md)) |

## Out of scope / honest limits

- **Nation-state adversary** with device exploit while unlocked
- **Compelled disclosure** of passphrase by user
- **mailbox.org** as mail/calendar provider — you trust them for mail contents
- **LLM API providers** (Chat app) — prompts leave device if you use cloud APIs; local Ollama avoids this

## Release build guarantees

- No logcat in production
- No crash telemetry
- No third-party SDK network calls

## Messaging (future)

Protocol choice (Session / SimpleX / Matrix) will define metadata protection properties. See [MESSAGING.md](MESSAGING.md).
