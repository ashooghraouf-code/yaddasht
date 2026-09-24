package ir.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.roundToInt
import kotlin.random.Random

// Import مدل‌های پروژه (بر اساس ساختار پکیج شما)
import ir.app.data.model.BoardImage
import ir.app.data.model.BoardItem
import ir.app.data.model.Note

private const val BASE_IMAGE_WIDTH = 120f
private const val BASE_NOTE_WIDTH = 140f

@Composable
fun BoardScreen(
    boardId: Long,
    onBackClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD7CCC8))
    ) {
        CorkTexture(bgIndex = 0)
        Vignette(bgIndex = 0)
        // ساختار اصلی بورد شما در اینجا قرار می‌گیرد
    }
}

@Composable
fun BoardImageItem(
    image: BoardImage,
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
        modifier = Modifier
            .absoluteOffset {
                IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx())
            }
            .width(widthDp)
            .onSizeChanged { s -> onMeasured(s.width, s.height) }
            .graphicsLayer { rotationZ = rotation }
            .pointerInput(image.id, image.boardId, image.x, image.y, image.rotation, image.scale) {
                val dragSlopPx = 6f * densityF
                val longPressMs = 500L

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    var isLongPressHandled = false
                    var isGestureActive = false

                    currentOnDragStart()

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.isEmpty()) break

                        val elapsed = System.currentTimeMillis() - startTime
                        if (!isGestureActive && activePointers.size == 1 && elapsed >= longPressMs && !isLongPressHandled) {
                            isLongPressHandled = true
                            currentOnLongPress()
                        }

                        if (activePointers.size == 1) {
                            val change = activePointers.first()
                            val dragAmount = change.positionChange()

                            if (dragAmount.getDistance() > dragSlopPx || isGestureActive) {
                                isGestureActive = true
                                change.consume()

                                val newX = (pos.x + dragAmount.x / densityF).coerceIn(0f, currentClampX)
                                val newY = (pos.y + dragAmount.y / densityF).coerceIn(0f, currentClampY)
                                pos = Offset(newX, newY)

                                currentOnDragUpdate(pos.x, pos.y, rotation, scale)
                            }
                        } else if (activePointers.size >= 2) {
                            isGestureActive = true

                            val zoomChange = event.calculateZoom()
                            val rotationChange = event.calculateRotation()
                            val panChange = event.calculatePan()

                            event.changes.forEach { it.consume() }

                            scale = (scale * zoomChange).coerceIn(0.3f, 3.0f)
                            rotation = (rotation + rotationChange) % 360f

                            val newX = (pos.x + panChange.x / densityF).coerceIn(0f, currentClampX)
                            val newY = (pos.y + panChange.y / densityF).coerceIn(0f, currentClampY)
                            pos = Offset(newX, newY)

                            currentOnDragUpdate(pos.x, pos.y, rotation, scale)
                        }
                    }

                    if (!isGestureActive && !isLongPressHandled) {
                        if (currentConnectMode) {
                            currentOnConnectSelect()
                        } else {
                            currentOnTap()
                        }
                    } else if (isGestureActive) {
                        currentOnMoved(pos.x, pos.y)
                        currentOnRotated(rotation)
                        currentOnScaleChanged(scale)
                    }

                    currentOnDragEnd()
                }
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color = Color.White,
            shadowElevation = if (isDraggingThis) 12.dp else 4.dp,
            border = if (currentConnectMode) BorderStroke(2.dp, Color(0xFFFFB74D)) else null
        ) {
            Box(Modifier.padding(4.dp)) {
                AsyncImage(
                    model = image.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun StickyNote(
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
    val usePin = ((item.noteId ?: 0L) % 2L) == 0L

    Box(
        modifier = Modifier
            .absoluteOffset {
                IntOffset(pos.x.dp.roundToPx(), pos.y.dp.roundToPx())
            }
            .width(widthDp)
            .onSizeChanged { s -> onMeasured(s.width, s.height) }
            .graphicsLayer { rotationZ = rotation }
            .pointerInput(item.noteId, item.boardId, item.x, item.y, item.rotation, item.scale) {
                val dragSlopPx = 6f * densityF
                val longPressMs = 500L

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    var isLongPressHandled = false
                    var isGestureActive = false

                    currentOnDragStart()

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.isEmpty()) break

                        val elapsed = System.currentTimeMillis() - startTime
                        if (!isGestureActive && activePointers.size == 1 && elapsed >= longPressMs && !isLongPressHandled) {
                            isLongPressHandled = true
                            currentOnLongPress()
                        }

                        if (activePointers.size == 1) {
                            val change = activePointers.first()
                            val dragAmount = change.positionChange()

                            if (dragAmount.getDistance() > dragSlopPx || isGestureActive) {
                                isGestureActive = true
                                change.consume()

                                val newX = (pos.x + dragAmount.x / densityF).coerceIn(0f, currentClampX)
                                val newY = (pos.y + dragAmount.y / densityF).coerceIn(0f, currentClampY)
                                pos = Offset(newX, newY)

                                currentOnDragUpdate(pos.x, pos.y, rotation, scale)
                            }
                        } else if (activePointers.size >= 2) {
                            isGestureActive = true

                            val zoomChange = event.calculateZoom()
                            val rotationChange = event.calculateRotation()
                            val panChange = event.calculatePan()

                            event.changes.forEach { it.consume() }

                            scale = (scale * zoomChange).coerceIn(0.3f, 3.0f)
                            rotation = (rotation + rotationChange) % 360f

                            val newX = (pos.x + panChange.x / densityF).coerceIn(0f, currentClampX)
                            val newY = (pos.y + panChange.y / densityF).coerceIn(0f, currentClampY)
                            pos = Offset(newX, newY)

                            currentOnDragUpdate(pos.x, pos.y, rotation, scale)
                        }
                    }

                    if (!isGestureActive && !isLongPressHandled) {
                        if (currentConnectMode) {
                            currentOnConnectSelect()
                        } else {
                            currentOnTap()
                        }
                    } else if (isGestureActive) {
                        currentOnMoved(pos.x, pos.y)
                        currentOnRotated(rotation)
                        currentOnScaleChanged(scale)
                    }

                    currentOnDragEnd()
                }
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(3.dp),
            color = stickyBody(note.color),
            shadowElevation = if (isDraggingThis) 10.dp else 3.dp,
            border = if (currentConnectMode) BorderStroke(2.dp, Color(0xFFFFB74D)) else null
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = if (usePin) 20.dp else 12.dp,
                    bottom = 12.dp
                )
            ) {
                Text(
                    text = note.title.ifBlank { "بدون عنوان" },
                    fontSize = (15f * scale).sp,
                    color = Color(0xFF3E2723),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.body,
                    fontSize = (11f * scale).sp,
                    lineHeight = (17f * scale).sp,
                    color = Color(0xFF5D4037),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (usePin) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
                    .size(14.dp)
                    .shadow(2.dp, CircleShape)
                    .background(pinColor(((note.id ?: 0L) % 6L).toInt()), CircleShape)
            )
        }
    }
}

@Composable
fun CorkTexture(bgIndex: Int) {
    ComposeCanvas(Modifier.fillMaxSize()) {
        val rand = Random(42)
        val w = size.width
        val h = size.height

        val colorA = corkDotA(bgIndex)
        val colorB = corkDotB(bgIndex)

        for (i in 0..600) {
            val cx = rand.nextFloat() * w
            val cy = rand.nextFloat() * h
            val r = rand.nextFloat() * 1.8f + 0.6f
            drawCircle(if (i % 2 == 0) colorA else colorB, r, Offset(cx, cy))
        }
    }
}

@Composable
fun Vignette(bgIndex: Int) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f))
                )
            )
    )
}

private fun stickyBody(colorInt: Int): Color = Color(if (colorInt == 0) 0xFFFFF9C4 else colorInt.toLong() or 0xFF000000)
private fun pinColor(index: Int): Color = when (index % 4) {
    0 -> Color(0xFFE53935)
    1 -> Color(0xFF1E88E5)
    2 -> Color(0xFF43A047)
    else -> Color(0xFFFB8C00)
}
private fun corkDotA(bgIndex: Int): Color = Color(0x1A000000)
private fun corkDotB(bgIndex: Int): Color = Color(0x0DFFFFFF)
