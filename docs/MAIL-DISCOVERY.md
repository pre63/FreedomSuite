# Mail server discovery

Inbox no longer assumes mailbox.org for every address. Setup discovers IMAP/SMTP endpoints from the email domain, tries each candidate until one connects, and falls back to manual entry.

## Discovery order

1. **Presets** — known providers (mailbox.org, Gmail, Outlook, Fastmail, Yahoo, iCloud, Zoho, Migadu).
2. **Mozilla autoconfig** — HTTPS `config-v1.1.xml` at `autoconfig.<domain>` or `/.well-known/autoconfig/mail/`.
3. **DNS heuristics** — common host patterns (`imap.<domain>`, `mail.<domain>`, etc.), preferring names that resolve.

Candidates are tried in order; the first successful IMAP login is saved.

## Manual fallback

If discovery or connection fails, the setup screen offers **Enter server settings manually** with IMAP/SMTP host and port fields.

## Privacy

Autoconfig uses direct HTTPS to the mail domain only. Thunderbird ISPDB (`autoconfig.thunderbird.net`) is not used, to avoid leaking the domain to a third party.

## Local dev testing

For emulator/device testing without a real mailbox, run the dev IMAP/SMTP server and use `test@freedom.test` — see [DEV-MAIL-SERVER.md](DEV-MAIL-SERVER.md).

## Code

- `core/account/.../discovery/` — discovery types and orchestration
- `apps/inbox/.../InboxRepository.configureAccount()` — probe and persist
- `core/ui/AccountSetupScreen.kt` — connect + manual fields
