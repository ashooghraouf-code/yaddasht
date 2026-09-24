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
    val density = pxPerDp.coerceAtLeast(1f)

    // Calculate map items
    val miniMarkers = remember(
        visibleItems,
        images,
        notes,
        noteSizes.value,
        imageSizes.value,
        boardWidthDp,
        boardHeightDp
    ) {
        val markers = mutableListOf<MiniMarker>()

        visibleItems.forEach { item ->
            val note = notes.firstOrNull { it.id == item.noteId } ?: return@forEach
            val key = "${item.noteId}:${item.boardId}"
            val measured = noteSizes.value[key]
            val sc = item.scale.coerceIn(0.3f, 3.0f)

            val w = measured?.first?.toFloat()?.let { it / density } ?: (BASE_NOTE_WIDTH * sc)
            val h = measured?.second?.toFloat()?.let { it / density } ?: (140f * sc)

            markers.add(
                MiniMarker(
                    id = item.noteId,
                    isImage = false,
                    x = item.x,
                    y = item.y,
                    w = w,
                    h = h,
                    rotation = item.rotation,
                    color = stickyBody(note.color)
                )
            )
        }

        images.forEach { img ->
            val measured = imageSizes.value[img.id]
            val sc = img.scale.coerceIn(0.3f, 3.0f)

            val w = measured?.first?.toFloat()?.let { it / density } ?: (BASE_IMAGE_WIDTH * sc)
            val h = measured?.second?.toFloat()?.let { it / density } ?: (BASE_IMAGE_WIDTH * sc)

            markers.add(
                MiniMarker(
                    id = img.id,
                    isImage = true,
                    x = img.x,
                    y = img.y,
                    w = w,
                    h = h,
                    rotation = img.rotation,
                    color = Color.White
                )
            )
        }

        markers
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val scaleX = canvasW / boardWidthDp.coerceAtLeast(1f)
            val scaleY = canvasH / boardHeightDp.coerceAtLeast(1f)

            val live = liveDragState.value

            // Draw items/markers
            miniMarkers.forEach { marker ->
                if (live != null && live.id == marker.id && live.isImage == marker.isImage) {
                    return@forEach
                }

                val cx = (marker.x + marker.w / 2f) * scaleX
                val cy = (marker.y + marker.h / 2f) * scaleY
                val halfW = (marker.w / 2f) * scaleX
                val halfH = (marker.h / 2f) * scaleY

                drawRotatedRect(
                    center = Offset(cx, cy),
                    halfW = halfW.coerceAtLeast(1f),
                    halfH = halfH.coerceAtLeast(1f),
                    rotation = marker.rotation,
                    fillColor = marker.color,
                    strokeColor = Color.Black.copy(alpha = 0.4f),
                    strokeWidth = 1f
                )
            }

            // Draw active drag preview
            live?.let { lv ->
                val cx = (lv.x + lv.w / 2f) * scaleX
                val cy = (lv.y + lv.h / 2f) * scaleY
                val halfW = (lv.w / 2f) * scaleX
                val halfH = (lv.h / 2f) * scaleY

                drawRotatedRect(
                    center = Offset(cx, cy),
                    halfW = halfW.coerceAtLeast(2f),
                    halfH = halfH.coerceAtLeast(2f),
                    rotation = lv.rotation,
                    fillColor = lv.color,
                    strokeColor = Color(0xFFFFB74D),
                    strokeWidth = 2f
                )
            }

            // Draw current viewport indicator
            val maxScrollH = (boardWidthDp - viewportWidthDp).coerceAtLeast(1f)
            val maxScrollV = (boardHeightDp - viewportHeightDp).coerceAtLeast(1f)

            val scrollRatioH = if (scrollStateH.maxValue > 0) {
                scrollStateH.value.toFloat() / scrollStateH.maxValue.toFloat()
            } else 0f

            val scrollRatioV = if (scrollStateV.maxValue > 0) {
                scrollStateV.value.toFloat() / scrollStateV.maxValue.toFloat()
            } else 0f

            val vpLeftDp = scrollRatioH * maxScrollH
            val vpTopDp = scrollRatioV * maxScrollV

            val vpLeft = vpLeftDp * scaleX
            val vpTop = vpTopDp * scaleY
            val vpWidth = (viewportWidthDp * scaleX).coerceAtMost(canvasW - vpLeft)
            val vpHeight = (viewportHeightDp * scaleY).coerceAtMost(canvasH - vpTop)

            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(vpLeft, vpTop),
                size = Size(vpWidth, vpHeight)
            )

            drawRect(
                color = Color(0xFFFFB74D),
                topLeft = Offset(vpLeft, vpTop),
                size = Size(vpWidth, vpHeight),
                style = Stroke(width = 1.5f * density)
            )

            // Draw active touch/finger position marker
            fingerState.value?.let { finger ->
                val fx = finger.x * scaleX
                val fy = finger.y * scaleY

                drawCircle(
                    color = Color.Red.copy(alpha = 0.8f),
                    radius = 3f * density,
                    center = Offset(fx, fy)
                )
            }
        }
    }
}

