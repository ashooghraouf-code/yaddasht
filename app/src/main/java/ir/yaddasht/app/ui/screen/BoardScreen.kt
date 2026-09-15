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
    androidx.compose.ui.graphics.Color(0xFFA1887F), androidx.compose.ui.graphics.Color(0xFF6D4C41), androidx.compose.ui.graphics.Color(0xFF263238), androidx.compose.ui.graphics.Color(0xFFECEFF1)
)[index.coerceIn(0, 3)]

private fun stickyBody(index: Int): androidx.compose.ui.graphics.Color = listOf(
    androidx.compose.ui.graphics.Color(0xFFFFF59D), androidx.compose.ui.graphics.Color(0xFFF8BBD0), androidx.compose.ui.graphics.Color(0xFFB3E5FC),
    androidx.compose.ui.graphics.Color(0xFFC8E6C9), androidx.compose.ui.graphics.Color(0xFFFFE0B2), androidx.compose.ui.graphics.Color(0xFFE1BEE7)
)[index.coerceIn(0, 5)]

private fun stickyEdge(index: Int): androidx.compose.ui.graphics.Color = stickyBody(index).copy(alpha = .55f)

private val BOARD_SIZE_LABELS = listOf("📱 گوشی", "📄 A4", "📐 A3", "🗺️ A2")
private val BOARD_SIZES_DP = listOf(Pair(400f, 700f), Pair(595f, 842f), Pair(842f, 1191f), Pair(1191f, 1684f))

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
                    val titleLayout = StaticLayout.Builder.obtain(note.title.ifBlank { "بدون عنوان" }, 0, note.title.ifBlank { "بدون عنوان" }.length, titlePaint, noteWidth.toInt() - 24)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                    canvas.save(); canvas.translate(12f, yPos); titleLayout.draw(canvas); canvas.restore()
                    yPos += titleLayout.height.toFloat() + 10f

                    val bodyLayout = StaticLayout.Builder.obtain(note.body, 0, note.body.length, textPaint, noteWidth.toInt() - 24)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                    canvas.save(); canvas.translate(12f, yPos); bodyLayout.draw(canvas); canvas.restore()

                    canvas.restore()
                }

                images.forEach { img ->
                    val bitmap = loadBitmapFromUri(context, img.uri)
                    if (bitmap != null) {
                        canvas.save()
                        canvas.translate(img.x * context.resources.displayMetrics.density, img.y * context.resources.displayMetrics.density)
                        canvas.rotate(img.rotation
