package ir.yaddasht.app.ui.screen

import android.content.res.Configuration
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.ui.reader.HighlightPickDialog
import ir.yaddasht.app.ui.reader.HighlightsSheet
import ir.yaddasht.app.ui.reader.PdfViewer
import ir.yaddasht.app.ui.reader.ReaderBridge
import ir.yaddasht.app.ui.reader.ReaderSettingsSheet
import ir.yaddasht.app.ui.reader.TextViewer
import ir.yaddasht.app.ui.reader.openPdfSession
import ir.yaddasht.app.util.Highlight
import ir.yaddasht.app.util.ReaderSettings
import ir.yaddasht.app.util.ReaderStore
import ir.yaddasht.app.util.TextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.UUID

class ReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("file_path")
        val isPdf = intent.getBooleanExtra("is_pdf", false)
        if (path == null || !File(path).exists()) {
            Toast.makeText(this, "فایل پیدا نشد", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ReaderScreen(path = path, isPdf = isPdf, onBack = { finish() })
            }
        }
    }
}

private class TtsController {
    val chunks = mutableListOf<String>()
    var index = 0
    var active = false
}

private fun buildTtsChunks(text: String): List<String> {
    val paragraphs = text.split("\n").filter { it.isNotBlank() }
    val chunks = mutableListOf<String>()
    val sb = StringBuilder()
    for (p in paragraphs) {
        if (sb.isNotEmpty() && sb.length + p.length > 400) {
            chunks.add(sb.toString())
            sb.setLength(0)
        }
        if (sb.isNotEmpty()) sb.append(" ")
        sb.append(p)
    }
    if (sb.isNotEmpty()) chunks.add(sb.toString())
    return chunks
}

