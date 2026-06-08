#!/usr/bin/env bash
# Downloads ONNX models for core/ml. Re-run when bumping model versions.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/core/ml/src/main/assets/models"
mkdir -p "$DEST"

fetch() {
  local url="$1"
  local out="$2"
  echo "→ $out"
  curl -fsSL "$url" -o "$DEST/$out"
}

fetch "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx" "yunet.onnx"
fetch "https://github.com/ultralytics/yolov5/releases/download/v7.0/yolov5n.onnx" "yolov5n.onnx"
fetch "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.8.0/onnx/PP-OCRv4/det/en_PP-OCRv3_det_mobile.onnx" "ocr_det.onnx"
fetch "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.8.0/onnx/PP-OCRv4/rec/en_PP-OCRv4_rec_mobile.onnx" "ocr_rec.onnx"
fetch "https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/release/2.6/ppocr/utils/dict/en_dict.txt" "en_dict.txt"

if [[ ! -f "$DEST/coco_labels.txt" ]]; then
  cat > "$DEST/coco_labels.txt" <<'EOF'
person
bicycle
car
motorcycle
airplane
bus
train
truck
boat
EOF
fi

ls -lh "$DEST"/*.onnx "$DEST"/en_dict.txt
