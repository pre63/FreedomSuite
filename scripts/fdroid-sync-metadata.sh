#!/usr/bin/env bash
# Copy Freedom Suite F-Droid metadata into an fdroiddata checkout.
# Usage: ./scripts/fdroid-sync-metadata.sh /path/to/fdroiddata
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 /path/to/fdroiddata" >&2
  exit 1
fi

FDROIDDATA="$(cd "$1" && pwd)"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/fdroid/metadata"

if [[ ! -d "$FDROIDDATA/metadata" ]]; then
  echo "Error: $FDROIDDATA does not look like an fdroiddata repo (missing metadata/)" >&2
  exit 1
fi

shopt -s nullglob
for file in "$SRC"/org.freedomsuite.*.yml; do
  base="$(basename "$file")"
  cp "$file" "$FDROIDDATA/metadata/$base"
  echo "→ metadata/$base"
done

echo "Done. Review diff in fdroiddata, then: fdroid lint <appid> && fdroid build <appid>"
