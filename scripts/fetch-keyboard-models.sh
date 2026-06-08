#!/usr/bin/env bash
# Downloads optional on-device assets for Freedom Keyboard (voice model).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/apps/keyboard/src/main/assets/models"
mkdir -p "$DEST"

echo "→ Streaming English ASR (Sherpa-ONNX Zipformer en-20M mobile)"
curl -fsSL \
  "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17-mobile.tar.bz2" \
  -o /tmp/zipformer-en-mobile.tar.bz2
tar -xjf /tmp/zipformer-en-mobile.tar.bz2 -C /tmp
cp -R /tmp/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17-mobile/* "$DEST/"
rm -rf /tmp/zipformer-en-mobile.tar.bz2 /tmp/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17-mobile

ls -lh "$DEST"
