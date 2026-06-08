package org.freedomsuite.core.keyboard.speech

import kotlinx.coroutines.flow.Flow

/**
 * On-device speech-to-text. Implementations must never send audio off-device.
 */
interface LocalSpeechRecognizer {
    val isReady: Boolean

    suspend fun prepare()

    fun startListening(): Flow<SpeechPartialResult>

    fun stopListening()

    fun release()
}

data class SpeechPartialResult(
    val text: String,
    val isFinal: Boolean,
)
