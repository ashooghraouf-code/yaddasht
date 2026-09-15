@file:OptIn(ExperimentalFoundationApi::class)

package ir.yaddasht.app.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import coil.compose.AsyncImage
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.VazirFont
import ir.yaddasht.app.util.BoardImage
import ir.yaddasht.app.util.BoardItem
import ir.yaddasht.app.util.BoardStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

private fun boardBase(index: Int): androidx.compose.ui.graphics.Color = listOf(
    androidx.compose.ui.graphics.Color(0xFFA1887F),
    androidx.compose.ui.graphics.Color(0xFF6D4C41),
    androidx.compose.ui.graphics.Color(0xFF263238),
    androidx.compose.ui.graphics.Color(0xFFECEFF1)
)[index.coerceIn(0, 3)]

private fun stickyBody(index: Int): androidx.compose.ui.graphics.Color = listOf(
    androidx.compose.ui.graphics.Color(0xFFFFF59D),
    androidx.compose.ui.graphics.Color(0xFFF8BBD0),
    androidx.compose.ui.graphics.Color(0xFFB3E5FC),
    androidx.compose.ui.graphics.Color(0xFFC8E6C9),
    androidx.compose.ui.graphics.Color(0xFFFFE0B2),
    androidx.compose.ui.graphics.Color(0xFFE1BEE7)
)[index.coerceIn(0, 5)]

private fun stickyEdge(index: Int): androidx.compose.ui.graphics.Color = stickyBody(index).copy(alpha = .55f)

private fun pinColor(index: Int): androidx.compose.ui.graphics.Color = listOf(
    androidx.compose.ui.graphics.Color(0xFFE53935),
    androidx.compose.ui.graphics.Color(0xFF1E88E5),
    androidx.compose.ui.graphics.Color(0xFF43A047),
    androidx.compose.ui.graphics.Color(0xFFFDD835),
    androidx.compose.ui.graphics.Color(0xFF8E24AA),
    androidx.compose.ui.graphics.Color(0xFFFB8C00)
)[index.coerceIn(0, 5)]

private fun corkDotA(index: Int): androidx.compose.ui.graphics.Color = when (index) {
    2 -> androidx.compose.ui.graphics.Color.White.copy(alpha = 0.07f)
    3 -> androidx.compose.ui.graphics.Color(0xFF8D6E63).copy(alpha = 0.12f)
    else -> androidx.compose.ui.graphics.Color(0xFF5D4037).copy(alpha = 0.30f)
}

private fun corkDotB(index: Int): androidx.compose.ui.graphics.Color = when (index) {
    2 -> androidx.compose.ui.graphics.Color.White.copy(alpha = 0.03f)
    3 -> androidx.compose.ui.graphics.Color(0xFFD7CCC8).copy(alpha = 0.30f)
    else -> androidx.compose.ui.graphics.Color(0xFFD7CCC8).copy(alpha = 0.24f)
}

private val BOARD_SIZE_LABELS = listOf("📱 گوشی", "📄 A4", "📐 A3", "🗺️ A2")
private val BOARD_SIZES_DP = listOf(
    Pair(0f, 0f),
    Pair(595f, 842f),
    Pair(842f, 1191f),
    Pair(1191f, 1684f)
)

private val BASE_NOTE_WIDTH = 150f
private val BASE_IMAGE_WIDTH = 150f

