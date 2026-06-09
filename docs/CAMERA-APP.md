# Freedom Camera — research & architecture

A **first-party camera app** so photos never pass through Google Camera, Samsung Camera, or other vendor apps with unknown network behavior. Shots are **encrypted at capture**, indexed locally, and optionally synced via Freedom Sync — same trust model as Freedom Files.

**Target platform:** [GrapheneOS](GRAPHENEOS.md) on Pixel (arm64). **Inference:** ONNX Runtime (existing `core/ml` stack).

---

## Why we need this

| Risk with stock camera apps | Freedom Camera response |
|----------------------------|-------------------------|
| Vendor cloud backup / AI features | No network permission by default |
| Photos land in unencrypted MediaStore | App-private AES-GCM immediately after shutter |
| Unknown EXIF / location leakage | Location off by default; strip or user-controlled EXIF |
| Third-party “beauty” SDKs phoning home | On-device ONNX only; audited blocklist |
| No integration with Files face/search index | Shared `LocalVisionEngine` pipeline after save |

Freedom Files today imports photos via SAF — it **does not capture**. A camera app closes the loop: **capture → encrypt → index → search → sync**.

---

## Principles

1. **Encrypt first** — JPEG/HEIC bytes never touch shared storage unencrypted.
2. **Effects are optional and local** — ML runs on-device; no “cloud enhance”.
3. **Small models, post-capture first** — preview stays smooth; full-res enhancement on save or in background.
4. **Same signing key as the suite** — optional bridge to Freedom Search / Files.
5. **Honest limits** — we are not competing with Google HDR+ or Apple Photonic Engine; we compete on **privacy and control**.

---

## Architecture (proposed)

```
apps/camera/
├── CameraX preview + capture (JPEG/HEIC)
├── CapturePipeline
│   ├── optional: EffectStack (ONNX, tiled)
│   └── EncryptedFileStore → camera_content/{uuid}.bin
├── CameraDatabase (SQLCipher) — shot metadata, effect settings, index refs
├── optional: CameraSearchProvider (signature SEARCH permission)
└── "Open in Freedom Files" / shared core/photos library (phase 2)

core/camera-effects/          # NEW — keep separate from indexing models in core/ml
├── onnx/ ZeroDce, MiDaSSmall, RtFocuser, QuickSRNet
└── cpu/  BokehRenderer (depth-guided blur, no ML)
```

### Storage (mirror Freedom Files)

| Layer | Mechanism |
|-------|-----------|
| Image bytes | `EncryptedFileStore` + per-file DEK in Keystore-wrapped prefs |
| Metadata | SQLCipher Room (`freedom_camera.db`) |
| Thumbnails | Encrypted sidecar or derived in DB blob |
| Export | Explicit user action → decrypt to SAF `CreateDocument` |
| Backup | `FreedomSyncEngine` namespace `camera` (E2EE blob) |
| Indexing | `LocalVisionEngine.indexImage()` — reuse existing ~18 MB vision pack |

**Do not** write to `DCIM/` or `MediaStore` unless the user explicitly exports.

### Security defaults

- `FLAG_SECURE` on preview (screen capture blocked in prod)
- `android:allowBackup="false"`
- No `INTERNET` in MVP (sync/export only in later phase if needed)
- Camera + microphone runtime permissions at use-time
- GrapheneOS: user grants camera; no Play Services dependency

---

## Effect categories & what actually works on mobile

| User-facing mode | Technical approach | Real-time preview? | ML needed? |
|------------------|-------------------|--------------------|------------|
| **Standard** | CameraX + device ISP | ✅ | No |
| **Night / clarity** | Zero-DCE curve estimation | Downscaled preview OK | Yes (~80K params) |
| **Portrait blur** | Monocular depth + variable bokeh | Downscaled depth map | Yes (~16M params, quantize) |
| **Motion / shake** | Deblur network OR multi-frame merge | Post-capture only | Yes (5–6M params) |
| **Denoise** | DnCNN / light bilateral | Post-capture | Optional (~0.5 MB INT8) |
| **2× detail zoom** | QuickSRNet / SPAN-S 2× SR | Post-capture or crop+SR | Yes (~0.1–2 MB) |
| **HDR** | Exposure bracketing + Mertens/OpenCV merge | Burst capture | No ML (phase 1) |

