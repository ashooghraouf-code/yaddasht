
@Composable
private fun TypeRow(selected: AnnotationType, onSelect: (AnnotationType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        AnnotationType.entries.forEach { type ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected == type) Color(0xFFFFB74D) else Color(0xFF404040))
                    .clickable { onSelect(type) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(type.emoji, fontSize = 16.sp)
                Text(type.faName, fontSize = 9.sp, color = if (selected == type) Color.Black else Color.White)
            }
        }
    }
}

@Composable
private fun AnnotationPickerDialog(
    selectedText: String,
    onSave: (colorKey: String, AnnotationType, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf("gold") }
    var selectedType by remember { mutableStateOf(AnnotationType.HIGHLIGHT) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("🎨 افزودن یادداشت", color = Color.White, fontFamily = LalezarFont, fontSize = 18.sp) },
        text = {
            Column {
                Text(
                    "«${selectedText.take(80)}${if (selectedText.length > 80) "…" else ""}»",
                    color = Color(0xFFB8B8B8), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Text("نوع:", color = Color(0xFF999999), fontSize = 12.sp)
                TypeRow(selectedType) { selectedType = it }
                Text("رنگ:", color = Color(0xFF999999), fontSize = 12.sp)
                ColorRow(selectedColor, AnnotationPalette.colors.take(5)) { selectedColor = it }
                ColorRow(selectedColor, AnnotationPalette.colors.drop(5)) { selectedColor = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)", color = Color(0xFF999999)) },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selectedColor, selectedType, note) }) { Text("ذخیره", color = Color(0xFFFFB74D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = Color(0xFF999999)) } }
    )
}

@Composable
private fun PdfAnnotationDialog(
    pageIndex: Int,
    relX: Float,
    relY: Float,
    onSave: (AnnotationType, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf("gold") }
    var selectedType by remember { mutableStateOf(AnnotationType.NOTE) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("📍 صفحهٔ ${pageIndex + 1}", color = Color.White, fontFamily = LalezarFont, fontSize = 18.sp) },
        text = {
            Column {
                Text("نوع:", color = Color(0xFF999999), fontSize = 12.sp)
                TypeRow(selectedType) { selectedType = it }
                Text("رنگ:", color = Color(0xFF999999), fontSize = 12.sp)
                ColorRow(selectedColor, AnnotationPalette.colors.take(5)) { selectedColor = it }
                ColorRow(selectedColor, AnnotationPalette.colors.drop(5)) { selectedColor = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)", color = Color(0xFF999999)) },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selectedType, selectedColor, note) }) { Text("ذخیره", color = Color(0xFFFFB74D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = Color(0xFF999999)) } }
    )
}

@Composable
private fun AnnotationViewDialog(
    annotation: Annotation,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember(annotation.id) { mutableStateOf(annotation.note) }
    var editing by remember(annotation.id) { mutableStateOf(annotation.note.isBlank()) }
    val c = AnnotationPalette.find(annotation.colorKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("${c.name} • ${annotation.type.faName}", fontFamily = LalezarFont, fontSize = 16.sp, color = Color.White)
            }
        },
        text = {
            Column {
                if (annotation.selectedText.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = c.color.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "«${annotation.selectedText}»",
                            color = Color.White, fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 4, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (editing) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("یادداشت", color = Color(0xFF999999)) },
                        minLines = 2, maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(annotation.note.ifBlank { "بدون یادداشت" }, color = Color(0xFFB8B8B8), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ویرایش", color = Color(0xFFFFB74D))
                    }
                }
            }
        },
        confirmButton = {
            if (editing) TextButton(onClick = { onEdit(note) }) { Text("ذخیره", color = Color(0xFFFFB74D)) }
            else TextButton(onClick = onDismiss) { Text("بستن", color = Color(0xFFFFB74D)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("حذف", color = Color(0xFFE57373)) }
                if (editing) TextButton(onClick = onDismiss) { Text("انصراف", color = Color(0xFF999999)) }
            }
        }
    )
}

@Composable
private fun ReaderSettingsDialog(
    themeIndex: Int,
    fontSize: Int,
    columns: Int,
    isPdf: Boolean,
    onTheme: (Int) -> Unit,
    onFontSize: (Int) -> Unit,
    onColumns: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("⚙️ تنظیمات", color = Color.White, fontFamily = LalezarFont, fontSize = 18.sp) },
        text = {
            Column {
                Text("تم صفحه", color = Color(0xFFB8B8B8), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple(0, "روشن ☀️", Color(0xFFFFF8E1)),
                        Triple(1, "سپیا 📜", Color(0xFFF4ECD8)),
                        Triple(2, "شب 🌙", Color(0xFF1A1A1A))
                    ).forEach { (i, name, color) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (themeIndex == i) 3.dp else 1.dp,
                                        if (themeIndex == i) Color(0xFFFFB74D) else Color.Gray,
                                        CircleShape
                                    )
                                    .clickable { onTheme(i) }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(name, color = Color(0xFFB8B8B8), fontSize = 10.sp)
                        }
                    }
                }
                if (!isPdf) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("اندازه قلم", color = Color(0xFFB8B8B8), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onFontSize((fontSize - 2).coerceAtLeast(12)) }) { Text("−", color = Color.White, fontSize = 22.sp) }
                        Text("$fontSize", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { onFontSize((fontSize + 2).coerceAtMost(28)) }) { Text("+", color = Color.White, fontSize = 22.sp) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("حالت کتاب", color = Color(0xFFB8B8B8), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onColumns(if (columns == 2) 1 else 2) }) {
                        Text(if (columns == 2) "✅ فعال" else "❌ غیرفعال", color = Color(0xFFFFB74D))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن", color = Color(0xFFFFB74D)) } }
    )
}
