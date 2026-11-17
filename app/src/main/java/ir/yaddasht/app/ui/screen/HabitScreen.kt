package ir.yaddasht.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.Habit
import ir.yaddasht.app.data.HabitLog
import ir.yaddasht.app.data.HabitSchedule
import ir.yaddasht.app.reminder.HabitReminderScheduler
import ir.yaddasht.app.ui.theme.DeepGreenSoft
import ir.yaddasht.app.ui.theme.Ink
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.LineGreen
import ir.yaddasht.app.ui.theme.MutedGreenText
import ir.yaddasht.app.ui.theme.PaperWhite
import ir.yaddasht.app.ui.theme.Saffron
import ir.yaddasht.app.ui.theme.VazirFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

// ✅ FINAL-SCREEN-v2
@Composable
fun HabitScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).habitDao() }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        // بازچینی آلارم‌ها هنگام باز شدن صفحه (force-stop / آپدیت / جامانده)
        try {
            HabitReminderScheduler.rescheduleAll(context)
        } catch (_: Exception) {
        }
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }

    val habits by dao.observeHabits().collectAsState(initial = emptyList())
    val logs by dao.observeAllLogs().collectAsState(initial = emptyList())
    val archived by dao.observeArchivedHabits().collectAsState(initial = emptyList())

    val logsByHabit = remember(logs) {
        logs.groupBy { it.habitId }
            .mapValues { entry -> entry.value.associateBy { it.dateKey } }
    }

    var showAdd by remember { mutableStateOf(false) }
    var editHabit by remember { mutableStateOf<Habit?>(null) }
    var archiveHabit by remember { mutableStateOf<Habit?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var dayEditHabit by remember { mutableStateOf<Habit?>(null) }

    val todayKey = HabitSchedule.dateKey(now)
    val nowMinute = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
        Calendar.getInstance().get(Calendar.MINUTE)

    val isDone: (Habit) -> Boolean = { habit ->
        HabitSchedule.isDone(habit, logsByHabit[habit.id]?.get(todayKey))
    }

    val todayScheduled = habits.filter { HabitSchedule.isScheduledOn(it, now) }
    val todayDone = todayScheduled.count(isDone)
    val progress = if (todayScheduled.isEmpty()) 0f else todayDone.toFloat() / todayScheduled.size

    val sortedHabits = remember(habits, now) {
        habits.sortedWith(
            compareBy(
                { HabitSchedule.reminderMinutes(it.pattern) == null },
                { HabitSchedule.reminderMinutes(it.pattern) ?: 0 }
            )
        )
    }

    val overdue = sortedHabits.firstOrNull { h ->
        val m = HabitSchedule.reminderMinutes(h.pattern)
        m != null && m < nowMinute && !isDone(h)
    }

    val upcoming = sortedHabits.firstOrNull { h ->
        val m = HabitSchedule.reminderMinutes(h.pattern)
        m != null && m >= nowMinute && !isDone(h)
    }

    val reminderLine = when {
        overdue != null -> "یادآور گذشته: ${overdue.title} • ${reminderLabel(HabitSchedule.reminderMinutes(overdue.pattern)!!)}"
        upcoming != null -> "نزدیک‌ترین یادآور: ${upcoming.title} • ${reminderLabel(HabitSchedule.reminderMinutes(upcoming.pattern)!!)}"
        else -> "یادآور فعالی برای امروز نداری"
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DeepGreenSoft)
                    .border(1.dp, LineGreen, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(fraction = progress, size = 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "امروز",
                            fontFamily = LalezarFont,
                            fontSize = 18.sp,
                            color = PaperWhite
                        )
                        Text(
                            "$todayDone از ${todayScheduled.size} عادت انجام شد",
                            fontSize = 13.sp,
                            color = MutedGreenText
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            reminderLine,
                            fontSize = 11.sp,
                            color = if (overdue != null) Color(0xFFFF8A80) else Saffron
                        )
                        if (archived.isNotEmpty()) {
                            TextButton(
                                onClick = { showArchived = true },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    "🗄 آرشیو (${archived.size})",
                                    fontSize = 11.sp,
                                    color = MutedGreenText
                                )
                            }
                        }
                    }
                }
            }

            if (habits.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "هنوز عادتی نداری\nبا دکمه پایین اولین را بساز",
                        color = MutedGreenText,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 110.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedHabits, key = { it.id }) { habit ->
                        HabitRow(
                            habit = habit,
                            byKey = logsByHabit[habit.id].orEmpty(),
                            now = now,
                            onToggle = { delta ->
                                scope.launch(Dispatchers.IO) {
                                    if (habit.kind == "BINARY") {
                                        dao.toggleBinary(habit.id, todayKey)
                                    } else {
                                        dao.addCount(habit.id, todayKey, delta)
                                    }
                                }
                            },
                            onRest = {
                                scope.launch(Dispatchers.IO) {
                                    dao.toggleRest(habit.id, todayKey)
                                }
                            },
                            onEdit = { editHabit = habit },
                            onArchive = { archiveHabit = habit },
                            onOpenDays = { dayEditHabit = habit }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            containerColor = Saffron,
            contentColor = Ink,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("عادت جدید", fontFamily = LalezarFont, fontSize = 15.sp)
        }
    }

    if (showAdd || editHabit != null) {
        AddHabitDialog(
            initial = editHabit,
            onDismiss = {
                showAdd = false
                editHabit = null
            },
            onSave = { h ->
                scope.launch(Dispatchers.IO) {
                    if (h.id == 0L) {
                        val id = dao.insertHabit(h)
                        HabitReminderScheduler.schedule(context, h.copy(id = id))
                    } else {
                        dao.updateHabit(h)
                        HabitReminderScheduler.schedule(context, h)
                    }
                }
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                showAdd = false
                editHabit = null
            }
        )
    }

    archiveHabit?.let { h ->
        AlertDialog(
            onDismissRequest = { archiveHabit = null },
            title = { Text("آرشیو عادت؟", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                Text("عادت از لیست فعال پنهان می‌شود و یادآورش لغو می‌گردد؛ رکوردها حفظ می‌مانند. از بخش «🗄 آرشیو» می‌توانی برگردانی یا کامل حذف کنی.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        archiveHabit = null
                        scope.launch(Dispatchers.IO) {
                            dao.archiveHabit(h.id)
                            HabitReminderScheduler.cancel(context, h.id)
                        }
                    }
                ) {
                    Text("آرشیو", color = Color(0xFFE5484D), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveHabit = null }) { Text("انصراف") }
            }
        )
    }

    if (showArchived) {
        AlertDialog(
            onDismissRequest = { showArchived = false },
            title = { Text("🗄 عادت‌های آرشیوشده", fontFamily = LalezarFont, fontSize = 18.sp) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (archived.isEmpty()) {
                        Text("چیزی در آرشیو نیست", fontSize = 12.sp, color = MutedGreenText)
                    }
                    archived.forEach { h ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(h.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                h.title,
                                fontSize = 13.sp,
                                color = PaperWhite,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        dao.unarchiveHabit(h.id)
                                        HabitReminderScheduler.schedule(context, h)
                                    }
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("بازگردانی", fontSize = 11.sp, color = Saffron)
                            }
                            TextButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        HabitReminderScheduler.cancel(context, h.id)
                                        dao.deleteHabit(h.id)
                                    }
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("حذف", fontSize = 11.sp, color = Color(0xFFE5484D))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchived = false }) { Text("بستن") }
            }
        )
    }

    dayEditHabit?.let { h ->
        DayEditorDialog(
            habit = h,
            byKey = logsByHabit[h.id].orEmpty(),
            now = now,
            onDismiss = { dayEditHabit = null },
            onSetCount = { key, count ->
                scope.launch(Dispatchers.IO) { dao.setCountForDate(h.id, key, count) }
            },
            onSetRest = { key, rest ->
                scope.launch(Dispatchers.IO) { dao.setRestForDate(h.id, key, rest) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitRow(
    habit: Habit,
    byKey: Map<String, HabitLog>,
    now: Long,
    onToggle: (Int) -> Unit,
    onRest: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onOpenDays: () -> Unit
) {
    val scheduled = HabitSchedule.isScheduledOn(habit, now)
    val color = HabitPalette.getOrElse(habit.colorIndex) { HabitPalette[0] }

    if (!scheduled) {
        UnscheduledHabitRow(habit, color, onEdit, onArchive)
        return
    }

    val todayKey = HabitSchedule.dateKey(now)
    val todayLog = byKey[todayKey]
    val done = HabitSchedule.isDone(habit, todayLog)
    val resting = todayLog?.rest == true
    val streak = HabitStreak.currentStreak(habit, byKey, now)
    val reminderMin = HabitSchedule.reminderMinutes(habit.pattern)
    val nowMinute = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
        Calendar.getInstance().get(Calendar.MINUTE)
    val overdue = reminderMin != null && reminderMin < nowMinute && !done && !resting

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DeepGreenSoft)
            .border(
                1.5.dp,
                when {
                    overdue -> Color(0xFFE5484D)
                    done -> color
                    else -> LineGreen
                },
                RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick = { onToggle(1) },
                onLongClick = onEdit
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(habit.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.title,
                    fontFamily = VazirFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PaperWhite
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "زنجیره: $streak",
                        fontSize = 11.sp,
                        color = Saffron,
                        fontWeight = FontWeight.Bold
                    )
                    if (reminderMin != null) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "⏰ ${reminderLabel(reminderMin)}",
                            fontSize = 11.sp,
                            color = if (overdue) Color(0xFFFF8A80) else MutedGreenText
                        )
                    }
                    if (habit.kind == "COUNT") {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${todayLog?.count ?: 0}/${habit.targetPerDay} ${habit.unit}",
                            fontSize = 11.sp,
                            color = MutedGreenText
                        )
                    }
                }
                if (overdue) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "از ساعت یادآور گذشته؛ اگر می‌توانی الان انجامش بده",
                        fontSize = 10.sp,
                        color = Color(0xFFFF8A80)
                    )
                }
                if (habit.cueText.isNotBlank()) {
                    Text(habit.cueText, fontSize = 10.sp, color = MutedGreenText)
                }
            }
            IconButton(onClick = onOpenDays, modifier = Modifier.size(30.dp)) {
                Text("📅", fontSize = 14.sp)
            }
            IconButton(onClick = onRest, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.Pause,
                    contentDescription = "استراحت",
                    tint = if (resting) Saffron else MutedGreenText,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onArchive, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "آرشیو",
                    tint = MutedGreenText,
                    modifier = Modifier.size(16.dp)
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (done || resting) color else Color.Transparent)
                    .border(2.dp, color, CircleShape)
                    .clickable { onToggle(1) },
                contentAlignment = Alignment.Center
            ) {
                when {
                    resting -> Text(
                        "\u23F8",
                        color = PaperWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    done -> Text(
                        "\u2713",
                        color = PaperWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (habit.kind == "COUNT" && !done) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountBtn("-") { onToggle(-1) }
                CountBtn("+") { onToggle(1) }
            }
        }
        Spacer(Modifier.height(10.dp))
        HeatStrip(habit, byKey, now, color)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnscheduledHabitRow(
    habit: Habit,
    color: Color,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DeepGreenSoft.copy(alpha = .45f))
            .border(1.dp, LineGreen.copy(alpha = .5f), RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onArchive
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(habit.emoji, fontSize = 22.sp, color = PaperWhite.copy(alpha = .55f))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.title,
                    fontFamily = VazirFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PaperWhite.copy(alpha = .65f)
                )
                Text("امروز نوبت این عادت نیست، لمس برای ویرایش", fontSize = 10.sp, color = MutedGreenText)
            }
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = .35f))
            )
        }
    }
}