**Portrait bokeh without ML:** depth map → normalize → separable Gaussian with radius ∝ depth. The **depth model** is the hard part; blur is cheap GLES/CPU.

**Motion blur:** true optical motion blur needs either **burst alignment** (HDR pipeline) or a **deblur net** trained on motion blur. RT-Focuser (Dec 2025) is the best recent open fit for edge deployment.

---

## Open-source model survey (2024–2026)

Evaluated for: **size**, **ONNX deployability**, **license**, **usefulness for camera**, **Freedom Suite fit**.

### Tier A — recommended for Freedom Camera

| Model | Task | Params | Est. ONNX (quantized) | License | Notes |
|-------|------|--------|----------------------|---------|-------|
| **Zero-DCE** | Low-light / exposure | **79K** | **~0.3–1 MB** | Verify upstream¹ | 7 conv layers, 8 iterations; Android NCNN ports exist; ideal “Night” mode |
| **QuickSRNetSmall** | 2–3× super-res | **33K** | **~42 KB (INT8)** | Qualcomm Hub export² | Sub-ms on Snapdragon NPU; good “digital zoom” polish |
| **MiDaS v2.1 Small** | Relative depth | **16.6M** | **~4–8 MB INT8** | **MIT** | 256×384 input; proven portrait bokeh; Luxonis/ONNX ports |
| **RT-Focuser** | Motion deblur | **5.85M** | **~6–8 MB INT8** | **MIT** | Dec 2025; 30.67 dB PSNR GoPro; ONNX checkpoints on Hugging Face |
| **DnCNN** (σ=25) | Gaussian denoise | **555K** | **~0.6 MB INT8** | BSD-style³ | Qualcomm Hub; pair with Night mode |

¹ Zero-DCE original code (Li-Chongyi et al.) — confirm license before F-Droid; many forks are Apache-2.0. Prefer a **clean ONNX export** with documented license.

² Qualcomm AI Hub models are exports of published architectures — redistribution OK for inference artifacts; document provenance like YOLOv5n.

³ DnCNN paper implementation; use INT8 export from AI Hub or own calibration.

### Tier B — viable alternatives

| Model | Task | Params | Size | Trade-off |
|-------|------|--------|------|-----------|
| **SPAN-S** | 2× SR | **426K** | **~1.7 MB** | NTIRE 2024 winner; better quality than QuickSRNet, heavier |
| **FSRCNN** | 2× SR | **8–25K** | **~50 KB** | Classic, fast, lower perceptual quality |
| **XLSR** | 2× SR | **22K** | **~100 KB** | UINT8-quantization robust (Mobile AI 2021) |
| **MiDaS EfficientNet-Lite3** | Depth | ~**11M** | ~**5 MB TFLite** | Older mobile variant; MiDaS v2.1 small is newer |

### Tier C — reject for Freedom Camera (today)

| Model | Why not |
|-------|---------|
| **Real-ESRGAN** | ~16.7M params, ~64 MB; quality great, far too heavy for default camera APK |
| **Depth Anything V2 Small** | 24.8M params, ~94 MB ONNX FP32; Apache-2.0 but **3× heavier** than MiDaS small for marginal gain |
| **HCDeblur** (SIGGRAPH 2024) | Smartphone deblur SOTA research; RAFT + hybrid nets; no mobile ONNX release |
| **MISCFilter** (CVPR 2024) | Strong deblur paper; PyTorch only, no edge package |
| **PiperSR** | Excellent (928 KB) but **CoreML / Apple ANE only** — wrong platform |
| **Google ML Kit / Play HDR** | Violates project constraints |
| **NCNN-only stacks** (RealSR-Android) | Powerful but second inference runtime; stay on **ONNX Runtime** unless perf forces NCNN |

