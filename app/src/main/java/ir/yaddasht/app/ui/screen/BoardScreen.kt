@file:OptIn(ExperimentalFoundationApi::class)

package ir.yaddasht.app.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val BOARD_SIZE_LABELS = listOf("📱 گوشی", "📄 A4", "📐 A3", "🗺️ A2")
private val BOARD_SIZE_DESC = listOf(
    "اندازهٔ صفحهٔ گوشی",
    "۲۱۰×۲۹۷ میلی‌متر",
    "۲۹۷×۴۲۰ میلی‌متر",
    "۴۲۰×۵۹۴ میلی‌متر"
)

private val BOARD_SIZES_PT = listOf(
    0f to 0f,
    595f to 842f,
    842f to 1191f,
    1191f to 1684f
)
private val BOARD_SIZES_DP = BOARD_SIZES_PT

private const val BASE_NOTE_WIDTH = 150f
private const val BASE_IMAGE_WIDTH = 150f

private val EXPORT_RASTER_SCALE = 300f / 72f
private const val MAX_EXPORT_PIXELS = 35_000_000f
private const val FINGER_THROTTLE_MS = 50L

private const val PREFS_BOARD_CONNECTIONS = "board_connections"
private const val CONN_TYPE_NOTE = "note"
private const val CONN_TYPE_IMAGE = "image"

private data class MiniMarker(
    val id: Long,
    val isImage: Boolean,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val rotation: Float,
    val color: Color
)

private data class LiveDrag(
    val id: Long,
    val isImage: Boolean,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val rotation: Float,
    val color: Color
)

private data class ConnRef(
    val type: String,
    val id: Long
)

private data class BoardConnection(
    val id: Long,
    val from: ConnRef,
    val to: ConnRef,
    val colorIndex: Int
)

private data class Endpoint(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

private fun boardBase(index: Int): Color = listOf(
    Color(0xFFF5F5DC),
    Color(0xFFE8EAF6),
    Color(0xFF263238),
    Color(0xFFFFF3E0),
    Color(0xFFE0F2F1),
    Color(0xFFFCE4EC)
)[index.coerceIn(0, 5)]

private fun stickyBody(index: Int): Color = listOf(
    Color(0xFFFFF59D),
    Color(0xFFF8BBD0),
    Color(0xFFB3E5FC),
    Color(0xFFC8E6C9),
    Color(0xFFFFE0B2),
    Color(0xFFE1BEE7)
)[index.coerceIn(0, 5)]

private fun stickyEdge(index: Int): Color = stickyBody(index).copy(alpha = .55f)

private fun pinColor(index: Int): Color = listOf(
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFFFDD835),
    Color(0xFF8E24AA),
    Color(0xFFFB8C00)
)[index.coerceIn(0, 5)]

private fun connectionColor(index: Int): Color = listOf(
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFFFDD835),
    Color(0xFF8E24AA),
    Color(0xFFFB8C00)
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
            ResourcesCompat.getFont(context, id)
                ?: if (bold) AndroidTypeface.DEFAULT_BOLD else AndroidTypeface.DEFAULT
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
        if (uri.scheme == "file") {
            uri.path?.let { BitmapFactory.decodeFile(it) }
        } else {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: Exception) {
        null
    }
}

private fun connectionsPrefs(context: Context) =
    context.getSharedPreferences(PREFS_BOARD_CONNECTIONS, Context.MODE_PRIVATE)

private fun connectionKey(boardId: Long): String = "conn_$boardId"

private fun loadConnections(context: Context, boardId: Long): List<BoardConnection> {
    val json = connectionsPrefs(context).getString(connectionKey(boardId), null)
        ?: return emptyList()

    return try {
        val arr = JSONArray(json)
        val out = mutableListOf<BoardConnection>()

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue

            val id = o.optLong("id", System.nanoTime())
            val fromType = o.optString("fromType")
            val fromId = o.optLong("fromId", -1L)
            val toType = o.optString("toType")
            val toId = o.optLong("toId", -1L)
            val colorIndex = o.optInt("colorIndex", i % 6)

            val fromValid = fromId >= 0L &&
                (fromType == CONN_TYPE_NOTE || fromType == CONN_TYPE_IMAGE)

            val toValid = toId >= 0L &&
                (toType == CONN_TYPE_NOTE || toType == CONN_TYPE_IMAGE)

            if (fromValid && toValid) {
                out.add(
                    BoardConnection(
                        id = id,
                        from = ConnRef(fromType, fromId),
                        to = ConnRef(toType, toId),
                        colorIndex = colorIndex
                    )
                )
            }
        }

        out
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveConnections(
    context: Context,
    boardId: Long,
    connections: List<BoardConnection>
) {
    val arr = JSONArray()

    connections.forEach { c ->
        val o = JSONObject()
        o.put("id", c.id)
        o.put("fromType", c.from.type)
        o.put("fromId", c.from.id)
        o.put("toType", c.to.type)
        o.put("toId", c.to.id)
        o.put("colorIndex", c.colorIndex)
        arr.put(o)
    }

    connectionsPrefs(context)
        .edit()
        .putString(connectionKey(boardId), arr.toString())
        .apply()
}

private fun connectionEndpoint(
    ref: ConnRef,
    boardId: Long,
    items: List<BoardItem>,
    images: List<BoardImage>,
    noteSizes: Map<String, Pair<Int, Int>>,
    imageSizes: Map<Long, Pair<Int, Int>>,
    densityF: Float,
    live: LiveDrag? = null
): Endpoint? {
    val d = densityF.coerceAtLeast(1f)

    if (ref.type == CONN_TYPE_NOTE) {
        val lv = live
        if (lv != null && lv.id == ref.id && !lv.isImage) {
            return Endpoint(
                x = lv.x + lv.w / 2f,
                y = lv.y + lv.h / 2f,
                w = lv.w,
                h = lv.h
            )
        }

        val item = items.firstOrNull { it.noteId == ref.id && it.boardId == boardId }
            ?: return null

        val sc = item.scale.coerceIn(0.3f, 3.0f)
        val key = "${item.noteId}:${item.boardId}"
        val measured = noteSizes[key]

        val w = measured?.first?.toFloat()?.let { it / d }
            ?: (BASE_NOTE_WIDTH * sc)

        val h = measured?.second?.toFloat()?.let { it / d }
            ?: (140f * sc)

        return Endpoint(
            x = item.x + w / 2f,
            y = item.y + h / 2f,
            w = w,
            h = h
        )
    } else {
        val lv = live
        if (lv != null && lv.id == ref.id && lv.isImage) {
            return Endpoint(
                x = lv.x + lv.w / 2f,
                y = lv.y + lv.h / 2f,
                w = lv.w,
                h = lv.h
            )
        }

        val img = images.firstOrNull { it.id == ref.id && it.boardId == boardId }
            ?: return null

        val sc = img.scale.coerceIn(0.3f, 3.0f)
        val measured = imageSizes[img.id]

        val w = measured?.first?.toFloat()?.let { it / d }
            ?: (BASE_IMAGE_WIDTH * sc)

        val h = measured?.second?.toFloat()?.let { it / d }
            ?: (BASE_IMAGE_WIDTH * sc)

        return Endpoint(
            x = img.x + w / 2f,
            y = img.y + h / 2f,
            w = w,
            h = h
        )
    }
}

private fun connectionEndpointLabel(
    ref: ConnRef,
    boardId: Long,
    items: List<BoardItem>,
    images: List<BoardImage>,
    notes: List<Note>
): String {
    return if (ref.type == CONN_TYPE_NOTE) {
        val onBoard = items.any { it.noteId == ref.id && it.boardId == boardId }
        val note = notes.firstOrNull { it.id == ref.id }

        when {
            note == null -> "یادداشت حذف‌شده"
            !onBoard -> "یادداشت خارج از تابلو"
            else -> note.title.ifBlank { note.body.take(24).ifBlank { "بدون عنوان" } }
        }
    } else {
        val img = images.firstOrNull { it.id == ref.id && it.boardId == boardId }
        if (img == null) "تصویر حذف‌شده" else "تصویر"
    }
}

private fun drawConnectionsToCanvas(
    canvas: AndroidCanvas,
    connections: List<BoardConnection>,
    boardId: Long,
    items: List<BoardItem>,
    images: List<BoardImage>,
    noteSizes: Map<String, Pair<Int, Int>>,
    imageSizes: Map<Long, Pair<Int, Int>>,
    density: Float,
    scale: Float
) {
    if (connections.isEmpty()) return

    val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    val outlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = android.graphics.Color.WHITE
        alpha = 191
    }

    connections.forEach { conn ->
        val a = connectionEndpoint(
            ref = conn.from,
            boardId = boardId,
            items = items,
            images = images,
            noteSizes = noteSizes,
            imageSizes = imageSizes,
            densityF = density
        ) ?: return@forEach

        val b = connectionEndpoint(
            ref = conn.to,
            boardId = boardId,
            items = items,
            images = images,
            noteSizes = noteSizes,
            imageSizes = imageSizes,
            densityF = density
        ) ?: return@forEach

        val dx = b.x - a.x
        val dy = b.y - a.y
        val distDp = sqrt(dx * dx + dy * dy)

        if (distDp < 1f) return@forEach

        val nx = -dy / distDp
        val ny = dx / distDp
        val bendDp = (distDp * 0.18f).coerceIn(20f, 120f)

        val cxDp = (a.x + b.x) / 2f + nx * bendDp
        val cyDp = (a.y + b.y) / 2f + ny * bendDp

        val ax = a.x * scale
        val ay = a.y * scale
        val bx = b.x * scale
        val by = b.y * scale
        val qx = cxDp * scale
        val qy = cyDp * scale

        val path = AndroidPath().apply {
            moveTo(ax, ay)
            quadTo(qx, qy, bx, by)
        }

        val colorArgb = connectionColor(conn.colorIndex).toArgb()

        strokePaint.color = colorArgb
        strokePaint.strokeWidth = (3f * scale).coerceAtLeast(2f)
        canvas.drawPath(path, strokePaint)

        val r = (4.5f * scale).coerceAtLeast(3f)

        fillPaint.color = colorArgb
        canvas.drawCircle(ax, ay, r, fillPaint)
        canvas.drawCircle(bx, by, r, fillPaint)

        outlinePaint.strokeWidth = (1.2f * scale).coerceAtLeast(1f)
        canvas.drawCircle(ax, ay, r, outlinePaint)
        canvas.drawCircle(bx, by, r, outlinePaint)
    }
}

