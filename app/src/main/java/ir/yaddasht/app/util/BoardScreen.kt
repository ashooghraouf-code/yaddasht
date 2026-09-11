package ir.yaddasht.app.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.VazirFont
import ir.yaddasht.app.util.BoardItem
import ir.yaddasht.app.util.BoardStore
import kotlin.random.Random

// ═══════════════════════════════════════════
//  پالت‌ها
// ═══════════════════════════════════════════

private fun boardBase(index: Int): Color = listOf(
    Color(0xFFA1887F), Color(0xFF8D6E63), Color(0xFF37474F), Color(0xFFECEFF1)
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

// ═══════════════════════════════════════════
//  صفحهٔ اصلی تابلو
// ═══════════════════════════════════════════

@Composable
fun BoardScreen(notes: List<Note>, onOpenNote: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var boards by remember { mutableStateOf(BoardStore.boards(context)) }
    var currentBoard by remember { mutableStateOf(boards.firstOrNull()?.id ?: 1L) }
    var items by remember { mutableStateOf(BoardStore.items(context, currentBoard)) }
    var showAddBoard by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var boardName by remember { mutableStateOf("") }

    fun refresh() { items = BoardStore.items(context, currentBoard) }
    val bgIndex = boards.firstOrNull { it.id == currentBoard }?.background ?: 0

    // قاب چوبی بیرونی
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
            // نوار تب‌ها
            Row(
                Modifier.fillMaxWidth()
                    .background(Color.Black.copy(alpha = .28f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = Color(0xFFFFE0B2)) }
                boards.forEach { b ->
                    val selected = b.id == currentBoard
                    Surface(
                        onClick = { currentBoard = b.id; refresh() },
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
                IconButton(onClick = { boardName = ""; showAddBoard = true }) { Icon(Icons.Filled.Add, "تابلو جدید", tint = Color(0xFFFFE0B2)) }
            }

            // سطح تابلو
            Box(Modifier.fillMaxSize()) {
                CorkTexture(bgIndex)
                Vignette()

                if (items.isEmpty()) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗒️", fontSize = 64.sp, modifier = Modifier.rotate(-6f))
                        Text("تابلو خالی است", fontFamily = LalezarFont, fontSize = 22.sp, color = Color.White.copy(alpha = .85f))
                        Text("با دکمهٔ پایین، اولین یادداشت را بچسبان", fontFamily = VazirFont, fontSize = 13.sp, color = Color.White.copy(alpha = .6f))
                    }
                }

                items.forEachIndexed { idx, item ->
                    val note = notes.firstOrNull { it.id == item.noteId }
                    if (note != null) {
                        StickyNote(
                            note = note, item = item, variant = idx,
                            onOpen = { onOpenNote(note.id) },
                            onMoved = { x, y -> BoardStore.move(context, note.id, currentBoard, x, y) },
                            onRemove = { BoardStore.removeItem(context, note.id, currentBoard); refresh() }
                        )
                    }
                }

                // دکمهٔ شناور پونزی
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(18.dp)
                        .size(60.dp).shadow(12.dp, CircleShape).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFB8C00))))
                        .clickable { showAddNote = true }
                        .rotate(-4f),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, "افزودن", tint = Color(0xFF3E2723), modifier = Modifier.size(28.dp)) }
            }
        }
    }

    if (showAddBoard) {
        AlertDialog(
            onDismissRequest = { showAddBoard = false },
            title = { Text("📌 تابلو جدید", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = { OutlinedTextField(boardName, { boardName = it }, label = { Text("نام تابلو") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = {
                    val b = BoardStore.addBoard(context, boardName.ifBlank { "تابلو جدید" })
                    boards = BoardStore.boards(context); currentBoard = b.id; refresh(); showAddBoard = false
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
                if (available.isEmpty()) Text("همهٔ یادداشت‌ها روی این تابلو هستند.")
                else LazyColumn {
                    items(available) { n ->
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .rotate(listOf(-1.5f, 1f, -0.5f, 2f)[n.id.toInt() % 4])
                                .clip(RoundedCornerShape(4.dp))
                                .background(stickyBody(n.color))
                                .shadow(4.dp, RoundedCornerShape(4.dp))
                                .clickable { BoardStore.addItem(context, n.id, currentBoard); refresh(); showAddNote = false }
                                .padding(12.dp)
                        ) {
                            Text(
                                n.title.ifBlank { n.body.take(30).ifBlank { "بدون عنوان" } },
                                fontFamily = VazirFont, fontSize = 14.sp, color = Color(0xFF3E2723),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddNote = false }) { Text("بستن") } }
        )
    }
}

// ═══════════════════════════════════════════
//  بافت چوب‌پنبه + وینیت
// ═══════════════════════════════════════════

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
                color = if (dark) Color(0xFF5D4037).copy(alpha = .16f) else Color(0xFFD7CCC8).copy(alpha = .12f),
                radius = r, center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun Vignette() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = .30f)),
                center = androidx.compose.ui.geometry.Offset.Unspecified,
                radius = 900f
            )
        )
    )
}

