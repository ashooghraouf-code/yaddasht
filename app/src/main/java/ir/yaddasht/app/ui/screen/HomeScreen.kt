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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import ir.yaddasht.app.ui.theme.Brick
import ir.yaddasht.app.ui.theme.DeepGreen
import ir.yaddasht.app.ui.theme.DeepGreenSoft
import ir.yaddasht.app.ui.theme.Ink
import ir.yaddasht.app.ui.theme.InkSoft
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.LineGreen
import ir.yaddasht.app.ui.theme.MutedGreenText
import ir.yaddasht.app.ui.theme.PaperWhite
import ir.yaddasht.app.ui.theme.Saffron
import ir.yaddasht.app.ui.theme.VazirFont
import ir.yaddasht.app.ui.theme.paperColor
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
    if (due <= 0L) return Color(0xFF888888)
    val now = System.currentTimeMillis()
    val days = (due - now).toFloat() / 86_400_000f
    val t = days.coerceIn(0f, 7f) / 7f
    val red = Color(0xFFE5484D)
    val amber = Color(0xFFF5A524)
    val green = Color(0xFF46A758)
    return if (t < .5f) lerp(red, amber, t / .5f) else lerp(amber, green, (t - .5f) / .5f)
}

@Composable
fun HomeScreen(
    dao: NoteDao,
    taskDao: TaskDao,
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenTask: (Long) -> Unit
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
    var newTaskOnDate by remember { mutableLongStateOf(0L) }

    val (tjy, tjm, tjd) = FaDate.jalali(System.currentTimeMillis())
    var calJy by remember { mutableIntStateOf(tjy) }
    var calJm by remember { mutableIntStateOf(tjm) }
    var calDay by remember { mutableIntStateOf(tjd) }

    LaunchedEffect(tab) {
        if (tab == 2) {
            val (jy, jm, jd) = FaDate.jalali(System.currentTimeMillis())
            calJy = jy; calJm = jm; calDay = jd
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val count = BackupManager.restore(context, dao, uri)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, if (count > 0) "$count یادداشت بازیابی شد ✅" else "فایل پشتیبان معتبر نبود ❌", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun doBackup() {
        scope.launch(Dispatchers.IO) {
            val allNotes = dao.allNotesSync()
            if (allNotes.isEmpty()) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "یادداشتی برای پشتیبان نیست", Toast.LENGTH_SHORT).show() }
                return@launch
            }
            val json = BackupManager.buildBackup(allNotes, dao.allAttachments().groupBy { it.noteId })
            val file = BackupManager.exportBackupFile(context, json)
            withContext(Dispatchers.Main) { shareBackupFile(context, file) }
        }
    }

    val filteredNotes = notes.filter { query.isBlank() || it.title.contains(query, true) || (!NoteLock.isLocked(it.body) && it.body.contains(query, true)) }
    val pinned = filteredNotes.filter { it.pinned }
    val others = filteredNotes.filterNot { it.pinned }
    val filteredTasks = tasks.filter { query.isBlank() || it.title.contains(query, true) }
    val memory = remember(notes) { pickMemory(notes) }

    val itemsByDay = remember(tasks, notes, calJy, calJm) {
        val list = mutableListOf<Triple<String, Long, Boolean>>()
        tasks.filter { it.dueDate > 0 }.forEach { list.add(Triple("task:${it.id}", it.dueDate, it.isCompleted)) }
        notes.filter { it.reminderAt > 0 }.forEach { list.add(Triple("note:${it.id}", it.reminderAt, false)) }
        list.groupBy { val (a, b, c) = FaDate.jalali(it.second); Triple(a, b, c) }
    }

    Scaffold(containerColor = DeepGreen,
        floatingActionButton = {
            when (tab) {
                0 -> NewNoteFab(onNewNote)
                else -> ExtendedFloatingActionButton(onClick = { showAddTask = true }, containerColor = Saffron, contentColor = Ink) {
                    Icon(Icons.Filled.Add, "جدید"); Spacer(Modifier.width(8.dp))
                    Text("وظیفه جدید", fontFamily = LalezarFont, fontSize = 17.sp)
                }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PaperDots()
            Column(Modifier.fillMaxSize()) {
                HomeHeader(count = notes.size, onStats = { showStats = true }, onBackup = { doBackup() }, onRestore = { restoreLauncher.launch(arrayOf("*/*")) })

                TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                    Tab(selected = tab == 0, onClick = { tab = 0 },
                        text = { Text("📝 یادداشت‌ها", fontFamily = LalezarFont, fontSize = 14.sp) },
                        selectedContentColor = Saffron, unselectedContentColor = MutedGreenText)
                    Tab(selected = tab == 1, onClick = { tab = 1 },
                        text = { Text("✅ وظایف", fontFamily = LalezarFont, fontSize = 14.sp) },
                        selectedContentColor = Saffron, unselectedContentColor = MutedGreenText)
                    Tab(selected = tab == 2, onClick = { tab = 2 },
                        text = { Text("📅 تقویم", fontFamily = LalezarFont, fontSize = 14.sp) },
                        selectedContentColor = Saffron, unselectedContentColor = MutedGreenText)
                }

                if (tab == 0 && !hideMemory && memory != null && query.isBlank()) {
                    Surface(onClick = { onOpenNote(memory.id) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp), color = Saffron.copy(alpha = .14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Saffron.copy(alpha = .4f))) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳", fontSize = 22.sp); Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("خاطره‌ای از گذشته", fontFamily = LalezarFont, fontSize = 14.sp, color = Saffron)
                                Text(memory.title.ifBlank { "بدون عنوان" }, fontSize = 12.sp, color = PaperWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { hideMemory = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Close, null, tint = MutedGreenText, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }

                if (tab != 2) SearchBox(query, { query = it }, Modifier.padding(horizontal = 20.dp))

                when (tab) {
                    0 -> when {
                        notes.isEmpty() -> EmptyState()
                        filteredNotes.isEmpty() -> CenterMessage("چیزی پیدا نشد 🔍")
                        else -> LazyVerticalGrid(columns = GridCells.Adaptive(168.dp),
                            contentPadding = PaddingValues(14.dp, 16.dp, 14.dp, 120.dp), modifier = Modifier.fillMaxSize()) {
                            if (pinned.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("📌 سنجاق‌شده") }
                                items(pinned, key = { "p${it.id}" }) { note ->
                                    NoteCard(note, countMap[note.id] ?: 0, { onOpenNote(note.id) }, { togglePin(scope, dao, note) }, { noteToDelete = note })
                                }
                            }
                            if (others.isNotEmpty()) {
                                if (pinned.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("🗒️ یادداشت‌ها") }
                                items(others, key = { "n${it.id}" }) { note ->
                                    NoteCard(note, countMap[note.id] ?: 0, { onOpenNote(note.id) }, { togglePin(scope, dao, note) }, { noteToDelete = note })
                                }
                            }
                        }
                    }
                    1 -> when {
                        tasks.isEmpty() -> EmptyTasksState()
                        filteredTasks.isEmpty() -> CenterMessage("وظیفه‌ای پیدا نشد 🔍")
                        else -> LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp), modifier = Modifier.fillMaxSize()) {
                            filteredTasks.forEach { task ->
                                item {
                                    TaskCard(task, onClick = { onOpenTask(task.id) },
                                        onToggle = { scope.launch(Dispatchers.IO) { taskDao.update(task.copy(isCompleted = !task.isCompleted)) } },
                                        onDelete = { scope.launch(Dispatchers.IO) { taskDao.deleteById(task.id) } })
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                    else -> {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (calJm > 1) calJm-- else { calJm = 12; calJy-- } }) { Icon(Icons.Filled.ChevronRight, "قبل", tint = Saffron) }
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${FaDate.monthName(calJm)} ${calJy.fa()}", fontFamily = LalezarFont, fontSize = 20.sp, color = PaperWhite)
                                    val infoMillis = dayMillis(calJy, calJm, calDay)
                                    Text("🌙 ${hijriFa(infoMillis)} • میلادی: ${gregorianFa(infoMillis)}", fontSize = 10.sp, color = MutedGreenText)
                                }
                                IconButton(onClick = { if (calJm < 12) calJm++ else { calJm = 1; calJy++ } }) { Icon(Icons.Filled.ChevronLeft, "بعد", tint = Saffron) }
                            }
                            Spacer(Modifier.height(6.dp))

                            // راهنما
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                LegendItem(Color(0xFFE5484D), "وظیفه نزدیک")
                                LegendItem(Color(0xFF46A758), "وظیفه دور")
                                LegendItem(Saffron, "یادآور یادداشت")
                            }
                            Spacer(Modifier.height(4.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                WeekDaysFa.forEach { w -> Text(w, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Saffron, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
                            }
                            Spacer(Modifier.height(4.dp))
                            val leading = firstDowIndex(calJy, calJm)
                            val len = FaDate.monthLength(calJy, calJm)
                            val cells = List(leading) { 0 } + (1..len).toList()
                            cells.chunked(7).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    row.forEach { d ->
                                        if (d == 0) Box(Modifier.weight(1f))
                                        else {
                                            val dayItems = itemsByDay[Triple(calJy, calJm, d)].orEmpty()
                                            val isToday = calJy == tjy && calJm == tjm && d == tjd
                                            val isSel = calDay == d
                                            Box(Modifier.weight(1f).height(72.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) DeepGreenSoft else Color.Transparent)
                                                .border(if (isToday) 1.5.dp else 0.dp, Saffron, RoundedCornerShape(12.dp))
                                                .combinedClickable(
                                                    onClick = { calDay = d },
                                                    onLongClick = {
                                                        calDay = d
                                                        val m = dayMillis(calJy, calJm, d)
                                                        newTaskOnDate = m
                                                    }
                                                )
                                                .padding(3.dp)) {
                                                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(d.fa(), fontSize = 12.sp, color = if (isToday) Saffron else PaperWhite,
                                                        fontWeight = if (isToday || isSel) FontWeight.Bold else FontWeight.Normal)
                                                    // نوارهای رنگی تفکیکی
                                                    val taskItems = dayItems.filter { it.first.startsWith("task:") }
                                                    val noteItems = dayItems.filter { it.first.startsWith("note:") }
                                                    taskItems.take(2).forEach { item ->
                                                        Box(Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp).clip(RoundedCornerShape(3.dp))
                                                            .background(taskTint(item.second, item.third)))
                                                    }
                                                    noteItems.take(2 - taskItems.size.coerceAtMost(2)).forEach { _ ->
                                                        Box(Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp).clip(RoundedCornerShape(3.dp))
                                                            .background(Saffron))
                                                    }
                                                    if (dayItems.size > 2) Text("+${(dayItems.size - 2).fa()}", fontSize = 8.sp, color = MutedGreenText)
                                                }
                                                // دکمه + کوچک گوشه
                                                if (dayItems.isNotEmpty() || isSel || isToday) {
                                                    Box(Modifier.align(Alignment.BottomEnd).size(18.dp)
                                                        .clip(CircleShape).background(Saffron)
                                                        .clickable {
                                                            calDay = d
                                                            newTaskOnDate = dayMillis(calJy, calJm, d)
                                                        }, contentAlignment = Alignment.Center) {
                                                        Text("+", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    repeat(7 - row.size) { Box(Modifier.weight(1f)) }
                                }
                                Spacer(Modifier.height(3.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("📍 ${calDay.fa()} ${FaDate.monthName(calJm)}", fontFamily = LalezarFont, fontSize = 16.sp, color = Saffron, modifier = Modifier.weight(1f))
                                Surface(onClick = { newTaskOnDate = dayMillis(calJy, calJm, calDay) },
                                    shape = RoundedCornerShape(10.dp), color = Saffron) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Add, null, tint = Ink, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("وظیفه جدید", fontSize = 11.sp, color = Ink, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            val selItems = itemsByDay[Triple(calJy, calJm, calDay)].orEmpty()
                            if (selItems.isEmpty()) Text("برای این روز یادآور یا وظیفه‌ای نیست 🌤️", fontSize = 12.sp, color = MutedGreenText)
                            selItems.forEach { item ->
                                if (item.first.startsWith("note:")) {
                                    val id = item.first.removePrefix("note:").toLongOrNull() ?: -1L
                                    val note = notes.find { it.id == id }
                                    if (note != null) {
                                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DeepGreenSoft)
                                            .border(1.5.dp, Saffron.copy(alpha = .75f), RoundedCornerShape(18.dp))
                                            .clickable { onOpenNote(note.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("📝", fontSize = 18.sp); Spacer(Modifier.width(8.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(note.title.ifBlank { "یادداشت" }, fontFamily = VazirFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaperWhite)
                                                Text("⏰ " + FaDate.full(note.reminderAt), fontSize = 11.sp, color = Saffron)
                                            }
                                        }
                                    }
                                } else {
                                    val id = item.first.removePrefix("task:").toLongOrNull() ?: -1L
                                    val task = tasks.find { it.id == id }
                                    if (task != null) {
                                        TaskCard(task, onClick = { onOpenTask(task.id) },
                                            onToggle = { scope.launch(Dispatchers.IO) { taskDao.update(task.copy(isCompleted = !task.isCompleted)) } },
                                            onDelete = { scope.launch(Dispatchers.IO) { taskDao.deleteById(task.id) } })
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            Spacer(Modifier.height(120.dp))
                        }
                    }
                }
            }
        }
    }

    if (showStats) StatsDialog(notes, counts.sumOf { it.count }) { showStats = false }

    if (showAddTask) AddTaskDialog(onDismiss = { showAddTask = false },
        onSave = { title, due, pr ->
            scope.launch(Dispatchers.IO) { taskDao.insert(Task(title = title, dueDate = due, priority = pr)) }
            showAddTask = false
            Toast.makeText(context, "وظیفه اضافه شد ✅", Toast.LENGTH_SHORT).show()
        })

    // ✅ دیالوگ افزودن وظیفه روی تاریخ مشخص (با کلیک طولانی یا دکمه +)
    if (newTaskOnDate > 0) {
        AddTaskDialog(
            initialDate = newTaskOnDate,
            onDismiss = { newTaskOnDate = 0L },
            onSave = { title, due, pr ->
                scope.launch(Dispatchers.IO) { taskDao.insert(Task(title = title, dueDate = due, priority = pr)) }
                newTaskOnDate = 0L
                Toast.makeText(context, "وظیفه برای ${FaDate.full(due)} اضافه شد ✅", Toast.LENGTH_SHORT).show()
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
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, color = MutedGreenText)
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    val tint = taskTint(task.dueDate, task.isCompleted)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DeepGreenSoft)
        .border(1.5.dp, tint.copy(alpha = .75f), RoundedCornerShape(18.dp)).clickable { onClick() }.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = tint, checkmarkColor = DeepGreen))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontFamily = VazirFont, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = if (task.isCompleted) MutedGreenText else PaperWhite,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                if (task.dueDate > 0) { Spacer(Modifier.height(2.dp)); Text("📅 " + FaDate.full(task.dueDate), fontSize = 11.sp, color = tint, fontWeight = FontWeight.Bold) }
            }
            if (task.priority == Priority.HIGH && !task.isCompleted) Text("🔴", fontSize = 12.sp)
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Close, "حذف", tint = Brick, modifier = Modifier.size(16.dp)) }
        }
    }
}

// ✅ دیالوگ بهبودیافته با قابلیت دریافت تاریخ اولیه
@Composable
private fun AddTaskDialog(initialDate: Long = 0L, onDismiss: () -> Unit, onSave: (String, Long, Priority) -> Unit) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableLongStateOf(initialDate) }
    var showCalendar by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("✅ وظیفه جدید", fontFamily = LalezarFont, fontSize = 20.sp) },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان وظیفه") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                // نمایش تاریخ انتخاب‌شده + دکمه تقویم
                Surface(onClick = { showCalendar = true },
                    shape = RoundedCornerShape(12.dp),
                    color = DeepGreenSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineGreen)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سررسید", fontSize = 10.sp, color = MutedGreenText)
                            Text(if (dueDate > 0) FaDate.full(dueDate) else "انتخاب نشده",
                                fontSize = 13.sp, color = if (dueDate > 0) Saffron else PaperWhite,
                                fontWeight = FontWeight.Bold)
                        }
                        if (dueDate > 0) {
                            IconButton(onClick = { dueDate = 0L }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, "حذف", tint = Brick, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("سرعتی:", fontSize = 12.sp, color = MutedGreenText)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickDateChip("امروز", onClick = {
                        dueDate = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
                    })
                    QuickDateChip("فردا", onClick = {
                        dueDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }.timeInMillis
                    })
                    QuickDateChip("هفته بعد", onClick = {
                        dueDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
                    })
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
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(title.trim(), dueDate, priority)
            }) { Text("افزودن", color = if (title.isNotBlank()) Saffron else Color.Gray, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })

    if (showCalendar) {
        ShamsiCalendarPickerDialog(
            onConfirm = { millis ->
                // ساعت پیش‌فرض ۲۱:۰۰ روی تاریخ انتخاب‌شده
                val finalTime = millis + 21L * 3600_000
                dueDate = finalTime
                showCalendar = false
            },
            onDismiss = { showCalendar = false })
    }
}

