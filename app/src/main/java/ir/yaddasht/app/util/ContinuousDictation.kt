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

class ContinuousDictation(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    @Volatile
    private var active = false

    private val _committed = MutableStateFlow("")
    val committed: StateFlow<String> = _committed

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private fun buildIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        // تحمل سکوت بیشتر؛ با مکث کوتاه قطع نشود
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
    }

    fun start() {
        if (active) return
        active = true
        _committed.value = ""
        _partial.value = ""
        destroy()

        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (active) restart()
            }

            override fun onError(error: Int) {
                // خطاهای دائمی: متوقف شو
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                    error == SpeechRecognizer.ERROR_CLIENT
                ) {
                    active = false
                    return
                }

                // بقیه خطاها: دوباره شروع کن تا دیکته نمیرد
                if (active) restart()
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                if (!text.isNullOrBlank()) {
                    _committed.value = if (_committed.value.isBlank()) {
                        text
                    } else {
                        _committed.value + "\n" + text
                    }
                }

                _partial.value = ""
                if (active) restart()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                _partial.value = text.orEmpty()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            r.startListening(buildIntent())
        } catch (_: Exception) {
            active = false
        }
    }

    private fun restart() {
        mainHandler.postDelayed({
            if (!active) return@postDelayed

            val r = recognizer
            if (r == null) {
                active = false
                start()
                return@postDelayed
            }

            try {
                r.startListening(buildIntent())
            } catch (_: Exception) {
                // اگر یک بار نشد، بعدی امتحان می‌شود
            }
        }, 250L)
    }

    fun stop(): String {
        active = false
        mainHandler.removeCallbacksAndMessages(null)

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {}

        val p = _partial.value
        val full = if (p.isNotBlank()) {
            if (_committed.value.isBlank()) p else _committed.value + "\n" + p
        } else {
            _committed.value
        }

        destroy()
        _committed.value = ""
        _partial.value = ""
        return full
    }

    fun cancel() {
        active = false
        mainHandler.removeCallbacksAndMessages(null)
        destroy()
        _committed.value = ""
        _partial.value = ""
    }

    private fun destroy() {
        try {
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }

    fun release() = cancel()
}
