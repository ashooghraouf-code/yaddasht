@file:OptIn(ExperimentalMaterial3Api::class)

package ir.yaddasht.app.ui.screen

import android.content.res.Configuration
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.ui.reader.PdfViewer
import ir.yaddasht.app.ui.reader.ReaderBridge
import ir.yaddasht.app.ui.reader.TextViewer
import ir.yaddasht.app.ui.reader.annotationsToJs
import ir.yaddasht.app.ui.reader.openPdfSession
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.util.Annotation
import ir.yaddasht.app.util.AnnotationColor
import ir.yaddasht.app.util.AnnotationPalette
import ir.yaddasht.app.util.AnnotationStore
import ir.yaddasht.app.util.AnnotationType
import ir.yaddasht.app.util.ReaderSettings
import ir.yaddasht.app.util.ReaderStore
import ir.yaddasht.app.util.TextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

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
    var showNoteDialog by remember { mutableStateOf<Annotation?>(null) }
    var showAnnotationsList by remember { mutableStateOf(false) }
    var annVersion by remember { mutableIntStateOf(0) }

    var pendingSelection by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }
    var pendingPdfTap by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }

    var speaking by remember { mutableStateOf(false) }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var webReady by remember { mutableStateOf(false) }
    var fullText by remember { mutableStateOf<String?>(null) }
    val pdfSession = remember { if (isPdf) openPdfSession(path) else null }

    val annotations = remember(annVersion) { AnnotationStore.all(context, path) }

    val themeBg = listOf(Color(0xFFFFF8E1), Color(0xFFF4ECD8), Color(0xFF1A1A1A))[themeIndex]
    val themeFg = listOf(Color(0xFF2C2C2C), Color(0xFF5B4636), Color(0xFFB8B8B8))[themeIndex]
    val themeMuted = listOf(Color(0xFF888888), Color(0xFF8B7355), Color(0xFF666666))[themeIndex]

    fun saveSettings() {
        ReaderStore.saveSettings(context, ReaderSettings(themeIndex, fontSize, columns))
    }

    fun pushContent(scroll: Int) {
        val wv = webView ?: return
        val txt = fullText ?: return
        val annJs = annotationsToJs(annotations, themeIndex)
        val isHtml = txt.trim().startsWith("<", ignoreCase = true)
        val escapedText = if (isHtml) txt else "<p>" + txt
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\n", "<br>") + "</p>"
        wv.evaluateJavascript(
            "initReader(${JSONObject.quote(escapedText)}, $themeIndex, $fontSize, $columns, $scroll, ${JSONObject.quote(annJs)});",
            null
        )
    }

    val bridge = remember { ReaderBridge() }
    DisposableEffect(Unit) {
        bridge.onScroll = { y ->
            currentScroll = y
            ReaderStore.saveProgress(context, path, y)
        }
        bridge.onSpeechEnd = { speaking = false }
        bridge.onTextSelected = { dataJson ->
            try {
                val obj = JSONObject(dataJson)
                pendingSelection = Triple(
                    obj.getString("text"),
                    obj.getInt("startOffset"),
                    obj.getInt("endOffset")
                )
            } catch (_: Exception) {}
        }
        bridge.onAnnotationClick = { id ->
            AnnotationStore.all(context, path).find { it.id == id }?.let { showNoteDialog = it }
        }
        onDispose {
            bridge.onScroll = null
            bridge.onSpeechEnd = null
            bridge.onTextSelected = null
            bridge.onAnnotationClick = null
        }
    }

    DisposableEffect(Unit) { onDispose { pdfSession?.close() } }

    LaunchedEffect(Unit) {
        if (savedProgress != null && savedProgress.pageOrScroll > 0) {
            Toast.makeText(context, "📖 ادامه از آخرین جای مطالعه", Toast.LENGTH_SHORT).show()
        }
        if (!isPdf) {
            val t = withContext(Dispatchers.Default) { TextExtractor.extract(path) }
            fullText = t
            if (t.isBlank()) {
                val reason = TextExtractor.lastError ?: "دلیل ناشناخته"
                Toast.makeText(context, "❌ $reason", Toast.LENGTH_LONG).show()
                onBack()
            }
        }
    }

    LaunchedEffect(isPdf) {
        if (isPdf && pdfSession == null) {
            Toast.makeText(context, "خطا در باز کردن PDF", Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    LaunchedEffect(webReady, fullText) {
        if (!isPdf && webReady && fullText != null) pushContent(currentScroll)
    }

    LaunchedEffect(annVersion, themeIndex) {
        if (!isPdf && webReady && fullText != null) pushContent(currentScroll)
    }

    LaunchedEffect(currentPage) {
        if (isPdf) ReaderStore.saveProgress(context, path, currentPage)
    }

    Column(Modifier.fillMaxSize().background(themeBg)) {

        // Top Bar
        Row(
            Modifier.fillMaxWidth().background(themeBg).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showAnnotationsList = true }) {
                Icon(Icons.Filled.Menu, "یادداشت‌ها", tint = themeFg)
            }
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = themeFg)
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    File(path).name,
                    color = themeFg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${annotations.size} یادداشت • ${if (isPdf) "صفحهٔ ${currentPage + 1}" else "قلم $fontSize"}",
                    color = themeMuted,
                    fontSize = 11.sp
                )
            }
            if (isPdf && pdfSession != null) {
                Text(
                    "${currentPage + 1}/${pdfSession.pageCount}",
                    color = themeFg,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
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

        // Content
        Box(Modifier.weight(1f)) {
            if (isPdf) {
                pdfSession?.let { session ->
                    PdfViewer(
                        session = session,
                        initialPage = currentPage,
                        zoom = zoomLevels[zoomIndex],
                        twoPage = columns == 2,
                        themeIndex = themeIndex,
                        annotations = annotations,
                        onPageChanged = { currentPage = it },
                        onPageTap = { page, rx, ry -> pendingPdfTap = Triple(page, rx, ry) }
                    )
                }
            } else {
                if (fullText != null) {
                    TextViewer(
                        bridge = bridge,
                        onPageFinished = { wv ->
                            webView = wv
                            if (!webReady) webReady = true
                            else pushContent(currentScroll)
                        }
                    )
                }
            }
        }

        // Bottom Toolbar
        Row(
            Modifier.fillMaxWidth().background(themeBg).padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (isPdf) {
                    Toast.makeText(context, "📍 روی محل دلخواه ضربه بزن", Toast.LENGTH_SHORT).show()
                } else {
                    webView?.evaluateJavascript("getSelectionData()") { result ->
                        if (result != null && result != "null" && result != "\"null\"") {
                            try {
                                val raw = result.trim('"').replace("\\\"", "\"").replace("\\\\", "\\")
                                val obj = JSONObject(raw)
                                pendingSelection = Triple(
                                    obj.getString("text"),
                                    obj.getInt("startOffset"),
                                    obj.getInt("endOffset")
                                )
                            } catch (_: Exception) {
                                Toast.makeText(context, "اول متنی را انتخاب کن ✋", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "اول متنی را انتخاب کن ✋", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Brush, "هایلایت", tint = themeFg)
                    Text("هایلایت", fontSize = 9.sp, color = themeFg)
                }
            }
            IconButton(onClick = {
                if (isPdf) {
                    AnnotationStore.save(
                        context, path, Annotation(
                            fileKey = AnnotationStore.fileKey(path),
                            type = AnnotationType.BOOKMARK,
                            colorKey = "gold",
                            pageIndex = currentPage,
                            relX = 0.02f, relY = 0.02f, relW = 0.06f, relH = 0.06f,
                            selectedText = "صفحهٔ ${currentPage + 1}"
                        )
                    )
                    annVersion++
                    Toast.makeText(context, "🔖 نشانک ثبت شد", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "برای متن، از انتخاب استفاده کن", Toast.LENGTH_SHORT).show()
                }
            }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Bookmark, "نشانک", tint = themeFg)
                    Text("نشانک", fontSize = 9.sp, color = themeFg)
                }
            }
            if (isPdf) {
                IconButton(onClick = { zoomIndex = (zoomIndex - 1).coerceAtLeast(0) }) {
                    Icon(Icons.Filled.Remove, "کوچک", tint = themeFg)
                }
                IconButton(onClick = { zoomIndex = (zoomIndex + 1).coerceAtMost(zoomLevels.size - 1) }) {
                    Icon(Icons.Filled.Add, "بزرگ", tint = themeFg)
                }
            } else {
                IconButton(onClick = {
                    if (!speaking && fullText != null) {
                        val plain = fullText!!.replace(Regex("<[^>]+>"), "")
                        webView?.evaluateJavascript("speak(${JSONObject.quote(plain.take(4000))}, 1.0);", null)
                        speaking = true
                    } else {
                        webView?.evaluateJavascript("stopSpeaking();", null)
                        speaking = false
                    }
                }) {
                    Icon(if (speaking) Icons.Filled.Stop else Icons.Filled.PlayArrow, "خواندن", tint = themeFg)
                }
            }
            IconButton(onClick = {
                columns = if (columns == 2) 1 else 2
                saveSettings()
                if (!isPdf) webView?.evaluateJavascript("setColumns($columns);", null)
            }) {
                Icon(Icons.Filled.ViewColumn, "دو صفحه", tint = themeFg)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Filled.Settings, "تنظیمات", tint = themeFg)
            }
        }
    }

    // Dialogs
    pendingSelection?.let { sel ->
        AnnotationPickerDialog(
            selectedText = sel.first,
            onSave = { colorKey, type, note ->
                AnnotationStore.save(
                    context, path, Annotation(
                        fileKey = AnnotationStore.fileKey(path),
                        type = type,
                        colorKey = colorKey,
                        selectedText = sel.first,
                        note = note,
                        startOffset = sel.second,
                        endOffset = sel.third
                    )
                )
                annVersion++
                webView?.evaluateJavascript("clearSelection();", null)
                pendingSelection = null
            },
            onDismiss = { pendingSelection = null }
        )
    }

    pendingPdfTap?.let { tap ->
        val (page, rx, ry) = tap
        PdfAnnotationDialog(
            pageIndex = page,
            relX = rx,
            relY = ry,
            onSave = { type, colorKey, note ->
                val (rw, rh) = when (type) {
                    AnnotationType.HIGHLIGHT -> 0.40f to 0.03f
                    AnnotationType.UNDERLINE -> 0.40f to 0.012f
                    AnnotationType.NOTE -> 0.06f to 0.06f
                    AnnotationType.BOOKMARK -> 0.06f to 0.06f
                }
                AnnotationStore.save(
                    context, path, Annotation(
                        fileKey = AnnotationStore.fileKey(path),
                        type = type,
                        colorKey = colorKey,
                        note = note,
                        pageIndex = page,
                        relX = rx, relY = ry, relW = rw, relH = rh,
                        selectedText = "صفحهٔ ${page + 1}"
                    )
                )
                annVersion++
                pendingPdfTap = null
            },
            onDismiss = { pendingPdfTap = null }
        )
    }

    showNoteDialog?.let { ann ->
        AnnotationViewDialog(
            annotation = ann,
            onEdit = { newNote ->
                AnnotationStore.updateNote(context, path, ann.id, newNote)
                annVersion++
                showNoteDialog = null
            },
            onDelete = {
                AnnotationStore.remove(context, path, ann.id)
                annVersion++
                showNoteDialog = null
            },
            onDismiss = { showNoteDialog = null }
        )
    }

    if (showAnnotationsList) {
        AnnotationsListDialog(
            annotations = annotations,
            themeFg = themeFg,
            themeMuted = themeMuted,
            onClick = { ann ->
                showAnnotationsList = false
                showNoteDialog = ann
            },
            onDelete = { id ->
                AnnotationStore.remove(context, path, id)
                annVersion++
            },
            onDismiss = { showAnnotationsList = false }
        )
    }

    if (showSettings) {
        ReaderSettingsDialog(
            themeIndex = themeIndex,
            fontSize = fontSize,
            columns = columns,
            isPdf = isPdf,
            onTheme = { i ->
                themeIndex = i
                saveSettings()
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
}

@Composable
private fun AnnotationsListDialog(
    annotations: List<Annotation>,
    themeFg: Color,
    themeMuted: Color,
    onClick: (Annotation) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("📑 همهٔ یادداشت‌ها", color = Color.White, fontFamily = LalezarFont, fontSize = 18.sp) },
        text = {
            if (annotations.isEmpty()) {
                Text("هنوز یادداشتی نیست 📝", color = themeMuted, fontSize = 13.sp)
            } else {
                LazyColumn {
                    items(annotations.sortedByDescending { it.updatedAt }) { ann ->
                        val color = AnnotationPalette.find(ann.colorKey)
                        Card(
                            onClick = { onClick(ann) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = color.color.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(color.emoji, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ann.selectedText.ifBlank { "صفحهٔ ${ann.pageIndex + 1}" },
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (ann.note.isNotBlank()) {
                                        Text(ann.note, color = themeMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                IconButton(onClick = { onDelete(ann.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Close, "حذف", tint = Color(0xFFE57373), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن", color = Color(0xFFFFB74D)) } }
    )
}

@Composable
private fun BottomToolbar(
    isPdf: Boolean,
    speaking: Boolean,
    themeBg: Color,
    themeFg: Color,
    onAddNote: () -> Unit,
    onAddBookmark: () -> Unit,
    onTts: () -> Unit,
    onColumns: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onSettings: () -> Unit
) {}

@Composable
private fun ColorRow(selected: String, list: List<AnnotationColor>, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
        list.forEach { c ->
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(c.color)
                    .border(if (selected == c.key) 3.dp else 0.dp, Color.White, CircleShape)
                    .clickable { onSelect(c.key) }
            )
        }
    }
}

@Composable
private fun TypeRow(selected: AnnotationType, onSelect: (AnnotationType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        AnnotationType.entries.forEach { type ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected == type) Color(0xFFFFB74D) else Color(0xFF404040))
                    .clickable { onSelect(type) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(type.emoji, fontSize = 16.sp)
                Text(type.faName, fontSize = 9.sp, color = if (selected == type) Color.Black else Color.White)
            }
        }
    }
}

@Composable
private fun AnnotationPickerDialog(
    selectedText: String,
    onSave: (colorKey: String, AnnotationType, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf("gold") }
    var selectedType by remember { mutableStateOf(AnnotationType.HIGHLIGHT) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("🎨 افزودن یادداشت", color = Color.White, fontFamily = LalezarFont, fontSize = 18.sp) },
        text = {
            Column {
                Text(
                    "«${selectedText.take(80)}${if (selectedText.length > 80) "…" else ""}»",
                    color = Color(0xFFB8B8B8), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Text("نوع:", color = Color(0xFF999999), fontSize = 12.sp)
                TypeRow(selectedType) { selectedType = it }
                Text("رنگ:", color = Color(0xFF999999), fontSize = 12.sp)
                ColorRow(selectedColor, AnnotationPalette.colors.take(5)) { selectedColor = it }
                ColorRow(selectedColor, AnnotationPalette.colors.drop(5)) { selectedColor = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)", color = Color(0xFF999999)) },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selectedColor, selectedType, note) }) { Text("ذخیره", color = Color(0xFFFFB74D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = Color(0xFF999999)) } }
    )
}

@Composable
private fun PdfAnnotationDialog(
    pageIndex: Int,
    relX: Float,
    relY: Float,
    onSave: (AnnotationType, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf("gold") }
    var selectedType by remember { mutableStateOf(AnnotationType.NOTE) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("📍 صفحهٔ ${pageIndex + 1}", color = Color.White, fontFamily = LalezarFont, fontSize = 18.sp) },
        text = {
            Column {
                Text("نوع:", color = Color(0xFF999999), fontSize = 12.sp)
                TypeRow(selectedType) { selectedType = it }
                Text("رنگ:", color = Color(0xFF999999), fontSize = 12.sp)
                ColorRow(selectedColor, AnnotationPalette.colors.take(5)) { selectedColor = it }
                ColorRow(selectedColor, AnnotationPalette.colors.drop(5)) { selectedColor = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)", color = Color(0xFF999999)) },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selectedType, selectedColor, note) }) { Text("ذخیره", color = Color(0xFFFFB74D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = Color(0xFF999999)) } }
    )
}

@Composable
private fun AnnotationViewDialog(
    annotation: Annotation,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember(annotation.id) { mutableStateOf(annotation.note) }
    var editing by remember(annotation.id) { mutableStateOf(annotation.note.isBlank()) }
    val c = AnnotationPalette.find(annotation.colorKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("${c.name} • ${annotation.type.faName}", fontFamily = LalezarFont, fontSize = 16.sp, color = Color.White)
            }
        },
        text = {
            Column {
                if (annotation.selectedText.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = c.color.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "«${annotation.selectedText}»",
                            color = Color.White, fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 4, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (editing) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("یادداشت", color = Color(0xFF999999)) },
                        minLines = 2, maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(annotation.note.ifBlank { "بدون یادداشت" }, color = Color(0xFFB8B8B8), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ویرایش", color = Color(0xFFFFB74D))
                    }
                }
            }
        },
        confirmButton = {
            if (editing) TextButton(onClick = { onEdit(note) }) { Text("ذخیره", color = Color(0xFFFFB74D)) }
            else TextButton(onClick = onDismiss) { Text("بستن", color = Color(0xFFFFB74D)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("حذف", color = Color(0xFFE57373)) }
                if (editing) TextButton(onClick = onDismiss) { Text("انصراف", color = Color(0xFF999999)) }
            }
        }
    )
}

@Composable
private fun ReaderSettingsDialog(
    themeIndex: Int,
    fontSize: Int,
    columns: Int,
    isPdf: Boolean,
    onTheme: (Int) -> Unit,
    onFontSize: (Int) -> Unit,
    onColumns: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("⚙️ تنظیمات", color = Color.White, fontFamily = LalezarFont, fontSize = 18.sp) },
        text = {
            Column {
                Text("تم صفحه", color = Color(0xFFB8B8B8), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple(0, "روشن ☀️", Color(0xFFFFF8E1)),
                        Triple(1, "سپیا 📜", Color(0xFFF4ECD8)),
                        Triple(2, "شب 🌙", Color(0xFF1A1A1A))
                    ).forEach { (i, name, color) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (themeIndex == i) 3.dp else 1.dp,
                                        if (themeIndex == i) Color(0xFFFFB74D) else Color.Gray,
                                        CircleShape
                                    )
                                    .clickable { onTheme(i) }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(name, color = Color(0xFFB8B8B8), fontSize = 10.sp)
                        }
                    }
                }
                if (!isPdf) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("اندازه قلم", color = Color(0xFFB8B8B8), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onFontSize((fontSize - 2).coerceAtLeast(12)) }) { Text("−", color = Color.White, fontSize = 22.sp) }
                        Text("$fontSize", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { onFontSize((fontSize + 2).coerceAtMost(28)) }) { Text("+", color = Color.White, fontSize = 22.sp) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("حالت کتاب", color = Color(0xFFB8B8B8), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onColumns(if (columns == 2) 1 else 2) }) {
                        Text(if (columns == 2) "✅ فعال" else "❌ غیرفعال", color = Color(0xFFFFB74D))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن", color = Color(0xFFFFB74D)) } }
    )
}