@Composable
private fun QuickDateChip(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = DeepGreenSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineGreen)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = PaperWhite)
    }
}

@Composable
private fun DueChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = if (selected) Saffron else DeepGreenSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Saffron else LineGreen)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = if (selected) Ink else PaperWhite)
    }
}

@Composable
private fun EmptyTasksState() {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(110.dp).clip(CircleShape).border(2.dp, Saffron.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) { Text("✅", fontSize = 46.sp) }
        Spacer(Modifier.height(20.dp))
        Text("لیست وظایفت خالی است", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
        Spacer(Modifier.height(8.dp))
        Text("با دکمه «وظیفه جدید» اولین کار را اضافه کن 🎯", fontSize = 13.sp, color = MutedGreenText, textAlign = TextAlign.Center)
    }
}

private fun pickMemory(notes: List<Note>): Note? {
    val candidates = notes.filterNot { NoteLock.isLocked(it.body) }
    val now = Calendar.getInstance()
    val sameDayPastYears = candidates.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
        c.get(Calendar.MONTH) == now.get(Calendar.MONTH) && c.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH) && c.get(Calendar.YEAR) < now.get(Calendar.YEAR)
    }
    val oldNotes = candidates.filter { System.currentTimeMillis() - it.createdAt > 45L * 24 * 3600 * 1000 }
    return sameDayPastYears.ifEmpty { oldNotes }.randomOrNull()
}

