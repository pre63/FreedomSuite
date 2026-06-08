#!/usr/bin/env bash
# Collect prod release APKs with install-friendly names.
# Usage: ./scripts/collect-release-apks.sh [out-dir] [app ...]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=scripts/lib/apps.sh
source "$ROOT/scripts/lib/apps.sh"

OUT="${1:-$ROOT/release-apks}"
shift || true

mkdir -p "$OUT"
rm -f "$OUT"/*.apk

if [[ $# -gt 0 ]]; then
  APPS=("$@")
else
  APPS=("${FDROID_APPS[@]}")
fi

MANIFEST="$OUT/manifest.json"
entries=()

for app in "${APPS[@]}"; do
  if ! is_valid_app "$app"; then
    echo "Unknown app: $app" >&2
    exit 1
  fi
  pkg="$(app_package_id "$app")"
  version="$(app_version_name "$app")"
  code="$(app_version_code "$app")"
  src="$ROOT/apps/$app/build/outputs/apk/prod/release"
  apk="$(find "$src" -maxdepth 1 -name '*.apk' -print -quit)"
  if [[ -z "$apk" ]]; then
    echo "Missing APK for $app — run assembleProdRelease first" >&2
    exit 1
  fi
  dest="$OUT/${pkg}-${version}.apk"
  cp "$apk" "$dest"
  echo "→ $(basename "$dest") ($(du -h "$dest" | cut -f1))"
  entries+=("{\"app\":\"$app\",\"package\":\"$pkg\",\"version\":\"$version\",\"versionCode\":$code,\"file\":\"$(basename "$dest")\"}")
done

{
  echo "["
  for i in "${!entries[@]}"; do
    if [[ $i -gt 0 ]]; then echo ","; fi
    echo -n "  ${entries[$i]}"
  done
  echo ""
  echo "]"
} > "$MANIFEST"
echo "Manifest: $MANIFEST"
