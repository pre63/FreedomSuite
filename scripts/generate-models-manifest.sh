#!/usr/bin/env bash
# Writes core/ml/src/main/assets/models/MODELS-MANIFEST.json with sha256 + sizes.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

python3 <<'PY'
import hashlib
import json
from datetime import date
from pathlib import Path

dest = Path("core/ml/src/main/assets/models")
meta = {
    "yunet.onnx": ("opencv/opencv_zoo face_detection_yunet_2026may (MIT)", "face_detection"),
    "sface.onnx": ("opencv/opencv_zoo face_recognition_sface_2021dec_int8 (Apache-2.0)", "face_embedding"),
    "yolov5n.onnx": ("ultralytics/yolov5 v7.0 yolov5n.onnx (GPL-3.0 weights)", "object_detection"),
    "ocr_det.onnx": ("RapidAI/RapidOCR PP-OCRv4 en det (Apache-2.0)", "ocr_detect"),
    "ocr_rec.onnx": ("RapidAI/RapidOCR PP-OCRv4 en rec (Apache-2.0)", "ocr_recognize"),
    "en_dict.txt": ("RapidAI/RapidOCR PP-OCRv4 en dict (Apache-2.0)", "ocr_dict"),
    "coco_labels.txt": ("bundled COCO label list", "object_labels"),
}
files = []
onnx_total = 0
for name in meta:
    path = dest / name
    if not path.exists():
        raise SystemExit(f"Missing {path}")
    data = path.read_bytes()
    if name.endswith(".onnx"):
        onnx_total += len(data)
    upstream, role = meta[name]
    files.append({
        "name": name,
        "bytes": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
        "upstream": upstream,
        "role": role,
    })
bundle = {
    "bundle_version": date.today().isoformat(),
    "total_onnx_bytes": onnx_total,
    "files": files,
}
(dest / "MODELS-MANIFEST.json").write_text(json.dumps(bundle, indent=2) + "\n", encoding="utf-8")
print(f"Wrote {dest / 'MODELS-MANIFEST.json'} ({onnx_total // (1024*1024)} MB ONNX)")
PY
