@file:OptIn(ExperimentalFoundationApi::class)

package ir.yaddasht.app.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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

private val BOARD_SIZE_LABELS = listOf("📱 گوشی", "📄 A4", "📐 A3", "🗺️ A2")
private val BOARD_SIZES_DP = listOf(
    Pair(400f, 700f),
    Pair(595f, 842f),
    Pair(842f, 1191f),
    Pair(1191f, 1684f)
)

private val BASE_NOTE_WIDTH = 150f
private val BASE_IMAGE_WIDTH = 150f

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
    var canUndo by remember { mutableStateOf(BoardStore.canUndo(context, currentBoard)) }
    var isExporting by remember { mutableStateOf(false) }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var draggingImageId by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var boardHeightDp by remember { mutableFloatStateOf(0f) }

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var imageToDelete by remember { mutableStateOf<BoardImage?>(null) }

    val refresh: () -> Unit = {
        boards = BoardStore.boards(context)
        items = BoardStore.items(context, currentBoard)
        images = BoardStore.images(context, currentBoard)
        canUndo = BoardStore.canUndo(context, currentBoard)
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { pickedUri ->
            try {
                context.contentResolver.takePersistableUriPermission(pickedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                BoardStore.addImage(context, currentBoard, pickedUri.toString())
                refresh()
                Toast.makeText(context, "🖼️ تصویر اضافه شد", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                try {
                    val inputStream = context.contentResolver.openInputStream(pickedUri)
                    if (inputStream != null) {
                        val dir = File(context.filesDir, "board_images")
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(dir, "img-${System.currentTimeMillis()}.jpg")
                        file.outputStream().use { out -> inputStream.copyTo(out) }
                        inputStream.close()
                        BoardStore.addImage(context, currentBoard, Uri.fromFile(file).toString())
                        refresh()
                        Toast.makeText(context, "🖼️ تصویر اضافه شد", Toast.LENGTH_SHORT).show()
                    }
                } catch (e2: Exception) {
                    Toast.makeText(context, "خطا: ${e2.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val currentBoardData = boards.firstOrNull { it.id == currentBoard }
    val bgIndex = currentBoardData?.background ?: 0
    val boardSizeIndex = currentBoardData?.sizeIndex ?: 1
    val (boardWidthDp, boardHeightDpActual) = BOARD_SIZES_DP[boardSizeIndex.coerceIn(0, 3)]

    val visibleItems = remember(items, searchQuery, notes) {
        if (searchQuery.isBlank()) items
        else items.filter { item ->
            val n = notes.firstOrNull { it.id == item.noteId } ?: return@filter false
            n.title.contains(searchQuery, true) || n.body.contains(searchQuery, true)
        }
    }

    val trashTopDp = if (boardHeightDp > 1f) boardHeightDp - 150f else Float.MAX_VALUE
    val isOverTrash = (draggingNoteId != null || draggingImageId != null) && dragY > trashTopDp

    fun exportToPdf() {
        if (isExporting) return
        isExporting = true
        scope.launch(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "board_exports")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "board-${System.currentTimeMillis()}.pdf")
                
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(boardWidthDp.toInt(), boardHeightDpActual.toInt(), 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(boardBase(bgIndex).toArgb())

                val textPaint = TextPaint().apply {
                    color = androidx.compose.ui.graphics.Color(0xFF3E2723).toArgb()
                    textSize = 11f * (context.resources.displayMetrics.density)
                    isAntiAlias = true
                }
                val titlePaint = TextPaint().apply {
                    color = androidx.compose.ui.graphics.Color(0xFF3E2723).toArgb()
                    textSize = 15f * (context.resources.displayMetrics.density)
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val bgPaint = Paint().apply { isAntiAlias = true }

                items.forEach { item ->
                    val note = notes.firstOrNull { it.id == item.noteId } ?: return@forEach
                    val noteWidth = (BASE_NOTE_WIDTH * item.scale) * context.resources.displayMetrics.density
                    val noteHeight = 200f * context.resources.displayMetrics.density

                    canvas.save()
                    canvas.translate(item.x * context.resources.displayMetrics.density, item.y * context.resources.displayMetrics.density)
                    canvas.rotate(item.rotation)
                    canvas.scale(item.scale, item.scale)

                    bgPaint.color = stickyBody(note.color).toArgb()
                    val rect = android.graphics.RectF(0f, 0f, noteWidth, noteHeight)
                    canvas.drawRoundRect(rect, 9f, 9f, bgPaint)

                    var yPos = 40f
                    
                    val titleText = note.title.ifBlank { "بدون عنوان" }
                    val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, noteWidth.toInt() - 24)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                    canvas.save()
                    canvas.translate(12f, yPos)
                    titleLayout.draw(canvas)
                    canvas.restore()
                    yPos += titleLayout.height.toFloat() + 10f

                    val bodyText = note.body
                    val bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, textPaint, noteWidth.toInt() - 24)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                    canvas.save()
                    canvas.translate(12f, yPos)
                    bodyLayout.draw(canvas)
                    canvas.restore()

                    canvas.restore()
                }

                images.forEach { img ->
                    val bitmap = loadBitmapFromUri(context, img.uri)
                    if (bitmap != null) {
                        canvas.save()
                        canvas.translate(img.x * context.resources.displayMetrics.density, img.y * context.resources.displayMetrics.density)
                        canvas.rotate(img.rotation)
                        canvas.scale(img.scale, img.scale)
                        val imgWidth = (BASE_IMAGE_WIDTH * context.resources.displayMetrics.density)
                        val imgHeight = (imgWidth * bitmap.height / bitmap.width.toFloat())
                        canvas.drawBitmap(bitmap, null, android.graphics.RectF(0f, 0f, imgWidth, imgHeight), null)
                        canvas.restore()
                        bitmap.recycle()
                    }
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
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .28f))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Spacer(modifier = Modifier.width(1.dp).height(28.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)))

                        IconButton(onClick = { BoardStore.setBackground(context, currentBoard, (bgIndex + 1) % 4); refresh() }) {
                            Icon(Icons.Filled.Palette, "تغییر پس‌زمینه", tint = androidx.compose.ui.graphics.Color(0xFFFFE0B2))
                        }
                        Surface(onClick = { BoardStore.setBoardSize(context, currentBoard, (boardSizeIndex + 1) % 4); refresh() }, shape = RoundedCornerShape(8.dp), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)) {
                            Text(BOARD_SIZE_LABELS[boardSizeIndex], fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFFFFE0B2), fontFamily = VazirFont, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        IconButton(onClick = { if (BoardStore.undo(context, currentBoard)) refresh() }, enabled = canUndo) {
                            Icon(Icons.Filled.Undo, "بازگشت", tint = if (canUndo) androidx.compose.ui.graphics.Color(0xFFFFE0B2) else androidx.compose.ui.graphics.Color(0xFF777777))
                        }
                        IconButton(onClick = { boardName = ""; showAddBoard = true }) {
                            Icon(Icons.Filled.Add, "تابلو جدید", tint = androidx.compose.ui.graphics.Color(0xFFFFE0B2))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(50.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                                    ),
                                    startX = 50f,
                                    endX = 0f
                                )
                            )
                    )
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

            Box(Modifier.fillMaxSize().onSizeChanged { s -> boardHeightDp = with(density) { s.height.toDp().value } }) {
                val scrollStateV = rememberScrollState()
                val scrollStateH = rememberScrollState()
                
                Box(Modifier.fillMaxSize().verticalScroll(scrollStateV).horizontalScroll(scrollStateH)) {
                    Box(Modifier.width(boardWidthDp.dp).height(boardHeightDpActual.dp)) {
                        CorkTexture(bgIndex, boardWidthDp, boardHeightDpActual)
                        Vignette(bgIndex, boardWidthDp, boardHeightDpActual)

                        if (visibleItems.isEmpty() && images.isEmpty() && !isExporting) {
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (searchQuery.isNotBlank()) "🔍" else "🗒️", fontSize = 64.sp, modifier = Modifier.rotate(if (searchQuery.isNotBlank()) 0f else -6f))
                                Text(if (searchQuery.isNotBlank()) "یادداشتی یافت نشد" else "تابلو خالی است", fontFamily = LalezarFont, fontSize = 22.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .85f))
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
                                    isDraggingThis = draggingNoteId == note.id,
                                    isOverTrash = isOverTrash && draggingNoteId == note.id,
                                    onOpen = { onOpenNote(note.id) },
                                    onMoved = { x, y -> BoardStore.move(context, note.id, currentBoard, x, y); refresh() },
                                    onRotated = { rot -> BoardStore.rotate(context, note.id, currentBoard, rot); refresh() },
                                    onScaleChanged = { scale -> BoardStore.setScale(context, note.id, currentBoard, scale); refresh() },
                                    onDragStart = { draggingNoteId = note.id },
                                    onDragUpdate = { y -> dragY = y },
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
                                isDraggingThis = draggingImageId == img.id,
                                isOverTrash = isOverTrash && draggingImageId == img.id,
                                onMoved = { x, y -> BoardStore.moveImage(context, img.id, currentBoard, x, y); refresh() },
                                onRotated = { rot -> BoardStore.rotateImage(context, img.id, currentBoard, rot); refresh() },
                                onScaleChanged = { scale -> BoardStore.setImageScale(context, img.id, currentBoard, scale); refresh() },
                                onDragStart = { draggingImageId = img.id },
                                onDragUpdate = { y -> dragY = y },
                                onDragEnd = { y, canceled ->
                                    if (!canceled && y > trashTopDp) imageToDelete = img
                                    draggingImageId = null
                                }
                            )
                        }
                    }
                }

                if ((draggingNoteId != null || draggingImageId != null) && !isExporting) {
                    TrashBin(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), highlighted = isOverTrash)
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
            }
        }

        if (!isExporting) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(40.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.Close, "بازگشت", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(22.dp))
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
                if (available.isEmpty()) {
                    Text("همهٔ یادداشت‌ها روی این تابلو هستند.")
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
private fun BoardImageItem(
    image: BoardImage,
    stagger: Int,
    isDraggingThis: Boolean,
    isOverTrash: Boolean,
    onMoved: (Float, Float) -> Unit,
    onRotated: (Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragUpdate: (Float) -> Unit,
    onDragEnd: (Float, Boolean) -> Unit
) {
    val density = LocalDensity.current
    var pos by remember(image.id, image.boardId) { mutableStateOf(Offset(image.x, image.y)) }
    var rotation by remember(image.id, image.boardId) { mutableFloatStateOf(image.rotation) }
    var scale by remember(image.id, image.boardId) { mutableFloatStateOf(image.scale) }
    var visualZoom by remember { mutableFloatStateOf(1f) }
    var appeared by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    var gestureActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(stagger * 70L)
        appeared = true
    }
    
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

    val entranceScale by animateFloatAsState(targetValue = if (appeared) 1f else 0.5f, label = "in-scale")
    val entranceAlpha by animateFloatAsState(targetValue = if (appeared) 1f else 0f, label = "in-alpha")
    val trashScale by animateFloatAsState(targetValue = if (isOverTrash) 0.4f else 1f, label = "trash-scale")
    val trashAlpha by animateFloatAsState(targetValue = if (isOverTrash) 0.3f else 1f, label = "trash-alpha")

    val widthDp = (BASE_IMAGE_WIDTH * scale).dp

    Box(
        Modifier
            .alpha(entranceAlpha * trashAlpha)
            .offset { with(density) { IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx()) } }
            .width(widthDp)
            .graphicsLayer {
                rotationZ = rotation
                val s = entranceScale * visualZoom * trashScale
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
                                    pos += panChange / density.density
                                    rotation += rotationChange
                                    scale = (scale * zoomChange).coerceIn(0.3f, 3.0f)
                                    visualZoom = zoomChange.coerceIn(0.5f, 2.0f)
                                    onDragUpdate(pos.y)
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
private fun CorkTexture(bgIndex: Int, widthDp: Float, heightDp: Float) {
    val base = boardBase(bgIndex)
    Canvas(Modifier.width(widthDp.dp).height(heightDp.dp)) {
        drawRect(base)
        val rnd = Random(1337)
        repeat(450) {
            val x = rnd.nextFloat() * size.width
            val y = rnd.nextFloat() * size.height
            val r = rnd.nextFloat() * 2.6f + 0.6f
            val dark = rnd.nextBoolean()
            drawCircle(
                color = if (dark) androidx.compose.ui.graphics.Color(0xFF5D4037).copy(alpha = .16f) else androidx.compose.ui.graphics.Color(0xFFD7CCC8).copy(alpha = .12f),
                radius = r,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun Vignette(bgIndex: Int, widthDp: Float, heightDp: Float) {
    val strength = if (bgIndex == 3) .12f else .30f
    Box(
        Modifier.width(widthDp.dp).height(heightDp.dp).background(
            Brush.radialGradient(
                colors = listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Black.copy(alpha = strength)),
                center = androidx.compose.ui.geometry.Offset.Unspecified,
                radius = 900f
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
    isDraggingThis: Boolean,
    isOverTrash: Boolean,
    onOpen: () -> Unit,
    onMoved: (Float, Float) -> Unit,
    onRotated: (Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragUpdate: (Float) -> Unit,
    onDragEnd: (Float, Boolean) -> Unit
) {
    val density = LocalDensity.current
    var pos by remember(item.noteId, item.boardId) { mutableStateOf(Offset(item.x, item.y)) }
    var rotation by remember(item.noteId, item.boardId) { mutableFloatStateOf(item.rotation) }
    var scale by remember(item.noteId, item.boardId) { mutableFloatStateOf(item.scale) }
    var visualZoom by remember { mutableFloatStateOf(1f) }
    var appeared by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    var gestureActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(stagger * 70L)
        appeared = true
    }
    
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

    val entranceScale by animateFloatAsState(targetValue = if (appeared) 1f else 0.5f, label = "in-scale")
    val entranceAlpha by animateFloatAsState(targetValue = if (appeared) 1f else 0f, label = "in-alpha")
    val trashScale by animateFloatAsState(targetValue = if (isOverTrash) 0.4f else 1f, label = "trash-scale")
    val trashAlpha by animateFloatAsState(targetValue = if (isOverTrash) 0.3f else 1f, label = "trash-alpha")

    val widthDp = (BASE_NOTE_WIDTH * scale).dp
    val body = stickyBody(note.color)
    val usePin = variant % 2 == 0

    Box(
        Modifier
            .alpha(entranceAlpha * trashAlpha)
            .offset { with(density) { IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx()) } }
            .width(widthDp)
            .graphicsLayer {
                rotationZ = rotation
                val s = entranceScale * visualZoom * trashScale
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
                                    pos += panChange / density.density
                                    rotation += rotationChange
                                    scale = (scale * zoomChange).coerceIn(0.3f, 3.0f)
                                    visualZoom = zoomChange.coerceIn(0.5f, 2.0f)
                                    onDragUpdate(pos.y)
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
            Column {
                Text(note.title.ifBlank { "بدون عنوان" }, fontFamily = LalezarFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF3E2723), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(note.body, fontFamily = VazirFont, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFF5D4037), maxLines = 5, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
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
