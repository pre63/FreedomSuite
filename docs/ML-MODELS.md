# On-device ML models — Freedom Suite

All inference runs **locally** via ONNX Runtime. Models are **fetched at build time** and bundled in the APK — no runtime download, no cloud APIs, no telemetry.

Run regression tests: `./gradlew mlIntegrationTest`

## Bundled models

| Asset | Role | Upstream | License | ~Size (quantized) |
|-------|------|----------|---------|-------------------|
| `yunet.onnx` | Face detection + 5 landmarks | [OpenCV Zoo YuNet 2026may](https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet) | MIT | ~125 KB |
| `sface.onnx` | Face embeddings (128-D) | [OpenCV Zoo SFace INT8](https://github.com/opencv/opencv_zoo/tree/main/models/face_recognition_sface) | Apache 2.0 | ~9.4 MB |
| `yolov5n.onnx` | COCO object labels | [Ultralytics YOLOv5n v7.0](https://github.com/ultralytics/yolov5) | GPL-3.0 (weights) | ~3.8 MB |
| `ocr_det.onnx` | Text region detection | [RapidOCR PP-OCRv4](https://github.com/RapidAI/RapidOCR) | Apache 2.0 | ~1.2 MB |
| `ocr_rec.onnx` | English OCR recognition | RapidOCR PP-OCRv4 | Apache 2.0 | ~3.7 MB |
| `en_dict.txt` | OCR character table | RapidOCR / Paddle | Apache 2.0 | <1 KB |
| `coco_labels.txt` | COCO class names | Ultralytics | — | <1 KB |

**Total ONNX:** ~18 MB after `fetch-ml-models.sh` + FP16 quantize (YuNet/OCR).

## Freedom / liability audit

| Check | Status |
|-------|--------|
| No runtime network for inference | ✅ |
| No Google ML Kit / Play Services | ✅ |
| No training telemetry in ORT integration | ✅ |
| Commercial use allowed | ✅ MIT + Apache models; see YOLO note below |
| No NC / “research only” face models (e.g. InsightFace ArcFace) | ✅ SFace chosen deliberately |
| Build-time CDN fetch only | ✅ `scripts/fetch-ml-models.sh` — not user-initiated |
| User photos never leave device for ML | ✅ Files + Chat index on-device |

### YOLOv5n (GPL-3.0)

Ultralytics **weights** are GPL-3.0. We use them only as an **on-device, unmodified inference artifact** bundled in our app. This is documented in [THIRD-PARTY-AUDIT.md](THIRD-PARTY-AUDIT.md). If F-Droid or legal review objects, swap to an Apache-licensed detector (e.g. OpenCV Zoo object model) without changing the pipeline API.

### SFace vs classical embeddings

Previously we used hand-crafted LBP/histogram vectors (~328-D) with a broken YuNet parser. **SFace** (MobileFaceNet + SFace loss) gives proper 128-D embeddings and ~99% LFW accuracy in OpenCV Zoo benchmarks. Cost: +~9 MB INT8 weights — acceptable for “find similar faces” in Freedom Files.

### Models we rejected

| Model | Why not |
|-------|---------|
| InsightFace / ArcFace ONNX | Often non-commercial licenses |
| Full SFace FP32 (37 MB) | Too large; INT8 zoo build is enough |
| Google ML Kit | Network + Play dependency |
| Runtime model download | Violates offline / privacy stance |

## Pipeline

```
Image → YuNet (faces) → SFace (128-D embed) → encrypted DB
      → YOLOv5n (objects) → search blob
      → PP-OCR (text) → search blob
```

Re-index photos in Freedom Files after upgrading embeddings (Settings → Index photos).

## Suite architecture — who ships models?

Models live in **one place**: `core/ml/src/main/assets/models/`. Apps opt in via Gradle — only dependents bundle the AAR assets.

| App | `implementation(:core:ml)` | ~ML in APK | Uses ML at runtime |
|-----|---------------------------|------------|-------------------|
| **Files** | ✅ | ~18 MB ONNX | Index photos, OCR, similar faces |
| **Chat** | ✅ | ~18 MB ONNX | Describe image attachments (not on F-Droid) |
| Search | ❌ | 0 | Reads Files index via provider — no inference |
| Inbox, Calendar, Messages, Auth, Keyboard | ❌ | 0 | None today |

**You do not pay 18 MB × 7 apps.** Mail, calendar, etc. stay small. If Inbox gains attachment OCR later, add `:core:ml` only to that app — or use the shared-pack pattern below.

## Distribution strategy audit

### Option A — Bundle in APK (current, recommended for F-Droid)

| Pros | Cons |
|------|------|
| Works offline immediately | +~18 MB on Files (and Chat) APK only |
| F-Droid reproducible via `prebuild: fetch-ml-models.sh` | CI must fetch upstream once per build |
| No runtime network / no extra permissions | |
| No third-party CDN at user runtime | |

**Verdict:** Keep for **F-Droid** and privacy-first default.

### Option B — Download once from **our** GitHub Releases

| Pros | Cons |
|------|------|
| Smaller initial APK (~40 MB vs ~55 MB for Files) | Needs `INTERNET` + user-initiated or first-run download |
| We control URL + checksums | F-Droid may flag **NonFreeNet** / binary download anti-feature |
| Same tarball serves all suite apps if designed right | Each separate APK has its own storage unless shared pack |

**Redistribution rights (investigated):**

| Asset | Host on `pre63/FreedomSuite` releases? | Notes |
|-------|----------------------------------------|-------|
| YuNet, SFace (OpenCV Zoo) | ✅ Yes | MIT / Apache-2.0 — include `NOTICE` in release |
| PP-OCR (RapidAI) | ✅ Yes | Apache-2.0 — our FP16 quantize is a derivative; document in manifest |
| YOLOv5n weights | ⚠️ Yes with GPL notice | GPL-3.0 allows redistributing the weights file; app compliance is separate |
| **Do not** runtime-fetch from ModelScope / opencv_zoo raw | ❌ | Foreign CDN, no checksum control, reproducibility breaks |

**Safe URL pattern (if we implement B):**

```
https://github.com/pre63/FreedomSuite/releases/download/ml-2026.06.08/freedom-ml-models.tar.zst
```

Verify with embedded `MODELS-MANIFEST.json` SHA-256 per file. Allowlist **only** `github.com/pre63/FreedomSuite/releases/download/ml-*` in `PrivacyHttpClient` — never arbitrary URLs.

**Verdict:** OK for **GitHub Releases channel** ( `./scripts/publish.sh` ), not as F-Droid default.

### Option C — Shared “Freedom Models” pack (best for full-suite UX)

One optional app (or on-demand module) holds ~18 MB once. Files, Chat, future Inbox call a **signature-protected `ContentProvider`** or bind to a small `org.freedomsuite.models` package.

| Pros | Cons |
|------|------|
| User installs suite once, models once | Extra app in store / F-Droid metadata |
| No duplicate 18 MB per app | More engineering (provider API, version skew) |
| Matches “everyone wants the whole suite” | User must install the pack |

**Verdict:** Best long-term if 3+ apps need ML. Not required today (only Files + Chat).

### Recommended hybrid

1. **F-Droid / prod default:** Option A (bundled, offline).
2. **GitHub releases:** Option A today; optional slim APK + Option B later with manifest verification.
3. **When Inbox OCR ships:** prefer Option C over adding `:core:ml` to every app.

## Model trade-off re-validation

| Goal | Choice | Still right? |
|------|--------|--------------|
| Local-first, no cloud vision | ONNX in `core/ml` | ✅ |
| Strong face matching | SFace INT8 (+9 MB) vs broken LBP | ✅ |
| Small mail/calendar APKs | Only Files/Chat depend on `core/ml` | ✅ |
| Apache/MIT where possible | SFace, OCR, YuNet | ✅ |
| Avoid InsightFace NC licenses | Rejected ArcFace | ✅ |
| GPL surface minimal | Only YOLOv5n; document swap path | ⚠️ monitor |

**SFace size:** Largest cost. Alternatives (classical embeds, no model) failed quality. Full SFace FP32 (37 MB) failed size. INT8 zoo build is the justified middle.

## Integrity manifest

`core/ml/src/main/assets/models/MODELS-MANIFEST.json` — SHA-256 and bytes per file. Regenerate after `fetch-ml-models.sh` when models change. Use for GitHub release verification and CI drift detection.

## Build

```bash
./scripts/fetch-ml-models.sh   # download + FP16 quantize YuNet/OCR
./gradlew mlIntegrationTest    # load + OCR + shape checks
```
