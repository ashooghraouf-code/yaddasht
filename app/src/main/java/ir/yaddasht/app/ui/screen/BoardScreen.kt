@file:OptIn(ExperimentalFoundationApi::class)

package ir.yaddasht.app.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface as AndroidTypeface
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
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
import ir.yaddasht.app.util.Board
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

private val BOARD_SIZE_LABELS = listOf("📱 گوشی", "📄 A4", "📐 A3", "🗺️ A2")
private val BOARD_SIZE_DESC = listOf("اندازهٔ صفحهٔ گوشی", "۲۱۰×۲۹۷ میلی‌متر", "۲۹۷×۴۲۰ میلی‌متر", "۴۲۰×۵۹۴ میلی‌متر")
private val BOARD_SIZES_PT = listOf(0f to 0f, 595f to 842f, 842f to 1191f, 1191f to 1684f)
private val BOARD_SIZES_DP = BOARD_SIZES_PT
private const val BASE_NOTE_WIDTH = 150f
private const val BASE_IMAGE_WIDTH = 150f
private const val EXPORT_QUALITY_SCALE = 3.5f
private const val FINGER_THROTTLE_MS = 50L
private data class MiniMarker(val x: Float, val y: Float, val w: Float, val h: Float, val color: Color)

private fun boardBase(i: Int) = listOf(Color(0xFFF5F5DC), Color(0xFFE8EAF6), Color(0xFF263238), Color(0xFFFFF3E0), Color(0xFFE0F2F1), Color(0xFFFCE4EC))[i.coerceIn(0, 5)]
private fun stickyBody(i: Int) = listOf(Color(0xFFFFF59D), Color(0xFFF8BBD0), Color(0xFFB3E5FC), Color(0xFFC8E6C9), Color(0xFFFFE0B2), Color(0xFFE1BEE7))[i.coerceIn(0, 5)]
private fun stickyEdge(i: Int) = stickyBody(i).copy(alpha = .55f)
private fun pinColor(i: Int) = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFB8C00))[i.coerceIn(0, 5)]
private fun corkDotA(i: Int) = when (i) { 2 -> Color.White.copy(alpha = 0.07f); 3 -> Color(0xFF8D6E63).copy(alpha = 0.12f); else -> Color(0xFF5D4037).copy(alpha = 0.30f) }
private fun corkDotB(i: Int) = when (i) { 2 -> Color.White.copy(alpha = 0.03f); 3 -> Color(0xFFD7CCC8).copy(alpha = 0.30f); else -> Color(0xFFD7CCC8).copy(alpha = 0.24f) }

private fun safeTypeface(context: Context, name: String, bold: Boolean): AndroidTypeface {
    val id = context.resources.getIdentifier(name, "font", context.packageName)
    return if (id != 0) { try { ResourcesCompat.getFont(context, id) ?: if (bold) AndroidTypeface.DEFAULT_BOLD else AndroidTypeface.DEFAULT } catch (_: Exception) { if (bold) AndroidTypeface.DEFAULT_BOLD else AndroidTypeface.DEFAULT } } else { if (bold) AndroidTypeface.DEFAULT_BOLD else AndroidTypeface.DEFAULT }
}

private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
    return try {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path) else { val s = context.contentResolver.openInputStream(uri); s?.use { BitmapFactory.decodeStream(it) } }
    } catch (e: Exception) { null }
}

