#!/usr/bin/env bash
# F-Droid / CI prebuild: fetch optional assets required for release APKs.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

need_ml=false
if [[ ! -f core/ml/src/main/assets/models/yolov5n.onnx ]]; then
  need_ml=true
fi

if $need_ml; then
  echo "Fetching ML models (Freedom Files)…"
  ./scripts/fetch-ml-models.sh
else
  echo "ML models present — skip fetch"
fi

# Keyboard voice model is optional; prod APK builds without it.
if [[ "${FETCH_KEYBOARD_MODELS:-0}" == "1" ]]; then
  echo "Fetching keyboard voice models…"
  ./scripts/fetch-keyboard-models.sh
fi

echo "fdroid-prebuild complete"