@Composable
private fun DayEditorDialog(
    habit: Habit,
    byKey: Map<String, HabitLog>,
    now: Long,
    onDismiss: () -> Unit,
    onSetCount: (String, Int) -> Unit,
    onSetRest: (String, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📅 ویرایش روزها: ${habit.title}", fontFamily = LalezarFont, fontSize = 18.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (i in 0..13) {
                    val millis = HabitStreak.dayBeforeN(now, i)
                    val key = HabitSchedule.dateKey(millis)
                    val log = byKey[key]
                    val done = HabitSchedule.isDone(habit, log)
                    val rest = log?.rest == true
                    val label = when (i) {
                        0 -> "امروز"
                        1 -> "دیروز"
                        else -> "$i روز پیش"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            color = PaperWhite,
                            modifier = Modifier.weight(1f)
                        )
                        DayChip("✔", done) {
                            onSetCount(key, if (habit.kind == "COUNT") habit.targetPerDay else 1)
                        }
                        Spacer(Modifier.width(6.dp))
                        DayChip("⏸", rest) { onSetRest(key, !rest) }
                        Spacer(Modifier.width(6.dp))
                        DayChip("✖", false) { onSetCount(key, 0) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (selected) Saffron else DeepGreenSoft)
            .border(1.dp, if (selected) Saffron else LineGreen, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, color = if (selected) Ink else PaperWhite)
    }
}

