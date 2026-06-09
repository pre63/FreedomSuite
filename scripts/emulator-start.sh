#!/usr/bin/env bash
# Start an Android emulator and wait until it is booted.
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
if [[ -z "$SDK" || ! -d "$SDK/emulator" ]]; then
  echo "error: Android SDK emulator not found. Install via Android Studio SDK Manager." >&2
  exit 1
fi

export PATH="${SDK}/platform-tools:${SDK}/emulator:${PATH}"

AVD_NAME="${AVD_NAME:-}"
if [[ -z "$AVD_NAME" ]]; then
  avd_count=0
  first_avd=""
  while IFS= read -r avd; do
    avd_count=$((avd_count + 1))
    [[ -z "$first_avd" ]] && first_avd="$avd"
    for preferred in FreedomSuite GrokChat Pixel_8_API_35; do
      if [[ "$avd" == "$preferred" ]]; then
        AVD_NAME="$avd"
        break 2
      fi
    done
  done < <(emulator -list-avds 2>/dev/null || true)

  if [[ "$avd_count" -eq 0 ]]; then
    echo "error: no AVDs found. Create one in Android Studio (Device Manager)." >&2
    echo "  Recommended: arm64-v8a system image (Android 14+)" >&2
    exit 1
  fi
  AVD_NAME="${AVD_NAME:-$first_avd}"
fi

if adb devices | awk 'NR>1 && $2=="device" {found=1} END {exit !found}'; then
  serial="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
  if [[ "$serial" == emulator-* ]]; then
    echo "Emulator already running: $serial"
    exit 0
  fi
fi

echo "Starting AVD: $AVD_NAME"
emulator -avd "$AVD_NAME" -no-snapshot-load -gpu host >/tmp/freedom-emulator.log 2>&1 &
echo "Emulator PID: $! (logs: /tmp/freedom-emulator.log)"

adb wait-for-device
echo "Waiting for boot…"
for _ in $(seq 1 120); do
  boot="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [[ "$boot" == "1" ]]; then
    abi="$(adb shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r')"
    echo "Emulator ready — ABI: $abi"
    exit 0
  fi
  sleep 2
done

echo "error: emulator boot timed out. See /tmp/freedom-emulator.log" >&2
exit 1
