#!/usr/bin/env bash
# Publish production APKs to GitHub Releases.
#
#   ./scripts/publish.sh              # all apps → GitHub builds & uploads APKs
#   ./scripts/publish.sh inbox        # one app
#   ./scripts/publish.sh --local      # build on this machine, upload with gh
#   ./scripts/publish.sh --local files
#
# First time: ./scripts/github-setup.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=scripts/lib/apps.sh
source "$ROOT/scripts/lib/apps.sh"

LOCAL=false
APP="all"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --local) LOCAL=true; shift ;;
    --help|-h)
      sed -n '2,10p' "$0"
      exit 0
      ;;
    all|inbox|calendar|messages|auth|files|keyboard|search)
      APP="$1"
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

require_gh() {
  if ! command -v gh >/dev/null 2>&1; then
    echo "Install GitHub CLI: https://cli.github.com/" >&2
    exit 1
  fi
  gh api user -q .login >/dev/null 2>&1 || {
    echo "Run: gh auth login" >&2
    exit 1
  }
}

require_git() {
  if ! git -C "$ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    echo "Not a git repo. Run: ./scripts/github-setup.sh" >&2
    exit 1
  fi
  if ! git -C "$ROOT" remote get-url origin >/dev/null 2>&1; then
    echo "No origin remote. Run: ./scripts/github-setup.sh" >&2
    exit 1
  fi
}

make_tag() {
  if [[ "$APP" == "all" ]]; then
    echo "release-$(date +%Y.%m.%d-%H%M)"
  else
    echo "${APP}-v$(app_version_name "$APP")"
  fi
}

build_local() {
  cd "$ROOT"
  ./scripts/fdroid-prebuild.sh
  if [[ "$APP" == "all" ]]; then
    ./gradlew --no-daemon assembleFdroidRelease
    ./scripts/collect-release-apks.sh
  else
    ./gradlew --no-daemon ":apps:${APP}:assembleProdRelease"
    ./scripts/collect-release-apks.sh "$ROOT/release-apks" "$APP"
  fi
}

publish_local() {
  local tag="$1"
  cd "$ROOT"
  build_local
  if gh release view "$tag" >/dev/null 2>&1; then
    echo "Release $tag exists — uploading APKs"
    gh release upload "$tag" release-apks/*.apk --clobber
  else
    gh release create "$tag" release-apks/*.apk \
      --title "Freedom Suite $tag" \
      --notes "Production APKs (prod flavor, PRIVACY_STRICT). See manifest.json in release-apks/."
  fi
  gh release upload "$tag" release-apks/manifest.json --clobber 2>/dev/null || true
  gh release view "$tag" --web
}

publish_remote() {
  local tag="$1"
  cd "$ROOT"
  if git rev-parse "$tag" >/dev/null 2>&1; then
    echo "Tag $tag already exists locally."
  else
    git tag -a "$tag" -m "Release $tag"
  fi
  echo "Pushing $tag → origin (GitHub Actions will build and publish APKs)…"
  git push origin "$tag"
  echo ""
  echo "Watching workflow…"
  sleep 3
  gh run watch "$(gh run list --workflow=publish.yml --limit 1 --json databaseId --jq '.[0].databaseId')"
  echo ""
  gh release view "$tag" --web 2>/dev/null || echo "Open Releases on GitHub when the workflow finishes."
}

main() {
  require_gh
  require_git
  local tag
  tag="$(make_tag)"
  echo "Publish target: $APP"
  echo "Release tag:    $tag"
  if $LOCAL; then
    publish_local "$tag"
  else
    # Ensure main is pushed so CI builds latest code
    current_branch="$(git -C "$ROOT" branch --show-current)"
    if [[ "$current_branch" == "main" || "$current_branch" == "master" ]]; then
      echo "Pushing $current_branch…"
      git -C "$ROOT" push origin "$current_branch" || true
    fi
    publish_remote "$tag"
  fi
}

main
