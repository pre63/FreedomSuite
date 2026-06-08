package org.freedomsuite.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.freedomsuite.core.keyboard.SuggestionEngine
import org.freedomsuite.core.keyboard.speech.StubSpeechRecognizer
import org.freedomsuite.keyboard.data.KeyboardRepository

class FreedomKeyboardService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var repository: KeyboardRepository
    private val speechRecognizer = StubSpeechRecognizer()

    private var partialWord by mutableStateOf("")
    private var previousWord by mutableStateOf<String?>(null)
    private var suggestions by mutableStateOf(SuggestionEngine.Suggestions(emptyList(), emptyList(), emptyList()))
    private var shiftEnabled by mutableStateOf(false)
    private var isListening by mutableStateOf(false)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        repository = KeyboardRepository(this)
        scope.launch { repository.warmSuggestions() }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FreedomKeyboardService)
            setViewTreeViewModelStoreOwner(this@FreedomKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@FreedomKeyboardService)
            setContent {
                KeyboardScreen(
                    partialWord = partialWord,
                    suggestions = suggestions,
                    shiftEnabled = shiftEnabled,
                    isListening = isListening,
                    onSuggestion = { applySuggestion(it) },
                    onKey = { handleKey(it) },
                    onDelete = { handleDelete() },
                    onSpace = { handleSpace() },
                    onEnter = { handleEnter() },
                    onMic = { toggleDictation() },
                )
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        partialWord = ""
        previousWord = null
        shiftEnabled = false
        refreshSuggestions()
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        speechRecognizer.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun handleKey(label: String) {
        vibrate()
        val text = if (shiftEnabled) {
            shiftEnabled = false
            label.uppercase()
        } else {
            label
        }
        currentInputConnection?.commitText(text, 1)
        partialWord += text
        refreshSuggestions()
    }

    private fun handleDelete() {
        vibrate()
        if (partialWord.isNotEmpty()) {
            partialWord = partialWord.dropLast(1)
            currentInputConnection?.deleteSurroundingText(1, 0)
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        refreshSuggestions()
    }

    private fun handleSpace() {
        vibrate()
        commitCurrentWord()
        currentInputConnection?.commitText(" ", 1)
        partialWord = ""
        refreshSuggestions()
    }

    private fun handleEnter() {
        vibrate()
        commitCurrentWord()
        currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
        partialWord = ""
        refreshSuggestions()
    }

    private fun applySuggestion(word: String) {
        vibrate()
        if (partialWord.isNotEmpty()) {
            currentInputConnection?.deleteSurroundingText(partialWord.length, 0)
        }
        val output = if (shiftEnabled) {
            shiftEnabled = false
            word.replaceFirstChar { it.uppercaseChar() }
        } else {
            word
        }
        currentInputConnection?.commitText(output, 1)
        scope.launch { repository.recordCommittedWord(output, previousWord) }
        previousWord = output.lowercase()
        partialWord = ""
        refreshSuggestions()
    }

    private fun commitCurrentWord() {
        if (partialWord.isBlank()) return
        val word = partialWord
        scope.launch { repository.recordCommittedWord(word, previousWord) }
        previousWord = word.lowercase()
    }

    private fun refreshSuggestions() {
        suggestions = repository.suggestions.suggest(partialWord, previousWord)
    }

    private fun toggleDictation() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
            return
        }
        isListening = true
        scope.launch {
            speechRecognizer.startListening().collect { result ->
                if (result.isFinal) {
                    currentInputConnection?.commitText(result.text + " ", 1)
                    isListening = false
                }
            }
        }
    }

    private fun vibrate() {
        window?.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