private fun renderBoardToCanvas(ctx: Context, canvas: AndroidCanvas, notes: List<Note>, items: List<BoardItem>, images: List<BoardImage>, noteSizes: Map<Long, Pair<Int, Int>>, imageSizes: Map<Long, Pair<Int, Int>>, bgIndex: Int, density: Float, pxW: Int, pxH: Int, baseW: Float, baseH: Float) {
    val scale = pxW / baseW
    val titleType = safeTypeface(ctx, "lalezar", true)
    val bodyType = safeTypeface(ctx, "vazir", false)
    canvas.drawColor(boardBase(bgIndex).toArgb())
    val titlePaint = TextPaint().apply { color = Color(0xFF3E2723).toArgb(); textSize = 15f * scale; isAntiAlias = true; typeface = titleType }
    val bodyPaint = TextPaint().apply { color = Color(0xFF5D4037).toArgb(); textSize = 11f * scale; isAntiAlias = true; typeface = bodyType }
    val targetLineHeight = 17f * scale
    val fm = bodyPaint.fontMetrics
    val extraLineSpacing = (targetLineHeight - (fm.descent - fm.ascent + fm.leading)).coerceAtLeast(0f)
    val bgPaint = Paint().apply { isAntiAlias = true }
    val framePaint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE }
    items.forEach { item ->
        val note = notes.firstOrNull { it.id == item.noteId } ?: return@forEach
        val itemScale = item.scale.coerceIn(0.3f, 3.0f)
        val usePin = item.noteId % 2 == 0L
        val topPadPx = (if (usePin) 20f else 14f) * scale
        val bottomPadPx = 16f * scale
        val padPx = 12f * scale
        val measured = noteSizes[item.noteId]
        val baseWItem = measured?.first?.toFloat()?.let { it / density } ?: (BASE_NOTE_WIDTH * itemScale)
        val baseHItem = measured?.second?.toFloat()?.let { it / density } ?: (140f * itemScale)
        val drawW = baseWItem * scale
        val drawH = baseHItem * scale
        val innerW = (drawW - 2f * padPx).toInt().coerceAtLeast(1)
        val titleText = note.title.ifBlank { "بدون عنوان" }
        val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, innerW).setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).build()
        val bodyBuilder = StaticLayout.Builder.obtain(note.body, 0, note.body.length, bodyPaint, innerW).setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(5).setEllipsize(TextUtils.TruncateAt.END)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) bodyBuilder.setLineSpacing(extraLineSpacing, 1f)
        val bodyLayout = bodyBuilder.build()
        val drawX = item.x * scale
        val drawY = item.y * scale
        canvas.save(); canvas.translate(drawX + drawW / 2f, drawY + drawH / 2f); canvas.rotate(item.rotation); canvas.translate(-drawW / 2f, -drawH / 2f)
        bgPaint.color = stickyBody(note.color).toArgb()
        canvas.drawRoundRect(RectF(0f, 0f, drawW, drawH), 3f * scale, 3f * scale, bgPaint)
        canvas.save(); canvas.translate(padPx, topPadPx); titleLayout.draw(canvas); canvas.restore()
        canvas.save(); canvas.translate(padPx, topPadPx + titleLayout.height); bodyLayout.draw(canvas); canvas.restore()
        canvas.restore()
    }
    images.forEach { img ->
        val bitmap = loadBitmapFromUri(ctx, img.uri) ?: return@forEach
        val itemScale = img.scale.coerceIn(0.3f, 3.0f)
        val measuredImg = imageSizes[img.id]
        val baseWImg = measuredImg?.first?.toFloat()?.let { it / density } ?: (BASE_IMAGE_WIDTH * itemScale)
        val baseHImg = measuredImg?.second?.toFloat()?.let { it / density } ?: (BASE_IMAGE_WIDTH * itemScale)
        val drawW = baseWImg * scale
        val drawH = baseHImg * scale
        val frame = 4f * scale
        val imgW = (drawW - 2f * frame).coerceAtLeast(1f)
        val imgH = (drawH - 2f * frame).coerceAtLeast(1f)
        val drawX = img.x * scale
        val drawY = img.y * scale
        canvas.save(); canvas.translate(drawX + drawW / 2f, drawY + drawH / 2f); canvas.rotate(img.rotation); canvas.translate(-drawW / 2f, -drawH / 2f)
        canvas.drawRoundRect(RectF(0f, 0f, drawW, drawH), 6f * scale, 6f * scale, framePaint)
        canvas.drawBitmap(bitmap, null, RectF(frame, frame, frame + imgW, frame + imgH), null)
        canvas.restore(); bitmap.recycle()
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
    var showExportDialog by remember { mutableStateOf(false) }
    var boardName by remember { mutableStateOf("") }
    var newBoardSizeIndex by remember { mutableStateOf(1) }
    var isExporting by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var boardPxW by remember { mutableStateOf(0) }
    var boardPxH by remember { mutableStateOf(0) }
    var vpW by remember { mutableStateOf(0) }
    var vpH by remember { mutableStateOf(0) }
    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var draggingImageId by remember { mutableStateOf<Long?>(null) }
    var boardTouchActive by remember { mutableStateOf(false) }
    val fingerState = remember { mutableStateOf<Offset?>(null) }
    var lastFingerWrite by remember { mutableStateOf(0L) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var imageToDelete by remember { mutableStateOf<BoardImage?>(null) }
    var boardToDelete by remember { mutableStateOf<Board?>(null) }
    val noteSizes = remember { mutableStateOf(mutableMapOf<Long, Pair<Int, Int>>()) }
    val imageSizes = remember { mutableStateOf(mutableMapOf<Long, Pair<Int, Int>>()) }
    val refresh: () -> Unit = {
        boards = BoardStore.boards(context); items = BoardStore.items(context, currentBoard); images = BoardStore.images(context, currentBoard)
    }
    val updateFinger: (Float, Float) -> Unit = { x, y ->
        val now = System.currentTimeMillis()
        if (now - lastFingerWrite >= FINGER_THROTTLE_MS) { lastFingerWrite = now; fingerState.value = Offset(x, y) }
    }
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val dir = File(context.filesDir, "board_images")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "img-${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out -> inputStream.copyTo(out) }; inputStream.close()
                val saved = BoardStore.addImage(context, currentBoard, Uri.fromFile(file).toString())
                images = images + saved
                Toast.makeText(context, "🖼️ تصویر اضافه شد", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(context, "خطا: نمی‌توان تصویر را خواند", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(context, "خطا: ${e.message}", Toast.LENGTH_LONG).show() }
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
        if (searchQuery.isBlank()) items else items.filter { item ->
            val n = notes.firstOrNull { it.id == item.noteId } ?: return@filter false
            n.title.contains(searchQuery, true) || n.body.contains(searchQuery, true)
        }
    }
    val miniMarkers = remember(visibleItems, images) {
        val list = mutableListOf<MiniMarker>()
        visibleItems.forEach { item -> val n = notes.firstOrNull { it.id == item.noteId }; if (n != null) list.add(MiniMarker(item.x, item.y, BASE_NOTE_WIDTH * item.scale, 120f * item.scale, stickyBody(n.color))) }
        images.forEach { img -> list.add(MiniMarker(img.x, img.y, BASE_IMAGE_WIDTH * img.scale, 120f * img.scale, Color.White)) }
        list
    }
    fun exportBoard(asPdf: Boolean) {
        if (isExporting) return
        isExporting = true
        val snapshotNotes = notes.toList(); val snapshotItems = items.toList(); val snapshotImages = images.toList()
        val snapshotNoteSizes = noteSizes.value.toMap(); val snapshotImageSizes = imageSizes.value.toMap()
        val snapshotBg = bgIndex; val sSizeIndex = boardSizeIndex; val sPhone = isPhoneSize
        val sPxW = boardPxW; val sPxH = boardPxH; val sName = currentBoardData?.name ?: ""
        scope.launch(Dispatchers.IO) {
            try {
                val baseW: Float; val baseH: Float
                if (sPhone) { baseW = sPxW.toFloat().coerceAtLeast(100f); baseH = sPxH.toFloat().coerceAtLeast(100f) } else { baseW = BOARD_SIZES_PT[sSizeIndex].first; baseH = BOARD_SIZES_PT[sSizeIndex].second }
                val pxW = (baseW * EXPORT_QUALITY_SCALE).toInt().coerceAtLeast(300)
                val pxH = (baseH * EXPORT_QUALITY_SCALE).toInt().coerceAtLeast(300)
                val dir = File(context.cacheDir, "board_exports"); if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "board-${System.currentTimeMillis()}.${if (asPdf) "pdf" else "png"}")
                if (asPdf) {
                    val pdfDocument = PdfDocument()
                    val page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pxW, pxH, 1).create())
                    renderBoardToCanvas(context, page.canvas, snapshotNotes, snapshotItems, snapshotImages, snapshotNoteSizes, snapshotImageSizes, snapshotBg, density.density, pxW, pxH, baseW, baseH)
                    pdfDocument.finishPage(page); FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }; pdfDocument.close()
                } else {
                    val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
                    renderBoardToCanvas(context, AndroidCanvas(bitmap), snapshotNotes, snapshotItems, snapshotImages, snapshotNoteSizes, snapshotImageSizes, snapshotBg, density.density, pxW, pxH, baseW, baseH)
                    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }; bitmap.recycle()
                }
                withContext(Dispatchers.Main) {
                    isExporting = false
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val mime = if (asPdf) "application/pdf" else "image/png"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = mime
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "تابلوی $sName")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری تابلو"))
                    Toast.makeText(context, "✅ خروجی با کیفیت بالا آماده شد", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isExporting = false; Toast.makeText(context, "خطا در خروجی: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(boardBase(bgIndex))) {
            if (!isExporting) {
                Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .28f)).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = Color(0xFFFFE0B2)) }
                    boards.forEach { b ->
                        val selected = b.id == currentBoard
                        Surface(modifier = Modifier.combinedClickable(onClick = { currentBoard = b.id; refresh(); searchQuery = "" }, onLongClick = { boardToDelete = b }), shape = RoundedCornerShape(10.dp), color = if (selected) Color(0xFFFFB74D) else Color.White.copy(alpha = .12f), shadowElevation = if (selected) 6.dp else 0.dp) {
                            Text(b.name, color = if (selected) Color(0xFF3E2723) else Color(0xFFFFE0B2), fontFamily = LalezarFont, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.12f)) { Text("📝${items.size} 🖼${images.size}", fontSize = 11.sp, color = Color(0xFFFFE0B2), fontFamily = VazirFont, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                    Surface(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }, shape = RoundedCornerShape(12.dp), color = Color(0xFFFB8C00).copy(alpha = 0.2f), border = BorderStroke(1.dp, Color(0xFFFB8C00).copy(alpha = 0.4f))) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Image, "عکس", tint = Color(0xFFFFE0B2), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("عکس", color = Color(0xFFFFE0B2), fontSize = 12.sp, fontFamily = VazirFont) }
                    }
                    Surface(onClick = { showSearch = !showSearch }, shape = RoundedCornerShape(12.dp), color = if (showSearch) Color(0xFFFFB74D).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (showSearch) Icons.Filled.SearchOff else Icons.Filled.Search, "جستجو", tint = Color(0xFFFFE0B2), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("جستجو", color = Color(0xFFFFE0B2), fontSize = 12.sp, fontFamily = VazirFont) }
                    }
                    Surface(onClick = { showExportDialog = true }, shape = RoundedCornerShape(12.dp), color = Color(0xFFE53935).copy(alpha = 0.2f), border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.PictureAsPdf, "خروجی", tint = Color(0xFFFFCDD2), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("خروجی", color = Color(0xFFFFCDD2), fontSize = 12.sp, fontFamily = VazirFont) }
                    }
                    IconButton(onClick = { BoardStore.setBackground(context, currentBoard, (bgIndex + 1) % 6); refresh() }) { Icon(Icons.Filled.Palette, "پس‌زمینه", tint = Color(0xFFFFE0B2)) }
                    Surface(onClick = { BoardStore.setBoardSize(context, currentBoard, (boardSizeIndex + 1) % 4); refresh() }, shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.1f)) { Text(BOARD_SIZE_LABELS[boardSizeIndex], fontSize = 11.sp, color = Color(0xFFFFE0B2), fontFamily = VazirFont, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                    IconButton(onClick = { boardName = ""; newBoardSizeIndex = 1; showAddBoard = true }) { Icon(Icons.Filled.Add, "تابلو جدید", tint = Color(0xFFFFE0B2)) }
                }
                if (showSearch) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .95f)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, "جستجو", tint = Color(0xFF5D4037))
                        BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, textStyle = TextStyle(fontFamily = VazirFont, fontSize = 14.sp, color = Color(0xFF3E2723)), singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), decorationBox = { inner -> Box { if (searchQuery.isEmpty()) Text("جستجو در یادداشت‌ها...", color = Color(0xFF8D6E63), fontSize = 14.sp, fontFamily = VazirFont); inner() } })
                        if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "پاک", tint = Color(0xFF5D4037), modifier = Modifier.size(16.dp)) }
                    }
                }
            }
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val scrollStateV = rememberScrollState(); val scrollStateH = rememberScrollState()
                    Box(Modifier.fillMaxSize().onSizeChanged { s -> vpW = s.width; vpH = s.height; boardPxW = s.width; boardPxH = s.height }.then(if (!isPhoneSize) Modifier.verticalScroll(scrollStateV).horizontalScroll(scrollStateH) else Modifier)) {
                        Box(Modifier.then(if (isPhoneSize) Modifier.fillMaxSize() else Modifier.width(boardWidthDp.dp).height(boardHeightDpActual.dp)).onSizeChanged { s -> boardPxW = s.width; boardPxH = s.height }.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false); boardTouchActive = true; updateFinger(down.position.x / density.density, down.position.y / density.density)
                                var pressed = true
                                while (pressed) { val ev = awaitPointerEvent(PointerEventPass.Main); val ch = ev.changes.firstOrNull(); if (ch != null && ch.pressed) updateFinger(ch.position.x / density.density, ch.position.y / density.density); pressed = ev.changes.any { it.pressed } }
                                boardTouchActive = false; fingerState.value = null
                            }
                        }) {
                            CorkTexture(bgIndex); Vignette(bgIndex)
                            if (visibleItems.isEmpty() && images.isEmpty() && !isExporting) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (searchQuery.isNotBlank()) "🔍" else "🗒️", fontSize = 64.sp, modifier = Modifier.rotate(if (searchQuery.isNotBlank()) 0f else -6f))
                                        Text(if (searchQuery.isNotBlank()) "یادداشتی یافت نشد" else "تابلو خالی است", fontFamily = LalezarFont, fontSize = 22.sp, color = Color.White.copy(alpha = .85f))
                                        Text("با دکمهٔ + یادداشت بچسبانید یا با دکمهٔ «عکس» تصویر اضافه کنید", fontFamily = VazirFont, fontSize = 13.sp, color = Color.White.copy(alpha = .6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                                    }
                                }
                            }
                            visibleItems.forEachIndexed { idx, item ->
                                val note = notes.firstOrNull { it.id == item.noteId }
                                if (note != null) StickyNote(note = note, item = item, variant = idx, stagger = idx, clampX = clampX, clampY = clampY, isDraggingThis = draggingNoteId == note.id, onOpen = { onOpenNote(note.id) }, onLongPress = { noteToDelete = note }, onMoved = { x, y -> BoardStore.move(context, note.id, currentBoard, x, y); items = items.map { if (it.noteId == note.id && it.boardId == currentBoard) it.copy(x = x, y = y) else it } }, onRotated = { rot -> BoardStore.rotate(context, note.id, currentBoard, rot); items = items.map { if (it.noteId == note.id && it.boardId == currentBoard) it.copy(rotation = rot) else it } }, onScaleChanged = { sc -> BoardStore.setScale(context, note.id, currentBoard, sc); items = items.map { if (it.noteId == note.id && it.boardId == currentBoard) it.copy(scale = sc) else it } }, onMeasured = { w, h -> noteSizes.value[item.noteId] = w to h }, onDragStart = { draggingNoteId = note.id }, onDragUpdate = { x, y -> updateFinger(x, y) }, onDragEnd = { draggingNoteId = null; fingerState.value = null })
                            }
                            images.forEachIndexed { idx, img ->
                                BoardImageItem(image = img, stagger = visibleItems.size + idx, clampX = clampX, clampY = clampY, isDraggingThis = draggingImageId == img.id, onLongPress = { imageToDelete = img }, onMoved = { x, y -> BoardStore.moveImage(context, img.id, currentBoard, x, y); images = images.map { if (it.id == img.id) it.copy(x = x, y = y) else it } }, onRotated = { rot -> BoardStore.rotateImage(context, img.id, currentBoard, rot); images = images.map { if (it.id == img.id) it.copy(rotation = rot) else it } }, onScaleChanged = { sc -> BoardStore.setImageScale(context, img.id, currentBoard, sc); images = images.map { if (it.id == img.id) it.copy(scale = sc) else it } }, onMeasured = { w, h -> imageSizes.value[img.id] = w to h }, onDragStart = { draggingImageId = img.id }, onDragUpdate = { x, y -> updateFinger(x, y) }, onDragEnd = { draggingImageId = null; fingerState.value = null })
                            }
                        }
                    }
                    if (!isPhoneSize && (boardTouchActive || draggingNoteId != null || draggingImageId != null) && boardPxW > 0 && boardPxH > 0 && vpW > 0 && vpH > 0) {
                        MiniMap(paperW = boardPxW.toFloat(), paperH = boardPxH.toFloat(), viewW = vpW.toFloat(), viewH = vpH.toFloat(), scrollStateH = scrollStateH, scrollStateV = scrollStateV, fingerState = fingerState, pxPerDp = density.density, markers = miniMarkers, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).width(96.dp).height((96f * boardPxH / boardPxW).dp))
                    }
                }
            }
        }
        if (!isExporting) Box(Modifier.align(Alignment.BottomEnd).padding(18.dp).size(60.dp).shadow(12.dp, CircleShape).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFB8C00)))).combinedClickable(onClick = { showAddNote = true }).rotate(-4f), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "افزودن", tint = Color(0xFF3E2723), modifier = Modifier.size(28.dp)) }
        if (isExporting) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = Color.White); Text("در حال ساخت خروجی با کیفیت بالا...", color = Color.White, modifier = Modifier.padding(top = 16.dp), fontFamily = VazirFont) } }
    }
    noteToDelete?.let { note ->
        AlertDialog(onDismissRequest = { noteToDelete = null }, title = { Text("🗑️ حذف یادداشت", fontFamily = LalezarFont, fontSize = 20.sp) }, text = { Column { Text("«${note.title.ifBlank { "بدون عنوان" }}»"); Text("می‌خواهی از تابلو حذف شود یا کلاً از دفترچه؟", fontSize = 13.sp, color = Color.Gray) } }, confirmButton = { TextButton(onClick = { BoardStore.removeItem(context, note.id, currentBoard); refresh(); noteToDelete = null; Toast.makeText(context, "از تابلو حذف شد", Toast.LENGTH_SHORT).show() }) { Text("فقط از تابلو", color = Color(0xFFFB8C00), fontWeight = FontWeight.Bold) } }, dismissButton = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { noteToDelete = null }) { Text("انصراف") }; TextButton(onClick = { scope.launch(Dispatchers.IO) { noteDao.deleteById(note.id); BoardStore.removeItem(context, note.id, currentBoard); withContext(Dispatchers.Main) { refresh(); noteToDelete = null; Toast.makeText(context, "کلاً حذف شد", Toast.LENGTH_SHORT).show() } } }) { Text("حذف کامل", color = Color.Red, fontWeight = FontWeight.Bold) } } })
    }
    imageToDelete?.let { img ->
        AlertDialog(onDismissRequest = { imageToDelete = null }, title = { Text("🗑️ حذف تصویر", fontFamily = LalezarFont, fontSize = 20.sp) }, text = { Text("این تصویر از تابلو حذف شود؟") }, confirmButton = { TextButton(onClick = { BoardStore.removeImage(context, img.id, currentBoard); refresh(); imageToDelete = null; Toast.makeText(context, "تصویر حذف شد", Toast.LENGTH_SHORT).show() }) { Text("حذف", color = Color.Red, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { imageToDelete = null }) { Text("انصراف") } })
    }
    boardToDelete?.let { b ->
        AlertDialog(onDismissRequest = { boardToDelete = null }, title = { Text("🗑️ حذف تابلو", fontFamily = LalezarFont, fontSize = 20.sp) }, text = { Text("تابلوی «${b.name}» همراه با همهٔ یادداشت‌ها و تصاویر روی آن حذف شود؟") }, confirmButton = { TextButton(onClick = { BoardStore.removeBoard(context, b.id); if (currentBoard == b.id) { val remaining = BoardStore.boards(context); currentBoard = remaining.firstOrNull()?.id ?: 1L }; refresh(); boardToDelete = null; Toast.makeText(context, "تابلو حذف شد", Toast.LENGTH_SHORT).show() }) { Text("حذف تابلو", color = Color.Red, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { boardToDelete = null }) { Text("انصراف") } })
    }
    if (showExportDialog) {
        AlertDialog(onDismissRequest = { showExportDialog = false }, title = { Text("📤 نوع خروجی", fontFamily = LalezarFont, fontSize = 20.sp) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("سایز تابلو: ${BOARD_SIZE_LABELS[boardSizeIndex]}", fontSize = 13.sp, color = Color.Gray, fontFamily = VazirFont); Spacer(Modifier.height(8.dp))
                Surface(onClick = { showExportDialog = false; exportBoard(true) }, shape = RoundedCornerShape(12.dp), color = Color(0xFFE53935).copy(alpha = 0.15f), border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.PictureAsPdf, null, tint = Color(0xFFE53935)); Spacer(Modifier.width(12.dp)); Column { Text("📄 PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = VazirFont); Text("مناسب چاپ و اشتراک‌گذاری", fontSize = 11.sp, color = Color.Gray, fontFamily = VazirFont) } }
                }
                Surface(onClick = { showExportDialog = false; exportBoard(false) }, shape = RoundedCornerShape(12.dp), color = Color(0xFF43A047).copy(alpha = 0.15f), border = BorderStroke(1.dp, Color(0xFF43A047).copy(alpha = 0.4f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Image, null, tint = Color(0xFF43A047)); Spacer(Modifier.width(12.dp)); Column { Text("🖼️ PNG", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = VazirFont); Text("تصویر با کیفیت بسیار بالا", fontSize = 11.sp, color = Color.Gray, fontFamily = VazirFont) } }
                }
            }
        }, confirmButton = {}, dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("انصراف") } })
    }
    if (showAddBoard) {
        AlertDialog(onDismissRequest = { showAddBoard = false }, title = { Text("📌 تابلو جدید", fontFamily = LalezarFont, fontSize = 20.sp) }, text = {
            Column {
                OutlinedTextField(boardName, { boardName = it }, label = { Text("نام تابلو") }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp))
                Text("سایز تابلو:", fontFamily = LalezarFont, fontSize = 15.sp); Spacer(Modifier.height(8.dp))
                BOARD_SIZE_LABELS.forEachIndexed { idx, label ->
                    val selected = idx == newBoardSizeIndex
                    Surface(onClick = { newBoardSizeIndex = idx }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(8.dp), color = if (selected) Color(0xFFFFB74D).copy(alpha = 0.3f) else Color.Transparent, border = BorderStroke(1.dp, if (selected) Color(0xFFFFB74D) else Color.Gray.copy(alpha = 0.3f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(20.dp).border(2.dp, Color.Gray, CircleShape), contentAlignment = Alignment.Center) { if (selected) Box(Modifier.size(12.dp).background(Color(0xFFFFB74D), CircleShape)) }
                            Spacer(Modifier.width(10.dp)); Column { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = VazirFont); Text(BOARD_SIZE_DESC[idx], fontSize = 11.sp, color = Color.Gray, fontFamily = VazirFont) }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { val b = BoardStore.addBoard(context, boardName.ifBlank { "تابلو جدید" }, background = 0, sizeIndex = newBoardSizeIndex); currentBoard = b.id; refresh(); showAddBoard = false }) { Text("ساخت") } }, dismissButton = { TextButton(onClick = { showAddBoard = false }) { Text("انصراف") } })
    }
    if (showAddNote) {
        val onBoard = items.map { it.noteId }.toSet()
        val available = notes.filter { it.id !in onBoard }
        AlertDialog(onDismissRequest = { showAddNote = false }, title = { Text("📝 چسباندن یادداشت", fontFamily = LalezarFont, fontSize = 20.sp) }, text = {
            if (notes.isEmpty()) Text("هنوز یادداشتی نساخته‌اید. لطفاً ابتدا از تب «یادداشت‌ها» یک یادداشت ایجاد کنید.", textAlign = TextAlign.Center) else if (available.isEmpty()) Text("همهٔ یادداشت‌ها در حال حاضر روی این تابلو هستند.") else LazyColumn {
                items(available) { n ->
                    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).rotate(listOf(-1.5f, 1f, -0.5f, 2f)[n.id.toInt() % 4]).clip(RoundedCornerShape(4.dp)).background(stickyBody(n.color)).shadow(4.dp, RoundedCornerShape(4.dp)).combinedClickable(onClick = { BoardStore.addItem(context, n.id, currentBoard); refresh(); showAddNote = false; Toast.makeText(context, "یادداشت چسبانده شد", Toast.LENGTH_SHORT).show() }).padding(12.dp)) {
                        Text(n.title.ifBlank { n.body.take(30).ifBlank { "بدون عنوان" } }, fontFamily = VazirFont, fontSize = 14.sp, color = Color(0xFF3E2723), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showAddNote = false }) { Text("بستن") } })
    }
}

