# Integration testing

Freedom Suite integration tests use a **local mock mailbox.org** — no real network calls.

## Modules

| Module | Role |
|--------|------|
| `testing/mock-server` | JVM mock IMAP, SMTP, CalDAV/WebDAV, and S3-compatible HTTP |
| `testing/integration` | Robolectric integration tests for protocols, TOTP, and Freedom Sync |

## Run

```bash
./scripts/ci-verify.sh      # full F-Droid gate (recommended)
./gradlew fdroidVerify      # same via Gradle
./gradlew integrationTest   # protocol + sync integration tests only
./gradlew storageAudit      # static check: no unencrypted local storage
./gradlew privacyAudit      # third-party deps, analytics SDKs, network policy
```

This runs:

1. **Privacy audit tests** (`:testing:mock-server:test`) — banned SDKs, manifest permissions, network egress, encrypted storage
2. **JVM protocol smoke tests** — raw TCP/HTTP against mocks
3. **Robolectric client tests** (`:testing:integration:testDevDebugUnitTest`) — IMAP, SMTP, CalDAV, Freedom Sync, TOTP, and end-to-end mailbox.org flow

## What is mocked

| Service | Mock | Port |
|---------|------|------|
| IMAP | `MockImapServer` | ephemeral TCP |
| SMTP | `MockSmtpServer` | ephemeral TCP |
| CalDAV | `MockHttpServer` | ephemeral HTTP |
| WebDAV sync | `MockHttpServer` | same HTTP server |
| S3-compatible sync | `MockHttpServer` | `/{bucket}/freedom-sync/...` |

All mocks bind to `127.0.0.1`. Protocol clients use `plainText = true` in tests (production always uses TLS).

## Adding tests

Extend `IntegrationTestBase` to get a running `MailboxOrgMockServer` fixture:

```kotlin
class MyIntegrationTest : IntegrationTestBase() {
    @Test
    fun example() = runBlocking {
        val account = testMailAccount()
        // ...
    }
}
```
