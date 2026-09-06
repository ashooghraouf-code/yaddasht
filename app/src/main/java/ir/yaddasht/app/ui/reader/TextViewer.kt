package ir.yaddasht.app.ui.reader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ir.yaddasht.app.util.Annotation
import ir.yaddasht.app.util.AnnotationPalette
import org.json.JSONArray
import org.json.JSONObject

class ReaderBridge {
    var onReady: (() -> Unit)? = null
    var onScroll: ((Int) -> Unit)? = null
    var onSpeechEnd: (() -> Unit)? = null
    var onTextSelected: ((String) -> Unit)? = null
    var onAnnotationClick: ((String) -> Unit)? = null

    @JavascriptInterface
    fun onReady() { onReady?.invoke() }

    @JavascriptInterface
    fun onScroll(y: Int) { onScroll?.invoke(y) }

    @JavascriptInterface
    fun onSpeechEnd() { onSpeechEnd?.invoke() }

    @JavascriptInterface
    fun onTextSelected(data: String) { onTextSelected?.invoke(data) }

    @JavascriptInterface
    fun onAnnotationClick(id: String, note: String, text: String) { onAnnotationClick?.invoke(id) }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TextViewer(
    bridge: ReaderBridge,
    onPageFinished: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.defaultTextEncodingName = "UTF-8"
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                setBackgroundColor(0x00000000)
                addJavascriptInterface(bridge, "Android")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onPageFinished(this@apply)
                    }
                }
                loadUrl("file:///android_asset/reader.html")
            }
        }
    )
}

fun annotationsToJs(annotations: List<Annotation>, themeIndex: Int): String {
    val arr = JSONArray()
    annotations.forEach { ann ->
        val ac = AnnotationPalette.find(ann.colorKey)
        arr.put(JSONObject().apply {
            put("id", ann.id)
            put("type", ann.type.name)
            put("startOffset", ann.startOffset)
            put("endOffset", ann.endOffset)
            put("color", ac.hex)
            put("alpha", ac.alpha(themeIndex).toDouble())
            put("note", ann.note)
            put("selectedText", ann.selectedText)
        })
    }
    return arr.toString()
}
