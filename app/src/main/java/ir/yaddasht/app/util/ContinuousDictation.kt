package ir.yaddasht.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class DictationState {
    object Idle : DictationState()
    object Listening : DictationState()
    object Hearing : DictationState()
    object Restarting : DictationState()
    data class Error(val message: String) : DictationState()
}

class ContinuousDictation(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    @Volatile
    private var active = false

    @Volatile
    private var restartPending = false

    private var noMatchStreak = 0

    private val _committed = MutableStateFlow("")
    val committed: StateFlow<String> = _committed

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial

    private val _state = MutableStateFlow<DictationState>(DictationState.Idle)
    val state: StateFlow<DictationState> = _state

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            noMatchStreak = 0
            if (active) _state.value = DictationState.Listening
        }

        override fun onBeginningOfSpeech() {
            if (active) _state.value = DictationState.Hearing
        }

        override fun onRmsChanged(rmsdB: Float) {
        }

        override fun onBufferReceived(buffer: ByteArray?) {
        }

        override fun onEndOfSpeech() {
            if (active) scheduleRestart(80L)
        }

        override fun onError(error: Int) {
            if (!active) return

            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    fail("دسترسی میکروفون نداری؛ دیکته متوقف شد.")
                }

                SpeechRecognizer.ERROR_CLIENT -> {
                    resetRecognizer()
                    transientRestart()
                }

                SpeechRecognizer.ERROR_NO_MATCH -> {
                    transientRestart()
                }

                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    transientRestart()
                }

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    transientRestart()
                }

                SpeechRecognizer.ERROR_AUDIO,
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_SERVER -> {
                    transientRestart()
                }

                else -> {
                    transientRestart()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            if (!active) return

            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

            if (!text.isNullOrBlank()) {
                appendFinal(text)
                noMatchStreak = 0
                _partial.value = ""
                scheduleRestart(80L)
            } else {
                _partial.value = ""
                transientRestart()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!active) return

            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

            if (!text.isNullOrBlank()) {
                _partial.value = text
                _state.value = DictationState.Hearing
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
        }
    }

    fun start() {
        if (active) return

        if (!isAvailable) {
            fail("این دستگاه سرویس گفتار فعال ندارد.")
            return
        }

        active = true
        noMatchStreak = 0
        restartPending = false
        _committed.value = ""
        _partial.value = ""
        _state.value = DictationState.Listening

        ensureRecognizer()
        beginListening()
    }

    fun stop(): String {
        val p = _partial.value
        val full = if (p.isNotBlank()) {
            if (_committed.value.isBlank()) p else _committed.value + "\n" + p
        } else {
            _committed.value
        }

        active = false
        restartPending = false
        noMatchStreak = 0
        mainHandler.removeCallbacksAndMessages(null)

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }

        resetRecognizer()

        _partial.value = ""
        _committed.value = ""
        _state.value = DictationState.Idle

        return full
    }

    fun cancel() {
        active = false
        restartPending = false
        noMatchStreak = 0
        mainHandler.removeCallbacksAndMessages(null)

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }

        resetRecognizer()

        _partial.value = ""
        _committed.value = ""
        _state.value = DictationState.Idle
    }

    fun release() = cancel()

    private fun buildIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10_000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8_000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 6_000L)
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return

        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(listener)
    }

    private fun beginListening() {
        if (!active) return

        ensureRecognizer()
        val r = recognizer ?: return

        try {
            r.startListening(buildIntent())
            _state.value = DictationState.Listening
        } catch (_: Exception) {
            resetRecognizer()
            ensureRecognizer()
            val r2 = recognizer ?: return

            try {
                r2.startListening(buildIntent())
                _state.value = DictationState.Listening
            } catch (_: Exception) {
                fail("شروع دیکته ناموفق بود. میکروفون/سرویس گفتار را بررسی کن.")
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!active || restartPending) return

        restartPending = true
        _state.value = DictationState.Restarting

        mainHandler.postDelayed({
            restartPending = false
            if (active) beginListening()
        }, delayMs.coerceAtLeast(50L))
    }

    private fun transientRestart() {
        _partial.value = ""
        noMatchStreak++
        scheduleRestart(restartDelay())
    }

    private fun restartDelay(): Long = when {
        noMatchStreak <= 1 -> 120L
        noMatchStreak <= 3 -> 800L
        noMatchStreak <= 5 -> 2_200L
        else -> 5_000L
    }

    private fun resetRecognizer() {
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    private fun appendFinal(text: String) {
        if (text.isBlank()) return

        _committed.value = if (_committed.value.isBlank()) {
            text
        } else {
            _committed.value + "\n" + text
        }
    }

    private fun fail(message: String) {
        active = false
        restartPending = false
        noMatchStreak = 0
        mainHandler.removeCallbacksAndMessages(null)
        _state.value = DictationState.Error(message)
        resetRecognizer()
    }
}
