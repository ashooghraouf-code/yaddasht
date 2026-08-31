package ir.yaddasht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.R
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.Priority
import ir.yaddasht.app.reminder.ReminderScheduler
import ir.yaddasht.app.util.FaDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class TaskWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, mgr, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> updateAll(context)
            ACTION_ADD_TASK -> {
                context.startActivity(Intent(context, MainActivity::class.java).apply {
                    putExtra("note_id", -1L)
                    putExtra("is_task", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            }
            ACTION_TOGGLE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId < 0) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.get(context.applicationContext)
                        db.taskDao().getTaskById(taskId)?.let { task ->
                            db.taskDao().update(task.copy(isCompleted = !task.isCompleted))
                            if (!task.isCompleted) ReminderScheduler.cancelAll(context, taskId, true)
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            updateAll(context)
                            pending.finish()
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "ir.yaddasht.app.REFRESH_TASKS"
        const val ACTION_ADD_TASK = "ir.yaddasht.app.ADD_TASK"
        const val ACTION_TOGGLE_TASK = "ir.yaddasht.app.TOGGLE_TASK"
        const val EXTRA_TASK_ID = "task_id"

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, TaskWidget::class.java))
            ids.forEach { updateAppWidget(context, mgr, it) }
        }

        fun updateAppWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.task_widget_layout)

            CoroutineScope(Dispatchers.IO).launch {
                val tasks = try {
                    AppDatabase.get(context.applicationContext).taskDao().getUpcomingTasksSync()
                } catch (e: Exception) { emptyList() }

                val slots = listOf(
                    Slot(R.id.task_item_1, R.id.task_check_1, R.id.task_title_1, R.id.task_due_1, R.id.task_priority_1),
                    Slot(R.id.task_item_2, R.id.task_check_2, R.id.task_title_2, R.id.task_due_2, R.id.task_priority_2),
                    Slot(R.id.task_item_3, R.id.task_check_3, R.id.task_title_3, R.id.task_due_3, R.id.task_priority_3)
                )

                slots.forEachIndexed { i, slot ->
                    if (i < tasks.size) {
                        val t = tasks[i]
                        views.setTextViewText(slot.checkId, if (t.isCompleted) "☑" else "☐")
                        views.setTextViewText(slot.titleId, t.title.ifBlank { "بدون عنوان" })
                        if (t.dueDate > 0) {
                            val (jy, jm, jd) = FaDate.jalali(t.dueDate)
                            val c = Calendar.getInstance().apply { timeInMillis = t.dueDate }
                            val h = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
                            val m = c.get(Calendar.MINUTE).toString().padStart(2, '0')
                            views.setTextViewText(slot.dueId, "${jd} ${FaDate.monthName(jm)} - $h:$m")
                        } else {
                            views.setTextViewText(slot.dueId, "")
                        }
                        val mark = if (t.isCompleted) "✓" else when (t.priority) {
                            Priority.HIGH -> "🔴"; Priority.NORMAL -> "🟡"; Priority.LOW -> "🟢"
                        }
                        views.setTextViewText(slot.priorityId, mark)

                        val toggle = Intent(context, TaskWidget::class.java).apply {
                            action = ACTION_TOGGLE_TASK
                            putExtra(EXTRA_TASK_ID, t.id)
                        }
                        views.setOnClickPendingIntent(slot.checkId, PendingIntent.getBroadcast(
                            context, (t.id + 5000).toInt(), toggle,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))

                        val open = Intent(context, MainActivity::class.java).apply {
                            putExtra("note_id", t.id)
                            putExtra("is_task", true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        views.setOnClickPendingIntent(slot.titleId, PendingIntent.getActivity(
                            context, (t.id + 6000).toInt(), open,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    } else {
                        views.setTextViewText(slot.checkId, "")
                        views.setTextViewText(slot.titleId, if (i == 0 && tasks.isEmpty()) "وظیفه‌ای نیست 🎯" else "")
                        views.setTextViewText(slot.dueId, "")
                        views.setTextViewText(slot.priorityId, "")
                        views.setOnClickPendingIntent(slot.checkId, null)
                        views.setOnClickPendingIntent(slot.titleId, null)
                    }
                }

                views.setOnClickPendingIntent(R.id.task_add, PendingIntent.getBroadcast(
                    context, 9101, Intent(context, TaskWidget::class.java).apply { action = ACTION_ADD_TASK },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

                views.setOnClickPendingIntent(R.id.task_refresh, PendingIntent.getBroadcast(
                    context, 9102, Intent(context, TaskWidget::class.java).apply { action = ACTION_REFRESH },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

                withContext(Dispatchers.Main) { mgr.updateAppWidget(id, views) }
            }
        }

        fun updateSingle(context: Context, widgetId: Int) {
            updateAppWidget(context, AppWidgetManager.getInstance(context), widgetId)
        }

        fun forceUpdate(context: Context) = updateAll(context)

        private data class Slot(val rootId: Int, val checkId: Int, val titleId: Int, val dueId: Int, val priorityId: Int)
    }
}
