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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.data.Priority
import ir.yaddasht.app.data.Task
import ir.yaddasht.app.data.TaskDao
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
import java.util.Calendar

private const val DAY = 86_400_000L

// 🎨 رنگ‌بندی هوشمند وظایف: هرچه نزدیک‌تر، قرمزتر؛ هرچه دورتر، سبزتر
private fun taskTint(due: Long, done: Boolean): Color = when {
    done -> Color(0xFFB0BEC5)
    due <= 0L -> Color(0xFF7CB87F)
    else -> {
        val diff = due - System.currentTimeMillis()
        when {
            diff < 0 -> Color(0xFFC62828)
            diff < DAY -> Color(0xFFE53935)
            diff < 3 * DAY -> Color(0xFFEF6C00)
            diff < 7 * DAY -> Color(0xFFF9A825)
            else -> Color(0xFF43A047)
        }
    }
}

private fun taskTextColor(due: Long, done: Boolean): Color = when {
    done -> Color(0xFF37474F)
    due <= 0L -> Color(0xFF1B3A20)
    else -> {
        val diff = due - System.currentTimeMillis()
        when {
            diff < 3 * DAY -> Color.White
            diff < 7 * DAY -> Color(0xFF4E342E)
            else -> Color(0xFF1B3A20)
        }
    }
}