private fun renderBoardToCanvas(
    ctx: Context,
    canvas: AndroidCanvas,
    notes: List<Note>,
    items: List<BoardItem>,
    images: List<BoardImage>,
    connections: List<BoardConnection>,
    boardId: Long,
    noteSizes: Map<String, Pair<Int, Int>>,
    imageSizes: Map<Long, Pair<Int, Int>>,
    bgIndex: Int,
    density: Float,
    fontScale: Float,
    outW: Int,
    outH: Int,
    baseWDp: Float,
    baseHDp: Float
) {
    val scale = min(
        outW / baseWDp.coerceAtLeast(1f),
        outH / baseHDp.coerceAtLeast(1f)
    )

    val titleType = safeTypeface(ctx, "lalezar", true)
    val bodyType = safeTypeface(ctx, "vazir", false)

    canvas.drawColor(boardBase(bgIndex).toArgb())

    drawConnectionsToCanvas(
        canvas = canvas,
        connections = connections,
        boardId = boardId,
        items = items,
        images = images,
        noteSizes = noteSizes,
        imageSizes = imageSizes,
        density = density,
        scale = scale
    )

    val titlePaint = TextPaint().apply {
        color = Color(0xFF3E2723).toArgb()
        textSize = 15f * scale * fontScale
        isAntiAlias = true
        typeface = titleType
    }

    val bodyPaint = TextPaint().apply {
        color = Color(0xFF5D4037).toArgb()
        textSize = 11f * scale * fontScale
        isAntiAlias = true
        typeface = bodyType
    }

    val targetLineHeight = 17f * scale * fontScale
    val fm = bodyPaint.fontMetrics
    val extraLineSpacing = (targetLineHeight - (fm.descent - fm.ascent + fm.leading)).coerceAtLeast(0f)

    val bgPaint = Paint().apply { isAntiAlias = true }
    val framePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
    }

    items.forEach { item ->
        val note = notes.firstOrNull { it.id == item.noteId } ?: return@forEach
        val itemScale = item.scale.coerceIn(0.3f, 3.0f)
        val usePin = item.noteId % 2 == 0L

        val topPadPx = (if (usePin) 20f else 14f) * scale
        val padPx = 12f * scale

        val sizeKey = "${item.noteId}:${item.boardId}"
        val measured = noteSizes[sizeKey]

        val baseWItem = measured?.first?.toFloat()?.let { it / density }
            ?: (BASE_NOTE_WIDTH * itemScale)
        val baseHItem = measured?.second?.toFloat()?.let { it / density }
            ?: (140f * itemScale)

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
        val bitmap = loadBitmapFromUri(ctx, img.uri) ?: return@forEach
        val itemScale = img.scale.coerceIn(0.3f, 3.0f)

        val measuredImg = imageSizes[img.id]
        val baseWImg = measuredImg?.first?.toFloat()?.let { it / density }
            ?: (BASE_IMAGE_WIDTH * itemScale)
        val baseHImg = measuredImg?.second?.toFloat()?.let { it / density }
            ?: (BASE_IMAGE_WIDTH * itemScale)

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
    val densityF = density.density
    val scope = rememberCoroutineScope()

    var boards by remember { mutableStateOf(BoardStore.boards(context)) }
    var currentBoard by remember { mutableStateOf(boards.firstOrNull()?.id ?: 1L) }
    var items by remember { mutableStateOf(BoardStore.items(context, currentBoard)) }
    var images by remember { mutableStateOf(BoardStore.images(context, currentBoard)) }
    var connections by remember(currentBoard) {
        mutableStateOf(loadConnections(context, currentBoard))
    }

    var showAddBoard by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showConnectionsDialog by remember { mutableStateOf(false) }
    var boardName by remember { mutableStateOf("") }
    var newBoardSizeIndex by remember { mutableStateOf(1) }
    var isExporting by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var connectMode by remember { mutableStateOf(false) }
    var pendingConnection by remember { mutableStateOf<ConnRef?>(null) }

    var boardPxW by remember { mutableStateOf(0) }
    var boardPxH by remember { mutableStateOf(0) }
    var vpW by remember { mutableStateOf(0) }
    var vpH by remember { mutableStateOf(0) }

    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var draggingImageId by remember { mutableStateOf<Long?>(null) }

    val boardTouchActive = remember { mutableStateOf(false) }
    val fingerState = remember { mutableStateOf<Offset?>(null) }
    val liveDragState = remember { mutableStateOf<LiveDrag?>(null) }
    var lastFingerWrite by remember { mutableStateOf(0L) }
    var lastPreviewWrite by remember { mutableStateOf(0L) }

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var imageToDelete by remember { mutableStateOf<BoardImage?>(null) }
    var boardToDelete by remember { mutableStateOf<Board?>(null) }

    val noteSizes = remember { mutableStateOf(mutableMapOf<String, Pair<Int, Int>>()) }
    val imageSizes = remember { mutableStateOf(mutableMapOf<Long, Pair<Int, Int>>()) }

    val refresh: () -> Unit = {
        boards = BoardStore.boards(context)
        items = BoardStore.items(context, currentBoard)
        images = BoardStore.images(context, currentBoard)
        connections = loadConnections(context, currentBoard)
    }

    LaunchedEffect(currentBoard) {
        pendingConnection = null
        connectMode = false
        showConnectionsDialog = false
    }

    val updateFinger: (Float, Float) -> Unit = { x, y ->
        val now = System.currentTimeMillis()
        if (now - lastFingerWrite >= FINGER_THROTTLE_MS) {
            lastFingerWrite = now
            fingerState.value = Offset(x, y)
        }
    }

    fun notePreviewSize(item: BoardItem, currentScale: Float): Pair<Float, Float> {
        val key = "${item.noteId}:${item.boardId}"
        val measured = noteSizes.value[key]
        val baseScale = item.scale.coerceIn(0.3f, 3.0f)

        if (measured != null) {
            val w = measured.first.toFloat() / densityF
            val h = measured.second.toFloat() / densityF
            val factor = if (baseScale > 0.01f) currentScale / baseScale else 1f
            return (w * factor) to (h * factor)
        }

        return (BASE_NOTE_WIDTH * currentScale) to (140f * currentScale)
    }

    fun imagePreviewSize(img: BoardImage, currentScale: Float): Pair<Float, Float> {
        val measured = imageSizes.value[img.id]
        val baseScale = img.scale.coerceIn(0.3f, 3.0f)

        if (measured != null) {
            val w = measured.first.toFloat() / densityF
            val h = measured.second.toFloat() / densityF
            val factor = if (baseScale > 0.01f) currentScale / baseScale else 1f
            return (w * factor) to (h * factor)
        }

        val w = BASE_IMAGE_WIDTH * currentScale
        return w to w
    }

    fun setPreviewNote(
        item: BoardItem,
        note: Note,
        x: Float,
        y: Float,
        rotation: Float,
        scale: Float,
        immediate: Boolean
    ) {
        val now = System.currentTimeMillis()
        val shouldImmediate = immediate || liveDragState.value == null
        if (!shouldImmediate && now - lastPreviewWrite < FINGER_THROTTLE_MS) return
        lastPreviewWrite = now

        val (w, h) = notePreviewSize(item, scale)
        liveDragState.value = LiveDrag(
            id = note.id,
            isImage = false,
            x = x,
            y = y,
            w = w,
            h = h,
            rotation = rotation,
            color = stickyBody(note.color)
        )
    }

    fun setPreviewImage(
        img: BoardImage,
        x: Float,
        y: Float,
        rotation: Float,
        scale: Float,
        immediate: Boolean
    ) {
        val now = System.currentTimeMillis()
        val shouldImmediate = immediate || liveDragState.value == null
        if (!shouldImmediate && now - lastPreviewWrite < FINGER_THROTTLE_MS) return
        lastPreviewWrite = now

        val (w, h) = imagePreviewSize(img, scale)
        liveDragState.value = LiveDrag(
            id = img.id,
            isImage = true,
            x = x,
            y = y,
            w = w,
            h = h,
            rotation = rotation,
            color = Color.White
        )
    }

    fun handleConnectSelect(ref: ConnRef) {
        if (!connectMode) return

        val p = pendingConnection

        if (p == null) {
            pendingConnection = ref
            Toast.makeText(context, "مبدأ انتخاب شد؛ حالا مقصد را بزنید", Toast.LENGTH_SHORT).show()
            return
        }

        if (p.type == ref.type && p.id == ref.id) {
            pendingConnection = null
            Toast.makeText(context, "انتخاب لغو شد", Toast.LENGTH_SHORT).show()
            return
        }

        val exists = connections.any {
            (it.from == p && it.to == ref) || (it.from == ref && it.to == p)
        }

        if (exists) {
            pendingConnection = null
            Toast.makeText(context, "این اتصال قبلاً وجود دارد", Toast.LENGTH_SHORT).show()
            return
        }

        val fromOk = connectionEndpoint(
            ref = p,
            boardId = currentBoard,
            items = items,
            images = images,
            noteSizes = noteSizes.value,
            imageSizes = imageSizes.value,
            densityF = densityF
        ) != null

        val toOk = connectionEndpoint(
            ref = ref,
            boardId = currentBoard,
            items = items,
            images = images,
            noteSizes = noteSizes.value,
            imageSizes = imageSizes.value,
            densityF = densityF
        ) != null

        if (!fromOk || !toOk) {
            pendingConnection = null
            Toast.makeText(context, "آیتم انتخاب‌شده روی تابلو نیست", Toast.LENGTH_SHORT).show()
            return
        }

        val newConn = BoardConnection(
            id = System.nanoTime(),
            from = p,
            to = ref,
            colorIndex = connections.size % 6
        )

        connections = connections + newConn
        saveConnections(context, currentBoard, connections)
        pendingConnection = null

        Toast.makeText(context, "🔗 اتصال ایجاد شد", Toast.LENGTH_SHORT).show()
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val dir = File(context.filesDir, "board_images")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "img-${System.currentTimeMillis()}.jpg")
                inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

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

    val boardWidthDp = if (isPhoneSize) {
        with(density) { boardPxW.toDp().value }
    } else {
        BOARD_SIZES_DP[boardSizeIndex].first
    }

    val boardHeightDpActual = if (isPhoneSize) {
        with(density) { boardPxH.toDp().value }
    } else {
        BOARD_SIZES_DP[boardSizeIndex].second
    }

    val clampX = (if (boardWidthDp > 60f) boardWidthDp - 60f else 300f).coerceAtLeast(0f)
    val clampY = (if (boardHeightDpActual > 60f) boardHeightDpActual - 60f else 400f).coerceAtLeast(0f)

    val visibleItems = remember(items, searchQuery, notes) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { item ->
                val n = notes.firstOrNull { it.id == item.noteId } ?: return@filter false
                n.title.contains(searchQuery, true) || n.body.contains(searchQuery, true)
            }
        }
    }

    fun exportBoard(asPdf: Boolean) {
        if (isExporting) return
        isExporting = true

        val snapshotNotes = notes.toList()
        val snapshotItems = items.toList()
        val snapshotImages = images.toList()
        val snapshotConnections = connections.toList()
        val snapshotNoteSizes = noteSizes.value.toMap()
        val snapshotImageSizes = imageSizes.value.toMap()

        val snapshotBg = bgIndex
        val sBoardId = currentBoard
        val sSizeIndex = boardSizeIndex
        val sPhone = isPhoneSize
        val sPxW = boardPxW
        val sPxH = boardPxH
        val sName = currentBoardData?.name ?: ""
        val sDensity = densityF
        val sFontScale = context.resources.configuration.fontScale

        scope.launch(Dispatchers.IO) {
            try {
                val baseWDp: Float
                val baseHDp: Float

                if (sPhone) {
                    baseWDp = sPxW.toFloat().coerceAtLeast(1f) / sDensity
                    baseHDp = sPxH.toFloat().coerceAtLeast(1f) / sDensity
                } else {
                    baseWDp = BOARD_SIZES_DP[sSizeIndex].first.coerceAtLeast(1f)
                    baseHDp = BOARD_SIZES_DP[sSizeIndex].second.coerceAtLeast(1f)
                }

                var renderScale = EXPORT_RASTER_SCALE
                val areaDp2 = baseWDp * baseHDp

                while (areaDp2 * renderScale * renderScale > MAX_EXPORT_PIXELS && renderScale > 1f) {
                    renderScale *= 0.9f
                }

                val rasterW = (baseWDp * renderScale).roundToInt().coerceAtLeast(1)
                val rasterH = (baseHDp * renderScale).roundToInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(rasterW, rasterH, Bitmap.Config.ARGB_8888)

                renderBoardToCanvas(
                    ctx = context,
                    canvas = AndroidCanvas(bitmap),
                    notes = snapshotNotes,
                    items = snapshotItems,
                    images = snapshotImages,
                    connections = snapshotConnections,
                    boardId = sBoardId,
                    noteSizes = snapshotNoteSizes,
                    imageSizes = snapshotImageSizes,
                    bgIndex = snapshotBg,
                    density = sDensity,
                    fontScale = sFontScale,
                    outW = rasterW,
                    outH = rasterH,
                    baseWDp = baseWDp,
                    baseHDp = baseHDp
                )

                val dir = File(context.cacheDir, "board_exports")
                if (!dir.exists()) dir.mkdirs()

                val file = File(
                    dir,
                    "board-${System.currentTimeMillis()}.${if (asPdf) "pdf" else "png"}"
                )

                if (asPdf) {
                    val pageW = baseWDp.roundToInt().coerceAtLeast(1)
                    val pageH = baseHDp.roundToInt().coerceAtLeast(1)

                    val pdfDocument = PdfDocument()
                    val page = pdfDocument.startPage(
                        PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
                    )

                    val paint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }

                    val pageCanvas = page.canvas
                    pageCanvas.save()
                    pageCanvas.scale(
                        pageW.toFloat() / rasterW.toFloat(),
                        pageH.toFloat() / rasterH.toFloat()
                    )
                    pageCanvas.drawBitmap(bitmap, 0f, 0f, paint)
                    pageCanvas.restore()

                    pdfDocument.finishPage(page)
                    FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
                    pdfDocument.close()
                } else {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                bitmap.recycle()

                withContext(Dispatchers.Main) {
                    isExporting = false

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    val mime = if (asPdf) "application/pdf" else "image/png"

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = mime
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "تابلوی $sName")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            if (asPdf) "اشتراک‌گذاری PDF تابلو" else "اشتراک‌گذاری PNG تابلو"
                        )
                    )

                    Toast.makeText(
                        context,
                        if (asPdf) "✅ PDF با همان کیفیت و سایز دقیق آماده شد"
                        else "✅ PNG با کیفیت بالا آماده شد",
                        Toast.LENGTH_LONG
                    ).show()
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
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = .28f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 10.dp),
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
                                onClick = {
                                    currentBoard = b.id
                                    refresh()
                                    searchQuery = ""
                                },
                                onLongClick = { boardToDelete = b }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFFFFB74D) else Color.White.copy(alpha = .12f),
                            shadowElevation = if (selected) 6.dp else 0.dp
                        ) {
                            Text(
                                b.name,
                                color = if (selected) Color(0xFF3E2723) else Color(0xFFFFE0B2),
                                fontFamily = LalezarFont,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.12f)) {
                        Text(
                            "📝${items.size} 🖼${images.size} 🔗${connections.size}",
                            fontSize = 11.sp,
                            color = Color(0xFFFFE0B2),
                            fontFamily = VazirFont,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFB8C00).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFFB8C00).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🖼️ عکس", color = Color(0xFFFFE0B2), fontSize = 12.sp, fontFamily = VazirFont)
                        }
                    }

                    Surface(
                        onClick = { showSearch = !showSearch },
                        shape = RoundedCornerShape(12.dp),
                        color = if (showSearch) Color(0xFFFFB74D).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔍 جستجو", color = Color(0xFFFFE0B2), fontSize = 12.sp, fontFamily = VazirFont)
                        }
                    }

                    Surface(
                        onClick = {
                            connectMode = !connectMode
                            pendingConnection = null
                            Toast.makeText(
                                context,
                                if (connectMode) "🔗 حالت اتصال فعال شد" else "🔗 حالت اتصال غیرفعال شد",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (connectMode) Color(0xFFFFB74D).copy(alpha = 0.35f) else Color(0xFF4FC3F7).copy(alpha = 0.18f),
                        border = BorderStroke(
                            1.dp,
                            if (connectMode) Color(0xFFFFB74D) else Color(0xFF4FC3F7).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (connectMode) "🔗 لغو اتصال" else "🔗 اتصال",
                                color = Color(0xFFFFE0B2),
                                fontSize = 12.sp,
                                fontFamily = VazirFont
                            )
                        }
                    }

                    Surface(
                        onClick = { showConnectionsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFAB47BC).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFFAB47BC).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🧵 لینک‌ها", color = Color(0xFFFFE0B2), fontSize = 12.sp, fontFamily = VazirFont)
                        }
                    }

                    Surface(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📤 خروجی", color = Color(0xFFFFCDD2), fontSize = 12.sp, fontFamily = VazirFont)
                        }
                    }

                    IconButton(
                        onClick = {
                            BoardStore.setBackground(context, currentBoard, (bgIndex + 1) % 6)
                            refresh()
                        }
                    ) {
                        Icon(Icons.Filled.Palette, "پس‌زمینه", tint = Color(0xFFFFE0B2))
                    }

                    Surface(
                        onClick = {
                            val newIdx = (boardSizeIndex + 1) % 4

                            val oldWDp = boardWidthDp.coerceAtLeast(1f)
                            val oldHDp = boardHeightDpActual.coerceAtLeast(1f)

                            val newWDp = if (newIdx == 0) {
                                if (vpW > 0) with(density) { vpW.toDp().value } else oldWDp
                            } else {
                                BOARD_SIZES_DP[newIdx].first
                            }.coerceAtLeast(1f)

                            val newHDp = if (newIdx == 0) {
                                if (vpH > 0) with(density) { vpH.toDp().value } else oldHDp
                            } else {
                                BOARD_SIZES_DP[newIdx].second
                            }.coerceAtLeast(1f)

                            val ratioX = newWDp / oldWDp
                            val ratioY = newHDp / oldHDp

                            if (abs(ratioX - 1f) > 0.001f || abs(ratioY - 1f) > 0.001f) {
                                items.forEach { item ->
                                    BoardStore.move(
                                        context,
                                        item.noteId,
                                        currentBoard,
                                        item.x * ratioX,
                                        item.y * ratioY
                                    )
                                }

                                images.forEach { img ->
                                    BoardStore.moveImage(
                                        context,
                                        img.id,
                                        currentBoard,
                                        img.x * ratioX,
                                        img.y * ratioY
                                    )
                                }
                            }

                            BoardStore.setBoardSize(context, currentBoard, newIdx)
                            refresh()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                            BOARD_SIZE_LABELS[boardSizeIndex],
                            fontSize = 11.sp,
                            color = Color(0xFFFFE0B2),
                            fontFamily = VazirFont,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            boardName = ""
                            newBoardSizeIndex = 1
                            showAddBoard = true
                        }
                    ) {
                        Icon(Icons.Filled.Add, "تابلو جدید", tint = Color(0xFFFFE0B2))
                    }
                }

                if (showSearch) {
                    Row(
                        Modifier
                            .fillMaxWidth()
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
                                fontFamily = VazirFont,
                                fontSize = 14.sp,
                                color = Color(0xFF3E2723)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "جستجو در یادداشت‌ها...",
                                            color = Color(0xFF8D6E63),
                                            fontSize = 14.sp,
                                            fontFamily = VazirFont
                                        )
                                    }
                                    inner()
                                }
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    "پاک",
                                    tint = Color(0xFF5D4037),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (connectMode) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFB74D).copy(alpha = .92f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (pendingConnection == null) {
                                "🔗 حالت اتصال فعال است. ابتدا روی آیتم مبدأ بزنید."
                            } else {
                                "🔗 مبدأ انتخاب شد. حالا روی آیتم مقصد بزنید."
                            },
                            color = Color(0xFF3E2723),
                            fontSize = 12.sp,
                            fontFamily = VazirFont
                        )
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                val scrollStateV = rememberScrollState()
                val scrollStateH = rememberScrollState()

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .onSizeChanged { s ->
                                    vpW = s.width
                                    vpH = s.height
                                }
                                .then(
                                    if (!isPhoneSize) {
                                        Modifier
                                            .verticalScroll(scrollStateV)
                                            .horizontalScroll(scrollStateH)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Box(
                                Modifier
                                    .then(
                                        if (isPhoneSize) {
                                            Modifier.fillMaxSize()
                                        } else {
                                            Modifier
                                                .width(boardWidthDp.dp)
                                                .height(boardHeightDpActual.dp)
                                        }
                                    )
                                    .onSizeChanged { s ->
                                        boardPxW = s.width
                                        boardPxH = s.height
                                    }
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(
                                                requireUnconsumed = false,
                                                pass = PointerEventPass.Initial
                                            )
                                            boardTouchActive.value = true
                                            updateFinger(down.position.x / densityF, down.position.y / densityF)

                                            var pressed = true
                                            while (pressed) {
                                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                                val ch = ev.changes.firstOrNull()
                                                if (ch != null && ch.pressed) {
                                                    updateFinger(ch.position.x / densityF, ch.position.y / densityF)
                                                }
                                                pressed = ev.changes.any { it.pressed }
                                            }

                                            boardTouchActive.value = false
                                            fingerState.value = null
                                        }
                                    }
                            ) {
                                CorkTexture(bgIndex)
                                Vignette(bgIndex)

                                ConnectionsLayer(
                                    connections = connections,
                                    pending = pendingConnection,
                                    live = liveDragState.value,
                                    boardId = currentBoard,
                                    items = items,
                                    images = images,
                                    notes = notes,
                                    noteSizes = noteSizes.value,
                                    imageSizes = imageSizes.value,
                                    densityF = densityF,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (visibleItems.isEmpty() && images.isEmpty() && !isExporting) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Column(
                                            Modifier.align(Alignment.Center),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                if (searchQuery.isNotBlank()) "🔍" else "🗒️",
                                                fontSize = 64.sp,
                                                modifier = Modifier.rotate(if (searchQuery.isNotBlank()) 0f else -6f)
                                            )
                                            Text(
                                                if (searchQuery.isNotBlank()) "یادداشتی یافت نشد" else "تابلو خالی است",
                                                fontFamily = LalezarFont,
                                                fontSize = 22.sp,
                                                color = Color.White.copy(alpha = .85f)
                                            )
                                            Text(
                                                "با دکمهٔ + یادداشت بچسبانید، با دکمهٔ عکس تصویر اضافه کنید و با 🔗 آیتم‌ها را به هم وصل کنید",
                                                fontFamily = VazirFont,
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = .6f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 24.dp)
                                            )
                                        }
                                    }
                                }

                                visibleItems.forEachIndexed { _, item ->
                                    val note = notes.firstOrNull { it.id == item.noteId }
                                    if (note != null) {
                                        StickyNote(
                                            note = note,
                                            item = item,
                                            clampX = clampX,
                                            clampY = clampY,
                                            isDraggingThis = draggingNoteId == note.id,
                                            connectMode = connectMode,
                                            onTap = { onOpenNote(note.id) },
                                            onConnectSelect = {
                                                handleConnectSelect(ConnRef(CONN_TYPE_NOTE, note.id))
                                            },
                                            onLongPress = { noteToDelete = note },
                                            onMoved = { x, y ->
                                                BoardStore.move(context, note.id, currentBoard, x, y)
                                                items = items.map {
                                                    if (it.noteId == note.id && it.boardId == currentBoard) {
                                                        it.copy(x = x, y = y)
                                                    } else {
                                                        it
                                                    }
                                                }
                                            },
                                            onRotated = { rot ->
                                                BoardStore.rotate(context, note.id, currentBoard, rot)
                                                items = items.map {
                                                    if (it.noteId == note.id && it.boardId == currentBoard) {
                                                        it.copy(rotation = rot)
                                                    } else {
                                                        it
                                                    }
                                                }
                                            },
                                            onScaleChanged = { sc ->
                                                BoardStore.setScale(context, note.id, currentBoard, sc)
                                                items = items.map {
                                                    if (it.noteId == note.id && it.boardId == currentBoard) {
                                                        it.copy(scale = sc)
                                                    } else {
                                                        it
                                                    }
                                                }
                                            },
                                            onMeasured = { w, h ->
                                                val key = "${item.noteId}:${item.boardId}"
                                                noteSizes.value = noteSizes.value.toMutableMap().apply {
                                                    put(key, w to h)
                                                }
                                            },
                                            onDragStart = {
                                                draggingNoteId = note.id
                                                setPreviewNote(
                                                    item = item,
                                                    note = note,
                                                    x = item.x,
                                                    y = item.y,
                                                    rotation = item.rotation,
                                                    scale = item.scale,
                                                    immediate = true
                                                )
                                            },
                                            onDragUpdate = { x, y, rot, sc ->
                                                updateFinger(x, y)
                                                setPreviewNote(
                                                    item = item,
                                                    note = note,
                                                    x = x,
                                                    y = y,
                                                    rotation = rot,
                                                    scale = sc,
                                                    immediate = false
                                                )
                                            },
                                            onDragEnd = {
                                                draggingNoteId = null
                                                fingerState.value = null
                                                liveDragState.value = null
                                                boardTouchActive.value = false
                                            }
                                        )
                                    }
                                }

                                images.forEachIndexed { _, img ->
                                    BoardImageItem(
                                        image = img,
                                        clampX = clampX,
                                        clampY = clampY,
                                        isDraggingThis = draggingImageId == img.id,
                                        connectMode = connectMode,
                                        onTap = { },
                                        onConnectSelect = {
                                            handleConnectSelect(ConnRef(CONN_TYPE_IMAGE, img.id))
                                        },
                                        onLongPress = { imageToDelete = img },
                                        onMoved = { x, y ->
                                            BoardStore.moveImage(context, img.id, currentBoard, x, y)
                                            images = images.map {
                                                if (it.id == img.id) it.copy(x = x, y = y) else it
                                            }
                                        },
                                        onRotated = { rot ->
                                            BoardStore.rotateImage(context, img.id, currentBoard, rot)
                                            images = images.map {
                                                if (it.id == img.id) it.copy(rotation = rot) else it
                                            }
                                        },
                                        onScaleChanged = { sc ->
                                            BoardStore.setImageScale(context, img.id, currentBoard, sc)
                                            images = images.map {
                                                if (it.id == img.id) it.copy(scale = sc) else it
                                            }
                                        },
                                        onMeasured = { w, h ->
                                            imageSizes.value = imageSizes.value.toMutableMap().apply {
                                                put(img.id, w to h)
                                            }
                                        },
                                        onDragStart = {
                                            draggingImageId = img.id
                                            setPreviewImage(
                                                img = img,
                                                x = img.x,
                                                y = img.y,
                                                rotation = img.rotation,
                                                scale = img.scale,
                                                immediate = true
                                            )
                                        },
                                        onDragUpdate = { x, y, rot, sc ->
                                            updateFinger(x, y)
                                            setPreviewImage(
                                                img = img,
                                                x = x,
                                                y = y,
                                                rotation = rot,
                                                scale = sc,
                                                immediate = false
                                            )
                                        },
                                        onDragEnd = {
                                            draggingImageId = null
                                            fingerState.value = null
                                            liveDragState.value = null
                                            boardTouchActive.value = false
                                        }
                                    )
                                }
                            }
                        }

                        val miniWidthDp = 120f
                        val miniHeightDp = if (boardWidthDp > 0f) {
                            miniWidthDp * boardHeightDpActual / boardWidthDp
                        } else {
                            miniWidthDp
                        }

                        MiniMapContainer(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .width(miniWidthDp.dp)
                                .height(miniHeightDp.dp),
                            touchActive = boardTouchActive,
                            draggingNoteId = draggingNoteId,
                            draggingImageId = draggingImageId,
                            isPhoneSize = isPhoneSize,
                            hasBoardSize = boardPxW > 0 && boardPxH > 0 && vpW > 0 && vpH > 0,
                            boardWidthDp = boardWidthDp.coerceAtLeast(1f),
                            boardHeightDp = boardHeightDpActual.coerceAtLeast(1f),
                            viewportWidthDp = with(density) { vpW.toDp().value }.coerceAtLeast(1f),
                            viewportHeightDp = with(density) { vpH.toDp().value }.coerceAtLeast(1f),
                            scrollStateH = scrollStateH,
                            scrollStateV = scrollStateV,
                            pxPerDp = densityF,
                            fingerState = fingerState,
                            liveDragState = liveDragState,
                            visibleItems = visibleItems,
                            images = images,
                            notes = notes,
                            noteSizes = noteSizes,
                            imageSizes = imageSizes
                        )
                    }
                }
            }
        }

        if (!isExporting) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .size(60.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFB8C00))))
                    .combinedClickable(onClick = { showAddNote = true })
                    .rotate(-4f),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, "افزودن", tint = Color(0xFF3E2723), modifier = Modifier.size(28.dp))
            }
        }

        if (isExporting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        "در حال ساخت خروجی با کیفیت بالا...",
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp),
                        fontFamily = VazirFont
                    )
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
                    Text(
                        "می‌خواهی از تابلو حذف شود یا کلاً از دفترچه؟",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        BoardStore.removeItem(context, note.id, currentBoard)
                        connections = connections.filterNot {
                            (it.from.type == CONN_TYPE_NOTE && it.from.id == note.id) ||
                                (it.to.type == CONN_TYPE_NOTE && it.to.id == note.id)
                        }
                        saveConnections(context, currentBoard, connections)
                        refresh()
                        noteToDelete = null
                        Toast.makeText(context, "از تابلو حذف شد", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("فقط از تابلو", color = Color(0xFFFB8C00), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text("انصراف")
                    }
                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                noteDao.deleteById(note.id)
                                BoardStore.removeItem(context, note.id, currentBoard)
                                withContext(Dispatchers.Main) {
                                    connections = connections.filterNot {
                                        (it.from.type == CONN_TYPE_NOTE && it.from.id == note.id) ||
                                            (it.to.type == CONN_TYPE_NOTE && it.to.id == note.id)
                                    }
                                    saveConnections(context, currentBoard, connections)
                                    refresh()
                                    noteToDelete = null
                                    Toast.makeText(context, "کلاً حذف شد", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("حذف کامل", color = Color.Red, fontWeight = FontWeight.Bold)
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
                TextButton(
                    onClick = {
                        BoardStore.removeImage(context, img.id, currentBoard)
                        connections = connections.filterNot {
                            (it.from.type == CONN_TYPE_IMAGE && it.from.id == img.id) ||
                                (it.to.type == CONN_TYPE_IMAGE && it.to.id == img.id)
                        }
                        saveConnections(context, currentBoard, connections)
                        refresh()
                        imageToDelete = null
                        Toast.makeText(context, "تصویر حذف شد", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حذف", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    boardToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { boardToDelete = null },
            title = { Text("🗑️ حذف تابلو", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = { Text("تابلوی «${b.name}» همراه با همهٔ یادداشت‌ها، تصاویر و اتصالات روی آن حذف شود؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        BoardStore.removeBoard(context, b.id)
                        connectionsPrefs(context).edit().remove(connectionKey(b.id)).apply()

                        if (currentBoard == b.id) {
                            val remaining = BoardStore.boards(context)
                            currentBoard = remaining.firstOrNull()?.id ?: 1L
                        }

                        refresh()
                        boardToDelete = null
                        Toast.makeText(context, "تابلو حذف شد", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حذف تابلو", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { boardToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showConnectionsDialog) {
        ConnectionsDialog(
            connections = connections,
            boardId = currentBoard,
            items = items,
            images = images,
            notes = notes,
            onDelete = { id ->
                connections = connections.filter { it.id != id }
                saveConnections(context, currentBoard, connections)
            },
            onClearAll = {
                connections = emptyList()
                saveConnections(context, currentBoard, connections)
                showConnectionsDialog = false
                Toast.makeText(context, "همهٔ اتصالات حذف شدند", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showConnectionsDialog = false }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("📤 نوع خروجی", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "سایز تابلو: ${BOARD_SIZE_LABELS[boardSizeIndex]}",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontFamily = VazirFont
                    )
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        onClick = {
                            showExportDialog = false
                            exportBoard(true)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, null, tint = Color(0xFFE53935))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("📄 PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = VazirFont)
                                Text("مناسب چاپ و اشتراک‌گذاری", fontSize = 11.sp, color = Color.Gray, fontFamily = VazirFont)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            showExportDialog = false
                            exportBoard(false)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF43A047).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF43A047).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Image, null, tint = Color(0xFF43A047))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("🖼️ PNG", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = VazirFont)
                                Text("تصویر با کیفیت بسیار بالا", fontSize = 11.sp, color = Color.Gray, fontFamily = VazirFont)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
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
                Column {
                    OutlinedTextField(
                        boardName,
                        { boardName = it },
                        label = { Text("نام تابلو") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("سایز تابلو:", fontFamily = LalezarFont, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))

                    BOARD_SIZE_LABELS.forEachIndexed { idx, label ->
                        val selected = idx == newBoardSizeIndex
                        Surface(
                            onClick = { newBoardSizeIndex = idx },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) Color(0xFFFFB74D).copy(alpha = 0.3f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (selected) Color(0xFFFFB74D) else Color.Gray.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .border(2.dp, Color.Gray, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Box(
                                            Modifier
                                                .size(12.dp)
                                                .background(Color(0xFFFFB74D), CircleShape)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = VazirFont)
                                    Text(
                                        BOARD_SIZE_DESC[idx],
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontFamily = VazirFont
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val b = BoardStore.addBoard(
                            context,
                            boardName.ifBlank { "تابلو جدید" },
                            background = 0,
                            sizeIndex = newBoardSizeIndex
                        )
                        currentBoard = b.id
                        refresh()
                        showAddBoard = false
                    }
                ) {
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
                    Text(
                        "هنوز یادداشتی نساخته‌اید. لطفاً ابتدا از تب «یادداشت‌ها» یک یادداشت ایجاد کنید.",
                        textAlign = TextAlign.Center
                    )
                } else if (available.isEmpty()) {
                    Text("همهٔ یادداشت‌ها در حال حاضر روی این تابلو هستند.")
                } else {
                    LazyColumn {
                        items(available) { n ->
                            val rotIdx = ((n.id % 4L + 4L) % 4L).toInt()
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .rotate(listOf(-1.5f, 1f, -0.5f, 2f)[rotIdx])
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(stickyBody(n.color))
                                    .shadow(4.dp, RoundedCornerShape(4.dp))
                                    .combinedClickable(
                                        onClick = {
                                            BoardStore.addItem(context, n.id, currentBoard)
                                            refresh()
                                            showAddNote = false
                                            Toast.makeText(context, "یادداشت چسبانده شد", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    n.title.ifBlank { n.body.take(30).ifBlank { "بدون عنوان" } },
                                    fontFamily = VazirFont,
                                    fontSize = 14.sp,
                                    color = Color(0xFF3E2723),
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

private fun DrawScope.drawRotatedRect(
    center: Offset,
    halfW: Float,
    halfH: Float,
    rotation: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float
) {
    val rad = rotation.toDouble() * PI / 180.0
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()

    fun corner(dx: Float, dy: Float): Offset {
        return Offset(
            center.x + dx * c - dy * s,
            center.y + dx * s + dy * c
        )
    }

    val p0 = corner(-halfW, -halfH)
    val p1 = corner(halfW, -halfH)
    val p2 = corner(halfW, halfH)
    val p3 = corner(-halfW, halfH)

    val path = Path().apply {
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }

    drawPath(path, fillColor)

    if (strokeWidth > 0f) {
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ConnectionsLayer(
    connections: List<BoardConnection>,
    pending: ConnRef?,
    live: LiveDrag?,
    boardId: Long,
    items: List<BoardItem>,
    images: List<BoardImage>,
    notes: List<Note>,
    noteSizes: Map<String, Pair<Int, Int>>,
    imageSizes: Map<Long, Pair<Int, Int>>,
    densityF: Float,
    modifier: Modifier
) {
    ComposeCanvas(modifier) {
        val d = densityF.coerceAtLeast(1f)

        connections.forEach { conn ->
            val a = connectionEndpoint(
                ref = conn.from,
                boardId = boardId,
                items = items,
                images = images,
                noteSizes = noteSizes,
                imageSizes = imageSizes,
                densityF = d,
                live = live
            ) ?: return@forEach

            val b = connectionEndpoint(
                ref = conn.to,
                boardId = boardId,
                items = items,
                images = images,
                noteSizes = noteSizes,
                imageSizes = imageSizes,
                densityF = d,
                live = live
            ) ?: return@forEach

            val dx = b.x - a.x
            val dy = b.y - a.y
            val distDp = sqrt(dx * dx + dy * dy)

            if (distDp < 1f) return@forEach

            val nx = -dy / distDp
            val ny = dx / distDp
            val bendDp = (distDp * 0.18f).coerceIn(20f, 120f)

            val cxDp = (a.x + b.x) / 2f + nx * bendDp
            val cyDp = (a.y + b.y) / 2f + ny * bendDp

            val ax = a.x * d
            val ay = a.y * d
            val bx = b.x * d
            val by = b.y * d
            val cx = cxDp * d
            val cy = cyDp * d

            val path = Path().apply {
                moveTo(ax, ay)
                quadraticTo(cx, cy, bx, by)
            }

            val isActive = pending != null &&
                (pending == conn.from || pending == conn.to)

            val color = connectionColor(conn.colorIndex)
            val alpha = if (isActive) 1f else 0.82f
            val strokeWidth = if (isActive) 4.5f * d else 3f * d

            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = 4.5f * d,
                center = Offset(ax, ay)
            )

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = 4.5f * d,
                center = Offset(bx, by)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = 4.5f * d,
                center = Offset(ax, ay),
                style = Stroke(1.2f * d)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = 4.5f * d,
                center = Offset(bx, by),
                style = Stroke(1.2f * d)
            )
        }

        pending?.let { p ->
            val e = connectionEndpoint(
                ref = p,
                boardId = boardId,
                items = items,
                images = images,
                noteSizes = noteSizes,
                imageSizes = imageSizes,
                densityF = d,
                live = live
            )

            if (e != null) {
                drawCircle(
                    color = Color(0xFFFFB74D),
                    radius = 10f * d,
                    center = Offset(e.x * d, e.y * d),
                    style = Stroke(2.5f * d)
                )
            }
        }
    }
}

@Composable
private fun ConnectionsDialog(
    connections: List<BoardConnection>,
    boardId: Long,
    items: List<BoardItem>,
    images: List<BoardImage>,
    notes: List<Note>,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🧵 مدیریت اتصالات", fontFamily = LalezarFont, fontSize = 20.sp) },
        text = {
            if (connections.isEmpty()) {
                Text(
                    "هنوز اتصالی ایجاد نشده است.\n\nدکمهٔ 🔗 اتصال را بزنید، سپس روی آیتم مبدأ و بعد آیتم مقصد لمس کنید.",
                    fontFamily = VazirFont,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(connections) { conn ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(connectionColor(conn.colorIndex), CircleShape)
                            )

                            Spacer(Modifier.width(8.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${connectionEndpointLabel(conn.from, boardId, items, images, notes)} ↔ ${connectionEndpointLabel(conn.to, boardId, items, images, notes)}",
                                    fontSize = 13.sp,
                                    fontFamily = VazirFont,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "برای حذف، دکمهٔ 🗑 را بزنید",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontFamily = VazirFont
                                )
                            }

                            TextButton(onClick = { onDelete(conn.id) }) {
                                Text("🗑", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClearAll) {
                Text("پاک کردن همه", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}

@Composable
private fun MiniMapContainer(
    modifier: Modifier,
    touchActive: State<Boolean>,
    draggingNoteId: Long?,
    draggingImageId: Long?,
    isPhoneSize: Boolean,
    hasBoardSize: Boolean,
    boardWidthDp: Float,
    boardHeightDp: Float,
    viewportWidthDp: Float,
    viewportHeightDp: Float,
    scrollStateH: ScrollState,
    scrollStateV: ScrollState,
    pxPerDp: Float,
    fingerState: State<Offset?>,
    liveDragState: State<LiveDrag?>,
    visibleItems: List<BoardItem>,
    images: List<BoardImage>,
    notes: List<Note>,
    noteSizes: State<Map<String, Pair<Int, Int>>>,
    imageSizes: State<Map<Long, Pair<Int, Int>>>
) {
    val active = touchActive.value

    if (
        !isPhoneSize &&
        hasBoardSize &&
        (active || draggingNoteId != null || draggingImageId != null)
    ) {
        MiniMap(
            modifier = modifier,
            boardWidthDp = boardWidthDp,
            boardHeightDp = boardHeightDp,
            viewportWidthDp = viewportWidthDp,
            viewportHeightDp = viewportHeightDp,
            scrollStateH = scrollStateH,
            scrollStateV = scrollStateV,
            pxPerDp = pxPerDp,
            fingerState = fingerState,
            liveDragState = liveDragState,
            visibleItems = visibleItems,
            images = images,
            notes = notes,
            noteSizes = noteSizes,
            imageSizes = imageSizes
        )
    }
}

@Composable
private fun MiniMap(
    modifier: Modifier,
    boardWidthDp: Float,
    boardHeightDp: Float,
    viewportWidthDp: Float,
    viewportHeightDp: Float,
    scrollStateH: ScrollState,
    scrollStateV: ScrollState,
    pxPerDp: Float,
    fingerState: State<Offset?>,
    liveDragState: State<LiveDrag?>,
    visibleItems: List<BoardItem>,
    images: List<BoardImage>,
    notes: List<Note>,
    noteSizes: State<Map<String, Pair<Int, Int>>>,
    imageSizes: State<Map<Long, Pair<Int, Int>>>
) {
    val finger = fingerState.value
    val live = liveDragState.value

    val noteSizeMap = noteSizes.value
    val imageSizeMap = imageSizes.value
    val dp = pxPerDp.coerceAtLeast(1f)

    val scrollXDp = scrollStateH.value / dp
    val scrollYDp = scrollStateV.value / dp

    val baseMarkers = mutableListOf<MiniMarker>()

    visibleItems.forEach { item ->
        val n = notes.firstOrNull { it.id == item.noteId } ?: return@forEach
        val key = "${item.noteId}:${item.boardId}"
        val measured = noteSizeMap[key]

        val wDp = measured?.first?.toFloat()?.let { it / dp }
            ?: (BASE_NOTE_WIDTH * item.scale)
        val hDp = measured?.second?.toFloat()?.let { it / dp }
            ?: (140f * item.scale)

        baseMarkers.add(
            MiniMarker(
                id = item.noteId,
                isImage = false,
                x = item.x,
                y = item.y,
                w = wDp,
                h = hDp,
                rotation = item.rotation,
                color = stickyBody(n.color)
            )
        )
    }

    images.forEach { img ->
        val measured = imageSizeMap[img.id]

        val wDp = measured?.first?.toFloat()?.let { it / dp }
            ?: (BASE_IMAGE_WIDTH * img.scale)
        val hDp = measured?.second?.toFloat()?.let { it / dp }
            ?: (BASE_IMAGE_WIDTH * img.scale)

        baseMarkers.add(
            MiniMarker(
                id = img.id,
                isImage = true,
                x = img.x,
                y = img.y,
                w = wDp,
                h = hDp,
                rotation = img.rotation,
                color = Color.White
            )
        )
    }

    val displayMarkers: List<MiniMarker> = if (live == null) {
        baseMarkers
    } else {
        baseMarkers.filterNot { it.id == live.id && it.isImage == live.isImage } +
                MiniMarker(
                    id = live.id,
                    isImage = live.isImage,
                    x = live.x,
                    y = live.y,
                    w = live.w,
                    h = live.h,
                    rotation = live.rotation,
                    color = live.color
                )
    }

    ComposeCanvas(
        modifier
            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.86f), RoundedCornerShape(8.dp))
    ) {
        val bw = boardWidthDp.coerceAtLeast(1f)
        val bh = boardHeightDp.coerceAtLeast(1f)

        val scaleX = size.width / bw
        val scaleY = size.height / bh
        val scale = min(scaleX, scaleY)

        val offsetX = (size.width - bw * scale) / 2f
        val offsetY = (size.height - bh * scale) / 2f

        drawRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(offsetX, offsetY),
            size = Size(bw * scale, bh * scale)
        )

        displayMarkers.forEach { m ->
            val cx = (m.x + m.w / 2f) * scale + offsetX
            val cy = (m.y + m.h / 2f) * scale + offsetY
            val hw = (m.w * scale / 2f).coerceAtLeast(1.5f)
            val hh = (m.h * scale / 2f).coerceAtLeast(1.5f)

            drawRotatedRect(
                center = Offset(cx, cy),
                halfW = hw,
                halfH = hh,
                rotation = m.rotation,
                fillColor = m.color.copy(alpha = 0.94f),
                strokeColor = Color.White.copy(alpha = 0.72f),
                strokeWidth = 1f
            )
        }

        val vw = (viewportWidthDp.coerceAtLeast(1f) * scale).coerceAtMost(size.width)
        val vh = (viewportHeightDp.coerceAtLeast(1f) * scale).coerceAtMost(size.height)

        val vx = (scrollXDp * scale + offsetX).coerceIn(0f, (size.width - vw).coerceAtLeast(0f))
        val vy = (scrollYDp * scale + offsetY).coerceIn(0f, (size.height - vh).coerceAtLeast(0f))

        drawRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(vx, vy),
            size = Size(vw, vh)
        )

        drawRect(
            color = Color.White,
            topLeft = Offset(vx, vy),
            size = Size(vw, vh),
            style = Stroke(1.5f)
        )

        finger?.let { f ->
            val fx = (f.x * scale + offsetX).coerceIn(0f, size.width)
            val fy = (f.y * scale + offsetY).coerceIn(0f, size.height)

            drawCircle(
                color = Color(0xFFFFB74D),
                radius = 5.5f,
                center = Offset(fx, fy)
            )

            drawCircle(
                color = Color.White,
                radius = 5.5f,
                center = Offset(fx, fy),
                style = Stroke(1.5f)
            )
        }
    }
}

@Composable
private fun BoardImageItem(
    image: BoardImage,
    clampX: Float,
    clampY: Float,
    isDraggingThis: Boolean,
    connectMode: Boolean,
    onTap: () -> Unit,
    onConnectSelect: () -> Unit,
    onLongPress: () -> Unit,
    onMoved: (Float, Float) -> Unit,
    onRotated: (Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onMeasured: (Int, Int) -> Unit,
    onDragStart: () -> Unit,
    onDragUpdate: (Float, Float, Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val densityF = density.density

    val currentClampX by rememberUpdatedState(clampX)
    val currentClampY by rememberUpdatedState(clampY)

    val currentConnectMode by rememberUpdatedState(connectMode)

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnConnectSelect by rememberUpdatedState(onConnectSelect)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragUpdate by rememberUpdatedState(onDragUpdate)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnMoved by rememberUpdatedState(onMoved)
    val currentOnRotated by rememberUpdatedState(onRotated)
    val currentOnScaleChanged by rememberUpdatedState(onScaleChanged)

    var pos by remember(image.id, image.boardId, image.x, image.y) {
        mutableStateOf(Offset(image.x.coerceIn(0f, clampX), image.y.coerceIn(0f, clampY)))
    }
    var rotation by remember(image.id, image.boardId, image.rotation) {
        mutableStateOf(image.rotation)
    }
    var scale by remember(image.id, image.boardId, image.scale) {
        mutableStateOf(image.scale)
    }

    val widthDp = (BASE_IMAGE_WIDTH * scale).dp

    Box(
        Modifier
            .absoluteOffset {
                with(density) {
                    IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx())
                }
            }
            .width(widthDp)
            .onSizeChanged { s -> onMeasured(s.width, s.height) }
            .graphicsLayer { rotationZ = rotation }
            .pointerInput(image.id, image.boardId, image.x, image.y, image.rotation, image.scale) {
                val dragSlopPx = 6f * densityF
                val longPressMs = 500L

                fun centroidOf(changes: List<PointerInputChange>): Offset {
                    var x = 0f
                    var y = 0f
                    changes.forEach {
                        x += it.position.x
                        y += it.position.y
                    }
                    val n = changes.size.toFloat().coerceAtLeast(1f)
                    return Offset(x / n, y / n)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )

                    val initialDown = down.position
                    var lastCentroid = initialDown
                    var previousPointerCount = 1
                    var totalMovement = 0f

                    var mode = 0
                    var dragStarted = false
                    var changed = false
                    var sawMultiTouch = false

                    val downTime = System.currentTimeMillis()
                    var continueGesture = true

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            continueGesture = false
                        } else {
                            val centroid = centroidOf(pressed)
                            val countChanged = pressed.size != previousPointerCount
                            val pan = if (countChanged) Offset.Zero else centroid - lastCentroid

                            lastCentroid = centroid
                            previousPointerCount = pressed.size
                            totalMovement += pan.getDistance()

                            if (pressed.size >= 2) {
                                sawMultiTouch = true

                                if (!countChanged) {
                                    val rawZoom = event.calculateZoom()
                                    val rawRot = event.calculateRotation()

                                    val zoom = if (rawZoom.isNaN() || rawZoom.isInfinite()) 1f else rawZoom
                                    val rot = if (rawRot.isNaN() || rawRot.isInfinite()) 0f else rawRot

                                    val hasChange = pan != Offset.Zero || zoom != 1f || rot != 0f

                                    if (hasChange) {
                                        changed = true
                                        if (!dragStarted) {
                                            dragStarted = true
                                            mode = 2
                                            currentOnDragStart()
                                        }

                                        mode = 2
                                        pos = Offset(
                                            (pos.x + pan.x / densityF).coerceIn(0f, currentClampX),
                                            (pos.y + pan.y / densityF).coerceIn(0f, currentClampY)
                                        )
                                        rotation += rot
                                        val newScale = scale * zoom
                                        scale = if (newScale.isNaN() || newScale.isInfinite()) {
                                            scale
                                        } else {
                                            newScale.coerceIn(0.3f, 3.0f)
                                        }

                                        currentOnDragUpdate(pos.x, pos.y, rotation, scale)
                                    }
                                }

                                event.changes.forEach { it.consume() }
                            } else {
                                if (mode == 2) mode = 1

                                val distanceFromDown = centroid - initialDown
                                if (mode == 0 && distanceFromDown.getDistance() > dragSlopPx) {
                                    mode = 1
                                    changed = true
                                    if (!dragStarted) {
                                        dragStarted = true
                                        currentOnDragStart()
                                    }
                                }

                                if (mode == 1) {
                                    if (pan != Offset.Zero) changed = true

                                    pos = Offset(
                                        (pos.x + pan.x / densityF).coerceIn(0f, currentClampX),
                                        (pos.y + pan.y / densityF).coerceIn(0f, currentClampY)
                                    )

                                    currentOnDragUpdate(pos.x, pos.y, rotation, scale)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } while (continueGesture)

                    if (dragStarted) {
                        if (changed) {
                            currentOnMoved(pos.x, pos.y)
                            currentOnRotated(rotation)
                            currentOnScaleChanged(scale)
                        }
                        currentOnDragEnd()
                    } else if (!sawMultiTouch && totalMovement <= dragSlopPx) {
                        if (currentConnectMode) {
                            currentOnConnectSelect()
                        } else {
                            val elapsed = System.currentTimeMillis() - downTime
                            if (elapsed >= longPressMs) {
                                currentOnLongPress()
                            } else {
                                currentOnTap()
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(if (isDraggingThis) 14.dp else 7.dp, RoundedCornerShape(6.dp))
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
    }
}

@Composable
private fun CorkTexture(bgIndex: Int) {
    val base = boardBase(bgIndex)
    val dotA = corkDotA(bgIndex)
    val dotB = corkDotB(bgIndex)

    ComposeCanvas(Modifier.fillMaxSize()) {
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
    val strength = if (bgIndex == 2) 0.40f else 0.25f
    val brush = remember(bgIndex) {
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = strength)),
            center = Offset.Unspecified,
            radius = 1200f
        )
    }
    Box(Modifier.fillMaxSize().background(brush))
}

@Composable
private fun StickyNote(
    note: Note,
    item: BoardItem,
    clampX: Float,
    clampY: Float,
    isDraggingThis: Boolean,
    connectMode: Boolean,
    onTap: () -> Unit,
    onConnectSelect: () -> Unit,
    onLongPress: () -> Unit,
    onMoved: (Float, Float) -> Unit,
    onRotated: (Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onMeasured: (Int, Int) -> Unit,
    onDragStart: () -> Unit,
    onDragUpdate: (Float, Float, Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val densityF = density.density

    val currentClampX by rememberUpdatedState(clampX)
    val currentClampY by rememberUpdatedState(clampY)

    val currentConnectMode by rememberUpdatedState(connectMode)

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnConnectSelect by rememberUpdatedState(onConnectSelect)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragUpdate by rememberUpdatedState(onDragUpdate)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnMoved by rememberUpdatedState(onMoved)
    val currentOnRotated by rememberUpdatedState(onRotated)
    val currentOnScaleChanged by rememberUpdatedState(onScaleChanged)

    var pos by remember(item.noteId, item.boardId, item.x, item.y) {
        mutableStateOf(Offset(item.x.coerceIn(0f, clampX), item.y.coerceIn(0f, clampY)))
    }
    var rotation by remember(item.noteId, item.boardId, item.rotation) {
        mutableStateOf(item.rotation)
    }
    var scale by remember(item.noteId, item.boardId, item.scale) {
        mutableStateOf(item.scale)
    }

    val widthDp = (BASE_NOTE_WIDTH * scale).dp
    val body = stickyBody(note.color)
    val usePin = item.noteId % 2 == 0L

    Box(
        Modifier
            .absoluteOffset {
                with(density) {
                    IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx())
                }
            }
            .width(widthDp)
            .onSizeChanged { s -> onMeasured(s.width, s.height) }
            .graphicsLayer { rotationZ = rotation }
            .pointerInput(item.noteId, item.boardId, item.x, item.y, item.rotation, item.scale) {
                val dragSlopPx = 6f * densityF
                val longPressMs = 500L

                fun centroidOf(changes: List<PointerInputChange>): Offset {
                    var x = 0f
                    var y = 0f
                    changes.forEach {
                        x += it.position.x
                        y += it.position.y
                    }
                    val n = changes.size.toFloat().coerceAtLeast(1f)
                    return Offset(x / n, y / n)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )

                    val initialDown = down.position
                    var lastCentroid = initialDown
                    var previousPointerCount = 1
                    var totalMovement = 0f

                    var mode = 0
                    var dragStarted = false
                    var changed = false
                    var sawMultiTouch = false

                    val downTime = System.currentTimeMillis()
                    var continueGesture = true

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            continueGesture = false
                        } else {
                            val centroid = centroidOf(pressed)
                            val countChanged = pressed.size != previousPointerCount
                            val pan = if (countChanged) Offset.Zero else centroid - lastCentroid

                            lastCentroid = centroid
                            previousPointerCount = pressed.size
                            totalMovement += pan.getDistance()

                            if (pressed.size >= 2) {
                                sawMultiTouch = true

                                if (!countChanged) {
                                    val rawZoom = event.calculateZoom()
                                    val rawRot = event.calculateRotation()

                                    val zoom = if (rawZoom.isNaN() || rawZoom.isInfinite()) 1f else rawZoom
                                    val rot = if (rawRot.isNaN() || rawRot.isInfinite()) 0f else rawRot

                                    val hasChange = pan != Offset.Zero || zoom != 1f || rot != 0f

                                    if (hasChange) {
                                        changed = true
                                        if (!dragStarted) {
                                            dragStarted = true
                                            mode = 2
                                            currentOnDragStart()
                                        }

                                        mode = 2
                                        pos = Offset(
                                            (pos.x + pan.x / densityF).coerceIn(0f, currentClampX),
                                            (pos.y + pan.y / densityF).coerceIn(0f, currentClampY)
                                        )
                                        rotation += rot
                                        val newScale = scale * zoom
                                        scale = if (newScale.isNaN() || newScale.isInfinite()) {
                                            scale
                                        } else {
                                            newScale.coerceIn(0.3f, 3.0f)
                                        }

                                        currentOnDragUpdate(pos.x, pos.y, rotation, scale)
                                    }
                                }

                                event.changes.forEach { it.consume() }
                            } else {
                                if (mode == 2) mode = 1

                                val distanceFromDown = centroid - initialDown
                                if (mode == 0 && distanceFromDown.getDistance() > dragSlopPx) {
                                    mode = 1
                                    changed = true
                                    if (!dragStarted) {
                                        dragStarted = true
                                        currentOnDragStart()
                                    }
                                }

                                if (mode == 1) {
                                    if (pan != Offset.Zero) changed = true

                                    pos = Offset(
                                        (pos.x + pan.x / densityF).coerceIn(0f, currentClampX),
                                        (pos.y + pan.y / densityF).coerceIn(0f, currentClampY)
                                    )

                                    currentOnDragUpdate(pos.x, pos.y, rotation, scale)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } while (continueGesture)

                    if (dragStarted) {
                        if (changed) {
                            currentOnMoved(pos.x, pos.y)
                            currentOnRotated(rotation)
                            currentOnScaleChanged(scale)
                        }
                        currentOnDragEnd()
                    } else if (!sawMultiTouch && totalMovement <= dragSlopPx) {
                        if (currentConnectMode) {
                            currentOnConnectSelect()
                        } else {
                            val elapsed = System.currentTimeMillis() - downTime
                            if (elapsed >= longPressMs) {
                                currentOnLongPress()
                            } else {
                                currentOnTap()
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(if (isDraggingThis) 14.dp else 7.dp, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.linearGradient(listOf(body, body, stickyEdge(note.color))))
                .padding(
                    top = if (usePin) 20.dp else 14.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 16.dp
                )
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
            }
            CurledCorner(Modifier.align(Alignment.BottomEnd))
        }

        if (usePin) {
            Thumbtack(
                pinColor(note.color),
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
            )
        } else {
            TapeStrip(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-9).dp)
            )
        }
    }
}

@Composable
private fun Thumbtack(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(20.dp)) {
        Box(
            Modifier
                .size(20.dp)
                .offset(y = 3.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = .30f))
        )
        Box(
            Modifier
                .size(20.dp)
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
            Modifier
                .size(6.dp)
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
        modifier
            .width(58.dp)
            .height(18.dp)
            .rotate(-3f)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = .38f))
    )
}

@Composable
private fun CurledCorner(modifier: Modifier = Modifier) {
    ComposeCanvas(modifier.size(26.dp)) {
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
// ✅ پایان کامل فایل BoardScreen.kt
