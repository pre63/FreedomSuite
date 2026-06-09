# Dev mail server (IMAP/SMTP)

A local plain-text mail server for testing **Inbox** without a real provider. It ships with sample messages and accepts any login.

## Start the server

```bash
make dev-mail-server
# or
./gradlew :testing:mock-server:run
```

Leave this terminal open while testing. Press **Ctrl+C** to stop.

If you see **Address already in use**, a previous instance is still running:

```bash
lsof -i :1143          # find the process
kill $(lsof -ti :1143) # stop it
```

Default ports (all interfaces, no TLS):

| Service | Port |
|---------|------|
| IMAP    | 1143 |
| SMTP    | 1025 |

## Credentials

| Field    | Value              |
|----------|--------------------|
| Email    | `test@freedom.test` |
| Password | `dev-password`     |

The mock server accepts any username/password, but use the values above so discovery and docs stay consistent.

## Connect from Inbox (emulator)

1. Start the dev server on your computer (`make dev-mail-server`).
2. Start the emulator (`make sim` or `make emulator-start`).
3. Install Inbox dev build (`make install-app APP=inbox`).
4. On the Inbox setup screen:
   - **Email:** `test@freedom.test`
   - **Password:** `dev-password`
   - Tap **Connect** — the `@freedom.test` preset points at `10.0.2.2` (host loopback from the emulator).

You should see two sample messages in INBOX. Compose sends mail through the mock SMTP server (captured locally, not delivered).

## Connect from Inbox (physical device)

The emulator shortcut `10.0.2.2` does not work on a real phone. Use manual server settings:

1. Find your computer's LAN IP (e.g. `192.168.1.42` on macOS: **System Settings → Network**).
2. Ensure phone and computer are on the same Wi‑Fi.
3. Start `make dev-mail-server`.
4. In Inbox setup:
   - **Email:** `test@freedom.test`
   - **Password:** `dev-password`
   - Tap **Enter server settings manually**
   - **IMAP server:** your computer's IP (e.g. `192.168.1.42`)
   - **IMAP port:** `1143`
   - **SMTP server:** same IP
   - **SMTP port:** `1025`
   - Tap **Connect**

Ports `1143` / `1025` enable plain-text mode automatically (no TLS).

## Sample data

| Folder  | Messages |
|---------|----------|
| INBOX   | "Welcome to FreedomSuite", "Second message" |
| Sent    | "Sent item" |
| Archive | Messages moved via swipe-to-archive |

## Integration tests

The same mock implementation powers JVM integration tests (`:testing:integration`) with ephemeral ports and `plainText = true`. The dev server uses fixed ports so you can point the app at it reliably.

## See also

- [MAIL-DISCOVERY.md](MAIL-DISCOVERY.md) — production mail setup
- `testing/mock-server/` — server implementation
