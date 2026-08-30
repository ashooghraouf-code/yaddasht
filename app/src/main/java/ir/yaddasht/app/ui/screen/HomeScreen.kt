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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import ir.yaddasht.app.data.*
import ir.yaddasht.app.reminder.*
import ir.yaddasht.app.ui.theme.*
import ir.yaddasht.app.util.*
import kotlinx.coroutines.*
import java.io.File
import java.util.Calendar

private val HOLIDAY_RED = Color(0xFFE5484D)
private val WEEK_FA = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
private const val DAY_MS = 86_400_000L

private fun jalaliMillis(jy: Int, jm: Int, jd: Int, hour: Int = 12): Long {
    var est = 1617220800000L + (jy - 1400).toLong() * 365L * DAY_MS + (jm - 1).toLong() * 30L * DAY_MS + (jd - 1).toLong() * DAY_MS
    for (i in 0 until 700) {
        val (y, m, d) = FaDate.jalali(est)
        if (y == jy && m == jm && d == jd) {
            val c = Calendar.getInstance()
            c.timeInMillis = est
            c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
        val dir = when { y < jy -> 1; y > jy -> -1; m < jm -> 1; m > jm -> -1; d < jd -> 1; else -> -1 }
        est += dir * DAY_MS
    }
    return est
}

private fun leadingBlanks(jy: Int, jm: Int): Int {
    val c = Calendar.getInstance(); c.timeInMillis = jalaliMillis(jy, jm, 1, 12)
    return when (c.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> 0; Calendar.SUNDAY -> 1; Calendar.MONDAY -> 2; Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4; Calendar.THURSDAY -> 5; else -> 6
    }
}

private fun monthLen(jy: Int, jm: Int): Int {
    val (ny, nm) = if (jm == 12) jy + 1 to 1 else jy to jm + 1
    return ((jalaliMillis(ny, nm, 1, 12) - jalaliMillis(jy, jm, 1, 12)) / DAY_MS).toInt()
}

private fun iranHijri(millis: Long): Triple<Int, Int, Int> {
    val cal = android.icu.util.IslamicCalendar(); cal.timeInMillis = millis + DAY_MS
    return Triple(cal.get(android.icu.util.Calendar.MONTH) + 1, cal.get(android.icu.util.Calendar.DAY_OF_MONTH), cal.get(android.icu.util.Calendar.YEAR))
}

private fun fullDateTime(millis: Long): String {
    val (jy, jm, jd) = FaDate.jalali(millis)
    val c = Calendar.getInstance(); c.timeInMillis = millis
    val h = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val m = c.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "${jd.fa()} ${FaDate.monthName(jm)} – ساعت ${h.fa()}:${m.fa()}"
}

private fun taskTint(due: Long, completed: Boolean): Color {
    if (completed) return Color(0xFF5E8077)
    if (due <= 0L) return Color(0xFF888888)
    val now = System.currentTimeMillis()
    val days = (due - now).toFloat() / DAY_MS
    val t = days.coerceIn(0f, 7f) / 7f
    val red = Color(0xFFE5484D); val amber = Color(0xFFF5A524); val green = Color(0xFF46A758)
    return if (t < .5f) lerp(red, amber, t / .5f) else lerp(amber, green, (t - .5f) / .5f)
}

private fun gregorianFullFa(millis: Long): String {
    val c = Calendar.getInstance(); c.timeInMillis = millis
    val d = c.get(Calendar.DAY_OF_MONTH); val m = c.get(Calendar.MONTH) + 1; val y = c.get(Calendar.YEAR)
    val names = arrayOf("ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن", "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر")
    return "${d.fa()} ${names.getOrElse(m - 1) { "" }} ${y.fa()}"
}

private fun hijriFullFa(millis: Long): String {
    val (m, d, y) = iranHijri(millis)
    val names = arrayOf("محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه")
    return "${d.fa()} ${names.getOrElse(m - 1) { "" }} ${y.fa()}"
}

private fun isIranHoliday(jy: Int, jm: Int, jd: Int): Boolean {
    val millis = jalaliMillis(jy, jm, jd, 12)
    val c = Calendar.getInstance(); c.timeInMillis = millis
    if (c.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) return true
    when {
        jm == 1 && jd >= 1 && jd <= 4 -> return true
        jm == 1 && jd == 12 -> return true
        jm == 1 && jd == 13 -> return true
        jm == 3 && jd == 14 -> return true
        jm == 3 && jd == 15 -> return true
        jm == 11 && jd == 22 -> return true
    }
    val (hm, hd) = iranHijri(millis)
    if (hm == 2 && hd == 29 && iranHijri(millis + DAY_MS).first == 3) return true
    return when {
        hm == 1 && hd == 10 -> true; hm == 2 && hd == 20 -> true; hm == 2 && hd == 28 -> true; hm == 2 && hd == 30 -> true
        hm == 3 && hd == 17 -> true; hm == 7 && hd == 13 -> true; hm == 7 && hd == 27 -> true; hm == 8 && hd == 15 -> true
        hm == 9 && hd == 21 -> true; hm == 10 && hd == 1 -> true; hm == 10 && hd == 10 -> true; hm == 12 && hd == 18 -> true
        else -> false
    }
}

private fun computeHolidayDays(jy: Int, jm: Int): Set<Int> {
    val set = mutableSetOf<Int>()
    for (d in 1..monthLen(jy, jm)) if (isIranHoliday(jy, jm, d)) set.add(d)
    return set
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(dao: NoteDao, taskDao: TaskDao, onOpenNote: (Long) -> Unit, onNewNote: () -> Unit, onOpenTask: (Long) -> Unit) {
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
    var editTask by remember { mutableStateOf<Task?>(null) }
    var newTaskOnDate by remember { mutableLongStateOf(0L) }

    val (tjy, tjm, tjd) = FaDate.jalali(System.currentTimeMillis())
    var calJy by remember { mutableIntStateOf(tjy) }
    var calJm by remember { mutableIntStateOf(tjm) }
    var calDay by remember { mutableIntStateOf(tjd) }

    LaunchedEffect(tab) { if (tab == 2) { val (jy, jm, jd) = FaDate.jalali(System.currentTimeMillis()); calJy = jy; calJm = jm; calDay = jd } }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val (n, t) = FullBackup.importAll(context, dao, taskDao, uri)
            withContext(Dispatchers.Main) { Toast.makeText(context, if (n + t > 0) "$n یادداشت و $t وظیفه بازیابی شد ✅" else "فایل پشتیبان معتبر نبود ❌", Toast.LENGTH_LONG).show() }
        }
    }

    fun doBackup() { scope.launch(Dispatchers.IO) { val file = FullBackup.exportAll(context, dao, taskDao); withContext(Dispatchers.Main) { shareBackupFile(context, file) } } }

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
                1 -> ExtendedFloatingActionButton(onClick = { newTaskOnDate = 0L; showAddTask = true }, containerColor = Saffron, contentColor = Ink) {
                    Icon(Icons.Filled.Add, "جدید"); Spacer(Modifier.width(8.dp)); Text("وظیفه جدید", fontFamily = LalezarFont, fontSize = 17.sp)
                }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PaperDots()
            Column(Modifier.fillMaxSize()) {
                HomeHeader(count = notes.size, onStats = { showStats = true }, onBackup = { doBackup() }, onRestore = { restoreLauncher.launch(arrayOf("*/*")) })
                TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("📝 یادداشت‌ها", fontFamily = LalezarFont, fontSize = 14.sp) }, selectedContentColor = Saffron, unselectedContentColor = MutedGreenText)
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("✅ وظایف", fontFamily = LalezarFont, fontSize = 14.sp) }, selectedContentColor = Saffron, unselectedContentColor = MutedGreenText)
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("📅 تقویم", fontFamily = LalezarFont, fontSize = 14.sp) }, selectedContentColor = Saffron, unselectedContentColor = MutedGreenText)
                }
                if (tab == 0 && !hideMemory && memory != null && query.isBlank()) {
                    Surface(onClick = { onOpenNote(memory.id) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp), color = Saffron.copy(alpha = .14f), border = androidx.compose.foundation.BorderStroke(1.dp, Saffron.copy(alpha = .4f))) {
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
                        else -> LazyVerticalGrid(columns = GridCells.Adaptive(168.dp), contentPadding = PaddingValues(14.dp, 16.dp, 14.dp, 120.dp), modifier = Modifier.fillMaxSize()) {
                            if (pinned.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("📌 سنجاق‌شده") }
                                items(pinned, key = { "p${it.id}" }) { note -> NoteCard(note, countMap[note.id] ?: 0, { onOpenNote(note.id) }, { togglePin(scope, dao, note) }, { noteToDelete = note }) }
                            }
                            if (others.isNotEmpty()) {
                                if (pinned.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("🗒️ یادداشت‌ها") }
                                items(others, key = { "n${it.id}" }) { note -> NoteCard(note, countMap[note.id] ?: 0, { onOpenNote(note.id) }, { togglePin(scope, dao, note) }, { noteToDelete = note }) }
                            }
                        }
                    }
                    1 -> when {
                        tasks.isEmpty() -> EmptyTasksState()
                        filteredTasks.isEmpty() -> CenterMessage("وظیفه‌ای پیدا نشد 🔍")
                        else -> LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp), modifier = Modifier.fillMaxSize()) {
                            filteredTasks.forEach { task ->
                                item {
                                    TaskCard(task,
                                        onClick = { editTask =
