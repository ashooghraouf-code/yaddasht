package ir.yaddasht.app.ui.reader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class ReaderBridge {
    var onReady: (() -> Unit)? = null
    var onScroll: ((Int) -> Unit)? = null
    var onSpeechEnd: (() -> Unit)? = null
    var onHighlightClick: ((String) -> Unit)? = null

    @JavascriptInterface
    fun onReady() { onReady?.invoke() }

    @JavascriptInterface
    fun onScroll(y: Int) { onScroll?.invoke(y) }

    @JavascriptInterface
    fun onSpeechEnd() { onSpeechEnd?.invoke() }

    @JavascriptInterface
    fun onHighlightClick(id: String) { onHighlightClick?.invoke(id) }
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
