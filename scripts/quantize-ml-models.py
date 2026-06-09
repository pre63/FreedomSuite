#!/usr/bin/env python3
"""Shrink bundled ONNX models: INT8 for YOLO/YuNet, FP16 for Paddle OCR exports."""
from __future__ import annotations

import shutil
import sys
import tempfile
from pathlib import Path

import onnx
from onnxconverter_common import float16
from onnxruntime.quantization import QuantType, quantize_dynamic

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / "core/ml/src/main/assets/models"

# Ultralytics yolov5n release is already FP16 (~3.8 MB); dynamic INT8 breaks mixed graphs.
# YuNet INT8 uses ConvInteger — skip (FP16 is enough at ~230 KB → ~120 KB).
INT8_MODELS: tuple[str, ...] = ()
FP16_MODELS = ("yunet.onnx", "ocr_det.onnx", "ocr_rec.onnx")


def quantize_int8(src: Path) -> Path:
    with tempfile.NamedTemporaryFile(suffix=".onnx", delete=False) as tmp:
        out = Path(tmp.name)
    quantize_dynamic(
        model_input=str(src),
        model_output=str(out),
        weight_type=QuantType.QInt8,
    )
    return out


def quantize_fp16(src: Path) -> Path:
    with tempfile.NamedTemporaryFile(suffix=".onnx", delete=False) as tmp:
        out = Path(tmp.name)
    model = onnx.load(str(src))
    converted = float16.convert_float_to_float16(model, keep_io_types=True)
    onnx.save(converted, str(out))
    return out


def replace_model(name: str, quantized: Path) -> None:
    target = MODELS / name
    backup = MODELS / f"{name}.fp32.bak"
    if not backup.exists():
        shutil.copy2(target, backup)
    shutil.move(str(quantized), str(target))


def main() -> int:
    if not MODELS.exists():
        print(f"Model directory missing: {MODELS}", file=sys.stderr)
        return 1

    before = sum((MODELS / n).stat().st_size for n in INT8_MODELS + FP16_MODELS)

    for name in INT8_MODELS:
        src = MODELS / name
        if not src.exists():
            print(f"Missing {src}", file=sys.stderr)
            return 1
        print(f"INT8 {name} ({src.stat().st_size // 1024} KB)...")
        tmp = quantize_int8(src)
        replace_model(name, tmp)
        print(f"  -> {(MODELS / name).stat().st_size // 1024} KB")

    for name in FP16_MODELS:
        src = MODELS / name
        if not src.exists():
            print(f"Missing {src}", file=sys.stderr)
            return 1
        print(f"FP16 {name} ({src.stat().st_size // 1024} KB)...")
        tmp = quantize_fp16(src)
        replace_model(name, tmp)
        print(f"  -> {(MODELS / name).stat().st_size // 1024} KB")

    after = sum((MODELS / n).stat().st_size for n in INT8_MODELS + FP16_MODELS)
    print(
        f"Quantized ONNX total: {before // 1024} KB -> {after // 1024} KB "
        f"({100 * after / before:.0f}%)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
