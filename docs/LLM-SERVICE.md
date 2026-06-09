# LLM service layer — Freedom Suite

On-device LLM is **foundational**. Remote providers (Grok, OpenAI-compatible APIs) are optional overlays — on-device inference is always wired.

## Architecture

```
apps/chat (UI)
    └── ChatLlmBridge
            └── FreedomLlmService (core/llm)
                    ├── OnDeviceLlmBackend  ← always present
                    │       └── GenAiLocalLlmEngine (ONNX Runtime GenAI)
                    └── RemoteOpenAiCompatibleBackend  ← optional
```

### Routing

| Chat setting | `LlmRouting` | Behavior |
|--------------|--------------|----------|
| On-device LLM | `ON_DEVICE_ONLY` | GenAI on phone only |
| Remote + fallback | `REMOTE_WITH_ON_DEVICE_FALLBACK` | Try remote; on failure use on-device |

Vision (YOLO, SFace, OCR) stays in `core/ml`. Only **text summaries** go to remote APIs.

## Modules

| Module | Role |
|--------|------|
| `core/llm` | `FreedomLlmService`, backends, prompt formatting, model manager |
| `core/ml` | Image attach → `VisionContext` |
| `apps/chat` | Compose UI, settings, `ChatLlmBridge` |

## On-device stack

| Component | Source |
|-----------|--------|
| Runtime | `onnxruntime-genai-android` AAR (v0.14.0) — `./scripts/fetch-genai-aar.sh` |
| Model | SmolLM2-360M-Instruct GenAI INT4 (~420 MB) — `./scripts/fetch-llm-model.sh` |
| Model on device | `make push-llm-model` → `files/llm/model/` |

## Developer setup

```bash
make fetch-llm              # AAR + model tarball on host
make install-app APP=chat   # install chat dev APK
make push-llm-model         # push model to emulator/device
```

In Chat → Settings, status should show **On-device model ready**.

## API (for future apps)

```kotlin
val service = FreedomLlmService.create(
    context = context,
    routing = LlmRouting.ON_DEVICE_ONLY,
    remoteConfig = null, // or RemoteLlmConfig(...)
)
val response = service.complete(
    LlmRequest(
        messages = listOf(LlmMessage(LlmRole.USER, "Hello")),
        visionContext = optionalVision,
    ),
)
```

Keyboard smart-compose and Inbox grammar fix can share `core/llm` later ([KEYBOARD.md](KEYBOARD.md)).

## Privacy

- On-device: no network for inference
- Remote: user-configured endpoint only; `PrivacyHttpClient` blocklist applies
- Images: never sent to remote — `VisionContext` text only

## F-Droid note

Chat bundles GenAI native libs when built. The **~420 MB model is not in the APK** — users fetch/push separately (same pattern as optional ML assets). F-Droid metadata for Chat is future work.
