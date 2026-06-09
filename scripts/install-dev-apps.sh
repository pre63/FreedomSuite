#!/usr/bin/env bash
# Install all Freedom Suite devDebug apps on a connected device or emulator.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${ANDROID_SDK_ROOT:-}" && -z "${ANDROID_HOME:-}" ]]; then
  if [[ -d "${HOME}/Library/Android/sdk" ]]; then
    export ANDROID_SDK_ROOT="${HOME}/Library/Android/sdk"
  elif [[ -d "${HOME}/Android/Sdk" ]]; then
    export ANDROID_SDK_ROOT="${HOME}/Android/Sdk"
  fi
fi

SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -n "$SDK" ]]; then
  export PATH="${SDK}/platform-tools:${SDK}/emulator:${PATH}"
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "error: adb not found. Set ANDROID_SDK_ROOT or install Android platform-tools." >&2
  echo "  macOS default: ${HOME}/Library/Android/sdk" >&2
  exit 1
fi

device_count=0
device_serial=""
while IFS= read -r line; do
  serial="${line%%[[:space:]]*}"
  if [[ -n "$serial" ]]; then
    device_serial="$serial"
    device_count=$((device_count + 1))
  fi
done < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')

if [[ "$device_count" -eq 0 ]]; then
  echo "error: no adb device/emulator connected." >&2
  echo "  Run: make emulator-start   (or plug in a phone with USB debugging)" >&2
  exit 1
fi

if [[ "$device_count" -gt 1 && -z "${ANDROID_SERIAL:-}" ]]; then
  echo "error: multiple devices connected; set ANDROID_SERIAL to one of:" >&2
  adb devices -l >&2
  exit 1
fi

abi="$(adb shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r' || true)"
echo "Target device: ${ANDROID_SERIAL:-$device_serial}  ABI: ${abi:-unknown}"

case "$abi" in
  x86|x86_64)
    if ! grep -q 'freedom.includeEmulatorAbis=true' local.properties 2>/dev/null; then
      echo "warning: x86 emulator detected. Add to local.properties:" >&2
      echo "  freedom.includeEmulatorAbis=true" >&2
      echo "Then rebuild: make install-dev" >&2
    fi
    ;;
esac

echo "Fetching assets if needed…"
./scripts/fetch-genai-aar.sh
./scripts/fdroid-prebuild.sh

echo "Installing devDebug apps…"
./gradlew --no-daemon installDevDebugApps

echo ""
echo "Installed packages (dev flavor):"
for pkg in inbox calendar messages auth files keyboard search chat; do
  if adb shell pm path "org.freedomsuite.${pkg}.dev" >/dev/null 2>&1; then
    echo "  ✓ org.freedomsuite.${pkg}.dev"
  else
    echo "  ✗ org.freedomsuite.${pkg}.dev (missing)"
  fi
done
