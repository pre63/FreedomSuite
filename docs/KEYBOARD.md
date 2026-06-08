# Freedom Keyboard — Architecture

Freedom Keyboard replaces cloud keyboards (Gboard, etc.) with on-device prediction and dictation. **No `INTERNET` permission.**

## Goals

| Feature | Approach |
|---------|----------|
| Word completion | Prefix trie + English frequency list (50k words, ~600 KB) |
| Next-word prediction | Learned bigrams + personal dictionary (encrypted SQLCipher) |
| Autocorrect | Edit-distance candidates ranked by word frequency |
| Personal learning | Remembers emails, names, and phrases you type — never leaves device |
| Voice-to-text | Sherpa-ONNX streaming ASR (planned; stub today) |
| Smart compose | Optional future: SmolLM2-135M for long suggestions only |

## What we deliberately avoid

- **Google SpeechRecognizer (online)** — sends audio to Google
- **Android SpeechRecognizer offline pack** — still a Google system component; acceptable fallback only
- **LLM on every keystroke** — too slow and battery-heavy; Gboard-style shimmer needs <5 ms suggestions
- **Anagram-only prediction** — too weak for quality completions; trie + frequency + bigrams is the standard local approach (HeliBoard, Urik, Privacy Keyboard)

## Voice-to-text pick: Sherpa-ONNX + streaming Zipformer

| Option | Model size | Streaming | Quality | Verdict |
|--------|------------|-----------|---------|---------|
| **Sherpa-ONNX Zipformer en-20M mobile** | **~105 MB** | **Yes (live partial text)** | Good for dictation | **Pick** |
| Sherpa-ONNX Moonshine tiny en | ~105 MB | Simulated streaming | Good | Alternate |
| Whisper tiny.en (whisper.cpp) | ~75 MB | No (batch) | Good accuracy, laggy | Poor for live mic |
| Google LiteRT / MediaPipe | 500 MB+ | Yes | Best | Ruled out (Google stack) |

Zipformer en-20M is designed for low-end ARM CPUs and true streaming — text appears as you speak, like Gboard dictation.

Model URL (bundled via `scripts/fetch-keyboard-models.sh` in a future step):

```
https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17-mobile.tar.bz2
```

## Word prediction — why not the LLM?

SmolLM2-135M (~105 MB) is excellent for **spellcheck paragraphs** and **primitive chat**, but wrong for per-key suggestions:

- Latency: 50–200 ms per token vs <1 ms for trie lookup
- Battery: continuous generation while typing drains power
- UX: users expect instant bar updates on each letter

**Use LLM later for:**

- “Fix grammar in this email” (Inbox compose action)
- Optional long-press “smart compose” on space bar
- Freedom Chat replies

**Use trie + bigrams + user dict for:**

- Tap suggestions bar
- Autocorrect on space
- Remembering `user@mailbox.org` after first use

## Module layout

```
core/keyboard/          Prediction engine + speech interface
apps/keyboard/          IME + RecognitionService + settings
```

## Privacy

- Manifest has **no** `INTERNET`
- Learned words in SQLCipher via `EncryptedRoom`
- Voice models stored in app-private storage after one-time asset copy
- `./gradlew privacyAudit` must pass before release

## Setup (developer)

```bash
./gradlew :apps:keyboard:assembleDevDebug
```

On device:

1. Install APK
2. Settings → Enable **Freedom Keyboard** + **Freedom Voice Input**
3. Pick keyboard from input method switcher

## Roadmap

1. ✅ IME shell, QWERTY, suggestion bar, encrypted learning
2. ⏳ Sherpa-ONNX JNI + Zipformer streaming dictation
3. ⏳ Shift/caps, symbols layer, emoji
4. ⏳ Optional SmolLM2 compose assist (Inbox integration)
