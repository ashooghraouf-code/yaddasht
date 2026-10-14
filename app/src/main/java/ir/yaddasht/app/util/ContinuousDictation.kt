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
    private var watchdog: Runnable? = null

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
            cancelWatchdog()
            if (active) {
                _state.value = DictationState.Listening
            }
        }

        override fun onBeginningOfSpeech() {
            if (active) {
                _state.value = DictationState.Hearing
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
        }

        override fun onBufferReceived(buffer: ByteArray?) {
        }

        override fun onEndOfSpeech() {
            if (active) {
                scheduleRestart(120L)
            }
        }

        override fun onError(error: Int) {
            if (!active) return

            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    fail("دسترسی میکروفون نداری؛ دیکته متوقف شد.")
                }

                else -> {
                    resetRecognizer()
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
                resetRecognizer()
                scheduleRestart(120L)
            } else {
                _partial.value = ""
                resetRecognizer()
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

        cancelWatchdog()
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

        cancelWatchdog()
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

    fun release() {
        cancel()
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }

    private fun beginListening() {
        if (!active) return

        resetRecognizer()

        val r = try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (_: Exception) {
            fail("ساخت موتور گفتار ناموفق بود.")
            return
        }

        recognizer = r
        r.setRecognitionListener(listener)

        try {
            r.startListening(buildIntent())
            _state.value = DictationState.Listening
            armWatchdog()
        } catch (_: Exception) {
            resetRecognizer()

            val r2 = try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (_: Exception) {
                fail("شروع دیکته ناموفق بود. میکروفون/سرویس گفتار را بررسی کن.")
                return
            }

            recognizer = r2
            r2.setRecognitionListener(listener)

            try {
                r2.startListening(buildIntent())
                _state.value = DictationState.Listening
                armWatchdog()
            } catch (_: Exception) {
                fail("شروع دیکته ناموفق بود. میکروفون/سرویس گفتار را بررسی کن.")
            }
        }
    }

    private fun armWatchdog() {
        cancelWatchdog()

        val runnable = Runnable {
            if (active && !restartPending) {
                resetRecognizer()
                scheduleRestart(250L)
            }
        }

        watchdog = runnable
        mainHandler.postDelayed(runnable, 3500L)
    }

    private fun cancelWatchdog() {
        watchdog?.let { mainHandler.removeCallbacks(it) }
        watchdog = null
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!active || restartPending) return

        restartPending = true
        _state.value = DictationState.Restarting

        mainHandler.postDelayed({
            restartPending = false
            if (active) {
                beginListening()
            }
        }, delayMs.coerceAtLeast(80L))
    }

    private fun transientRestart() {
        _partial.value = ""
        noMatchStreak++

        if (noMatchStreak >= 15) {
            fail("سرویس گفتار پاسخ نداد؛ چند بار تلاش کرد و متوقف شد.")
            return
        }

        scheduleRestart(restartDelay())
    }

    private fun restartDelay(): Long = when {
        noMatchStreak <= 1 -> 150L
        noMatchStreak <= 3 -> 500L
        noMatchStreak <= 5 -> 1200L
        noMatchStreak <= 8 -> 2200L
        else -> 3500L
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

        cancelWatchdog()
        mainHandler.removeCallbacksAndMessages(null)

        _state.value = DictationState.Error(message)
        resetRecognizer()
    }
}
// ✅ END OF ContinuousDictation.kt