@Composable
fun HomeScreen(dao: NoteDao, taskDao: TaskDao, onOpenNote: (Long) -> Unit, onNewNote: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notes by dao.observeNotes().collectAsState(initial = emptyList())
    val counts by dao.observeAttachmentCounts().collectAsState(initial = emptyList())
    val countMap = counts.associate { it.noteId to it.count }
    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showStats by remember { mutableStateOf(false) }
    var hideMemory by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

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

    val filteredNotes = notes.filter {
        query.isBlank() || it.title.contains(query, true) ||
        (!NoteLock.isLocked(it.body) && it.body.contains(query, true))
    }
    val pinned = filteredNotes.filter { it.pinned }
    val others = filteredNotes.filterNot { it.pinned }

    val filteredTasks = tasks.filter {
        query.isBlank() || it.title.contains(query, true) || it.description.contains(query, true)
    }.sortedWith(compareBy<Task> { it.isCompleted }
        .thenBy { if (it.dueDate > 0) it.dueDate else Long.MAX_VALUE })

    val memory = remember(notes) { pickMemory(notes) }

    Scaffold(containerColor = DeepGreen, floatingActionButton = {
        if (tab == 0) NewNoteFab(onNewNote)
        else ExtendedFloatingActionButton(
            onClick = { editingTask = null; showTaskDialog = true },
            containerColor = Saffron, contentColor = Ink,
            shape = RoundedCornerShape(18.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)) {
            Icon(Icons.Filled.Add, "جدید")
            Spacer(Modifier.width(8.dp))
            Text("وظیفه جدید", fontFamily = LalezarFont, fontSize = 17.sp)
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PaperDots()
            Column(Modifier.fillMaxSize()) {
                HomeHeader(count = notes.size, onStats = { showStats = true },
                    onBackup = { doBackup() },
                    onRestore = { restoreLauncher.launch(arrayOf("*/*")) })

                // ✅ تب‌های یادداشت / وظایف
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TabPill("📝 یادداشت‌ها", notes.size.fa(), tab == 0) { tab = 0 }
                    TabPill("✅ وظایف", tasks.size.fa(), tab == 1) { tab = 1 }
                }

                if (tab == 0 && !hideMemory && memory != null && query.isBlank()) {
                    Surface(onClick = { onOpenNote(memory.id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Saffron.copy(alpha = .12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Saffron.copy(alpha = .35f))) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("خاطره‌ای از گذشته", fontFamily = LalezarFont, fontSize = 15.sp, color = Saffron)
                                Text(memory.title.ifBlank { "بدون عنوان" }, fontSize = 12.sp,
                                    color = PaperWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { hideMemory = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, null, tint = MutedGreenText, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                SearchBox(query, { query = it }, Modifier.padding(horizontal = 20.dp))

                if (tab == 0) {
                    when {
                        notes.isEmpty() -> EmptyState()
                        filteredNotes.isEmpty() -> CenterMessage("چیزی پیدا نشد 🔍")
                        else -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(168.dp),
                            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp),
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
                } else {
                    when {
                        tasks.isEmpty() -> EmptyTasksState()
                        filteredTasks.isEmpty() -> CenterMessage("وظیفه‌ای پیدا نشد 🔍")
                        else -> LazyColumn(
                            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            columnItems(filteredTasks, key = { "t${it.id}" }) { task ->
                                TaskCard(task,
                                    onToggle = {
                                        scope.launch(Dispatchers.IO) {
                                            taskDao.update(task.copy(isCompleted = !task.isCompleted))
                                        }
                                    },
                                    onEdit = { editingTask = task; showTaskDialog = true })
                                Spacer(Modifier.height(10.dp))
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

    if (showTaskDialog) {
        TaskEditorDialog(
            initial = editingTask,
            onDismiss = { showTaskDialog = false; editingTask = null },
            onSave = { t ->
                scope.launch(Dispatchers.IO) {
                    if (editingTask == null) taskDao.insert(t) else taskDao.update(t)
                }
                showTaskDialog = false; editingTask = null
            },
            onDelete = editingTask?.let { et ->
                {
                    scope.launch(Dispatchers.IO) { taskDao.deleteById(et.id) }
                    showTaskDialog = false; editingTask = null
                }
            })
    }
}

@Composable
private fun TabPill(label: String, count: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Saffron else DeepGreenSoft,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, LineGreen)) {
        Row(Modifier.padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontFamily = LalezarFont, fontSize = 15.sp,
                color = if (selected) Ink else PaperWhite)
            Spacer(Modifier.width(6.dp))
            Text(count, fontSize = 12.sp,
                color = if (selected) Ink.copy(alpha = .7f) else MutedGreenText)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(task: Task, onToggle: () -> Unit, onEdit: () -> Unit) {
    val bg = taskTint(task.dueDate, task.isCompleted)
    val fg = taskTextColor(task.dueDate, task.isCompleted)
    Box(Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(bg)
        .combinedClickable(onClick = onEdit)
        .padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = fg, checkmarkColor = bg, uncheckedColor = fg.copy(alpha = .7f)))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (task.description.isNotBlank()) {
                    Text(task.description, fontSize = 12.sp, color = fg.copy(alpha = .8f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (task.dueDate > 0) {
                    Spacer(Modifier.height(4.dp))
                    val overdue = !task.isCompleted && task.dueDate < System.currentTimeMillis()
                    Text((if (overdue) "⚠️ " else "📅 ") + FaDate.full(task.dueDate),
                        fontSize = 11.sp, color = fg.copy(alpha = .9f),
                        fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(when (task.priority) {
                Priority.HIGH -> "🔴"
                Priority.NORMAL -> "🟡"
                Priority.LOW -> "🟢"
            }, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TaskEditorDialog(
    initial: Task?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.NORMAL) }
    var due by remember { mutableLongStateOf(initial?.dueDate ?: 0L) }

    fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val dueOptions = listOf(
        "بدون تاریخ" to 0L,
        "امروز" to startOfToday() + DAY - 1,
        "فردا" to startOfToday() + 2 * DAY - 1,
        "این هفته" to startOfToday() + 7 * DAY - 1
    )

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "✅ وظیفه جدید" else "✏️ ویرایش وظیفه",
            fontFamily = LalezarFont, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان وظیفه") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(desc, { desc = it }, label = { Text("توضیح (اختیاری)") },
                    maxLines = 2, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.values().forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p },
                            label = { Text(when (p) {
                                Priority.HIGH -> "🔴 مهم"
                                Priority.NORMAL -> "🟡 عادی"
                                Priority.LOW -> "🟢 کم"
                            }, fontSize = 12.sp) })
                    }
                }
                Text("سررسید:", fontSize = 12.sp, color = MutedGreenText)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dueOptions.forEach { (label, value) ->
                        FilterChip(selected = due == value, onClick = { due = value },
                            label = { Text(label, fontSize = 11.sp) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) return@TextButton
                onSave(Task(
                    id = initial?.id ?: 0,
                    title = title.trim(),
                    description = desc.trim(),
                    dueDate = due,
                    priority = priority,
                    isCompleted = initial?.isCompleted ?: false,
                    reminderTime = initial?.reminderTime ?: 0,
                    hasReminder = initial?.hasReminder ?: false))
            }) { Text("ذخیره", color = Saffron, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, null, tint = Brick, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حذف", color = Brick, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        })
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
    val oldNotes = candidates.filter { System.currentTimeMillis() - it.createdAt > 45L * DAY }
    return sameDayPastYears.ifEmpty { oldNotes }.randomOrNull()
}

private fun togglePin(scope: CoroutineScope, dao: NoteDao, note: Note) {
    scope.launch(Dispatchers.IO) {
        dao.update(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis()))
    }
}

@Composable
private fun HomeHeader(count: Int, onStats: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier
        .fillMaxWidth()
        .background(Brush.verticalGradient(listOf(DeepGreenSoft, DeepGreen)))
        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Saffron)
                .border(2.dp, Color.White.copy(alpha = .25f), CircleShape),
                contentAlignment = Alignment.Center) {
                Text("ی", fontFamily = LalezarFont, fontSize = 32.sp, color = Ink)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("چراغ راه", fontFamily = LalezarFont, fontSize = 34.sp, color = PaperWhite)
                Spacer(Modifier.height(2.dp))
                Text("بدون محدودیت • ${count.fa()} یادداشت • 🤝 تکان بده = جدید",
                    fontSize = 10.sp, color = Saffron)
            }
            IconButton(onClick = onBackup) { Icon(Icons.Filled.FileUpload, "پشتیبان", tint = MutedGreenText) }
            IconButton(onClick = onRestore) { Icon(Icons.Filled.FileDownload, "بازیابی", tint = MutedGreenText) }
            IconButton(onClick = onStats) { Icon(Icons.Filled.BarChart, "آمار", tint = MutedGreenText) }
        }
        Spacer(Modifier.height(8.dp))
        Text(FaDate.full(System.currentTimeMillis()),
            fontSize = 13.sp, color = MutedGreenText, modifier = Modifier.padding(start = 2.dp))
    }
}

@Composable
private fun StatsDialog(notes: List<Note>, attachTotal: Int, onDismiss: () -> Unit) {
    val unlocked = notes.filterNot { NoteLock.isLocked(it.body) }
    val totalWords = unlocked.sumOf { it.body.split(Regex("\\s+")).count(String::isNotBlank) }
    val thisWeek = notes.count { System.currentTimeMillis() - it.updatedAt < 7L * DAY }
    val longest = unlocked.maxByOrNull { it.body.length }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("📊 آمار دفترچه", fontFamily = LalezarFont, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        .heightIn(min = 52.dp)
        .clip(RoundedCornerShape(18.dp)).background(DeepGreenSoft)
        .border(1.dp, LineGreen, RoundedCornerShape(18.dp))
        .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, null, tint = Saffron, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        TextField(value = query, onValueChange = onQueryChange,
            placeholder = { Text("جستجو در یادداشت‌ها و وظایف…", color = MutedGreenText) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            textStyle = androidx.compose.ui.text.TextStyle(color = PaperWhite, fontSize = 15.sp),
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
                drawCircle(Saffron.copy(alpha = 0.06f), radius = 1.1.dp.toPx(),
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
    val angle = remember(note.id) { ((note.id % 3) - 1) * 0.9f }
    val locked = NoteLock.isLocked(note.body)
    val checklist = Checklist.isChecklist(note.body)
    val hasReminder = note.reminderAt > System.currentTimeMillis()
    Box(Modifier.padding(6.dp).fillMaxWidth()
        .graphicsLayer {
            rotationZ = angle; shadowElevation = 7f
            shape = RoundedCornerShape(20.dp); clip = true
        }) {
        Column(Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(paperColor(note.color))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text((if (note.pinned) "📌 " else "") + note.title.ifBlank { "بدون عنوان" },
                    fontFamily = LalezarFont, fontSize = 18.sp, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, "حذف", tint = InkSoft.copy(alpha = .6f),
                        modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                locked -> Text("🔒 محتوای محرمانه", fontSize = 12.sp, color = InkSoft)
                checklist -> {
                    val (done, total) = Checklist.progress(note.body)
                    Text("✅ ${done.fa()} از ${total.fa()} انجام شد",
                        fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(note.body.lines().take(3).joinToString("\n") {
                        it.removePrefix("☐ ").removePrefix("☑ ")
                    }, fontSize = 12.sp, color = InkSoft, maxLines = 3, lineHeight = 20.sp)
                }
                else -> if (note.body.isNotBlank())
                    Text(note.body, fontFamily = VazirFont, fontSize = 13.sp, color = InkSoft,
                        maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (attachCount > 0) Text("📎 ${attachCount.fa()}", fontSize = 11.sp, color = InkSoft)
                if (hasReminder) Text("  ⏰", fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(relativeTimeFa(note.updatedAt), fontSize = 10.sp, color = InkSoft.copy(alpha = .8f))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontFamily = LalezarFont, fontSize = 17.sp, color = Saffron,
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 4.dp))
}

@Composable
private fun NewNoteFab(onNewNote: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onNewNote,
        containerColor = Saffron, contentColor = Ink,
        shape = RoundedCornerShape(18.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)) {
        Icon(Icons.Filled.Add, "جدید")
        Spacer(Modifier.width(8.dp))
        Text("یادداشت جدید", fontFamily = LalezarFont, fontSize = 17.sp)
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Saffron.copy(alpha = .07f))
            .border(2.dp, Saffron.copy(alpha = .45f), CircleShape),
            contentAlignment = Alignment.Center) { Text("✍️", fontSize = 50.sp) }
        Spacer(Modifier.height(24.dp))
        Text("دفترچه‌ات خالی است", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
        Spacer(Modifier.height(10.dp))
        Text("یادداشت بنویس، صدا ضبط کن، نقاشی بکش، چک‌لیست بساز و رویشان قفل بگذار ✨",
            fontSize = 13.sp, color = MutedGreenText, textAlign = TextAlign.Center, lineHeight = 22.sp)
    }
}

@Composable
private fun EmptyTasksState() {
    Column(Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Saffron.copy(alpha = .07f))
            .border(2.dp, Saffron.copy(alpha = .45f), CircleShape),
            contentAlignment = Alignment.Center) { Text("✅", fontSize = 50.sp) }
        Spacer(Modifier.height(24.dp))
        Text("وظیفه‌ای نداری", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
        Spacer(Modifier.height(10.dp))
        Text("با دکمه «وظیفه جدید» اولین کار را اضافه کن 🌱",
            fontSize = 13.sp, color = MutedGreenText, textAlign = TextAlign.Center, lineHeight = 22.sp)
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 15.sp, color = MutedGreenText)
    }
}