@Composable
private fun CorkTexture(bgIndex: Int) {
    ComposeCanvas(Modifier.fillMaxSize()) {
        val dotA = corkDotA(bgIndex)
        val dotB = corkDotB(bgIndex)
        val random = Random(42)

        val step = 28f
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                val offsetX = random.nextFloat() * 20f
                val offsetY = random.nextFloat() * 20f
                val radius = random.nextFloat() * 2f + 1f
                val useA = random.nextBoolean()

                drawCircle(
                    color = if (useA) dotA else dotB,
                    radius = radius,
                    center = Offset(x + offsetX, y + offsetY)
                )
                y += step
            }
            x += step
        }
    }
}

@Composable
private fun Vignette(bgIndex: Int) {
    ComposeCanvas(Modifier.fillMaxSize()) {
        val darkColor = if (bgIndex == 2) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.25f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, darkColor),
                center = center,
                radius = size.maxDimension / 1.2f
            )
        )
    }
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
    var offsetX by remember(item.x) { mutableStateOf(item.x) }
    var offsetY by remember(item.y) { mutableStateOf(item.y) }
    var rotation by remember(item.rotation) { mutableStateOf(item.rotation) }
    var scale by remember(item.scale) { mutableStateOf(item.scale.coerceIn(0.3f, 3.0f)) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .onSizeChanged { onMeasured(it.width, it.height) }
            .pointerInput(connectMode) {
                if (connectMode) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onConnectSelect()
                    }
                } else {
                    awaitEachGesture {
                        awaitFirstDown()
                        onDragStart()

                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val rot = event.calculateRotation()

                            if (zoom != 1f || pan != Offset.Zero || rot != 0f) {
                                scale = (scale * zoom).coerceIn(0.3f, 3.0f)
                                rotation += rot
                                offsetX = (offsetX + pan.x).coerceIn(0f, clampX)
                                offsetY = (offsetY + pan.y).coerceIn(0f, clampY)

                                onDragUpdate(offsetX, offsetY, rotation, scale)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })

                        onMoved(offsetX, offsetY)
                        onRotated(rotation)
                        onScaleChanged(scale)
                        onDragEnd()
                    }
                }
            }
            .combinedClickable(
                onClick = { if (connectMode) onConnectSelect() else onTap() },
                onLongClick = onLongPress
            )
            .shadow(if (isDraggingThis) 12.dp else 4.dp, RoundedCornerShape(4.dp))
            .background(stickyBody(note.color))
            .border(1.dp, stickyEdge(note.color), RoundedCornerShape(4.dp))
            .padding(12.dp)
            .width(BASE_NOTE_WIDTH.dp)
    ) {
        Column {
            Text(
                text = note.title.ifBlank { "بدون عنوان" },
                fontFamily = LalezarFont,
                fontSize = 15.sp,
                color = Color(0xFF3E2723),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = note.body,
                fontFamily = VazirFont,
                fontSize = 11.sp,
                color = Color(0xFF5D4037),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
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
    var offsetX by remember(image.x) { mutableStateOf(image.x) }
    var offsetY by remember(image.y) { mutableStateOf(image.y) }
    var rotation by remember(image.rotation) { mutableStateOf(image.rotation) }
    var scale by remember(image.scale) { mutableStateOf(image.scale.coerceIn(0.3f, 3.0f)) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .onSizeChanged { onMeasured(it.width, it.height) }
            .pointerInput(connectMode) {
                if (connectMode) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onConnectSelect()
                    }
                } else {
                    awaitEachGesture {
                        awaitFirstDown()
                        onDragStart()

                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val rot = event.calculateRotation()

                            if (zoom != 1f || pan != Offset.Zero || rot != 0f) {
                                scale = (scale * zoom).coerceIn(0.3f, 3.0f)
                                rotation += rot
                                offsetX = (offsetX + pan.x).coerceIn(0f, clampX)
                                offsetY = (offsetY + pan.y).coerceIn(0f, clampY)

                                onDragUpdate(offsetX, offsetY, rotation, scale)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })

                        onMoved(offsetX, offsetY)
                        onRotated(rotation)
                        onScaleChanged(scale)
                        onDragEnd()
                    }
                }
            }
            .combinedClickable(
                onClick = { if (connectMode) onConnectSelect() else onTap() },
                onLongClick = onLongPress
            )
            .shadow(if (isDraggingThis) 12.dp else 4.dp, RoundedCornerShape(6.dp))
            .background(Color.White)
            .padding(4.dp)
            .size(BASE_IMAGE_WIDTH.dp)
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = "تصویر تابلو",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
        )
    }
}
