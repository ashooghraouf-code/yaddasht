package ir.yaddasht.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ContinuousDictation(context: Context) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    @Volatile private var active = false

    private val _committed = MutableStateFlow("")
    val committed: StateFlow<String> = _committed
    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    private fun buildIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    fun start() {
        if (active) return
        active = true
        _committed.value = ""
        _partial.value = ""
        destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { if (active) restart() }
            override fun onError(error: Int) { if (active) restart() }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    _committed.value = if (_committed.value.isBlank()) text else _committed.value + "\n" + text
                }
                _partial.value = ""
                if (active) restart()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                _partial.value = text.orEmpty()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try { r.startListening(buildIntent()) } catch (_: Exception) { active = false }
    }

    private fun restart() {
        val r = recognizer ?: return
        try { r.startListening(buildIntent()) } catch (_: Exception) {}
    }

    fun stop(): String {
        active = false
        try { recognizer?.stopListening() } catch (_: Exception) {}
        destroy()
        val p = _partial.value
        val full = if (p.isNotBlank()) (if (_committed.value.isBlank()) p else _committed.value + "\n" + p) else _committed.value
        _committed.value = ""
        _partial.value = ""
        return full
    }

    fun cancel() {
        active = false
        destroy()
        _committed.value = ""
        _partial.value = ""
    }

    private fun destroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    fun release() = cancel()
}