private fun safeTypeface(context: Context, name: String, bold: Boolean): Typeface {
    val id = context.resources.getIdentifier(name, "font", context.packageName)
    return if (id != 0) {
        try {
            ResourcesCompat.getFont(context, id) ?: if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        } catch (_: Exception) {
            if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    } else {
        if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}

@Composable
fun BoardScreen(notes: List<Note>, noteDao: NoteDao, onOpenNote: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var boards by remember { mutableStateOf(BoardStore.boards(context)) }
    var currentBoard by remember { mutableStateOf(boards.firstOrNull()?.id ?: 1L) }
    var items by remember { mutableStateOf(BoardStore.items(context, currentBoard)) }
    var images by remember { mutableStateOf(BoardStore.images(context, currentBoard)) }

    var showAddBoard by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var boardName by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var draggingImageId by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var fingerX by remember { mutableFloatStateOf(0f) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    var boardAreaHeightDp by remember { mutableFloatStateOf(0f) }
    var boardPxW by remember { mutableIntStateOf(0) }
    var boardPxH by remember { mutableIntStateOf(0) }
    var vpW by remember { mutableIntStateOf(0) }
    var vpH by remember { mutableIntStateOf(0) }

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var imageToDelete by remember { mutableStateOf<BoardImage?>(null) }

    val refresh: () -> Unit = {
        boards = BoardStore.boards(context)
        items = BoardStore.items(context, currentBoard)
        images = BoardStore.images(context, currentBoard)
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val dir = File(context.filesDir, "board_images")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "img-${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out -> inputStream.copyTo(out) }
                inputStream.close()
                BoardStore.addImage(context, currentBoard, Uri.fromFile(file).toString())
                refresh()
                Toast.makeText(context, "🖼️ تصویر اضافه شد", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "خطا: نمی‌توان تصویر را خواند", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val currentBoardData = boards.firstOrNull { it.id == currentBoard }
    val bgIndex = currentBoardData?.background ?: 0
    val boardSizeIndex = currentBoardData?.sizeIndex ?: 0
    val isPhoneSize = boardSizeIndex == 0
    val boardWidthDp = if (isPhoneSize) with(density) { boardPxW.toDp().value } else BOARD_SIZES_DP[boardSizeIndex].first
    val boardHeightDpActual = if (isPhoneSize) with(density) { boardPxH.toDp().value } else BOARD_SIZES_DP[boardSizeIndex].second

    val clampX = (if (boardWidthDp > 60f) boardWidthDp - 60f else 300f).coerceAtLeast(0f)
    val clampY = (if (boardHeightDpActual > 60f) boardHeightDpActual - 60f else 400f).coerceAtLeast(0f)

    val visibleItems = remember(items, searchQuery, notes) {
        if (searchQuery.isBlank()) items
        else items.filter { item ->
            val n = notes.firstOrNull { it.id == item.noteId } ?: return@filter false
            n.title.contains(searchQuery, true) || n.body.contains(searchQuery, true)
        }
    }

    // ✅ سطح حساس سطل زباله کاهش یافت: ۱۵۰dp → ۷۰dp
    val trashTopDp = if (boardAreaHeightDp > 1f) boardAreaHeightDp - 70f else Float.MAX_VALUE
    val isOverTrash = (draggingNoteId != null || draggingImageId != null) && dragY > trashTopDp

    fun exportToPdf() {
        if (isExporting) return
        isExporting = true
        scope.launch(Dispatchers.IO) {
            try {
                val d = density.density
                val fontScale = context.resources.configuration.fontScale
                val spPx = d * fontScale
                val pxW = (if (boardPxW > 0) boardPxW else boardWidthDp * d).toInt().coerceAtLeast(1)
                val pxH = (if (boardPxH > 0) boardPxH else boardHeightDpActual * d).toInt().coerceAtLeast(1)
                val dir = File(context.cacheDir, "board_exports")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "board-${System.currentTimeMillis()}.pdf")

                val titleType = safeTypeface(context, "lalezar", true)
                val bodyType = safeTypeface(context, "vazir", false)

                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(pxW, pxH, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(boardBase(bgIndex).toArgb())

                val titlePaint = TextPaint().apply {
                    color = androidx.compose.ui.graphics.Color(0xFF3E2723).toArgb()
                    textSize = 15f * spPx
                    isAntiAlias = true
                    typeface = titleType
                }
                val bodyPaint = TextPaint().apply {
                    color = androidx.compose.ui.graphics.Color(0xFF5D4037).toArgb()
                    textSize = 11f * spPx
                    isAntiAlias = true
                    typeface = bodyType
                }

                // ✅ محاسبهٔ lineSpacing برای انطباق دقیق با صفحه (lineHeight = 17sp)
                val targetLineHeight = 17f * spPx
                val fm = bodyPaint.fontMetrics
                val naturalLineHeight = fm.descent - fm.ascent + fm.leading
                val extraLineSpacing = (targetLineHeight - naturalLineHeight).coerceAtLeast(0f)

                val bgPaint = Paint().apply { isAntiAlias = true }
                val framePaint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE }

                items.forEach { item ->
                    val note = notes.firstOrNull { it.id == item.noteId } ?: return@forEach
                    val scale = item.scale.coerceIn(0.3f, 3.0f)
                    val wPx = BASE_NOTE_WIDTH * scale * d
                    val padPx = 12f * d
                    val topPadPx = (if (item.noteId.hashCode() and 1 == 0) 20f else 14f) * d
                    val bottomPadPx = 16f * d
                    val innerW = (wPx - 2f * padPx).toInt().coerceAtLeast(1)

                    val titleText = note.title.ifBlank { "بدون عنوان" }
                    val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, innerW)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setMaxLines(1)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build()

                    val bodyBuilder = StaticLayout.Builder.obtain(note.body, 0, note.body.length, bodyPaint, innerW)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setMaxLines(5)
                        .setEllipsize(TextUtils.TruncateAt.END)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bodyBuilder.setLineSpacing(extraLineSpacing, 1f)
                    }
                    val bodyLayout = bodyBuilder.build()

                    val hPx = topPadPx + titleLayout.height + bodyLayout.height + bottomPadPx

                    val cx = item.x.coerceIn(0f, clampX) * d + wPx / 2f
                    val cy = item.y.coerceIn(0f, clampY) * d + hPx / 2f

                    canvas.save()
                    canvas.translate(cx, cy)
                    canvas.rotate(item.rotation)
                    canvas.translate(-wPx / 2f, -hPx / 2f)

                    bgPaint.color = stickyBody(note.color).toArgb()
                    canvas.drawRoundRect(android.graphics.RectF(0f, 0f, wPx, hPx), 3f * d, 3f * d, bgPaint)

                    canvas.save()
                    canvas.translate(padPx, topPadPx)
                    titleLayout.draw(canvas)
                    canvas.restore()

                    canvas.save()
                    canvas.translate(padPx, topPadPx + titleLayout.height)
                    bodyLayout.draw(canvas)
                    canvas.restore()

                    canvas.restore()
                }

                images.forEach { img ->
                    val bitmap = loadBitmapFromUri(context, img.uri) ?: return@forEach
                    val scale = img.scale.coerceIn(0.3f, 3.0f)
                    val frame = 4f * d
                    val imgW = (BASE_IMAGE_WIDTH * scale - 8f) * d
                    val imgH = imgW * bitmap.height / bitmap.width.toFloat()
                    val totalW = imgW + 2f * frame
                    val totalH = imgH + 2f * frame

                    val cx = img.x.coerceIn(0f, clampX) * d + totalW / 2f
                    val cy = img.y.coerceIn(0f, clampY) * d + totalH / 2f

                    canvas.save()
                    canvas.translate(cx, cy)
                    canvas.rotate(img.rotation)
                    canvas.translate(-totalW / 2f, -totalH / 2f)

                    canvas.drawRoundRect(android.graphics.RectF(0f, 0f, totalW, totalH), 6f * d, 6f * d, framePaint)
                    canvas.drawBitmap(bitmap, null, android.graphics.RectF(frame, frame, frame + imgW, frame + imgH), null)

                    canvas.restore()
                    bitmap.recycle()
                }

                pdfDocument.finishPage(page)
                FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    isExporting = false
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "تابلوی ${currentBoardData?.name ?: ""}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری PDF تابلو"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isExporting = false
                    Toast.makeText(context, "خطا در ساخت PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(boardBase(bgIndex))) {
            if (!isExporting) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .28f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = androidx.compose.ui.graphics.Color(0xFFFFE0B2))
                    }

                    boards.forEach { b ->
                        val selected = b.id == currentBoard
                        Surface(
                            onClick = { currentBoard = b.id; refresh(); searchQuery = "" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) androidx.compose.ui.graphics.Color(0xFFFFB74D) else androidx.compose.ui.graphics.Color.White.copy(alpha = .12f),
                            shadowElevation = if (selected) 6.dp else 0.dp
                        ) {
                            Text(
                                b.name,
                                color = if (selected) androidx.compose.ui.graphics.Color(0xFF3E2723) else androidx.compose.ui.graphics.Color(0xFFFFE0B2),
                                fontFamily = LalezarFont, fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Surface(shape = RoundedCornerShape(8.dp), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)) {
                        Text(
                            "📝${items.size} 🖼${images.size}",
                            fontSize = 11.sp,
                            color = androidx.compose.ui.graphics.Color(0xFFFFE0B2),
                            fontFamily = VazirFont,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                        shape = RoundedCornerShape(12.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFFB8C00).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFFB8C00).copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Image, "افزودن تصویر", tint = androidx.compose.ui.graphics.Color(0xFFFFE0B2), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("عکس", color = androidx.compose.ui.graphics.Color(0xFFFFE0B2), fontSize = 12.sp, fontFamily = VazirFont)
                        }
                    }

                    Surface(
                        onClick = { showSearch = !showSearch },
                        shape = RoundedCornerShape(12.dp),
                        color = if (showSearch) androidx.compose.ui.graphics.Color(0xFFFFB74D).copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (showSearch) Icons.Filled.SearchOff else Icons.Filled.Search, "جستجو", tint = androidx.compose.ui.graphics.Color(0xFFFFE0B2), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("جستجو", color = androidx.compose.ui.graphics.Color(0xFFFFE0B2), fontSize = 12.sp, fontFamily = VazirFont)
                        }
                    }

                    Surface(
                        onClick = { exportToPdf() },
                        shape = RoundedCornerShape(12.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFE53935).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE53935).copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PictureAsPdf, "خروجی PDF", tint = androidx.compose.ui.graphics.Color(0xFFFFCDD2), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF", color = androidx.compose.ui.graphics.Color(0xFFFFCDD2), fontSize = 12.sp, fontFamily = VazirFont)
                        }
                    }

                    IconButton(onClick = { BoardStore.setBackground(context, currentBoard, (bgIndex + 1) % 4); refresh() }) {
                        Icon(Icons.Filled.Palette, "تغییر پس‌زمینه", tint = androidx.compose.ui.graphics.Color(0xFFFFE0B2))
                    }
                    Surface(onClick = { BoardStore.setBoardSize(context, currentBoard, (boardSizeIndex + 1) % 4); refresh() }, shape = RoundedCornerShape(8.dp), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)) {
                        Text(BOARD_SIZE_LABELS[boardSizeIndex], fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFFFFE0B2), fontFamily = VazirFont, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    IconButton(onClick = { boardName = ""; showAddBoard = true }) {
                        Icon(Icons.Filled.Add, "تابلو جدید", tint = androidx.compose.ui.graphics.Color(0xFFFFE0B2))
                    }
                }

                if (showSearch) {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = .95f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, "جستجو", tint = androidx.compose.ui.graphics.Color(0xFF5D4037))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(fontFamily = VazirFont, fontSize = 14.sp, color = androidx.compose.ui.graphics.Color(0xFF3E2723)),
                            singleLine = true,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text("جستجو در یادداشت‌ها...", color = androidx.compose.ui.graphics.Color(0xFF8D6E63), fontSize = 14.sp, fontFamily = VazirFont)
                                    }
                                    inner()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, "پاک", tint = androidx.compose.ui.graphics.Color(0xFF5D4037), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxSize().onSizeChanged { s -> boardAreaHeightDp = with(density) { s.height.toDp().value } }) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val scrollStateV = rememberScrollState()
                    val scrollStateH = rememberScrollState()

                    Box(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { s -> vpW = s.width; vpH = s.height }
                            .then(if (!isPhoneSize) Modifier.verticalScroll(scrollStateV).horizontalScroll(scrollStateH) else Modifier)
                    ) {
                        Box(
                            Modifier
                                .then(if (isPhoneSize) Modifier.fillMaxSize() else Modifier.width(boardWidthDp.dp).height(boardHeightDpActual.dp))
                                .onSizeChanged { s -> boardPxW = s.width; boardPxH = s.height }
                        ) {
                            CorkTexture(bgIndex)
                            Vignette(bgIndex)

                            if (visibleItems.isEmpty() && images.isEmpty() && !isExporting) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (searchQuery.isNotBlank()) "🔍" else "🗒️", fontSize = 64.sp, modifier = Modifier.rotate(if (searchQuery.isNotBlank()) 0f else -6f))
                                        Text(if (searchQuery.isNotBlank()) "یادداشتی یافت نشد" else "تابلو خالی است", fontFamily = LalezarFont, fontSize = 22.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .85f))
                                        Text("با دکمهٔ + یادداشت بچسبانید یا با دکمهٔ «عکس» تصویر اضافه کنید", fontFamily = VazirFont, fontSize = 13.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                                    }
                                }
                            }

                            visibleItems.forEachIndexed { idx, item ->
                                val note = notes.firstOrNull { it.id == item.noteId }
                                if (note != null) {
                                    StickyNote(
                                        note = note,
                                        item = item,
                                        variant = idx,
                                        stagger = idx,
                                        clampX = clampX,
                                        clampY = clampY,
                                        isDraggingThis = draggingNoteId == note.id,
                                        isOverTrash = isOverTrash && draggingNoteId == note.id,
                                        onOpen = { onOpenNote(note.id) },
                                        onMoved = { x, y -> BoardStore.move(context, note.id, currentBoard, x, y); refresh() },
                                        onRotated = { rot -> BoardStore.rotate(context, note.id, currentBoard, rot); refresh() },
                                        onScaleChanged = { scale -> BoardStore.setScale(context, note.id, currentBoard, scale); refresh() },
                                        onDragStart = { draggingNoteId = note.id },
                                        onDragUpdate = { x, y -> dragY = y; fingerX = x; fingerY = y },
                                        onDragEnd = { y, canceled ->
                                            if (!canceled && y > trashTopDp) noteToDelete = note
                                            draggingNoteId = null
                                        }
                                    )
                                }
                            }

                            images.forEachIndexed { idx, img ->
                                BoardImageItem(
                                    image = img,
                                    stagger = visibleItems.size + idx,
                                    clampX = clampX,
                                    clampY = clampY,
                                    isDraggingThis = draggingImageId == img.id,
                                    isOverTrash = isOverTrash && draggingImageId == img.id,
                                    onMoved = { x, y -> BoardStore.moveImage(context, img.id, currentBoard, x, y); refresh() },
                                    onRotated = { rot -> BoardStore.rotateImage(context, img.id, currentBoard, rot); refresh() },
                                    onScaleChanged = { scale -> BoardStore.setImageScale(context, img.id, currentBoard, scale); refresh() },
                                    onDragStart = { draggingImageId = img.id },
                                    onDragUpdate = { x, y -> dragY = y; fingerX = x; fingerY = y },
                                    onDragEnd = { y, canceled ->
                                        if (!canceled && y > trashTopDp) imageToDelete = img
                                        draggingImageId = null
                                    }
                                )
                            }
                        }
                    }

                    if (!isPhoneSize && boardPxW > 0 && boardPxH > 0 && vpW > 0 && vpH > 0) {
                        MiniMap(
                            paperW = boardPxW.toFloat(),
                            paperH = boardPxH.toFloat(),
                            viewW = vpW.toFloat(),
                            viewH = vpH.toFloat(),
                            scrollX = scrollStateH.value.toFloat(),
                            scrollY = scrollStateV.value.toFloat(),
                            finger = if (draggingNoteId != null || draggingImageId != null) Offset(fingerX, fingerY) else null,
                            pxPerDp = density.density,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .width(96.dp)
                                .height((96f * boardPxH / boardPxW).dp)
                        )
                    }
                }

                if ((draggingNoteId != null || draggingImageId != null) && !isExporting) {
                    TrashBin(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), highlighted = isOverTrash)
                }
            }
        }

        if (!isExporting) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(18.dp)
                    .size(60.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFFD54F), androidx.compose.ui.graphics.Color(0xFFFB8C00))))
                    .combinedClickable(onClick = { showAddNote = true })
                    .rotate(-4f),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, "افزودن", tint = androidx.compose.ui.graphics.Color(0xFF3E2723), modifier = Modifier.size(28.dp))
            }
        }

        if (isExporting) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
                    Text("در حال ساخت PDF از کل تابلو...", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(top = 16.dp), fontFamily = VazirFont)
                }
            }
        }
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("🗑️ حذف یادداشت", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                Column {
                    Text("«${note.title.ifBlank { "بدون عنوان" }}»")
                    Text("می‌خواهی از تابلو حذف شود یا کلاً از دفترچه؟", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    BoardStore.removeItem(context, note.id, currentBoard)
                    refresh()
                    noteToDelete = null
                    Toast.makeText(context, "از تابلو حذف شد", Toast.LENGTH_SHORT).show()
                }) {
                    Text("فقط از تابلو", color = androidx.compose.ui.graphics.Color(0xFFFB8C00), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text("انصراف")
                    }
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            noteDao.deleteById(note.id)
                            BoardStore.removeItem(context, note.id, currentBoard)
                            withContext(Dispatchers.Main) {
                                refresh()
                                noteToDelete = null
                                Toast.makeText(context, "کلاً حذف شد", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("حذف کامل", color = androidx.compose.ui.graphics.Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    imageToDelete?.let { img ->
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            title = { Text("🗑️ حذف تصویر", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = { Text("این تصویر از تابلو حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    BoardStore.removeImage(context, img.id, currentBoard)
                    refresh()
                    imageToDelete = null
                    Toast.makeText(context, "تصویر حذف شد", Toast.LENGTH_SHORT).show()
                }) {
                    Text("حذف", color = androidx.compose.ui.graphics.Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showAddBoard) {
        AlertDialog(
            onDismissRequest = { showAddBoard = false },
            title = { Text("📌 تابلو جدید", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                OutlinedTextField(boardName, { boardName = it }, label = { Text("نام تابلو") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val b = BoardStore.addBoard(context, boardName.ifBlank { "تابلو جدید" })
                    currentBoard = b.id
                    refresh()
                    showAddBoard = false
                }) {
                    Text("ساخت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBoard = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showAddNote) {
        val onBoard = items.map { it.noteId }.toSet()
        val available = notes.filter { it.id !in onBoard }

        AlertDialog(
            onDismissRequest = { showAddNote = false },
            title = { Text("📝 چسباندن یادداشت", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                if (notes.isEmpty()) {
                    Text("هنوز یادداشتی نساخته‌اید. لطفاً ابتدا از تب «یادداشت‌ها» یک یادداشت ایجاد کنید.", textAlign = TextAlign.Center)
                } else if (available.isEmpty()) {
                    Text("همهٔ یادداشت‌ها در حال حاضر روی این تابلو هستند.")
                } else {
                    LazyColumn {
                        items(available) { n ->
                            Box(
                                Modifier.fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .rotate(listOf(-1.5f, 1f, -0.5f, 2f)[n.id.toInt() % 4])
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(stickyBody(n.color))
                                    .shadow(4.dp, RoundedCornerShape(4.dp))
                                    .combinedClickable(onClick = {
                                        BoardStore.addItem(context, n.id, currentBoard)
                                        refresh()
                                        showAddNote = false
                                        Toast.makeText(context, "یادداشت چسبانده شد", Toast.LENGTH_SHORT).show()
                                    })
                                    .padding(12.dp)
                            ) {
                                Text(
                                    n.title.ifBlank { n.body.take(30).ifBlank { "بدون عنوان" } },
                                    fontFamily = VazirFont,
                                    fontSize = 14.sp,
                                    color = androidx.compose.ui.graphics.Color(0xFF3E2723),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddNote = false }) {
                    Text("بستن")
                }
            }
        )
    }
}

private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
    return try {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path)
        } else {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun MiniMap(
    paperW: Float,
    paperH: Float,
    viewW: Float,
    viewH: Float,
    scrollX: Float,
    scrollY: Float,
    finger: Offset?,
    pxPerDp: Float,
    modifier: Modifier
) {
    Canvas(
        modifier
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
    ) {
        val sx = size.width / paperW
        val sy = size.height / paperH
        val vw = (viewW * sx).coerceAtMost(size.width)
        val vh = (viewH * sy).coerceAtMost(size.height)
        val vx = (scrollX * sx).coerceIn(0f, (size.width - vw).coerceAtLeast(0f))
        val vy = (scrollY * sy).coerceIn(0f, (size.height - vh).coerceAtLeast(0f))
        drawRect(
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f),
            topLeft = Offset(vx, vy),
            size = Size(vw, vh)
        )
        drawRect(
            androidx.compose.ui.graphics.Color.White,
            topLeft = Offset(vx, vy),
            size = Size(vw, vh),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f)
        )
        finger?.let { f ->
            val fx = (f.x * pxPerDp * sx).coerceIn(0f, size.width)
            val fy = (f.y * pxPerDp * sy).coerceIn(0f, size.height)
            drawCircle(androidx.compose.ui.graphics.Color(0xFFFFB74D), radius = 4.5f, center = Offset(fx, fy))
            drawCircle(
                androidx.compose.ui.graphics.Color.White,
                radius = 4.5f,
                center = Offset(fx, fy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
            )
        }
    }
}

@Composable
private fun BoardImageItem(
    image: BoardImage,
    stagger: Int,
    clampX: Float,
    clampY: Float,
    isDraggingThis: Boolean,
    isOverTrash: Boolean,
    onMoved: (Float, Float) -> Unit,
    onRotated: (Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragUpdate: (Float, Float) -> Unit,
    onDragEnd: (Float, Boolean) -> Unit
) {
    val density = LocalDensity.current
    var pos by remember(image.id, image.boardId) {
        mutableStateOf(Offset(image.x.coerceIn(0f, clampX), image.y.coerceIn(0f, clampY)))
    }
    var rotation by remember(image.id, image.boardId) { mutableFloatStateOf(image.rotation) }
    var scale by remember(image.id, image.boardId) { mutableFloatStateOf(image.scale) }
    var visualZoom by remember { mutableFloatStateOf(1f) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    var gestureActive by remember { mutableStateOf(false) }

    LaunchedEffect(lastInteraction) {
        if (lastInteraction > 0 && !gestureActive) {
            delay(500)
            onMoved(pos.x, pos.y)
            onRotated(rotation)
            onScaleChanged(scale)
        }
    }

    LaunchedEffect(lastInteraction) {
        if (lastInteraction > 0) {
            delay(400)
            visualZoom = 1f
        }
    }

    val trashScale by animateFloatAsState(targetValue = if (isOverTrash) 0.4f else 1f, label = "trash-scale")
    val trashAlpha by animateFloatAsState(targetValue = if (isOverTrash) 0.3f else 1f, label = "trash-alpha")

    val widthDp = (BASE_IMAGE_WIDTH * scale).dp

    Box(
        Modifier
            .alpha(trashAlpha)
            .absoluteOffset { with(density) { IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx()) } }
            .width(widthDp)
            .graphicsLayer {
                rotationZ = rotation
                val s = visualZoom * trashScale
                scaleX = s
                scaleY = s
            }
            .pointerInput(image.id, image.boardId) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var moved = false
                    var canceled = false
                    do {
                        val event = awaitPointerEvent()
                        canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val rotationChange = event.calculateRotation()
                            val panChange = event.calculatePan()
                            val active = panChange != Offset.Zero || rotationChange != 0f || zoomChange != 1f
                            if (active) {
                                if (!moved && (zoomChange != 1f || rotationChange != 0f || panChange.getDistance() > 8f)) {
                                    moved = true
                                    gestureActive = true
                                    onDragStart()
                                }
                                if (moved) {
                                    pos = Offset(
                                        (pos.x + panChange.x / density.density).coerceIn(0f, clampX),
                                        (pos.y + panChange.y / density.density).coerceIn(0f, clampY)
                                    )
                                    rotation += rotationChange
                                    scale = (scale * zoomChange).coerceIn(0.3f, 3.0f)
                                    visualZoom = zoomChange.coerceIn(0.5f, 2.0f)
                                    onDragUpdate(pos.x, pos.y)
                                    lastInteraction = System.currentTimeMillis()
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                    if (moved) {
                        gestureActive = false
                        onDragEnd(pos.y, canceled)
                    }
                }
            }
    ) {
        Box(
            Modifier.fillMaxWidth()
                .shadow(7.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(androidx.compose.ui.graphics.Color.White)
                .padding(4.dp)
        ) {
            AsyncImage(
                model = Uri.parse(image.uri),
                contentDescription = "تصویر",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TrashBin(modifier: Modifier = Modifier, highlighted: Boolean) {
    val size by animateDpAsState(if (highlighted) 80.dp else 64.dp, label = "trash-size")
    val bg by animateFloatAsState(if (highlighted) 0.95f else 0.55f, label = "trash-bg")
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(size).shadow(8.dp, CircleShape).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFE53935).copy(alpha = bg)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Delete, "حذف", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(size * 0.5f))
        }
        Text(
            if (highlighted) "رها کن تا حذف شود!" else "بکش اینجا",
            color = if (highlighted) androidx.compose.ui.graphics.Color(0xFFFFCDD2) else androidx.compose.ui.graphics.Color.White.copy(alpha = .7f),
            fontFamily = LalezarFont,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CorkTexture(bgIndex: Int) {
    val base = boardBase(bgIndex)
    val dotA = corkDotA(bgIndex)
    val dotB = corkDotB(bgIndex)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(base)
        val rnd = Random(1337)
        repeat(600) {
            val x = rnd.nextFloat() * size.width
            val y = rnd.nextFloat() * size.height
            val r = rnd.nextFloat() * 3.2f + 0.8f
            val dark = rnd.nextBoolean()
            drawCircle(color = if (dark) dotA else dotB, radius = r, center = Offset(x, y))
        }
    }
}

@Composable
private fun Vignette(bgIndex: Int) {
    val strength = if (bgIndex == 3) .10f else .25f
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Black.copy(alpha = strength)),
                center = androidx.compose.ui.geometry.Offset.Unspecified,
                radius = 1200f
            )
        )
    )
}

@Composable
private fun StickyNote(
    note: Note,
    item: BoardItem,
    variant: Int,
    stagger: Int,
    clampX: Float,
    clampY: Float,
    isDraggingThis: Boolean,
    isOverTrash: Boolean,
    onOpen: () -> Unit,
    onMoved: (Float, Float) -> Unit,
    onRotated: (Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragUpdate: (Float, Float) -> Unit,
    onDragEnd: (Float, Boolean) -> Unit
) {
    val density = LocalDensity.current
    var pos by remember(item.noteId, item.boardId) {
        mutableStateOf(Offset(item.x.coerceIn(0f, clampX), item.y.coerceIn(0f, clampY)))
    }
    var rotation by remember(item.noteId, item.boardId) { mutableFloatStateOf(item.rotation) }
    var scale by remember(item.noteId, item.boardId) { mutableFloatStateOf(item.scale) }
    var visualZoom by remember { mutableFloatStateOf(1f) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    var gestureActive by remember { mutableStateOf(false) }

    LaunchedEffect(lastInteraction) {
        if (lastInteraction > 0 && !gestureActive) {
            delay(500)
            onMoved(pos.x, pos.y)
            onRotated(rotation)
            onScaleChanged(scale)
        }
    }

    LaunchedEffect(lastInteraction) {
        if (lastInteraction > 0) {
            delay(400)
            visualZoom = 1f
        }
    }

    val trashScale by animateFloatAsState(targetValue = if (isOverTrash) 0.4f else 1f, label = "trash-scale")
    val trashAlpha by animateFloatAsState(targetValue = if (isOverTrash) 0.3f else 1f, label = "trash-alpha")

    val widthDp = (BASE_NOTE_WIDTH * scale).dp
    val body = stickyBody(note.color)
    val usePin = variant % 2 == 0

    Box(
        Modifier
            .alpha(trashAlpha)
            .absoluteOffset { with(density) { IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx()) } }
            .width(widthDp)
            .graphicsLayer {
                rotationZ = rotation
                val s = visualZoom * trashScale
                scaleX = s
                scaleY = s
            }
            .pointerInput(item.noteId, item.boardId) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var moved = false
                    var canceled = false
                    do {
                        val event = awaitPointerEvent()
                        canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val rotationChange = event.calculateRotation()
                            val panChange = event.calculatePan()
                            val active = panChange != Offset.Zero || rotationChange != 0f || zoomChange != 1f
                            if (active) {
                                if (!moved && (zoomChange != 1f || rotationChange != 0f || panChange.getDistance() > 8f)) {
                                    moved = true
                                    gestureActive = true
                                    onDragStart()
                                }
                                if (moved) {
                                    pos = Offset(
                                        (pos.x + panChange.x / density.density).coerceIn(0f, clampX),
                                        (pos.y + panChange.y / density.density).coerceIn(0f, clampY)
                                    )
                                    rotation += rotationChange
                                    scale = (scale * zoomChange).coerceIn(0.3f, 3.0f)
                                    visualZoom = zoomChange.coerceIn(0.5f, 2.0f)
                                    onDragUpdate(pos.x, pos.y)
                                    lastInteraction = System.currentTimeMillis()
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                    if (moved) {
                        gestureActive = false
                        onDragEnd(pos.y, canceled)
                    }
                }
            }
    ) {
        Box(
            Modifier.fillMaxWidth()
                .shadow(7.dp, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.linearGradient(listOf(body, body, stickyEdge(note.color))))
                .combinedClickable(onClick = onOpen)
                .padding(top = if (usePin) 20.dp else 14.dp, start = 12.dp, end = 12.dp, bottom = 16.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column {
                    Text(note.title.ifBlank { "بدون عنوان" }, fontFamily = LalezarFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF3E2723), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(note.body, fontFamily = VazirFont, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFF5D4037), maxLines = 5, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                }
            }
            CurledCorner(Modifier.align(Alignment.BottomEnd))
        }
        if (usePin) {
            Thumbtack(pinColor(note.color), Modifier.align(Alignment.TopCenter).offset(y = (-8).dp))
        } else {
            TapeStrip(Modifier.align(Alignment.TopCenter).offset(y = (-9).dp))
        }
    }
}

@Composable
private fun Thumbtack(color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(modifier.size(20.dp)) {
        Box(Modifier.size(20.dp).offset(y = 3.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .30f)))
        Box(Modifier.size(20.dp).clip(CircleShape).background(Brush.radialGradient(listOf(color.copy(alpha = .95f), color, color.copy(alpha = .55f)))))
        Box(Modifier.size(6.dp).align(Alignment.TopStart).offset(4.dp, 4.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White.copy(alpha = .75f)))
    }
}

@Composable
private fun TapeStrip(modifier: Modifier = Modifier) {
    Box(
        modifier.width(58.dp)
            .height(18.dp)
            .rotate(-3f)
            .clip(RoundedCornerShape(2.dp))
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = .38f))
    )
}

@Composable
private fun CurledCorner(modifier: Modifier = Modifier) {
    Canvas(modifier.size(26.dp)) {
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(p, Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color.Black.copy(alpha = .22f), androidx.compose.ui.graphics.Color.Black.copy(alpha = .05f))))
    }
}