private fun togglePin(scope: CoroutineScope, dao: NoteDao, note: Note) {
    scope.launch(Dispatchers.IO) { dao.update(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis())) }
}

@Composable
private fun HomeHeader(count: Int, onStats: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Saffron), contentAlignment = Alignment.Center) { Text("ی", fontFamily = LalezarFont, fontSize = 30.sp, color = Ink) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("چراغ راه", fontFamily = LalezarFont, fontSize = 38.sp, color = PaperWhite)
                Text("بدون محدودیت • ${count.fa()} یادداشت • 🤝 تکان بده = جدید", fontSize = 10.sp, color = Saffron)
            }
            IconButton(onClick = onBackup) { Icon(Icons.Filled.FileUpload, "پشتیبان", tint = MutedGreenText) }
            IconButton(onClick = onRestore) { Icon(Icons.Filled.FileDownload, "بازیابی", tint = MutedGreenText) }
            IconButton(onClick = onStats) { Icon(Icons.Filled.BarChart, "آمار", tint = MutedGreenText) }
        }
        Spacer(Modifier.height(6.dp))
        Text(FaDate.full(System.currentTimeMillis()), fontSize = 13.sp, color = MutedGreenText, modifier = Modifier.padding(start = 2.dp))
    }
}

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
                if (longest != null && longest.body.length > 50) {
                    StatRow("بلندترین یادداشت", "${longest.body.length.fa()} حرف")
                }
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
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DeepGreenSoft).border(1.dp, LineGreen, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, null, tint = Saffron); Spacer(Modifier.width(10.dp))
        TextField(value = query, onValueChange = onQueryChange, placeholder = { Text("جستجو در یادداشت‌ها و وظایف…", color = MutedGreenText) },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            textStyle = androidx.compose.ui.text.TextStyle(color = PaperWhite, fontSize = 14.sp), singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PaperDots() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 30.dp.toPx()
        var y = step / 2
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) step / 2 else step
            while (x < size.width) {
                drawCircle(Saffron.copy(alpha = 0.07f), radius = 1.1.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                x += step
            }
            y += step
            row++
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
    Box(Modifier.padding(5.dp).fillMaxWidth().graphicsLayer { rotationZ = angle; shadowElevation = 9f; shape = RoundedCornerShape(18.dp); clip = true }) {
        Column(Modifier.clip(RoundedCornerShape(18.dp)).background(paperColor(note.color)).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text((if (note.pinned) "📌 " else "") + note.title.ifBlank { "بدون عنوان" }, fontFamily = LalezarFont, fontSize = 17.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Close, "حذف", tint = InkSoft.copy(alpha = .55f), modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onDelete).padding(5.dp))
            }
            Spacer(Modifier.height(6.dp))
            when {
                locked -> Text("🔒 محتوای محرمانه", fontSize = 12.sp, color = InkSoft)
                checklist -> {
                    val (done, total) = Checklist.progress(note.body)
                    Text("✅ ${done.fa()} از ${total.fa()} انجام شد", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(note.body.lines().take(3).joinToString("\n") { it.removePrefix("☐ ").removePrefix("☑ ") }, fontSize = 11.5.sp, color = InkSoft, maxLines = 3, lineHeight = 18.sp)
                }
                else -> if (note.body.isNotBlank()) Text(note.body, fontFamily = VazirFont, fontSize = 12.5.sp, color = InkSoft, maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
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
    Text(text, fontFamily = LalezarFont, fontSize = 16.sp, color = Saffron, modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp))
}

@Composable
private fun NewNoteFab(onNewNote: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onNewNote, containerColor = Saffron, contentColor = Ink) {
        Icon(Icons.Filled.Add, "جدید"); Spacer(Modifier.width(8.dp)); Text("یادداشت جدید", fontFamily = LalezarFont, fontSize = 17.sp)
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(110.dp).clip(CircleShape).border(2.dp, Saffron.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) { Text("✍️", fontSize = 46.sp) }
        Spacer(Modifier.height(20.dp))
        Text("دفترچه‌ات خالی است", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
        Spacer(Modifier.height(8.dp))
        Text("یادداشت بنویس، صدا ضبط کن، نقاشی بکش، چک‌لیست بساز و رویشان قفل بگذار ✨", fontSize = 13.sp, color = MutedGreenText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, fontSize = 15.sp, color = MutedGreenText) }
}
