package ir.yaddasht.app.ui.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.Habit
import ir.yaddasht.app.data.HabitLog
import ir.yaddasht.app.ui.theme.DeepGreenSoft
import ir.yaddasht.app.ui.theme.Ink
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.LineGreen
import ir.yaddasht.app.ui.theme.MutedGreenText
import ir.yaddasht.app.ui.theme.PaperWhite
import ir.yaddasht.app.ui.theme.Saffron
import ir.yaddasht.app.ui.theme.VazirFont
import ir.yaddasht.app.util.HabitStreak
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HabitScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).habitDao() }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }

    val habits by dao.observeHabits().collectAsState(initial = emptyList())
    val logs by dao.observeAllLogs().collectAsState(initial = emptyList())

    val logsByHabit = remember(logs) {
        logs.groupBy { it.habitId }
            .mapValues { entry -> entry.value.associateBy { it.dateKey } }
    }

    var showAdd by remember { mutableStateOf(false) }
    var editHabit by remember { mutableStateOf<Habit?>(null) }
    var archiveHabit by remember { mutableStateOf<Habit?>(null) }

    val todayKey = HabitStreak.dateKey(now)
    val todayScheduled = habits.filter { HabitStreak.isScheduled(it, now) }
    val todayDone = todayScheduled.count {
        HabitStreak.isDone(it, logsByHabit[it.id]?.get(todayKey))
    }

    val progress = if (todayScheduled.isEmpty()) {
        0f
    } else {
        todayDone.toFloat() / todayScheduled.size
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
                    ProgressRing(
                        fraction = progress,
                        size = 64.dp
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "امروز",
                            fontFamily = LalezarFont,
                            fontSize = 18.sp,
                            color = PaperWhite
                        )

                        Text(
                            todayDone.toString() + " از " + todayScheduled.size.toString() + " عادت انجام شد",
                            fontSize = 13.sp,
                            color = MutedGreenText
                        )
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
                    items(habits, key = { it.id }) { habit ->
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
                            onEdit = {
                                editHabit = habit
                            },
                            onArchive = {
                                archiveHabit = habit
                            }
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
            Icon(
                Icons.Filled.Add,
                contentDescription = null
            )

            Spacer(Modifier.width(6.dp))

            Text(
                "عادت جدید",
                fontFamily = LalezarFont,
                fontSize = 15.sp
            )
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
                        dao.insertHabit(h)
                    } else {
                        dao.updateHabit(h)
                    }
                }

                showAdd = false
                editHabit = null
            }
        )
    }

    archiveHabit?.let { h ->
        AlertDialog(
            onDismissRequest = {
                archiveHabit = null
            },
            title = {
                Text(
                    "آرشیو عادت?",
                    fontFamily = LalezarFont,
                    fontSize = 20.sp
                )
            },
            text = {
                Text("عادت از لیست فعال پنهان می شود، رکوردها حفظ می مانند.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        archiveHabit = null

                        scope.launch(Dispatchers.IO) {
                            dao.archiveHabit(h.id)
                        }
                    }
                ) {
                    Text(
                        "آرشیو",
                        color = Color(0xFFE5484D),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        archiveHabit = null
                    }
                ) {
                    Text("انصراف")
                }
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
    onArchive: () -> Unit
) {
    val scheduled = HabitStreak.isScheduled(habit, now)
    val color = HabitPalette.getOrElse(habit.colorIndex) { HabitPalette[0] }

    if (!scheduled) {
        UnscheduledHabitRow(
            habit = habit,
            color = color,
            onEdit = onEdit,
            onArchive = onArchive
        )
        return
    }

    val todayKey = HabitStreak.dateKey(now)
    val todayLog = byKey[todayKey]
    val done = HabitStreak.isDone(habit, todayLog)
    val resting = todayLog?.rest == true
    val streak = HabitStreak.currentStreak(habit, byKey, now)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DeepGreenSoft)
            .border(
                1.5.dp,
                if (done) color else LineGreen,
                RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick = { onToggle(1) },
                onLongClick = onEdit
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                habit.emoji,
                fontSize = 24.sp
            )

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
                        "زنجیره: " + streak.toString(),
                        fontSize = 11.sp,
                        color = Saffron,
                        fontWeight = FontWeight.Bold
                    )

                    if (habit.kind == "COUNT") {
                        Spacer(Modifier.width(10.dp))

                        val currentCount = todayLog?.count ?: 0

                        Text(
                            currentCount.toString() + "/" + habit.targetPerDay.toString() + " " + habit.unit,
                            fontSize = 11.sp,
                            color = MutedGreenText
                        )
                    }
                }

                if (habit.cueText.isNotBlank()) {
                    Text(
                        habit.cueText,
                        fontSize = 10.sp,
                        color = MutedGreenText
                    )
                }
            }

            IconButton(
                onClick = onRest,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Filled.Pause,
                    contentDescription = "rest",
                    tint = if (resting) Saffron else MutedGreenText,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onArchive,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "archive",
                    tint = MutedGreenText,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (done || resting) {
                            color
                        } else {
                            Color.Transparent
                        }
                    )
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
                CountBtn("-") {
                    onToggle(-1)
                }

                CountBtn("+") {
                    onToggle(1)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        HeatStrip(
            habit = habit,
            byKey = byKey,
            now = now,
            color = color
        )
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
            .border(
                1.dp,
                LineGreen.copy(alpha = .5f),
                RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onArchive
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                habit.emoji,
                fontSize = 22.sp,
                color = PaperWhite.copy(alpha = .55f)
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    habit.title,
                    fontFamily = VazirFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PaperWhite.copy(alpha = .65f)
                )

                Text(
                    "امروز نوبت این عادت نیست، لمس برای ویرایش",
                    fontSize = 10.sp,
                    color = MutedGreenText
                )
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
private fun CountBtn(
    label: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(DeepGreenSoft)
            .border(1.dp, LineGreen, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = PaperWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeatStrip(
    habit: Habit,
    byKey: Map<String, HabitLog>,
    now: Long,
    color: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 29 downTo 0) {
            val millis = HabitStreak.dayBeforeN(now, i)
            val scheduled = HabitStreak.isScheduled(habit, millis)
            val key = HabitStreak.dateKey(millis)
            val done = HabitStreak.isDone(habit, byKey[key])

            val dotColor = when {
                !scheduled -> Color.White.copy(alpha = 0.06f)
                done -> color
                else -> Color(0xFFE5484D).copy(alpha = 0.35f)
            }

            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun ProgressRing(
    fraction: Float,
    size: Dp
) {
    Box(
        Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val arcSize = Size(
                this.size.width - stroke,
                this.size.height - stroke
            )
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

        val percent = (fraction * 100).toInt()

        Text(
            percent.toString() + "%",
            color = PaperWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
