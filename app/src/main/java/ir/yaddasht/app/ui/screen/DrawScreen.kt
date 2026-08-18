package ir.yaddasht.app.ui.screen

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.data.Attachment
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.ui.theme.*
import ir.yaddasht.app.util.AttachmentStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private data class DrawStrokeData(val points: List<androidx.compose.ui.geometry.Offset>, val color: Color, val width: Float)

@Composable
fun DrawScreen(dao: NoteDao, noteId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var strokes by remember { mutableStateOf(emptyList<DrawStrokeData>()) }
    var currentPoints by remember { mutableStateOf(emptyList<androidx.compose.ui.geometry.Offset>()) }
    var brushColor by remember { mutableStateOf(Color(0xFF22302B)) }
    var brushWidth by remember { mutableFloatStateOf(7f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val brushColors = listOf(
        Color(0xFF22302B), Color(0xFFC64B2C), Color(0xFFD98A16),
        Color(0xFF2E7D52), Color(0xFF2B6CB0), Color(0xFFB83280), Color.White
    )

    Box(Modifier.fillMaxSize().background(PaperWhite)) {
        Canvas(Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentPoints = listOf(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPoints = currentPoints + change.position
                    },
                    onDragEnd = {
                        if (currentPoints.size > 1)
                            strokes = strokes + DrawStrokeData(currentPoints, brushColor, brushWidth)
                        currentPoints = emptyList()
                    },
                    onDragCancel = { currentPoints = emptyList() }
                )
            }
        ) {
            (strokes + DrawStrokeData(currentPoints, brushColor, brushWidth)).forEach { s ->
                if (s.points.size > 1) {
                    val path = Path().apply {
                        moveTo(s.points.first().x, s.points.first().y)
                        for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                    }
                    drawPath(path, s.color, style = DrawStroke(s.width))
                } else if (s.points.size == 1) {
                    drawCircle(s.color, s.width / 2, s.points.first())
                }
            }
        }

        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            DrawButton("✕") { onBack() }
            DrawButton("↩") { strokes = strokes.dropLast(1) }
            DrawButton("🗑") { strokes = emptyList() }
            Spacer(Modifier.weight(1f))
            Surface(onClick = {
                if (strokes.isEmpty() || canvasSize == IntSize.Zero) {
                    Toast.makeText(context, "اول یه چیزی بکش! 🖌️", Toast.LENGTH_SHORT).show()
                    return@Surface
                }
                scope.launch(Dispatchers.IO) {
                    val bmp = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
                    val c = android.graphics.Canvas(bmp)
                    c.drawColor(android.graphics.Color.WHITE)
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                    strokes.forEach { s ->
                        paint.color = s.color.toArgb()
                        paint.strokeWidth = s.width
                        val p = android.graphics.Path()
                        p.moveTo(s.points.first().x, s.points.first().y)
                        for (i in 1 until s.points.size) p.lineTo(s.points[i].x, s.points[i].y)
                        c.drawPath(p, paint)
                    }
                    val file = File(AttachmentStore.attachmentsDir(context), "DRAW_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    dao.insertAttachment(Attachment(noteId = noteId, fileName = file.name, filePath = file.absolutePath, mimeType = "image/png", isImage = true))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "نقاشی ضمیمه شد 🎨", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }
            }, shape = RoundedCornerShape(14.dp), color = Saffron) {
                Text("💾 ذخیره", Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = Ink, fontFamily = LalezarFont, fontSize = 15.sp)
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(DeepGreen.copy(alpha = .93f)).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                brushColors.forEach { c ->
                    Box(Modifier.size(30.dp).clip(CircleShape).background(c)
                        .border(if (brushColor == c) 3.dp else 1.dp,
                            if (brushColor == c) Saffron else Color.Black.copy(alpha = .2f), CircleShape)
                        .clickable { brushColor = c })
                }
                Spacer(Modifier.weight(1f))
                Text(if (brushColor == Color.White) "پاک‌کن 🧽" else "قلم 🖌️",
                    fontSize = 12.sp, color = MutedGreenText)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("〰️", fontSize = 15.sp)
                Slider(value = brushWidth, onValueChange = { brushWidth = it },
                    valueRange = 2f..28f, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Saffron, activeTrackColor = Saffron))
            }
        }
    }
}

@Composable
private fun DrawButton(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = DeepGreen.copy(alpha = .85f)) {
        Text(label, Modifier.padding(horizontal = 14.dp, vertical = 7.dp), fontSize = 16.sp, color = PaperWhite)
    }
}