@Composable
private fun CountBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(DeepGreenSoft)
            .border(1.dp, LineGreen, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = PaperWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeatStrip(
    habit: Habit,
    byKey: Map<String, HabitLog>,
    now: Long,
    color: Color
) {
    val todayStart = HabitStreak.dayBeforeN(now, 0)
    val reminderMin = HabitSchedule.reminderMinutes(habit.pattern)
    val nowMinute = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
        Calendar.getInstance().get(Calendar.MINUTE)

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 29 downTo 0) {
            val millis = HabitStreak.dayBeforeN(now, i)
            val sched = HabitSchedule.isScheduledOn(habit, millis)
            val log = byKey[HabitSchedule.dateKey(millis)]
            val done = HabitSchedule.isDone(habit, log)
            val rest = log?.rest == true

            val missed = sched && !done && !rest &&
                (millis < todayStart || (millis == todayStart && reminderMin != null && reminderMin < nowMinute))
            val pending = sched && !done && !rest && !missed

            val dot = when {
                !sched -> Color.White.copy(alpha = 0.06f)
                done -> color
                rest -> PaperWhite.copy(alpha = 0.35f)
                missed -> Color(0xFFE5484D).copy(alpha = 0.35f)
                pending -> LineGreen.copy(alpha = 0.6f)
                else -> Color.White.copy(alpha = 0.06f)
            }

            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
        }
    }
}

