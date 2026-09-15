@file:OptIn(ExperimentalFoundationApi::class)

package ir.yaddasht.app.ui.screen

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.view.PixelCopy
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.random.Random

private fun boardBase(index: Int): Color = listOf(
    Color(0xFFA1887F), Color(0xFF6D4C41), Color(0xFF263238), Color(0xFFECEFF1)
)[index.coerceIn(0, 3)]

private fun stickyBody(index: Int): Color = listOf(
    Color(0xFFFFF59D), Color(0xFFF8BBD0), Color(0xFFB3E5FC),
    Color(0xFFC8E6C9), Color(0xFFFFE0B2), Color(0xFFE1BEE7)
)[index.coerceIn(0, 5)]

private fun stickyEdge(index: Int): Color = stickyBody(index).copy(alpha = .55f)

private fun pinColor(index: Int): Color = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFB8C00)
)[index.coerceIn(0, 5)]

private val BOARD_SIZE_LABELS = listOf("📱 صفحه گوشی", "📄 A4", "📐 A3", "🗺️ A2")
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
    val scope = rememberCoroutineScope() // ✅ اضافه شد

    var boards by remember { mutableStateOf(BoardStore.boards(context)) }
    var currentBoard by remember { mutableStateOf(boards.firstOrNull()?.id ?: 1L) }
    var items by remember { mutableStateOf(BoardStore.items(context, currentBoard)) }
    var images by remember { mutableStateOf(BoardStore.images(context, currentBoard)) }

    var showAddBoard by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var boardName by remember { mutableStateOf("") }
    var canUndo by remember { mutableStateOf(BoardStore.canUndo(context, currentBoard)) }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var draggingImageId by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var boardHeightDp by remember { mutableFloatStateOf(0f) }

    var capturingForShare by remember { mutableStateOf(false) }

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var imageToDelete by remember { mutableStateOf<BoardImage?>(null) }

    val refresh: () -> Unit = {
        boards = BoardStore.boards(context)
        items = BoardStore.items(context, currentBoard)
        images = BoardStore.images(context, currentBoard)
        canUndo = BoardStore.canUndo(context, currentBoard)
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
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
                    } else {
                        Toast.makeText(context, "خطا: نمی‌توان تصویر را خواند", Toast.LENGTH_LONG).show()
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

    fun captureAndShare() {
        try {
            val activity = context as? Activity
            if (activity == null) {
                Toast.makeText(context, "خطا: دسترسی به صفحه نیست", Toast.LENGTH_SHORT).show()
                return
            }
            val view = activity.window.decorView.rootView
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(activity.window, bitmap, { result ->
                    if (result == PixelCopy.SUCCESS) {
                        val dir = File(context.cacheDir, "board_shares")
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(dir, "board-${System.currentTimeMillis()}.png")
                        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                        bitmap.recycle()
                        
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "تابلوی ${currentBoardData?.name ?: ""}")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری تابلو"))
                    }
                }, android.os.Handler(activity.mainLooper))
            } else {
                val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                view.draw(canvas)
                
                val dir = File(context.cacheDir, "board_shares")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "board-${System.currentTimeMillis()}.png")
                file.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
                bmp.recycle()
                
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "تابلوی ${currentBoardData?.name ?: ""}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری تابلو"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(capturingForShare) {
        if (capturingForShare) {
            delay(120)
            captureAndShare()
            capturingForShare = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .background(boardBase(bgIndex))
        ) {
            if (!capturingForShare) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Color.Black.copy(alpha = .28f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    boards.forEach { b ->
                        val selected = b.id == currentBoard
                        Surface(
                            onClick = { currentBoard = b.id; refresh(); searchQuery = "" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFFFFB74D) else Color.White.copy(alpha = .12f),
                            shadowElevation = if (selected) 6.dp else 0.dp
                        ) {
                            Text(
                                b.name,
                                color = if (selected) Color(0xFF3E2723) else Color(0xFFFFE0B2),
                                fontFamily = LalezarFont, fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    IconButton(onClick = {
                        BoardStore.setBackground(context, currentBoard, (bgIndex + 1) % 4)
                        refresh()
                    }) { Text("🎨", fontSize = 18.sp) }
                    IconButton(onClick = {
                        val next = (boardSizeIndex + 1) % 4
                        BoardStore.setBoardSize(context, currentBoard, next)
                        refresh()
                    }) { Text(BOARD_SIZE_LABELS[boardSizeIndex], fontSize = 12.sp, color = Color(0xFFFFE0B2)) }
                    IconButton(
                        onClick = { if (BoardStore.undo(context, currentBoard)) refresh() },
                        enabled = canUndo
                    ) {
                        Icon(
                            Icons.Filled.Undo, "بازگشت",
                            tint = if (canUndo) Color(0xFFFFE0B2) else Color(0xFF777777)
                        )
                    }
                    IconButton(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Filled.Image, "تصویر", tint = Color(0xFFFFE0B2))
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            if (showSearch) Icons.Filled.SearchOff else Icons.Filled.Search,
                            "جستجو", tint = Color(0xFFFFE0B2)
                        )
                    }
                    IconButton(onClick = { capturingForShare = true }) {
                        Icon(Icons.Filled.Share, "اشتراک", tint = Color(0xFFFFE0B2))
                    }
                    IconButton(onClick = { boardName = ""; showAddBoard = true }) {
                        Icon(Icons.Filled.Add, "تابلو جدید", tint = Color(0xFFFFE0B2))
                    }
                }

                if (showSearch) {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = .95f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, "جستجو", tint = Color(0xFF5D4037))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                fontFamily = VazirFont, fontSize = 14.sp, color = Color(0xFF3E2723)
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "جستجو در یادداشت‌ها...",
                                            color = Color(0xFF8D6E63), fontSize = 14.sp,
                                            fontFamily = VazirFont
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, "پاک", tint = Color(0xFF5D4037), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (searchQuery.isNotBlank()) {
                        Text(
                            "${visibleItems.size} یادداشت یافت شد",
                            fontSize = 11.sp, color = Color.White.copy(alpha = .7f),
                            fontFamily = VazirFont,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Box(
                Modifier.fillMaxSize()
                    .onSizeChanged { s -> boardHeightDp = with(density) { s.height.toDp().value } }
            ) {
                val scrollStateV = rememberScrollState()
                val scrollStateH = rememberScrollState()
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollStateV)
                        .horizontalScroll(scrollStateH)
                ) {
                    Box(
                        Modifier
                            .width(boardWidthDp.dp)
                            .height(boardHeightDpActual.dp)
                    ) {
                        CorkTexture(bgIndex, boardWidthDp, boardHeightDpActual)
                        Vignette(bgIndex, boardWidthDp, boardHeightDpActual)

                        if (visibleItems.isEmpty() && images.isEmpty()) {
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (searchQuery.isNotBlank()) "🔍" else "🗒️",
                                    fontSize = 64.sp,
                                    modifier = Modifier.rotate(if (searchQuery.isNotBlank()) 0f else -6f)
                                )
                                Text(
                                    if (searchQuery.isNotBlank()) "یادداشتی یافت نشد" else "تابلو خالی است",
                                    fontFamily = LalezarFont, fontSize = 22.sp,
                                    color = Color.White.copy(alpha = .85f)
                                )
                                Text(
                                    if (searchQuery.isNotBlank()) "عبارت دیگری امتحان کن"
                                    else "با دکمهٔ پایین، اولین یادداشت را بچسبان",
                                    fontFamily = VazirFont, fontSize = 13.sp,
                                    color = Color.White.copy(alpha = .6f)
                                )
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
                                    onMoved = { x, y ->
                                        BoardStore.move(context, note.id, currentBoard, x, y)
                                        refresh()
                                    },
                                    onRotated = { rot ->
                                        BoardStore.rotate
