#!/usr/bin/env bash
# Fetch ONNX Runtime GenAI Android AAR (required for core/llm on-device inference).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="${GENAI_VERSION:-0.14.0}"
OUT="libs/onnxruntime-genai-android.aar"
URL="https://github.com/microsoft/onnxruntime-genai/releases/download/v${VERSION}/onnxruntime-genai-android-${VERSION}.aar"

mkdir -p libs
if [[ -f "$OUT" ]]; then
  echo "GenAI AAR already present: $OUT"
  exit 0
fi

echo "Downloading $URL …"
curl -fL "$URL" -o "$OUT"
echo "Saved $OUT ($(du -h "$OUT" | awk '{print $1}'))"
