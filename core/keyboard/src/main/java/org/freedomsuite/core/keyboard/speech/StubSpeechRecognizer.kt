package org.freedomsuite.core.keyboard.speech

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Placeholder until Sherpa-ONNX streaming ASR is wired in.
 */
class StubSpeechRecognizer : LocalSpeechRecognizer {
    override val isReady: Boolean = false

    override suspend fun prepare() = Unit

    override fun startListening(): Flow<SpeechPartialResult> = flow {
        delay(200)
        emit(
            SpeechPartialResult(
                text = "Install the English voice model in Freedom Keyboard settings.",
                isFinal = true,
            ),
        )
    }

    override fun stopListening() = Unit

    override fun release() = Unit
}
