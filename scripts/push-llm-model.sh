#!/usr/bin/env bash
# Push fetched GenAI model to a connected device/emulator for Freedom Chat.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MODEL_DIR="${LLM_MODEL_DIR:-vendor/llm-model}"
PACKAGE="${LLM_PACKAGE:-org.freedomsuite.chat.dev}"

if [[ -z "${ANDROID_SDK_ROOT:-}" && -z "${ANDROID_HOME:-}" ]]; then
  if [[ -d "${HOME}/Library/Android/sdk" ]]; then
    export ANDROID_SDK_ROOT="${HOME}/Library/Android/sdk"
  fi
fi
export PATH="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}/platform-tools:${PATH}"

if ! command -v adb >/dev/null; then
  echo "error: adb not found" >&2
  exit 1
fi

if [[ ! -f "$MODEL_DIR/genai_config.json" ]]; then
  echo "error: model missing — run ./scripts/fetch-llm-model.sh first" >&2
  exit 1
fi

REMOTE_DIR="/data/data/${PACKAGE}/files/llm/model"
echo "Pushing model to $PACKAGE …"
adb shell "run-as ${PACKAGE} mkdir -p ${REMOTE_DIR}" 2>/dev/null || {
  echo "error: cannot run-as $PACKAGE — install chat devDebug first (make install-app APP=chat)" >&2
  exit 1
}

for f in genai_config.json model.onnx model.onnx.data tokenizer.json tokenizer_config.json chat_template.jinja; do
  if [[ -f "$MODEL_DIR/$f" ]]; then
    adb push "$MODEL_DIR/$f" "/data/local/tmp/$f"
    adb shell "run-as ${PACKAGE} cp /data/local/tmp/$f ${REMOTE_DIR}/$f"
    adb shell "rm /data/local/tmp/$f"
    echo "  ✓ $f"
  fi
done

echo "On-device model installed for $PACKAGE"