// ═══════════════════════════════════════════
//  یادداشت چسبان (پونز / چسب + سایه + تا)
// ═══════════════════════════════════════════

@Composable
private fun StickyNote(
    note: Note,
    item: BoardItem,
    variant: Int,
    onOpen: () -> Unit,
    onMoved: (Float, Float) -> Unit,
    onRemove: () -> Unit
) {
    val density = LocalDensity.current
    var pos by remember(item.noteId, item.boardId) { mutableStateOf(Offset(item.x, item.y)) }
    var dragging by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (dragging) 1.07f else 1f, label = "lift")
    val elev by animateDpAsState(if (dragging) 22.dp else 7.dp, label = "shadow")
    val rot by animateFloatAsState(if (dragging) 0f else item.rotation, label = "rot")

    val widthDp = listOf(150, 190, 230)[item.sizeIndex.coerceIn(0, 2)].dp
    val body = stickyBody(note.color)
    val usePin = variant % 2 == 0

    Box(
        Modifier
            .offset { with(density) { IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx()) } }
            .width(widthDp)
            .graphicsLayer { rotationZ = rot; scaleX = scale; scaleY = scale }
            .pointerInput(item.noteId) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false; onMoved(pos.x, pos.y) },
                    onDragCancel = { dragging = false }
                ) { change, drag ->
                    change.consume()
                    pos += drag / density.density
                }
            }
    ) {
        // برگه
        Box(
            Modifier.fillMaxWidth()
                .shadow(elev, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.linearGradient(listOf(body, body, stickyEdge(note.color))))
                .clickable(onClick = onOpen)
                .padding(top = if (usePin) 20.dp else 14.dp, start = 12.dp, end = 12.dp, bottom = 16.dp)
        ) {
            Column {
                Text(
                    note.title.ifBlank { "بدون عنوان" },
                    fontFamily = LalezarFont, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    note.body, fontFamily = VazirFont, fontSize = 11.sp,
                    color = Color(0xFF5D4037), maxLines = 5, overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }
            CurledCorner()
        }

        // پونز یا چسب
        if (usePin) Thumbtack(pinColor(note.color), Modifier.align(Alignment.TopCenter).offset(y = (-8).dp))
        else TapeStrip(Modifier.align(Alignment.TopCenter).offset(y = (-9).dp))

        // دکمهٔ حذف
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = .35f))
        ) { Icon(Icons.Filled.Close, "حذف", tint = Color.White, modifier = Modifier.size(13.dp)) }
    }
}

// پونز سه‌بعدی
@Composable
private fun Thumbtack(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(20.dp)) {
        Box(Modifier.size(20.dp).offset(y = 3.dp).clip(CircleShape).background(Color.Black.copy(alpha = .30f)))
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(
                Brush.radialGradient(listOf(color.copy(alpha = .95f), color, color.copy(alpha = .55f)))
            )
        )
        Box(
            Modifier.size(6.dp).align(Alignment.TopStart).offset(4.dp, 4.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = .75f))
        )
    }
}

// چسب نواری
@Composable
private fun TapeStrip(modifier: Modifier = Modifier) {
    Box(
        modifier.width(58.dp).height(18.dp).rotate(-3f)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = .38f))
    )
}

// گوشهٔ تاخوردهٔ کاغذ
@Composable
private fun CurledCorner() {
    Canvas(Modifier.size(26.dp).align(Alignment.BottomEnd)) {
        val p = Path().apply {
            moveTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            p, Brush.linearGradient(
                listOf(Color.Black.copy(alpha = .22f), Color.Black.copy(alpha = .05f))
            )
        )
    }
}