@Composable
private fun MiniMap(paperW: Float, paperH: Float, viewW: Float, viewH: Float, scrollStateH: ScrollState, scrollStateV: ScrollState, fingerState: State<Offset?>, pxPerDp: Float, markers: List<MiniMarker>, modifier: Modifier) {
    val finger = fingerState.value
    val scrollX = scrollStateH.value.toFloat()
    val scrollY = scrollStateV.value.toFloat()
    ComposeCanvas(modifier.background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp)).border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))) {
        val sx = size.width / paperW; val sy = size.height / paperH
        markers.forEach { m ->
            val mw = (m.w * sx).coerceAtLeast(3f); val mh = (m.h * sy).coerceAtLeast(3f)
            drawRect(color = m.color.copy(alpha = 0.9f), topLeft = Offset(m.x * sx, m.y * sy), size = Size(mw, mh))
            drawRect(color = Color.White.copy(alpha = 0.6f), topLeft = Offset(m.x * sx, m.y * sy), size = Size(mw, mh), style = Stroke(1f))
        }
        val vw = (viewW * sx).coerceAtMost(size.width); val vh = (viewH * sy).coerceAtMost(size.height)
        val vx = (scrollX * sx).coerceIn(0f, (size.width - vw).coerceAtLeast(0f))
        val vy = (scrollY * sy).coerceIn(0f, (size.height - vh).coerceAtLeast(0f))
        drawRect(Color.White.copy(alpha = 0.15f), topLeft = Offset(vx, vy), size = Size(vw, vh))
        drawRect(Color.White, topLeft = Offset(vx, vy), size = Size(vw, vh), style = Stroke(1.5f))
        finger?.let { f ->
            val fx = (f.x * pxPerDp * sx).coerceIn(0f, size.width); val fy = (f.y * pxPerDp * sy).coerceIn(0f, size.height)
            drawCircle(Color(0xFFFFB74D), radius = 5f, center = Offset(fx, fy))
            drawCircle(Color.White, radius = 5f, center = Offset(fx, fy), style = Stroke(1.5f))
        }
    }
}