@Composable
private fun ProgressRing(fraction: Float, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = Saffron,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
        }
        Text(
            "${(fraction * 100).toInt()}%",
            color = PaperWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun reminderLabel(min: Int): String {
    if (min < 0) return ""
    return "${(min / 60).toString().padStart(2, '0')}:${(min % 60).toString().padStart(2, '0')}"
}

private object HabitStreak {
    fun dayBeforeN(millis: Long, n: Int): Long {
        var day = startOfDay(millis)
        repeat(n) { day = previousDay(day) }
        return day
    }

    fun currentStreak(
        habit: Habit,
        byKey: Map<String, HabitLog>,
        todayMillis: Long
    ): Int {
        val todayStart = startOfDay(todayMillis)
        val todayKey = HabitSchedule.dateKey(todayStart)
        val scheduledToday = HabitSchedule.isScheduledOn(habit, todayStart)
        val doneToday = HabitSchedule.isDone(habit, byKey[todayKey])
        var day = todayStart
        if (scheduledToday && !doneToday) day = previousDay(day)
        var streak = 0
        var guard = 0
        while (guard < 3650) {
            if (HabitSchedule.isScheduledOn(habit, day)) {
                val key = HabitSchedule.dateKey(day)
                val log = byKey[key]
                if (HabitSchedule.isDone(habit, log) || log?.rest == true) streak++ else break
            }
            day = previousDay(day)
            guard++
        }
        return streak
    }

    private fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun previousDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = startOfDay(millis)
        c.add(Calendar.DAY_OF_YEAR, -1)
        return startOfDay(c.timeInMillis)
    }
}
