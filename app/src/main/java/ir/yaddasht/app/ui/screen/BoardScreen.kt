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

private fun boardBase(index: Int): Color = listOf(
    Color(0xFFF5F5DC), Color(0xFFE8EAF6), Color(0xFF263238),
    Color(0xFFFFF3E0), Color(0xFFE0F2F1), Color(0xFFFCE4EC)
)[index.coerceIn(0, 5)]

private fun stickyBody(index: Int): Color = listOf(
    Color(0xFFFFF59D), Color(0xFFF8BBD0), Color(0xFFB3E5FC),
    Color(0xFFC8E6C9), Color(0xFFFFE0B2), Color(0xFFE1BEE7)
)[index.coerceIn(0, 5)]

private fun stickyEdge(index: Int): Color = stickyBody(index).copy(alpha = .55f)

private fun pinColor(index: Int): Color = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFB8C00)
)[index.coerceIn(0, 5)]

private fun corkDotA(index: Int): Color = when (index) {
    2 -> Color.White.copy(alpha = 0.07f)
    3 -> Color(0xFF8D6E63).copy(alpha = 0.12f)
    else -> Color(0xFF5D4037).copy(alpha = 0.30f)
}

private fun corkDotB(index: Int): Color = when (index) {
    2 -> Color.White.copy(alpha = 0.03f)
    3 -> Color(0xFFD7CCC8).copy(alpha = 0.30f)
    else -> Color(0xFFD7CCC8).copy(alpha = 0.24f)
}

private fun safeTypeface(context: Context, name: String, bold: Boolean): AndroidTypeface {
    val id = context.resources.getIdentifier(name, "font", context.packageName)
    return if (id != 0) {
        try {
            ResourcesCompat.getFont(context, id) ?: if (bold) AndroidTypeface.DEFAULT_BOLD else AndroidTypeface.DEFAULT
        } catch (_: Exception) {
            if (bold) AndroidTypeface.DEFAULT_BOLD else AndroidTypeface.DEFAULT
        }
    } else {
        if (bold) AndroidTypeface.DEFAULT_BOLD else AndroidTypeface.DEFAULT
    }
}

private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
    return try {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path)
        else {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: Exception) {
        null
    }
}

private fun renderBoardToCanvas(
    context: Context,
    canvas: AndroidCanvas,
    notes: List<Note>,
    items: List<BoardItem>,
    images: List<BoardImage>,
    noteSizes: Map<Long, Pair<Int, Int>>,
    imageSizes: Map<Long, Pair<Int, Int>>,
    bgIndex: Int,
    density: Float,
    pxW: Int,
    pxH: Int,
    baseW: Float,
    baseH: Float
) {
    val scale = pxW / baseW
    val titleType = safeTypeface(context, "lalezar", true)
    val bodyType = safeTypeface(context, "vazir", false)
    
    canvas.drawColor(boardBase(bgIndex).toArgb())
    
    val titlePaint = TextPaint().apply {
        color = Color(0xFF3E2723).toArgb()
        textSize = 15f * scale
        isAntiAlias = true
        typeface = titleType
    }
    val bodyPaint = TextPaint().apply {
        color = Color(0xFF5D4037).toArgb()
        textSize = 11f * scale
        isAntiAlias = true
        typeface = bodyType
    }
    
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
        val titleLayout = StaticLayout.Builder
            .obtain(titleText, 0, titleText.length, titlePaint, innerW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        
        val bodyBuilder = StaticLayout.Builder
            .obtain(note.body, 0, note.body.length, bodyPaint, innerW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(5)
            .setEllipsize(TextUtils.TruncateAt.END)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bodyBuilder.setLineSpacing(extraLineSpacing, 1f)
        }
        val bodyLayout = bodyBuilder.build()
        
        val drawX = item.x * scale
        val drawY = item.y * scale
        
        canvas.save()
        canvas.translate(drawX + drawW / 2f, drawY + drawH / 2f)
        canvas.rotate(item.rotation)
        canvas.translate(-drawW / 2f, -drawH / 2f)
        
        bgPaint.color = stickyBody(note.color).toArgb()
        canvas.drawRoundRect(RectF(0f, 0f, drawW, drawH), 3f * scale, 3f * scale, bgPaint)
        
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
        
        canvas.save()
        canvas.translate(drawX + drawW / 2f, drawY + drawH / 2f)
        canvas.rotate(img.rotation)
        canvas.translate(-drawW / 2f, -drawH / 2f)
        canvas.drawRoundRect(RectF(0f, 0f, drawW, drawH), 6f * scale, 6f * scale, framePaint)
        canvas.drawBitmap(bitmap, null, RectF(frame, frame, frame + imgW, frame + imgH), null)
        canvas.restore()
        bitmap.recycle()
    }
}

