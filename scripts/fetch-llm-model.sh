#!/usr/bin/env bash
# Download SmolLM2 GenAI INT4 model pack for on-device chat (~420 MB).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MODEL_DIR="${LLM_MODEL_DIR:-vendor/llm-model}"
REPO="homen3/SmolLM2-360M-Instruct-ort-genai-int4-cpu"
BASE="https://huggingface.co/${REPO}/resolve/main"

FILES=(
  genai_config.json
  model.onnx
  model.onnx.data
  tokenizer.json
  tokenizer_config.json
  chat_template.jinja
)

mkdir -p "$MODEL_DIR"

need_fetch=false
for f in "${FILES[@]}"; do
  if [[ ! -f "$MODEL_DIR/$f" ]]; then
    need_fetch=true
    break
  fi
done

if ! $need_fetch; then
  echo "LLM model already present in $MODEL_DIR"
  exit 0
fi

echo "Fetching SmolLM2 GenAI model into $MODEL_DIR …"
for f in "${FILES[@]}"; do
  echo "  → $f"
  curl -fL "${BASE}/${f}" -o "$MODEL_DIR/$f"
done

echo "LLM model ready ($(du -sh "$MODEL_DIR" | awk '{print $1}'))"
