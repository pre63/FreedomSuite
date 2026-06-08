#!/usr/bin/env bash
# Local / CI gate: audits + integration tests + prod release builds for F-Droid apps.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

./scripts/fdroid-prebuild.sh
./gradlew --no-daemon fdroidVerify "$@"
