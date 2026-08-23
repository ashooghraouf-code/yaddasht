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
import androidx.compose.ui.graphics.lerp
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

private fun taskTint(due: Long, completed: Boolean): Color {
    if (completed) return Color(0xFF5E8077)
    if (due <= 0L) return Color(0xFFE5484D)
    val now = System.currentTimeMillis()
    val days = (due - now).toFloat() / 86_400_000f
    val t = days.coerceIn(0f, 7f) / 7f
    val red = Color(0xFFE5484D)
    val amber = Color(0xFFF5A524)
    val green = Color(0xFF46A758)
    return if (t < .5f) lerp(red, amber, t / .5f) else lerp(amber, green, (t - .5f) / .5f)
}

private fun dueMillis(opt: Int): Long {
    val c = Calendar.getInstance()
    return when (opt) {
        1 -> { c.set(Calendar.HOUR_OF_DAY, 21); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.timeInMillis }
        2 -> { c.add(Calendar.DAY_OF_YEAR, 1); c.set(Calendar.HOUR_OF_DAY, 12); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.timeInMillis }
        3 -> { c.add(Calendar.DAY_OF_YEAR, 7); c.timeInMillis }
        else -> 0L
    }
}

@Composable
fun HomeScreen(
    dao: NoteDao,
    taskDao: TaskDao,
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit
) {
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
    var showAddTask by remember { mutableStateOf(false) }

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
    val filteredTasks = tasks.filter { query.isBlank() || it.title.contains(query, true) }
    val memory = remember(notes) { pickMemory(notes) }

    Scaffold(containerColor = DeepGreen,
        floatingActionButton = {
            if (tab == 0) NewNoteFab(onNewNote)
            else ExtendedFloatingActionButton(onClick = { showAddTask = true },
                containerColor = Saffron, contentColor = Ink) {
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

                TabRow(selectedTabIndex = tab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
                    Tab(selected = tab == 0, onClick = { tab = 0 },
                        text = { Text("📝 یادداشت‌ها (${notes.size.fa()})", fontFamily = LalezarFont, fontSize = 15.sp) },
                        selectedContentColor = Saffron, unselectedContentColor = MutedGreenText)
                    Tab(selected = tab == 1, onClick = { tab = 1 },
                        text = { Text("✅ وظایف (${tasks.count { !it.isCompleted }.fa()})", fontFamily = LalezarFont, fontSize = 15.sp) },
                        selectedContentColor = Saffron, unselectedContentColor = MutedGreenText)
                }

                if (!hideMemory && memory != null && query.isBlank() && tab == 0) {
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

                if (tab == 0) {
                    when {
                        notes.isEmpty() -> EmptyState()
                        filtered.isEmpty() -> CenterMessage("چیزی پیدا نشد 🔍")
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
                } else {
                    when {
                        tasks.isEmpty() -> EmptyTasksState()
                        filteredTasks.isEmpty() -> CenterMessage("وظیفه‌ای پیدا نشد 🔍")
                        else -> LazyColumn(
                            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            filteredTasks.forEach { task ->
                                item {
                                    TaskCard(task,
                                        onToggle = {
                                            scope.launch(Dispatchers.IO) {
                                                taskDao.update(task.copy(isCompleted = !task.isCompleted))
                                            }
                                        },
                                        onDelete = {
                                            scope.launch(Dispatchers.IO) { taskDao.deleteById(task.id) }
                                        })
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStats) StatsDialog(notes, counts.sumOf { it.count }) { showStats = false }

    if (showAddTask) {
        AddTaskDialog(
            onDismiss = { showAddTask = false },
            onSave = { title, due, pr ->
                scope.launch(Dispatchers.IO) {
                    taskDao.insert(Task(title = title, dueDate = due, priority = pr))
                }
                showAddTask = false
                Toast.makeText(context, "وظیفه اضافه شد ✅", Toast.LENGTH_SHORT).show()
            })
    }

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

@Composable
private fun TaskCard(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    val tint = taskTint(task.dueDate, task.isCompleted)
    Column(Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DeepGreenSoft)
        .border(1.5.dp, tint.copy(alpha = .75f), RoundedCornerShape(18.dp))
        .padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = tint, checkmarkColor = DeepGreen))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontFamily = VazirFont, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (task.isCompleted) MutedGreenText else PaperWhite,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                if (task.dueDate > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text("📅 " + FaDate.full(task.dueDate), fontSize = 11.sp, color = tint,
                        fontWeight = FontWeight.Bold)
                }
            }
            if (task.priority == Priority.HIGH && !task.isCompleted) Text("🔴", fontSize = 12.sp)
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.Close, "حذف", tint = Brick, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, Long, Priority) -> Unit) {
    var title by remember { mutableStateOf("") }
    var dueOpt by remember { mutableIntStateOf(0) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("✅ وظیفه جدید", fontFamily = LalezarFont, fontSize = 20.sp) },
        text = {
            Column {
                OutlinedTextField(title, { title = it },
                    label = { Text("عنوان وظیفه") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("سررسید:", fontSize = 12.sp, color = MutedGreenText)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DueChip("بدون تاریخ", dueOpt == 0) { dueOpt = 0 }
                    DueChip("امروز", dueOpt == 1) { dueOpt = 1 }
                    DueChip("فردا", dueOpt == 2) { dueOpt = 2 }
                    DueChip("هفته بعد", dueOpt == 3) { dueOpt = 3 }
                }
                Spacer(Modifier.height(12.dp))
                Text("اولویت:", fontSize = 12.sp, color = MutedGreenText)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DueChip("🔴 مهم", priority == Priority.HIGH) { priority = Priority.HIGH }
                    DueChip("🟡 عادی", priority == Priority.NORMAL) { priority = Priority.NORMAL }
                    DueChip("🟢 کم", priority == Priority.LOW) { priority = Priority.LOW }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onSave(title.trim(), dueMillis(dueOpt), priority)
            }) { Text("افزودن", color = Saffron, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun DueChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp),
        color = if (selected) Saffron else DeepGreenSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Saffron else LineGreen)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp, color = if (selected) Ink else PaperWhite)
    }
}

@Composable
private fun EmptyTasksState() {
    Column(Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(110.dp).clip(CircleShape).border(2.dp, Saffron.copy(alpha = .5f), CircleShape),
            contentAlignment = Alignment.Center) { Text("✅", fontSize = 46.sp) }
        Spacer(Modifier.height(20.dp))
        Text("لیست وظایفت خالی است", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
        Spacer(Modifier.height(8.dp))
        Text("با دکمه «وظیفه جدید» اولین کار را اضافه کن؛ هرچه سررسید نزدیک‌تر، رنگش قرمزتر! 🎯",
            fontSize = 13.sp, color = MutedGreenText, textAlign = TextAlign.Center)
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
                Text("چراغ راه", fontFamily = LalezarFont, fontSize = 38.sp, color = PaperWhite)
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
            placeholder = { Text("جستجو در یادداشت‌ها و وظایف…", color = MutedGreenText) },
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
        Text("دفترچه‌ات خالی است", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
        Spacer(Modifier.height(8.dp))
        Text("یادداشت بنویس، صدا ضبط کن، نقاشی بکش، چک‌لیست بساز و رویشان قفل بگذار ✨",
            fontSize = 13.sp, color = MutedGreenText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 15.sp, color = MutedGreenText)
    }
}