@Composable
fun BoardScreen(
    notes: List<Note>,
    noteDao: NoteDao,
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit
) {
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
        boards = BoardStore.boards(context)
        items = BoardStore.items(context, currentBoard)
        images = BoardStore.images(context, currentBoard)
    }
    
    val updateFinger: (Float, Float) -> Unit = { x, y ->
        val now = System.currentTimeMillis()
        if (now - lastFingerWrite >= FINGER_THROTTLE_MS) {
            lastFingerWrite = now
            fingerState.value = Offset(x, y)
        }
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
                val saved = BoardStore.addImage(context, currentBoard, Uri.fromFile(file).toString())
                images = images + saved
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
    
    val miniMarkers = remember(visibleItems, images) {
        val list = mutableListOf<MiniMarker>()
        visibleItems.forEach { item ->
            val n = notes.firstOrNull { it.id == item.noteId }
            if (n != null) {
                list.add(MiniMarker(item.x, item.y, BASE_NOTE_WIDTH * item.scale, 120f * item.scale, stickyBody(n.color)))
            }
        }
        images.forEach { img ->
            list.add(MiniMarker(img.x, img.y, BASE_IMAGE_WIDTH * img.scale, 120f * img.scale, Color.White))
        }
        list
    }
    
    fun exportBoard(asPdf: Boolean) {
        if (isExporting) return
        isExporting = true
        
        val snapshotNotes = notes.toList()
        val snapshotItems = items.toList()
        val snapshotImages = images.toList()
        val snapshotNoteSizes = noteSizes.value.toMap()
        val snapshotImageSizes = imageSizes.value.toMap()
        val snapshotBg = bgIndex
        val sSizeIndex = boardSizeIndex
        val sPhone = isPhoneSize
        val sPxW = boardPxW
        val sPxH = boardPxH
        val sName = currentBoardData?.name ?: ""
        
        scope.launch(Dispatchers.IO) {
            try {
                val baseW: Float
                val baseH: Float
                if (sPhone) {
                    baseW = sPxW.toFloat().coerceAtLeast(100f)
                    baseH = sPxH.toFloat().coerceAtLeast(100f)
                } else {
                    baseW = BOARD_SIZES_PT[sSizeIndex].first
                    baseH = BOARD_SIZES_PT[sSizeIndex].second
                }
                
                val pxW = (baseW * EXPORT_QUALITY_SCALE).toInt().coerceAtLeast(300)
                val pxH = (baseH * EXPORT_QUALITY_SCALE).toInt().coerceAtLeast(300)
                
                val dir = File(context.cacheDir, "board_exports")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "board-${System.currentTimeMillis()}.${if (asPdf) "pdf" else "png"}")
                
                if (asPdf) {
                    val pdfDocument = PdfDocument()
                    val page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pxW, pxH, 1).create())
                    renderBoardToCanvas(context, page.canvas, snapshotNotes, snapshotItems, snapshotImages, snapshotNoteSizes, snapshotImageSizes, snapshotBg, density.density, pxW, pxH, baseW, baseH)
                    pdfDocument.finishPage(page)
                    FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
                    pdfDocument.close()
                } else {
                    val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
                    renderBoardToCanvas(context, AndroidCanvas(bitmap), snapshotNotes, snapshotItems, snapshotImages, snapshotNoteSizes, snapshotImageSizes, snapshotBg, density.density, pxW, pxH, baseW, baseH)
                    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                    bitmap.recycle()
                }
                
                withContext(Dispatchers.Main) {
                    isExporting = false
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (asPdf) "application/pdf" else "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "تابلوی $sName")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری تابلو"))
                    Toast.makeText(context, "✅ خروجی با کیفیت بالا آماده شد", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isExporting = false
                    Toast.makeText(context, "خطا در خروجی: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(boardBase(bgIndex))) {
            if (!isExporting) {
                Row(
                    Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .28f)).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = Color(0xFFFFE0B2))
                    }
                    
                    boards.forEach { b ->
                        val selected = b.id == currentBoard
                        Surface(
                            modifier = Modifier.combinedClickable(
                                onClick = { currentBoard = b.id; refresh(); searchQuery = "" },
                                onLongClick = { boardToDelete = b }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFFFFB74D) else Color.White.copy(alpha = .12f),
                            shadowElevation = if (selected) 6.dp else 0.dp
                        ) {
                            Text(b.name, color = if (selected) Color(0xFF3E2723) else Color(0xFFFFE0B2), fontFamily = LalezarFont, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                    }
                    
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.12f)) {
                        Text("📝${items.size} 🖼${images.size}", fontSize = 11.sp
