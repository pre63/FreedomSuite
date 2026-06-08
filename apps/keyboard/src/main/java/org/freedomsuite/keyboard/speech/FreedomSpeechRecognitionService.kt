package org.freedomsuite.keyboard.speech

import android.content.Intent
import android.speech.RecognitionService
import android.speech.RecognitionService.Callback
import android.speech.SpeechRecognizer
import org.freedomsuite.core.keyboard.speech.StubSpeechRecognizer

/**
 * System voice-input provider. Uses on-device ASR only (Sherpa-ONNX in a future update).
 */
class FreedomSpeechRecognitionService : RecognitionService() {
    private val recognizer = StubSpeechRecognizer()

    override fun onStartListening(intent: Intent?, callback: Callback) {
        callback.error(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
    }

    override fun onCancel(callback: Callback) {
        recognizer.stopListening()
    }

    override fun onStopListening(callback: Callback) {
        recognizer.stopListening()
        callback.endOfSpeech()
    }

}
