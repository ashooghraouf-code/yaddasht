package ir.yaddasht.app.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.ui.theme.*
import ir.yaddasht.app.util.BackupManager
import ir.yaddasht.app.util.Checklist
import ir.yaddasht.app.util.FaDate
import ir.yaddasht.app.util.NoteLock
import ir.yaddasht.app.util.fa
import ir.yaddasht.app.util.relativeTimeFa
import ir.yaddasht.app.util.shareBackupFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(dao: NoteDao, onOpenNote: (Long) -> Unit, onNewNote: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notes by dao.observeNotes().collectAsState(initial = emptyList())
    val counts by dao.observeAttachmentCounts().collectAsState(initial = emptyList())
    val countMap = counts.associate { it.noteId to it.count }

    var query by rememberSaveable { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showStats by remember { mutableStateOf(false) }
    var hideMemory by rememberSaveable { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val count = BackupManager.restore(context, dao, uri)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context,
                        if (count > 0) "$count یادداشت بازیابی شد ✅" else "فایل پشتیبان معتبر نبود ❌",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun doBackup() {
        scope.launch(Dispatchers.IO) {
            val allNotes = dao.allNotesSync()
            if (allNotes.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "یادداشتی برای پشتیبان نیست", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            val json = BackupManager.buildBackup(allNotes, dao.allAttachments().groupBy { it.noteId })
            val file = BackupManager.exportBackupFile(context, json)
            withContext(Dispatchers.Main) { shareBackupFile(context, file) }
        }
    }

    val filtered = notes.filter {
        query.isBlank() || it.title.contains(query, true) ||
        (!NoteLock.isLocked(it.body) && it.body.contains(query, true))
    }
    val pinned = filtered.filter { it.pinned }
    val others = filtered.filterNot { it.pinned }
    val memory = remember(notes) { pickMemory(notes) }

    Scaffold(containerColor = DeepGreen, floatingActionButton = { NewNoteFab(onNewNote) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PaperDots()
            Column(Modifier.fillMaxSize()) {
                HomeHeader(count = notes.size, onStats = { showStats = true },
                    onBackup = { doBackup() },
                    onRestore = { restoreLauncher.launch(arrayOf("*/*")) })

                if (!hideMemory && memory != null && query.isBlank()) {
                    Surface(onClick = { onOpenNote(memory.id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Saffron.copy(alpha = .14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Saffron.copy(alpha = .4f))) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("خاطره‌ای از گذشته", fontFamily = LalezarFont, fontSize = 14.sp, color = Saffron)
                                Text(memory.title.ifBlank { "بدون عنوان" }, fontSize = 12.sp,
                                    color = PaperWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { hideMemory = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Close, null, tint = MutedGreenText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                SearchBox(query, { query = it }, Modifier.padding(horizontal = 20.dp))
                when {
                    notes.isEmpty() -> EmptyState()
                    filtered.isEmpty() -> CenterMessage("چیزی پیدا نشد! 🔍")
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(168.dp),
                        contentPadding = PaddingValues(14.dp, 16.dp, 14.dp, 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (pinned.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("📌 سنجاق‌شده") }
                            items(pinned, key = { "p${it.id}" }) { note ->
                                NoteCard(note, countMap[note.id] ?: 0,
                                    { onOpenNote(note.id) },
                                    { togglePin(scope, dao, note) },
                                    { noteToDelete = note })
                            }
                        }
                        if (others.isNotEmpty()) {
                            if (pinned.isNotEmpty())
                                item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("🗒️ یادداشت‌ها") }
                            items(others, key = { "n${it.id}" }) { note ->
                                NoteCard(note, countMap[note.id] ?: 0,
                                    { onOpenNote(note.id) },
                                    { togglePin(scope, dao, note) },
                                    { noteToDelete = note })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStats) StatsDialog(notes, counts.sumOf { it.count }) { showStats = false }

    noteToDelete?.let { note ->
        AlertDialog(onDismissRequest = { noteToDelete = null },
            title = { Text("حذف یادداشت؟", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = { Text("«${note.title.ifBlank { "بدون عنوان" }}» همراه با ضمیمه‌هایش برای همیشه حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    noteToDelete = null
                    scope.launch(Dispatchers.IO) {
                        val atts = dao.attachmentsByNote(note.id)
                        dao.deleteById(note.id)
                        atts.forEach { File(it.filePath).delete() }
                    }
                }) { Text("حذف", color = Brick, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("انصراف") } })
    }
}

private fun pickMemory(notes: List<Note>): Note? {
    val candidates = notes.filterNot { NoteLock.isLocked(it.body) }
    val now = Calendar.getInstance()
    val sameDayPastYears = candidates.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
        c.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
        c.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH) &&
        c.get(Calendar.YEAR) < now.get(Calendar.YEAR)
    }
    val oldNotes = candidates.filter { System.currentTimeMillis() - it.createdAt > 45L * 24 * 3600 * 1000 }
    return sameDayPastYears.ifEmpty { oldNotes }.randomOrNull()
}

private fun togglePin(scope: CoroutineScope, dao: NoteDao, note: Note) {
    scope.launch(Dispatchers.IO) {
        dao.update(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis()))
    }
}

@Composable
private fun HomeHeader(count: Int, onStats: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Saffron), contentAlignment = Alignment.Center) {
                Text("ی", fontFamily = LalezarFont, fontSize = 30.sp, color = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("یادداشت", fontFamily = LalezarFont, fontSize = 38.sp, color = PaperWhite)
                Text("بدون محدودیت • ${count.fa()} یادداشت • 🤝 تکان بده = جدید",
                    fontSize = 10.sp, color = Saffron)
            }
            IconButton(onClick = onBackup) { Icon(Icons.Filled.FileUpload, "پشتیبان", tint = MutedGreenText) }
            IconButton(onClick = onRestore) { Icon(Icons.Filled.FileDownload, "بازیابی", tint = MutedGreenText) }
            IconButton(onClick = onStats) { Icon(Icons.Filled.BarChart, "آمار", tint = MutedGreenText) }
        }
        Spacer(Modifier.height(6.dp))
        Text(todayFa(), fontSize = 13.sp, color = MutedGreenText, modifier = Modifier.padding(start = 2.dp))
    }
}

private fun todayFa(): String = FaDate.full(System.currentTimeMillis())

@Composable
private fun StatsDialog(notes: List<Note>, attachTotal: Int, onDismiss: () -> Unit) {
    val unlocked = notes.filterNot { NoteLock.isLocked(it.body) }
    val totalWords = unlocked.sumOf { it.body.split(Regex("\\s+")).count(String::isNotBlank) }
    val thisWeek = notes.count { System.currentTimeMillis() - it.updatedAt < 7L * 24 * 3600 * 1000 }
    val longest = unlocked.maxByOrNull { it.body.length }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("📊 آمار دفترچه", fontFamily = LalezarFont, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatRow("کل یادداشت‌ها", notes.size.fa())
                StatRow("کلمه‌های نوشته‌شده", totalWords.fa())
                StatRow("ویرایش در این هفته", thisWeek.fa())
                StatRow("ضمیمه‌ها", attachTotal.fa())
                StatRow("سنجاق‌شده", notes.count { it.pinned }.fa())
                StatRow("قفل‌شده 🔒", notes.count { NoteLock.isLocked(it.body) }.fa())
                StatRow("چک‌لیست فعال", notes.count { Checklist.isChecklist(it.body) }.fa())
                if (longest != null && longest.body.length > 50)
                    StatRow("بلندترین یادداشت", "${longest.body.length.fa()} حرف")
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("بستن", color = Saffron, fontWeight = FontWeight.Bold) } })
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = MutedGreenText, modifier = Modifier.weight(1f))
        Text(value, fontFamily = LalezarFont, fontSize = 18.sp, color = Saffron)
    }
}

@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp)).background(DeepGreenSoft)
        .border(1.dp, LineGreen, RoundedCornerShape(16.dp))
        .padding(horizontal = 14.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, null, tint = Saffron)
        Spacer(Modifier.width(10.dp))
        TextField(value = query, onValueChange = onQueryChange,
            placeholder = { Text("جستجو در یادداشت‌ها…", color = MutedGreenText) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            textStyle = androidx.compose.ui.text.TextStyle(color = PaperWhite, fontSize = 14.sp),
            singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PaperDots() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 30.dp.toPx()
        var y = step / 2; var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) step / 2 else step
            while (x < size.width) {
                drawCircle(Saffron.copy(alpha = 0.07f), radius = 1.1.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, y))
                x += step
            }
            y += step; row++
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(note: Note, attachCount: Int, onClick: () -> Unit, onLongClick: () -> Unit, onDelete: () -> Unit) {
    val angle = remember(note.id) { ((note.id % 3) - 1) * 1.3f }
    val locked = NoteLock.isLocked(note.body)
    val checklist = Checklist.isChecklist(note.body)
    val hasReminder = note.reminderAt > System.currentTimeMillis()

    Box(Modifier.padding(5.dp).fillMaxWidth()
        .graphicsLayer {
            rotationZ = angle; shadowElevation = 9f
            shape = RoundedCornerShape(18.dp); clip = true
        }) {
        Column(Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(paperColor(note.color))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text((if (note.pinned) "📌 " else "") + note.title.ifBlank { "بدون عنوان" },
                    fontFamily = LalezarFont, fontSize = 17.sp, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Close, "حذف", tint = InkSoft.copy(alpha = .55f),
                    modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onDelete).padding(5.dp))
            }
            Spacer(Modifier.height(6.dp))
            when {
                locked -> Text("🔒 محتوای محرمانه", fontSize = 12.sp, color = InkSoft)
                checklist -> {
                    val (done, total) = Checklist.progress(note.body)
                    Text("✅ ${done.fa()} از ${total.fa()} انجام شد",
                        fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(note.body.lines().take(3).joinToString("\n") {
                        it.removePrefix("☐ ").removePrefix("☑ ")
                    }, fontSize = 11.5.sp, color = InkSoft, maxLines = 3, lineHeight = 18.sp)
                }
                else -> if (note.body.isNotBlank())
                    Text(note.body, fontFamily = VazirFont, fontSize = 12.5.sp, color = InkSoft,
                        maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (attachCount > 0) Text("📎 ${attachCount.fa()}", fontSize = 11.sp, color = InkSoft)
                if (hasReminder) Text("   ⏰", fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(relativeTimeFa(note.updatedAt), fontSize = 10.sp, color = InkSoft.copy(alpha = .8f))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontFamily = LalezarFont, fontSize = 16.sp, color = Saffron,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp))
}

@Composable
private fun NewNoteFab(onNewNote: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onNewNote, containerColor = Saffron, contentColor = Ink) {
        Icon(Icons.Filled.Add, "جدید")
        Spacer(Modifier.width(8.dp))
        Text("یادداشت جدید", fontFamily = LalezarFont, fontSize = 17.sp)
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(110.dp).clip(CircleShape).border(2.dp, Saffron.copy(alpha = .5f), CircleShape),
            contentAlignment = Alignment.Center) { Text("✍️", fontSize = 46.sp) }
        Spacer(Modifier.height(20.dp))
        Text("دفترچه‌ات خالی است!", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
        Spacer(Modifier.height(8.dp))
        Text("یادداشت بنویس، صدا ضبط کن، نقاشی بکش، چک‌لیست بساز و رویشان قفل بگذار!",
            fontSize = 13.sp, color = MutedGreenText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 15.sp, color = MutedGreenText)
    }
}
