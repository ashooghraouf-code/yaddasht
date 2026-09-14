@file:OptIn(ExperimentalFoundationApi::class)

package ir.yaddasht.app.ui.screen

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.VazirFont
import ir.yaddasht.app.util.BoardImage
import ir.yaddasht.app.util.BoardItem
import ir.yaddasht.app.util.BoardStore
import kotlinx.coroutines.delay
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

private val SIZE_WIDTHS = listOf(130, 180, 240)
private val SIZE_LABELS = listOf("کوچک S", "متوسط M", "بزرگ L")
private val IMAGE_WIDTHS = listOf(120, 180, 240)

@Composable
fun BoardScreen(notes: List<Note>, onOpenNote: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var boards by remember { mutableStateOf(BoardStore.boards(context)) }
    var currentBoard by remember { mutableStateOf(boards.firstOrNull()?.id ?: 1L) }
    var items by remember { mutableStateOf(BoardStore.items(context, currentBoard)) }
    var images by remember { mutableStateOf(BoardStore.images(context, currentBoard)) }

    var showAddBoard by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var boardName by remember { mutableStateOf("") }
    var sizeForNote by remember { mutableStateOf<Long?>(null) }
    var canUndo by remember { mutableStateOf(BoardStore.canUndo(context, currentBoard)) }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var draggingImageId by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var boardHeightDp by remember { mutableFloatStateOf(0f) }

    var capturingForShare by remember { mutableStateOf(false) }

    // ═══ ✅ تعریف refresh قبل از launcher ═══
    fun refresh() {
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

    val bgIndex = boards.firstOrNull { it.id == currentBoard }?.background ?: 0

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
                putExtra(Intent.EXTRA_SUBJECT, "تابلوی ${boards.firstOrNull { it.id == currentBoard }?.name ?: ""}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری تابلو"))
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

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color(0xFF4E342E), Color(0xFF795548), Color(0xFF3E2723), Color(0xFF6D4C41))
            )
        ).padding(12.dp)
    ) {
        Column(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = Color(0xFFFFE0B2))
                    }
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
                CorkTexture(bgIndex)
                Vignette(bgIndex)

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
                                BoardStore.rotate(context, note.id, currentBoard, rot)
                                refresh()
                            },
                            onSize = { sizeForNote = note.id },
                            onDragStart = { draggingNoteId = note.id },
                            onDragUpdate = { y -> dragY = y },
                            onDragEnd = { y, canceled ->
                                if (!canceled && y > trashTopDp) {
                                    BoardStore.removeItem(context, note.id, currentBoard)
                                    refresh()
                                    Toast.makeText(context, "🗑️ یادداشت حذف شد", Toast.LENGTH_SHORT).show()
                                }
                                draggingNoteId = null
                            },
                            onRemove = {
                                BoardStore.removeItem(context, note.id, currentBoard)
                                refresh()
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
                        onMoved = { x, y ->
                            BoardStore.moveImage(context, img.id, currentBoard, x, y)
                            refresh()
                        },
                        onRotated = { rot ->
                            BoardStore.rotateImage(context, img.id, currentBoard, rot)
                            refresh()
                        },
                        onDragStart = { draggingImageId = img.id },
                        onDragUpdate = { y -> dragY = y },
                        onDragEnd = { y, canceled ->
                            if (!canceled && y > trashTopDp) {
                                BoardStore.removeImage(context, img.id, currentBoard)
                                refresh()
                                Toast.makeText(context, "🗑️ تصویر حذف شد", Toast.LENGTH_SHORT).show()
                            }
                            draggingImageId = null
                        },
                        onRemove = {
                            BoardStore.removeImage(context, img.id, currentBoard)
                            refresh()
                        }
                    )
                }

                if (draggingNoteId != null || draggingImageId != null) {
                    TrashBin(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                        highlighted = isOverTrash
                    )
                }

                if (!capturingForShare) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(18.dp)
                            .size(60.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFB8C00)))
                            )
                            .combinedClickable(onClick = { showAddNote = true })
                            .rotate(-4f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, "افزودن", tint = Color(0xFF3E2723), modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }

    if (showAddBoard) {
        AlertDialog(
            onDismissRequest = { showAddBoard = false },
            title = { Text("📌 تابلو جدید", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                OutlinedTextField(
                    boardName, { boardName = it },
                    label = { Text("نام تابلو") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val b = BoardStore.addBoard(context, boardName.ifBlank { "تابلو جدید" })
                    currentBoard = b.id
                    refresh()
                    showAddBoard = false
                }) { Text("ساخت") }
            },
            dismissButton = { TextButton(onClick = { showAddBoard = false }) { Text("انصراف") } }
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
                                Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
                                    fontFamily = VazirFont, fontSize = 14.sp,
                                    color = Color(0xFF3E2723),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddNote = false }) { Text("بستن") } }
        )
    }

    sizeForNote?.let { noteId ->
        val current = items.firstOrNull { it.noteId == noteId }?.sizeIndex ?: 1
        AlertDialog(
            onDismissRequest = { sizeForNote = null },
            title = { Text("📐 اندازه یادداشت", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SIZE_LABELS.forEachIndexed { i, label ->
                        Surface(
                            onClick = {
                                BoardStore.setSize(context, noteId, currentBoard, i)
                                refresh()
                                sizeForNote = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (i == current) Color(0xFFFFB74D) else Color(0xFFEFEFEF),
                            shadowElevation = 3.dp
                        ) {
                            Text(
                                label,
                                color = if (i == current) Color(0xFF3E2723) else Color(0xFF555555),
                                fontFamily = VazirFont, fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sizeForNote = null }) { Text("بستن") } }
        )
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
    onDragStart: () -> Unit,
    onDragUpdate: (Float) -> Unit,
    onDragEnd: (Float, Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val density = LocalDensity.current
    var pos by remember(image.id, image.boardId) { mutableStateOf(Offset(image.x, image.y)) }
    var rotation by remember(image.id, image.boardId) { mutableFloatStateOf(image.rotation) }
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
        }
    }

    LaunchedEffect(lastInteraction) {
        if (lastInteraction > 0) {
            delay(400)
            visualZoom = 1f
        }
    }

    val entranceScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        label = "in-scale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        label = "in-alpha"
    )

    val trashScale by animateFloatAsState(
        targetValue = if (isOverTrash) 0.4f else 1f,
        label = "trash-scale"
    )
    val trashAlpha by animateFloatAsState(
        targetValue = if (isOverTrash) 0.3f else 1f,
        label = "trash-alpha"
    )

    val widthDp = IMAGE_WIDTHS[image.sizeIndex.coerceIn(0, 2)].dp

    Box(
        Modifier
            .alpha(entranceAlpha * trashAlpha)
            .offset {
                with(density) {
                    IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx())
                }
            }
            .width(widthDp)
            .graphicsLayer {
                rotationZ = rotation
                val scale = entranceScale * visualZoom * trashScale
                scaleX = scale
                scaleY = scale
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
                                    visualZoom = (visualZoom * zoomChange).coerceIn(0.5f, 2.0f)
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
                .background(Color.White)
                .padding(4.dp)
        ) {
            AsyncImage(
                model = Uri.parse(image.uri),
                contentDescription = "تصویر",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!isDraggingThis) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = .55f))
            ) {
                Icon(
                    Icons.Filled.Close, "حذف",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun TrashBin(modifier: Modifier = Modifier, highlighted: Boolean) {
    val size by animateDpAsState(if (highlighted) 80.dp else 64.dp, label = "trash-size")
    val bg by animateFloatAsState(if (highlighted) 0.95f else 0.55f, label = "trash-bg")

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = bg)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Delete, "حذف",
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        }
        Text(
            if (highlighted) "رها کن تا حذف شود!" else "بکش اینجا",
            color = if (highlighted) Color(0xFFFFCDD2) else Color.White.copy(alpha = .7f),
            fontFamily = LalezarFont, fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CorkTexture(bgIndex: Int) {
    val base = boardBase(bgIndex)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(base)
        val rnd = Random(1337)
        repeat(450) {
            val x = rnd.nextFloat() * size.width
            val y = rnd.nextFloat() * size.height
            val r = rnd.nextFloat() * 2.6f + 0.6f
            val dark = rnd.nextBoolean()
            drawCircle(
                color = if (dark) Color(0xFF5D4037).copy(alpha = .16f)
                else Color(0xFFD7CCC8).copy(alpha = .12f),
                radius = r,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun Vignette(bgIndex: Int) {
    val strength = if (bgIndex == 3) .12f else .30f
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = strength)),
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
    onSize: () -> Unit,
    onDragStart: () -> Unit,
    onDragUpdate: (Float) -> Unit,
    onDragEnd: (Float, Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val density = LocalDensity.current
    var pos by remember(item.noteId, item.boardId) { mutableStateOf(Offset(item.x, item.y)) }
    var rotation by remember(item.noteId, item.boardId) { mutableFloatStateOf(item.rotation) }
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
        }
    }

    LaunchedEffect(lastInteraction) {
        if (lastInteraction > 0) {
            delay(400)
            visualZoom = 1f
        }
    }

    val entranceScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        label = "in-scale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        label = "in-alpha"
    )

    val trashScale by animateFloatAsState(
        targetValue = if (isOverTrash) 0.4f else 1f,
        label = "trash-scale"
    )
    val trashAlpha by animateFloatAsState(
        targetValue = if (isOverTrash) 0.3f else 1f,
        label = "trash-alpha"
    )

    val widthDp = SIZE_WIDTHS[item.sizeIndex.coerceIn(0, 2)].dp
    val body = stickyBody(note.color)
    val usePin = variant % 2 == 0

    Box(
        Modifier
            .alpha(entranceAlpha * trashAlpha)
            .offset {
                with(density) {
                    IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx())
                }
            }
            .width(widthDp)
            .graphicsLayer {
                rotationZ = rotation
                val scale = entranceScale * visualZoom * trashScale
                scaleX = scale
                scaleY = scale
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
                                    visualZoom = (visualZoom * zoomChange).coerceIn(0.5f, 2.0f)
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
                .background(
                    Brush.linearGradient(listOf(body, body, stickyEdge(note.color)))
                )
                .combinedClickable(onClick = onOpen, onLongClick = onSize)
                .padding(
                    top = if (usePin) 20.dp else 14.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 16.dp
                )
        ) {
            Column {
                Text(
                    note.title.ifBlank { "بدون عنوان" },
                    fontFamily = LalezarFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    note.body,
                    fontFamily = VazirFont,
                    fontSize = 11.sp,
                    color = Color(0xFF5D4037),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }
            CurledCorner(Modifier.align(Alignment.BottomEnd))
        }

        if (usePin) {
            Thumbtack(
                pinColor(note.color),
                Modifier.align(Alignment.TopCenter).offset(y = (-8).dp)
            )
        } else {
            TapeStrip(Modifier.align(Alignment.TopCenter).offset(y = (-9).dp))
        }

        if (!isDraggingThis) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = .35f))
            ) {
                Icon(
                    Icons.Filled.Close, "حذف",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun Thumbtack(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(20.dp)) {
        Box(
            Modifier.size(20.dp)
                .offset(y = 3.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = .30f))
        )
        Box(
            Modifier.size(20.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            color.copy(alpha = .95f),
                            color,
                            color.copy(alpha = .55f)
                        )
                    )
                )
        )
        Box(
            Modifier.size(6.dp)
                .align(Alignment.TopStart)
                .offset(4.dp, 4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = .75f))
        )
    }
}

@Composable
private fun TapeStrip(modifier: Modifier = Modifier) {
    Box(
        modifier.width(58.dp)
            .height(18.dp)
            .rotate(-3f)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = .38f))
    )
}

@Composable
private fun CurledCorner(modifier: Modifier = Modifier) {
    Canvas(modifier.size(26.dp)) {
        val p = Path().apply {
            moveTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            p,
            Brush.linearGradient(
                listOf(
                    Color.Black.copy(alpha = .22f),
                    Color.Black.copy(alpha = .05f)
                )
            )
        )
    }
}
