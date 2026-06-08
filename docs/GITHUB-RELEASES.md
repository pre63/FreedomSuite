# GitHub Releases (production APKs)

Download production APKs from **GitHub → Releases**. No F-Droid builder, no manual APK assembly.

## First time only

```bash
gh auth login
./scripts/github-setup.sh
```

Creates a **public** repo on your GitHub account (`YOU/FreedomSuite`) and pushes `main`.

## Publish (one command)

```bash
./scripts/publish.sh
```

Builds all apps on GitHub Actions and uploads APKs to a new Release:

- `org.freedomsuite.inbox-1.0.0.apk`
- `org.freedomsuite.files-1.0.0.apk`
- … plus `manifest.json`

Single app:

```bash
./scripts/publish.sh inbox
```

Tag will be `inbox-v1.0.0` (read from `apps/inbox/build.gradle.kts`).

## Build locally and upload (no cloud wait)

```bash
./scripts/publish.sh --local
./scripts/publish.sh --local files
```

Runs Gradle on your machine, then `gh release create` / `gh release upload`.

## What runs in the cloud

Workflow: `.github/workflows/publish.yml`

1. Privacy audit + integration tests
2. `assembleProdRelease` (prod flavor, `PRIVACY_STRICT`)
3. Rename APKs → publish GitHub Release

Triggered by:

- `./scripts/publish.sh` pushing a tag
- Manual: GitHub → Actions → Publish → Run workflow

## CI on every push

`.github/workflows/ci.yml` runs `fdroidVerify` on `main` (same quality gate, artifacts for 14 days).

## Updating versions

1. Bump `versionCode` / `versionName` in `apps/<app>/build.gradle.kts`
2. `./scripts/publish.sh` or `./scripts/publish.sh <app>`

## Install on device

```bash
adb install org.freedomsuite.inbox-1.0.0.apk
```

APKs are unsigned (debug-style CI signing). Enable “Install unknown apps” or use `adb install`. For Play-style signing, add a keystore secret later — not required for sideloading from your own Releases.
