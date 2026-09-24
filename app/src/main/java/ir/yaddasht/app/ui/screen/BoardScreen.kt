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
                    val n = changes.size.toFloat()
                    return Offset(x / n, y / n)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = down.uptimeMillis
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
