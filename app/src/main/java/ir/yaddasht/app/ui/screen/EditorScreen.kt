package ir.yaddasht.app.ui.screen

import android.app.Activity
import android.graphics.Color
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// مدل TextBox
data class TextBoxData(
    val id: Int,
    var text: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var rotation: Float,
    var fontSize: Float,
    var textColor: Int,
    var backgroundColor: Int,
    var isBold: Boolean,
    var isItalic: Boolean
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    dao: NoteDao,
    noteId: Long,
    onBack: () -> Unit,
    onOpenDraw: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNewNote = noteId < 0
    
    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(DeepGreen) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStylePicker by remember { mutableStateOf(false) }
    var showFontSettings by remember { mutableStateOf(false) }
    var notebookStyle by remember { mutableStateOf("blank") }
    var fontSize by remember { mutableStateOf(16f) }
    var textColor by remember { mutableStateOf(Color.DarkGray) }
    var isRTL by remember { mutableStateOf(true) }
    var textBoxes by remember { mutableStateOf<List<TextBoxData>>(emptyList()) }
    var selectedText by remember { mutableStateOf("") }
    var showTextFormatting by remember { mutableStateOf(false) }
    
    val pageStyles = listOf(
        "blank" to "📄 خالی",
        "lined" to " خط‌دار",
        "lined_narrow" to "📝 خط‌باریک",
        "lined_wide" to "📝 خط‌پهن",
        "grid" to "📐 شطرنجی",
        "grid_narrow" to "📐 شطرنجی‌ریز",
        "columns" to "📰 ستونی",
        "planner" to "📅 برنامه‌ریز"
    )
    
    val backgroundColors = listOf(
        0xFFFFF8E1.toInt(),
        0xFFE3F2FD.toInt(),
        0xFFF3E5F5.toInt(),
        0xFFE8F5E9.toInt(),
        0xFFFFF3E0.toInt(),
        0xFFECEFF1.toInt(),
        0xFF1A1A1A.toInt(),
        0xFF2E7D32.toInt(),
        0xFF1565C0.toInt(),
        0xFF6A1B9A.toInt(),
        0xFFD7CCC8.toInt(),
        0xFFFFEBEE.toInt()
    )
    
    LaunchedEffect(noteId) {
        if (!isNewNote) {
            dao.getNoteById(noteId)?.let { n ->
                note = n
                title = n.title
                body = n.body
                selectedColor = Color(n.color)
                notebookStyle = n.notebookStyle
                fontSize = n.fontSize
                textColor = Color(n.textColor)
                isRTL = n.isRTL
                try {
                    val jsonArray = JSONArray(n.textBoxes)
                    textBoxes = (0 until jsonArray.length()).map {
                        val obj = jsonArray.getJSONObject(it)
                        TextBoxData(
                            id = obj.getInt("id"),
                            text = obj.getString("text"),
                            x = obj.getDouble("x").toFloat(),
                            y = obj.getDouble("y").toFloat(),
                            width = obj.getDouble("width").toFloat(),
                            height = obj.getDouble("height").toFloat(),
                            rotation = obj.getDouble("rotation").toFloat(),
                            fontSize = obj.getDouble("fontSize").toFloat(),
                            textColor = obj.getInt("textColor"),
                            backgroundColor = obj.getInt("backgroundColor"),
                            isBold = obj.getBoolean("isBold"),
                            isItalic = obj.getBoolean("isItalic")
                        )
                    }
                } catch (e: Exception) {
                    textBoxes = emptyList()
                }
            }
        }
    }
    
    Box(Modifier.fillMaxSize().background(DeepGreen)) {
        Column(Modifier.fillMaxSize()) {
            EditorHeader(
                title = title,
                onTitleChange = { title = it },
                onBack = onBack,
                onSave = {
                    scope.launch {
                        val updatedNote = if (isNewNote) {
                            Note(
                                title = title,
                                body = body,
                                color = selectedColor.value,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                                pinned = false,
                                reminderAt = 0,
                                notebookStyle = notebookStyle,
                                fontSize = fontSize,
                                textColor = textColor.value,
                                isRTL = isRTL,
                                textBoxes = textBoxesToJSON(textBoxes)
                            )
                        } else {
                            note?.copy(
                                title = title,
                                body = body,
                                color = selectedColor.value,
                                updatedAt = System.currentTimeMillis(),
                                notebookStyle = notebookStyle,
                                fontSize = fontSize,
                                textColor = textColor.value,
                                isRTL = isRTL,
                                textBoxes = textBoxesToJSON(textBoxes)
                            )
                        }
                        updatedNote?.let {
                            if (isNewNote) dao.insert(it) else dao.update(it)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "ذخیره شد ✅", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                },
                onColorClick = { showColorPicker = true },
                onStyleClick = { showStylePicker = true },
                onFontClick = { showFontSettings = true },
                onDrawClick = { onOpenDraw(noteId) },
                selectedColor = selectedColor
            )
            
            if (showTextFormatting) {
                TextFormattingToolbar(
                    selectedText = selectedText,
                    onBold = { },
                    onItalic = { },
                    onUnderline = { },
                    onHighlight = { },
                    onAlignChange = { },
                    onClose = { showTextFormatting = false }
                )
            }
            
            Box(
                Modifier
                    .fillMaxSize()
                    .background(selectedColor)
                    .then(getPageStyleModifier(notebookStyle))
            ) {
                BasicTextField(
                    value = body,
                    onValueChange = { 
                        body = it.text
                        if (it.selection.start != it.selection.end) {
                            selectedText = it.text.substring(it.selection.start, it.selection.end)
                            showTextFormatting = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .then(if (isRTL) Modifier.wrapContentWidth(Alignment.End) else Modifier.wrapContentWidth(Alignment.Start)),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = fontSize.sp,
                        color = textColor
                    )
                )
                
                textBoxes.forEach { textBox ->
                    DraggableResizableTextBox(
                        textBox = textBox,
                        onUpdate = { updatedBox ->
                            textBoxes = textBoxes.map { if (it.id == updatedBox.id) updatedBox else it }
                        },
                        onDelete = {
                            textBoxes = textBoxes.filter { it.id != textBox.id }
                        }
                    )
                }
                
                FloatingActionButton(
                    onClick = {
                        textBoxes = textBoxes + TextBoxData(
                            id = System.currentTimeMillis().toInt(),
                            text = "متن جدید",
                            x = 50f,
                            y = 300f,
                            width = 200f,
                            height = 100f,
                            rotation = 0f,
                            fontSize = fontSize,
                            textColor = textColor.value,
                            backgroundColor = Color.White.value.toInt(),
                            isBold = false,
                            isItalic = false
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = Saffron,
                    contentColor = Ink
                ) {
                    Icon(Icons.Filled.TextFields, "افزودن متن")
                }
            }
        }
        
        if (showColorPicker) {
            ColorPickerDialog(
                colors = backgroundColors,
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it; showColorPicker = false },
                onDismiss = { showColorPicker = false }
            )
        }
        
        if (showStylePicker) {
            StylePickerDialog(
                styles = pageStyles,
                selectedStyle = notebookStyle,
                onStyleSelected = { notebookStyle = it; showStylePicker = false },
                onDismiss = { showStylePicker = false }
            )
        }
        
        if (showFontSettings) {
            FontSettingsDialog(
                fontSize = fontSize,
                textColor = textColor,
                isRTL = isRTL,
                onFontSizeChange = { fontSize = it },
                onTextColorChange = { textColor = it },
                onRTLChange = { isRTL = it },
                onDismiss = { showFontSettings = false }
            )
        }
    }
}

fun textBoxesToJSON(textBoxes: List<TextBoxData>): String {
    val jsonArray = JSONArray()
    textBoxes.forEach { box ->
        val obj = JSONObject().apply {
            put("id", box.id)
            put("text", box.text)
            put("x", box.x.toDouble())
            put("y", box.y.toDouble())
            put("width", box.width.toDouble())
            put("height", box.height.toDouble())
            put("rotation", box.rotation.toDouble())
            put("fontSize", box.fontSize.toDouble())
            put("textColor", box.textColor)
            put("backgroundColor", box.backgroundColor)
            put("isBold", box.isBold)
            put("isItalic", box.isItalic)
        }
        jsonArray.put(obj)
    }
    return jsonArray.toString()
}

@Composable
fun getPageStyleModifier(style: String): Modifier {
    return when (style) {
        "lined" -> Modifier.background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = List(50) { if (it % 2 == 0) Color.Transparent else Color.Gray.copy(alpha = 0.3f) }
            )
        )
        "grid" -> Modifier.background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = List(50) { if (it % 5 == 0) Color.Gray.copy(alpha = 0.3f) else Color.Transparent }
            )
        )
        else -> Modifier
    }
}

