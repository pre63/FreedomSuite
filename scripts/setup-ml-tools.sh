#!/usr/bin/env bash
# Local venv for ONNX quantization and ML integration tests (not committed).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VENV="$ROOT/.venv-ml-quantize"

if [[ ! -d "$VENV" ]]; then
  python3 -m venv "$VENV"
fi

"$VENV/bin/python" -m pip install -q --upgrade pip
"$VENV/bin/python" -m pip install -q \
  "numpy<2" \
  "onnx==1.16.2" \
  "onnxruntime==1.19.2" \
  "onnxconverter-common==1.16.0" \
  pillow