@Composable
private fun BoardImageItem(image: BoardImage, stagger: Int, clampX: Float, clampY: Float, isDraggingThis: Boolean, onLongPress: () -> Unit, onMoved: (Float, Float) -> Unit, onRotated: (Float) -> Unit, onScaleChanged: (Float) -> Unit, onMeasured: (Int, Int) -> Unit, onDragStart: () -> Unit, onDragUpdate: (Float, Float) -> Unit, onDragEnd: () -> Unit) {
    val density = LocalDensity.current
    var pos by remember(image.id, image.boardId) { mutableStateOf(Offset(image.x.coerceIn(0f, clampX), image.y.coerceIn(0f, clampY))) }
    var rotation by remember(image.id, image.boardId) { mutableStateOf(image.rotation) }
    var scale by remember(image.id, image.boardId) { mutableStateOf(image.scale) }
    var lastInteraction by remember { mutableStateOf(0L) }
    var gestureActive by remember { mutableStateOf(false) }
    LaunchedEffect(lastInteraction) { if (lastInteraction > 0 && !gestureActive) { delay(500); onMoved(pos.x, pos.y); onRotated(rotation); onScaleChanged(scale) } }
    val widthDp = (150f * scale).dp
    Box(Modifier.absoluteOffset { with(density) { IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx()) } }.width(widthDp).onSizeChanged { s -> onMeasured(s.width, s.height) }.graphicsLayer { rotationZ = rotation }.pointerInput(image.id, image.boardId) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false); var moved = false; var canceled = false
            do {
                val event = awaitPointerEvent(); canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val pointerCount = event.changes.count { it.pressed }; val panChange = event.calculatePan()
                    if (pointerCount >= 2) {
                        val zoomChange = event.calculateZoom(); val rotationChange = event.calculateRotation()
                        if (!moved && (zoomChange != 1f || rotationChange != 0f || panChange.getDistance() > 8f)) { moved = true; gestureActive = true }
                        if (moved) { pos = Offset((pos.x + panChange.x / density.density).coerceIn(0f, clampX), (pos.y + panChange.y / density.density).coerceIn(0f, clampY)); rotation += rotationChange; scale = (scale * zoomChange).coerceIn(0.3f, 3.0f); onDragUpdate(pos.x, pos.y); lastInteraction = System.currentTimeMillis() }
                        event.changes.forEach { it.consume() }
                    } else {
                        if (panChange.getDistance() > 0f) {
                            if (!moved && panChange.getDistance() > 4f) { moved = true; gestureActive = true; onDragStart() }
                            if (moved) { pos = Offset((pos.x + panChange.x / density.density).coerceIn(0f, clampX), (pos.y + panChange.y / density.density).coerceIn(0f, clampY)); onDragUpdate(pos.x, pos.y); lastInteraction = System.currentTimeMillis() }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            } while (!canceled && event.changes.any { it.pressed })
            if (moved) { gestureActive = false; onDragEnd() }
        }
    }) {
        Box(Modifier.fillMaxWidth().shadow(if (isDraggingThis) 14.dp else 7.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(Color.White).padding(4.dp).combinedClickable(onClick = { }, onLongClick = { onLongPress() })) {
            AsyncImage(model = Uri.parse(image.uri), contentDescription = "تصویر", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CorkTexture(bgIndex: Int) {
    val base = boardBase(bgIndex); val dotA = corkDotA(bgIndex); val dotB = corkDotB(bgIndex)
    ComposeCanvas(Modifier.fillMaxSize()) {
        drawRect(base); val rnd = Random(1337)
        repeat(600) {
            val x = rnd.nextFloat() * size.width; val y = rnd.nextFloat() * size.height
            val r = rnd.nextFloat() * 3.2f + 0.8f; val dark = rnd.nextBoolean()
            drawCircle(color = if (dark) dotA else dotB, radius = r, center = Offset(x, y))
        }
    }
}

@Composable
private fun Vignette(bgIndex: Int) {
    val strength = if (bgIndex == 2) 0.40f else 0.25f
    val brush = remember(bgIndex) { Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = strength)), center = Offset.Unspecified, radius = 1200f) }
    Box(Modifier.fillMaxSize().background(brush))
}

@Composable
private fun StickyNote(note: Note, item: BoardItem, variant: Int, stagger: Int, clampX: Float, clampY: Float, isDraggingThis: Boolean, onOpen: () -> Unit, onLongPress: () -> Unit, onMoved: (Float, Float) -> Unit, onRotated: (Float) -> Unit, onScaleChanged: (Float) -> Unit, onMeasured: (Int, Int) -> Unit, onDragStart: () -> Unit, onDragUpdate: (Float, Float) -> Unit, onDragEnd: () -> Unit) {
    val density = LocalDensity.current
    var pos by remember(item.noteId, item.boardId) { mutableStateOf(Offset(item.x.coerceIn(0f, clampX), item.y.coerceIn(0f, clampY))) }
    var rotation by remember(item.noteId, item.boardId) { mutableStateOf(item.rotation) }
    var scale by remember(item.noteId, item.boardId) { mutableStateOf(item.scale) }
    var lastInteraction by remember { mutableStateOf(0L) }
    var gestureActive by remember { mutableStateOf(false) }
    LaunchedEffect(lastInteraction) { if (lastInteraction > 0 && !gestureActive) { delay(500); onMoved(pos.x, pos.y); onRotated(rotation); onScaleChanged(scale) } }
    val widthDp = (150f * scale).dp; val body = stickyBody(note.color); val usePin = item.noteId % 2 == 0L
    Box(Modifier.absoluteOffset { with(density) { IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx()) } }.width(widthDp).onSizeChanged { s -> onMeasured(s.width, s.height) }.graphicsLayer { rotationZ = rotation }.pointerInput(item.noteId, item.boardId) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false); var moved = false; var canceled = false
            do {
                val event = awaitPointerEvent(); canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val pointerCount = event.changes.count { it.pressed }; val panChange = event.calculatePan()
                    if (pointerCount >= 2) {
                        val zoomChange = event.calculateZoom(); val rotationChange = event.calculateRotation()
                        if (!moved && (zoomChange != 1f || rotationChange != 0f || panChange.getDistance() > 8f)) { moved = true; gestureActive = true }
                        if (moved) { pos = Offset((pos.x + panChange.x / density.density).coerceIn(0f, clampX), (pos.y + panChange.y / density.density).coerceIn(0f, clampY)); rotation += rotationChange; scale = (scale * zoomChange).coerceIn(0.3f, 3.0f); onDragUpdate(pos.x, pos.y); lastInteraction = System.currentTimeMillis() }
                        event.changes.forEach { it.consume() }
                    } else {
                        if (panChange.getDistance() > 0f) {
                            if (!moved && panChange.getDistance() > 4f) { moved = true; gestureActive = true; onDragStart() }
                            if (moved) { pos = Offset((pos.x + panChange.x / density.density).coerceIn(0f, clampX), (pos.y + panChange.y / density.density).coerceIn(0f, clampY)); onDragUpdate(pos.x, pos.y); lastInteraction = System.currentTimeMillis() }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            } while (!canceled && event.changes.any { it.pressed })
            if (moved) { gestureActive = false; onDragEnd() }
        }
    }) {
        Box(Modifier.fillMaxWidth().shadow(if (isDraggingThis) 14.dp else 7.dp, RoundedCornerShape(3.dp)).clip(RoundedCornerShape(3.dp)).background(Brush.linearGradient(listOf(body, body, stickyEdge(note.color)))).combinedClickable(onClick = onOpen, onLongClick = onLongPress).padding(top = if (usePin) 20.dp else 14.dp, start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column {
                    Text(note.title.ifBlank { "بدون عنوان" }, fontFamily = LalezarFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(note.body, fontFamily = VazirFont, fontSize = 11.sp, color = Color(0xFF5D4037), maxLines = 5, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                }
            }
            CurledCorner(Modifier.align(Alignment.BottomEnd))
        }
        if (usePin) Thumbtack(pinColor(note.color), Modifier.align(Alignment.TopCenter).offset(y = (-8).dp)) else TapeStrip(Modifier.align(Alignment.TopCenter).offset(y = (-9).dp))
    }
}

@Composable
private fun Thumbtack(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(20.dp)) {
        Box(Modifier.size(20.dp).offset(y = 3.dp).clip(CircleShape).background(Color.Black.copy(alpha = .30f)))
        Box(Modifier.size(20.dp).clip(CircleShape).background(Brush.radialGradient(listOf(color.copy(alpha = .95f), color, color.copy(alpha = .55f)))))
        Box(Modifier.size(6.dp).align(Alignment.TopStart).offset(4.dp, 4.dp).clip(CircleShape).background(Color.White.copy(alpha = .75f)))
    }
}

@Composable
private fun TapeStrip(modifier: Modifier = Modifier) { Box(modifier.width(58.dp).height(18.dp).rotate(-3f).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .38f))) }

@Composable
private fun CurledCorner(modifier: Modifier = Modifier) {
    ComposeCanvas(modifier.size(26.dp)) {
        val p = Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(p, Brush.linearGradient(listOf(Color.Black.copy(alpha = .22f), Color.Black.copy(alpha = .05f))))
    }
}
// ✅ پایان کامل فایل BoardScreen.kt