@Composable
fun DraggableResizableTextBox(
    textBox: TextBoxData,
    onUpdate: (TextBoxData) -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(textBox.x) }
    var offsetY by remember { mutableStateOf(textBox.y) }
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(textBox.rotation) }
    var isEditing by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.rotationZ = rotation
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotationChange ->
                    offsetX += pan.x
                    offsetY += pan.y
                    scale *= zoom
                    rotation += rotationChange
                    onUpdate(textBox.copy(x = offsetX, y = offsetY, rotation = rotation))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    offsetX += change.delta.x
                    offsetY += change.delta.y
                    onUpdate(textBox.copy(x = offsetX, y = offsetY))
                }
            }
            .background(Color(textBox.backgroundColor), RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(8.dp)
            .width(textBox.width.dp)
            .height(textBox.height.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝",
                    fontSize = 12.sp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            offsetX += change.delta.x
                            offsetY += change.delta.y
                            onUpdate(textBox.copy(x = offsetX, y = offsetY))
                        }
                    }
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
            
            if (isEditing) {
                BasicTextField(
                    value = textBox.text,
                    onValueChange = { onUpdate(textBox.copy(text = it.text)) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = textBox.fontSize.sp,
                        color = Color(textBox.textColor)
                    )
                )
            } else {
                Text(
                    text = textBox.text,
                    fontSize = textBox.fontSize.sp,
                    color = Color(textBox.textColor),
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isEditing = true }
                )
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    colors: List<Int>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("رنگ زمینه") },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(4)) {
                items(colors) { color ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                if (Color(color) == selectedColor) 3.dp else 1.dp,
                                if (Color(color) == selectedColor) Saffron else Color.Gray
                            )
                            .clickable { onColorSelected(Color(color)) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
fun StylePickerDialog(
    styles: List<Pair<String, String>>,
    selectedStyle: String,
    onStyleSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سبک صفحه") },
        text = {
            Column {
                styles.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (key == selectedStyle) Saffron.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { onStyleSelected(key) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
fun FontSettingsDialog(
    fontSize: Float,
    textColor: Color,
    isRTL: Boolean,
    onFontSizeChange: (Float) -> Unit,
    onTextColorChange: (Color) -> Unit,
    onRTLChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیمات فونت") },
        text = {
            Column {
                Text("اندازه فونت: ${fontSize.toInt()}")
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 10f..32f
                )
                
                Spacer(Modifier.height(16.dp))
                Text("رنگ متن:")
                Row {
                    listOf(Color.Black, Color.DarkGray, Color.Red, Color.Blue, Color(0xFF4CAF50)).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, if (color == textColor) Saffron else Color.Transparent, CircleShape)
                                .clickable { onTextColorChange(color) }
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRTL, onCheckedChange = onRTLChange)
                    Text("راست‌به‌چپ")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
fun TextFormattingToolbar(
    selectedText: String,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onHighlight: () -> Unit,
    onAlignChange: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepGreenSoft)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onBold) { Icon(Icons.Filled.FormatBold, "Bold") }
        IconButton(onClick = onItalic) { Icon(Icons.Filled.FormatItalic, "Italic") }
        IconButton(onClick = onUnderline) { Icon(Icons.Filled.FormatUnderlined, "Underline") }
        IconButton(onClick = onHighlight) { Icon(Icons.Filled.FormatColorHighlight, "Highlight") }
        IconButton(onClick = onAlignChange) { Icon(Icons.Filled.FormatAlignRight, "Align") }
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
    }
}

@Composable
fun EditorHeader(
    title: String,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onColorClick: () -> Unit,
    onStyleClick: () -> Unit,
    onFontClick: () -> Unit,
    onDrawClick: () -> Unit,
    selectedColor: Color
) {
    Column(Modifier.background(DeepGreen).padding(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "بازگشت", tint = PaperWhite)
            }
            
            Text("چراغ راه", fontFamily = LalezarFont, fontSize = 20.sp, color = PaperWhite)
            
            Row {
                IconButton(onClick = onColorClick) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(2.dp, Saffron, CircleShape)
                    )
                }
                IconButton(onClick = onStyleClick) {
                    Icon(Icons.Filled.GridOn, "استایل", tint = PaperWhite)
                }
                IconButton(onClick = onFontClick) {
                    Icon(Icons.Filled.Title, "فونت", tint = PaperWhite)
                }
                IconButton(onClick = onDrawClick) {
                    Icon(Icons.Filled.Brush, "نقاشی", tint = PaperWhite)
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Filled.Save, "ذخیره", tint = Saffron)
                }
            }
        }
        
        TextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("عنوان یادداشت...", color = MutedGreenText) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = PaperWhite,
                fontSize = 18.sp,
                fontFamily = LalezarFont
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
