#!/usr/bin/env bash
# Quantize downloaded ONNX models in core/ml assets (OCR FP16, YuNet INT8).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

./scripts/setup-ml-tools.sh
.venv-ml-quantize/bin/python scripts/quantize-ml-models.py