---

## Recommended model pack (MVP → v1)

Keep camera **effects models separate** from the **indexing pack** in `core/ml` (YOLO, SFace, OCR). Users who only want capture without portrait ML should get a smaller APK.

### MVP (no ML) — ship first

- CameraX Photo capture
- Encrypt + thumbnail + gallery UI
- Optional multi-frame HDR (OpenCV/Android `RenderScript` replacement / simple merge)
- Hand off to `LocalVisionEngine` for search (reuse existing 18 MB pack in same app or via Files bridge)

**Added APK weight:** ~0 MB ML (CameraX only)

### v1 “Useful camera” pack — **~8–12 MB** total ONNX

| Asset | Mode | Priority |
|-------|------|----------|
| `zerodce.onnx` | Night / clarity | P0 |
| `midas_small_int8.onnx` | Portrait depth | P0 |
| `rt_focuser_int8.onnx` | Motion fix (post) | P1 |
| `quicksrnet_small_int8.onnx` | 2× zoom enhance | P2 |

**Bokeh renderer:** pure CPU/GPU — **0 MB**.

**Denoise:** fold into Night via Zero-DCE first; add DnCNN only if night shots still noisy.

### Processing pipeline

```
Shutter
  → raw JPEG from CameraX
  → [optional] EffectStack.apply(settings)  // tiled 256–512 px patches
  → EncryptedFileStore.writeEncrypted
  → [async] LocalVisionEngine.indexImage     // existing suite index
  → UI thumbnail
```

**Tiling:** mandatory for full-resolution (12–50 MP). Process 256×256 or 384×384 windows with overlap blend — same pattern as Real-ESRGAN mobile ports.

**Preview:** run Zero-DCE + MiDaS on **720p preview stream** only; full-res on save.

---

## Performance expectations (GrapheneOS / Pixel class)

Rough order-of-magnitude on **arm64 CPU** via ONNX Runtime (no NPU guarantee on GOS):

| Model | Input | Est. time (CPU) | Notes |
|-------|-------|-----------------|-------|
| Zero-DCE | 256×256 | 20–80 ms | Feasible near-real-time on preview |
| MiDaS Small | 256×384 | 200–800 ms | Portrait preview at 2–5 fps acceptable |
| RT-Focuser | 256×256 tile | 100–400 ms/tile | 12 MP image ≈ 50–200 tiles → 5–30 s post; show progress |
| QuickSRNetSmall | 128×128 | <50 ms/tile | Zoom polish on center crop only |

NNAPI EP may help on some devices; **CPU must work** on GrapheneOS.

---

## Integration with Freedom Suite

| Integration | Approach |
|-------------|----------|
| **Freedom Files** | Phase 1: “Save to Files” copies encrypted blob into Files vault. Phase 2: shared `core/photos` module |
| **Freedom Search** | `CameraSearchProvider` + `SearchBridge.Source.CAMERA` |
| **Freedom Sync** | New `camera` namespace; same E2EE as Files |
| **ML indexing** | `implementation(project(":core:ml"))` — same YOLO/SFace/OCR search as imported photos |
| **F-Droid** | Camera is a good F-Droid citizen (no network MVP); document GPL YOLO if indexing bundled |

### APK size scenarios

| Build | Contents | ~Total ML |
|-------|----------|-----------|
| Camera lite | Capture + encrypt only | 0 |
| Camera + index | + `core/ml` vision pack | ~18 MB |
| Camera + effects | + camera-effects pack | ~8–12 MB |
| Camera full | index + effects | ~26–30 MB |

**Recommendation:** default F-Droid build = **lite + effects** (~10 MB); indexing either bundled or “link to Freedom Files” to avoid 30 MB monolith. Long-term: shared **Freedom Models** pack app ([ML-MODELS.md](ML-MODELS.md) Option C).

---

## Capture stack (non-ML)