@Composable
private fun ReaderScreen(path: String, isPdf: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current

    val saved = remember { ReaderStore.getSettings(context) }
    var themeIndex by remember { mutableIntStateOf(saved.themeIndex) }
    var fontSize by remember { mutableIntStateOf(saved.fontSize) }
    var columns by remember {
        mutableIntStateOf(
            if (saved.columns == 2) 2
            else if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2
            else 1
        )
    }

    val savedProgress = remember { ReaderStore.getProgress(context, path) }
    var currentPage by remember { mutableIntStateOf(if (isPdf) savedProgress?.pageOrScroll ?: 0 else 0) }
    var currentScroll by remember { mutableIntStateOf(if (!isPdf) savedProgress?.pageOrScroll ?: 0 else 0) }

    val zoomLevels = listOf(1f, 1.3f, 1.6f, 2f)
    var zoomIndex by remember { mutableIntStateOf(0) }

    var showSettings by remember { mutableStateOf(false) }
    var showHighlights by remember { mutableStateOf(false) }
    var highlightsVersion by remember { mutableIntStateOf(0) }
    var pendingSelection by remember { mutableStateOf<JSONObject?>(null) }

    var speaking by remember { mutableStateOf(false) }
    val tts = remember { TtsController() }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var webReady by remember { mutableStateOf(false) }
    var fullText by remember { mutableStateOf<String?>(null) }

    val pdfSession = remember { if (isPdf) openPdfSession(path) else null }

    val barBg = listOf(Color(0xFFFFF8E1), Color(0xFFF4ECD8), Color(0xFF1A1A1A))[themeIndex]
    val barFg = listOf(Color(0xFF2C2C2C), Color(0xFF5B4636), Color(0xFFB8B8B8))[themeIndex]

    fun saveSettings() {
        ReaderStore.saveSettings(context, ReaderSettings(themeIndex, fontSize, columns))
    }

    fun pushContent(scroll: Int) {
        val wv = webView ?: return
        val txt = fullText ?: return
        val hj = ReaderStore.getHighlightsJson(context, path)
        wv.evaluateJavascript(
            "initReader(${JSONObject.quote(txt)}, $themeIndex, $fontSize, $columns, $scroll, ${JSONObject.quote(hj)});",
            null
        )
    }

    fun startTts() {
        val wv = webView ?: return
        val txt = fullText ?: return
        tts.chunks.clear()
        tts.chunks.addAll(buildTtsChunks(txt))
        if (tts.chunks.isEmpty()) return
        tts.index = 0
        tts.active = true
        speaking = true
        wv.evaluateJavascript("speak(${JSONObject.quote(tts.chunks[0])}, 1.0);", null)
    }

    fun stopTts() {
        tts.active = false
        speaking = false
        webView?.evaluateJavascript("stopSpeaking();", null)
    }

    fun advanceTts() {
        if (!tts.active) return
        tts.index++
        if (tts.index < tts.chunks.size) {
            webView?.evaluateJavascript("speak(${JSONObject.quote(tts.chunks[tts.index])}, 1.0);", null)
        } else {
            tts.active = false
            speaking = false
        }
    }

    fun requestHighlight() {
        webView?.evaluateJavascript("getSelectionData()") { result ->
            if (result != null && result != "null") {
                val raw = try { JSONTokener(result).nextValue() as? String } catch (_: Exception) { null }
                if (raw != null) {
                    val obj = try { JSONObject(raw) } catch (_: Exception) { null }
                    if (obj != null) pendingSelection = obj
                }
            }
        }
    }

    val bridge = remember { ReaderBridge() }
    DisposableEffect(Unit) {
        bridge.onScroll = { y ->
            currentScroll = y
            ReaderStore.saveProgress(context, path, y)
        }
        bridge.onSpeechEnd = { advanceTts() }
        bridge.onHighlightClick = { showHighlights = true }
        onDispose {
            bridge.onScroll = null
            bridge.onSpeechEnd = null
            bridge.onHighlightClick = null
        }
    }

    DisposableEffect(Unit) {
        onDispose { pdfSession?.close() }
    }

    LaunchedEffect(Unit) {
        if (savedProgress != null && savedProgress.pageOrScroll > 0) {
            Toast.makeText(context, "ادامه از آخرین جای مطالعه 📖", Toast.LENGTH_SHORT).show()
        }
        if (!isPdf) {
            val t = withContext(Dispatchers.Default) { TextExtractor.extract(path) }
            fullText = t
            if (t.isBlank()) {
                Toast.makeText(context, "متنی یافت نشد", Toast.LENGTH_SHORT).show()
                onBack()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isPdf && pdfSession == null) {
            Toast.makeText(context, "خطا در باز کردن PDF", Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    LaunchedEffect(webReady, fullText) {
        if (!isPdf && webReady && fullText != null) {
            pushContent(currentScroll)
        }
    }

    LaunchedEffect(currentPage) {
        if (isPdf) ReaderStore.saveProgress(context, path, currentPage)
    }

    Column(Modifier.fillMaxSize().background(barBg)) {
        // ─── نوار بالا ───
        Row(
            Modifier.fillMaxWidth().background(barBg).padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = barFg) }
            Text(
                File(path).name,
                color = barFg,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            if (isPdf && pdfSession != null) {
                Text("${(currentPage + 1)} / ${pdfSession.pageCount}", color = barFg, fontSize = 12.sp)
            }
        }
        if (isPdf && pdfSession != null) {
            LinearProgressIndicator(
                progress = { (currentPage + 1).toFloat() / pdfSession.pageCount.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Color(0xFFFFB74D),
                trackColor = Color(0xFF3A3A3A)
            )
        }

        // ─── محتوا ───
        Box(Modifier.weight(1f)) {
            if (isPdf) {
                pdfSession?.let { session ->
                    PdfViewer(
                        session = session,
                        initialPage = currentPage,
                        zoom = zoomLevels[zoomIndex],
                        twoPage = columns == 2,
                        onPageChanged = { currentPage = it }
                    )
                }
            } else {
                if (fullText != null) {
                    TextViewer(
                        bridge = bridge,
                        onPageFinished = { wv ->
                            webView = wv
                            if (!webReady) {
                                webReady = true
                            } else {
                                pushContent(currentScroll)
                            }
                        }
                    )
                }
            }
        }

        // ─── نوار پایین ───
        Row(
            Modifier.fillMaxWidth().background(barBg).padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (isPdf) {
                IconButton(onClick = { zoomIndex = (zoomIndex - 1).coerceAtLeast(0) }) {
                    Icon(Icons.Filled.Remove, "کوچک‌نمایی", tint = barFg)
                }
                IconButton(onClick = { zoomIndex = (zoomIndex + 1).coerceAtMost(zoomLevels.size - 1) }) {
                    Icon(Icons.Filled.Add, "بزرگ‌نمایی", tint = barFg)
                }
            } else {
                IconButton(onClick = { if (!speaking) startTts() }) {
                    Icon(Icons.Filled.PlayArrow, "خواندن", tint = if (speaking) barFg.copy(alpha = 0.4f) else barFg)
                }
                IconButton(onClick = { stopTts() }) {
                    Icon(Icons.Filled.Stop, "توقف", tint = if (speaking) barFg else barFg.copy(alpha = 0.4f))
                }
                IconButton(onClick = { requestHighlight() }) {
                    Icon(Icons.Filled.Brush, "هایلایت", tint = barFg)
                }
            }
            IconButton(onClick = {
                columns = if (columns == 2) 1 else 2
                saveSettings()
                if (!isPdf) webView?.evaluateJavascript("setColumns($columns);", null)
            }) {
                Icon(Icons.Filled.ViewColumn, "دو صفحه", tint = barFg)
            }
            IconButton(onClick = { showHighlights = true }) {
                Icon(Icons.Filled.List, "هایلایت‌ها", tint = barFg)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Filled.Settings, "تنظیمات", tint = barFg)
            }
        }
    }

    // ─── دیالوگ‌ها و شیت‌ها ───
    if (showSettings) {
        ReaderSettingsSheet(
            themeIndex = themeIndex,
            fontSize = fontSize,
            columns = columns,
            isPdf = isPdf,
            onTheme = { i ->
                themeIndex = i
                saveSettings()
                if (!isPdf) webView?.evaluateJavascript("setTheme($i);", null)
            },
            onFontSize = { v ->
                fontSize = v
                saveSettings()
                if (!isPdf) webView?.evaluateJavascript("setFontSize($v);", null)
            },
            onColumns = { c ->
                columns = c
                saveSettings()
                if (!isPdf) webView?.evaluateJavascript("setColumns($c);", null)
            },
            onDismiss = { showSettings = false }
        )
    }

    val highlights = remember(highlightsVersion) { ReaderStore.getHighlights(context, path) }
    if (showHighlights) {
        HighlightsSheet(
            highlights = highlights,
            onDelete = { id ->
                ReaderStore.removeHighlight(context, path, id)
                highlightsVersion++
                if (!isPdf) pushContent(currentScroll)
            },
            onDismiss = { showHighlights = false }
        )
    }

    pendingSelection?.let { obj ->
        HighlightPickDialog(
            selectedText = obj.optString("text"),
            onSave = { color, note ->
                val h = Highlight(
                    id = UUID.randomUUID().toString(),
                    text = obj.optString("text"),
                    startOffset = obj.optInt("startOffset"),
                    endOffset = obj.optInt("endOffset"),
                    color = color,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
                ReaderStore.saveHighlight(context, path, h)
                highlightsVersion++
                webView?.evaluateJavascript("clearSelection();", null)
                pushContent(currentScroll)
                pendingSelection = null
            },
            onDismiss = { pendingSelection = null }
        )
    }
}
