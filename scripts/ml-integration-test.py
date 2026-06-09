#!/usr/bin/env python3
"""Regression tests for bundled ONNX vision/OCR models (runs on CI JVM, no emulator)."""
from __future__ import annotations

import argparse
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / "core/ml/src/main/assets/models"

REQUIRED = [
    "yunet.onnx",
    "sface.onnx",
    "yolov5n.onnx",
    "ocr_det.onnx",
    "ocr_rec.onnx",
    "en_dict.txt",
    "coco_labels.txt",
    "MODELS-MANIFEST.json",
]


def load_dict() -> list[str]:
    chars = ["blank"]
    chars.extend(MODELS.joinpath("en_dict.txt").read_text(encoding="utf-8").splitlines())
    chars.append(" ")
    return chars


def text_image(text: str, width: int = 480, height: int = 120, size: int = 56) -> Image.Image:
    img = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", size)
    except OSError:
        font = ImageFont.load_default()
    draw.text((24, int(height * 0.25)), text, fill="black", font=font)
    return img


def decode_ctc(time_steps: np.ndarray, characters: list[str]) -> str:
    ignored = {0}
    chars: list[str] = []
    prev = -1
    for step in time_steps:
        best_idx = int(np.argmax(step))
        if best_idx not in ignored and best_idx != prev:
            ch = characters[best_idx] if best_idx < len(characters) else ""
            if ch and ch != "blank":
                chars.append(ch)
        prev = best_idx
    return "".join(chars)


def recognize_line(session, characters: list[str], crop: Image.Image) -> str:
    import onnxruntime as ort

    img_h = 48
    max_ratio = 8.0
    ratio = crop.width / max(crop.height, 1)
    max_w = int(img_h * max_ratio)
    resized_w = max(8, min(int(math.ceil(img_h * ratio)), max_w))
    scaled = crop.resize((resized_w, img_h), Image.Resampling.BILINEAR)
    img_w = max(int(img_h * max_ratio), resized_w)

    arr = np.asarray(scaled, dtype=np.float32) / 255.0
    arr = (arr - 0.5) / 0.5
    tensor = np.zeros((1, 3, img_h, img_w), dtype=np.float32)
    tensor[0, 0, :scaled.height, :scaled.width] = arr[:, :, 0]
    tensor[0, 1, :scaled.height, :scaled.width] = arr[:, :, 1]
    tensor[0, 2, :scaled.height, :scaled.width] = arr[:, :, 2]

    input_name = session.get_inputs()[0].name
    preds = session.run(None, {input_name: tensor})[0]
    return decode_ctc(preds[0], characters)


def ocr_image(det_session, rec_session, characters: list[str], image: Image.Image) -> str:
    # Full-image fallback path (matches Kotlin when det finds no boxes).
    return recognize_line(rec_session, characters, image.convert("RGB"))


def assert_manifest_matches_files() -> None:
    import hashlib
    import json

    manifest = json.loads((MODELS / "MODELS-MANIFEST.json").read_text(encoding="utf-8"))
    for entry in manifest["files"]:
        path = MODELS / entry["name"]
        data = path.read_bytes()
        digest = hashlib.sha256(data).hexdigest()
        if digest != entry["sha256"]:
            raise AssertionError(f"SHA256 mismatch for {entry['name']}")
        if path.stat().st_size != entry["bytes"]:
            raise AssertionError(f"Size mismatch for {entry['name']}")


def assert_models_present() -> None:
    missing = [name for name in REQUIRED if not (MODELS / name).exists()]
    if missing:
        raise AssertionError(
            f"Missing ML models: {missing}. Run ./scripts/fetch-ml-models.sh"
        )


def assert_model_sizes(max_mb: float) -> int:
    total = sum((MODELS / name).stat().st_size for name in REQUIRED if name.endswith(".onnx"))
    total_mb = total / (1024 * 1024)
    if total_mb > max_mb:
        raise AssertionError(
            f"ONNX bundle {total_mb:.1f} MB exceeds budget {max_mb:.1f} MB"
        )
    return total


def assert_sessions_load() -> None:
    import onnxruntime as ort

    opts = ort.SessionOptions()
    for name in ("yunet.onnx", "sface.onnx", "yolov5n.onnx", "ocr_det.onnx", "ocr_rec.onnx"):
        path = MODELS / name
        session = ort.InferenceSession(str(path), opts, providers=["CPUExecutionProvider"])
        assert session.get_inputs(), f"{name} has no inputs"


def assert_yolov5_runs() -> None:
    import onnxruntime as ort

    session = ort.InferenceSession(
        str(MODELS / "yolov5n.onnx"),
        providers=["CPUExecutionProvider"],
    )
    name = session.get_inputs()[0].name
    input_type = session.get_inputs()[0].type
    dtype = np.float16 if "float16" in input_type else np.float32
    tensor = np.zeros((1, 3, 640, 640), dtype=dtype)
    out = session.run(None, {name: tensor})[0]
    assert out.ndim >= 2, "yolov5n output rank"
    assert out.shape[-1] >= 85, "yolov5n output columns"


def assert_yunet_runs() -> None:
    import onnxruntime as ort

    session = ort.InferenceSession(
        str(MODELS / "yunet.onnx"),
        providers=["CPUExecutionProvider"],
    )
    name = session.get_inputs()[0].name
    tensor = np.zeros((1, 3, 320, 320), dtype=np.float32)
    outs = session.run(None, {name: tensor})
    assert len(outs) >= 12, "yunet should expose 12 feature maps"


def assert_sface_runs() -> None:
    import onnxruntime as ort

    session = ort.InferenceSession(
        str(MODELS / "sface.onnx"),
        providers=["CPUExecutionProvider"],
    )
    name = session.get_inputs()[0].name
    tensor = np.zeros((1, 3, 112, 112), dtype=np.float32)
    out = session.run(None, {name: tensor})[0]
    assert np.array(out).shape[-1] == 128, "sface embedding dim"


def assert_ocr_recognizes_freedom() -> None:
    import onnxruntime as ort

    characters = load_dict()
    det = ort.InferenceSession(
        str(MODELS / "ocr_det.onnx"),
        providers=["CPUExecutionProvider"],
    )
    rec = ort.InferenceSession(
        str(MODELS / "ocr_rec.onnx"),
        providers=["CPUExecutionProvider"],
    )
    image = text_image("FREEDOM")
    text = ocr_image(det, rec, characters, image).upper().replace(" ", "")
    if "FREEDOM" not in text and "FREEDO" not in text:
        raise AssertionError(f"OCR expected FREEDOM, got: {text!r}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--max-mb",
        type=float,
        default=19.0,
        help="Maximum total ONNX size in megabytes (default: 19 with SFace INT8)",
    )
    args = parser.parse_args()

    assert_models_present()
    assert_manifest_matches_files()
    total = assert_model_sizes(args.max_mb)
    assert_sessions_load()
    assert_yolov5_runs()
    assert_yunet_runs()
    assert_sface_runs()
    assert_ocr_recognizes_freedom()

    print(
        f"ml-integration-test OK ({total / (1024 * 1024):.1f} MB ONNX, budget {args.max_mb:.1f} MB)"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ml-integration-test FAILED: {exc}", file=sys.stderr)
        raise
