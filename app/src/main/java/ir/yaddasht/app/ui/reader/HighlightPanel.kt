package ir.yaddasht.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.util.Highlight

fun highlightColor(name: String): Color = when (name) {
    "yellow" -> Color(0xFFFFEB3B)
    "pink" -> Color(0xFFFF69B4)
    "green" -> Color(0xFF81C784)
    "blue" -> Color(0xFF81D4FA)
    "orange" -> Color(0xFFFFB74D)
    else -> Color(0xFFFFEB3B)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsSheet(
    highlights: List<Highlight>,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF202020)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
            Text("هایلایت‌ها و حاشیه‌ها 🖍️", color = Color.White, fontFamily = LalezarFont, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            if (highlights.isEmpty()) {
                Text("هنوز هایلایتی نداری…\nمتن را انتخاب کن و دکمهٔ قلم را بزن.", color = Color(0xFF888888), fontSize = 13.sp)
            }
            highlights.forEach { h ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(highlightColor(h.color)))
                    Spacer(Modifier.padding(start = 10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.text, color = Color.White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (h.note.isNotBlank()) {
                            Text("📝 " + h.note, color = Color(0xFF999999), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = { onDelete(h.id) }) {
                        Icon(Icons.Filled.Delete, "حذف", tint = Color(0xFFE57373))
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightPickDialog(
    selectedText: String,
    onSave: (color: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("yellow") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("هایلایت انتخاب شود؟ 🖍️", color = Color.White, fontFamily = LalezarFont) },
        text = {
            Column {
                Text(selectedText, color = Color(0xFFB8B8B8), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("yellow", "pink", "green", "blue", "orange").forEach { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(highlightColor(c))
                                .border(if (color == c) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { color = c }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)", color = Color(0xFF999999)) },
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(color, note) }) { Text("ذخیره", color = Color(0xFFFFB74D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = Color(0xFF999999)) } }
    )
}

@Composable
fun PdfPageNoteDialog(
    pageNumber: Int,
    existingNote: String?,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf(existingNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202020),
        title = { Text("📝 یادداشت روی صفحهٔ $pageNumber", color = Color.White, fontFamily = LalezarFont) },
        text = {
            Column {
                Text(
                    "چون PDF متن قابل‌انتخاب ندارد، می‌توانی روی هر صفحه یادداشت بگذاری.",
                    color = Color(0xFF999999),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت", color = Color(0xFF999999)) },
                    modifier = Modifier.padding(horizontal = 0.dp),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note) }) {
                Text("ذخیره", color = Color(0xFFFFB74D))
            }
        },
        dismissButton = {
            Row {
                if (existingNote != null) {
                    TextButton(onClick = onDelete) {
                        Text("حذف", color = Color(0xFFE57373))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("انصراف", color = Color(0xFF999999))
                }
            }
        }
    )
}