**CameraX** (`androidx.camera`) — not Google Play Services; standard Jetpack, used by F-Droid apps.

| Component | Use |
|-----------|-----|
| `Preview` | Viewfinder |
| `ImageCapture` | Still JPEG/HEIC |
| `ImageAnalysis` | Optional preview frame for effects |
| `Camera2Interop` | Manual exposure for HDR brackets |

**Permissions:** `CAMERA` only for MVP. `RECORD_AUDIO` only if video mode ships later.

**Not in scope v1:** Video, RAW DNG, computational burst matching Google Camera.

---

## Phased roadmap

### Phase 0 — Research complete ✅
This document.

### Phase 1 — MVP Camera (4–6 weeks)
- `apps/camera` module, CameraX, encrypt-at-capture
- Encrypted gallery, delete, share-via-export
- `FLAG_SECURE`, AppLock, GrapheneOS checklist
- No ML effects

### Phase 2 — Night + Portrait (4–6 weeks)
- `core/camera-effects` module
- Zero-DCE + MiDaS + CPU bokeh
- Settings: effect strength, reprocess shot

### Phase 3 — Motion + Zoom (2–4 weeks)
- RT-Focuser post-capture
- QuickSRNet on digital zoom crop
- Progress UI for tiled enhance

### Phase 4 — Suite cohesion
- Files bridge / shared photos core
- Search provider
- Freedom Sync namespace

---

## Freedom / liability audit (camera models)

| Check | Zero-DCE | MiDaS Small | RT-Focuser | QuickSRNet |
|-------|----------|-------------|------------|------------|
| Commercial use | Verify¹ | MIT ✅ | MIT ✅ | Hub export² |
| No NC / research-only | ✅ | ✅ | ✅ | ✅ |
| No runtime download | Build-time bundle | ✅ | ✅ | ✅ |
| ONNX Runtime (no ML Kit) | ✅ | ✅ | ✅ | ✅ |
| Redistributable weights | Verify¹ | ✅ | ✅ | ✅ |

---

## Open questions

1. **Shared `core/photos` vs duplicate storage** — one encrypted vault for Files + Camera, or copy-on-save?
2. **HEIC vs JPEG** — HEIC saves space; ONNX pipelines expect RGB bitmaps (both fine).
3. **NNAPI on GrapheneOS** — test ORT execution providers on real device.
4. **Zero-DCE license** — pin a specific export commit with SPDX.
5. **Burst HDR** — OpenCV Android module vs lightweight native merge.

---

## References

| Resource | URL |
|----------|-----|
| Zero-DCE paper | https://arxiv.org/abs/2001.06826 |
| MiDaS | https://github.com/isl-org/MiDaS |
| RT-Focuser (MIT, ONNX) | https://github.com/ReaganWu/RT-Focuser |
| QuickSRNetSmall | https://huggingface.co/qualcomm/QuickSRNetSmall |
| SPAN (NTIRE 2024) | https://github.com/hongyuanyu/SPAN |
| ONNX Runtime mobile SR tutorial | https://onnxruntime.ai/docs/tutorials/mobile/superres.html |
| Android Zero-DCE NCNN | https://github.com/DigRabbit666/Android-implementation-of-LLIE-methods-Opt2Ada-and-ZeroDce |
| Freedom ML stack | [ML-MODELS.md](ML-MODELS.md) |
| GrapheneOS target | [GRAPHENEOS.md](GRAPHENEOS.md) |

---

## Bottom line

**Yes — a useful privacy-first camera is feasible with ~8–12 MB of lightweight ONNX models**, separate from the existing 18 MB indexing pack. Start with **CameraX + encrypt-at-capture** (no ML), then add **Zero-DCE (night)** and **MiDaS small + CPU bokeh (portrait)**. Add **RT-Focuser** for motion and **QuickSRNet** for zoom as polish layers. Skip Real-ESRGAN and Depth Anything V2 for default builds — quality gains do not justify size on GrapheneOS phones.
