package ir.yaddasht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.R
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, mgr, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle?) {
        updateAppWidget(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId < 0) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.get(context.applicationContext)
                        val task = db.taskDao().getTaskById(taskId)
                        if (task != null) {
                            db.taskDao().update(task.copy(isCompleted = !task.isCompleted))
                            if (!task.isCompleted) ReminderScheduler.cancelAll(context, taskId, true)
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            forceUpdate(context)
                            pending.finish()
                        }
                    }
                }
            }
            ACTION_REFRESH -> forceUpdate(context)
            ACTION_ADD_TASK -> {
                val addIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("note_id", -1L)
                    putExtra("is_task", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(addIntent)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TASK = "ir.yaddasht.app.TOGGLE_TASK"
        const val ACTION_REFRESH = "ir.yaddasht.app.REFRESH_TASKS"
        const val ACTION_ADD_TASK = "ir.yaddasht.app.ADD_TASK"
        const val EXTRA_TASK_ID = "task_id"

        fun updateAppWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.task_widget_layout)

            val svc = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.task_list, svc)
            views.setEmptyView(R.id.task_list, R.id.task_empty)

            val clickIntent = Intent(context, TaskWidget::class.java).apply {
                action = ACTION_TOGGLE_TASK
            }
            val clickPending = PendingIntent.getBroadcast(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.task_list, clickPending)

            val addIntent = Intent(context, TaskWidget::class.java).apply {
                action = ACTION_ADD_TASK
            }
            val addPi = PendingIntent.getBroadcast(
                context, 2001, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.task_add, addPi)

            val refreshIntent = Intent(context, TaskWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPi = PendingIntent.getBroadcast(
                context, 2002, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.task_refresh, refreshPi)

            mgr.notifyAppWidgetViewDataChanged(id, R.id.task_list)
            mgr.updateAppWidget(id, views)
        }

        fun forceUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, TaskWidget::class.java))
            ids.forEach { updateAppWidget(context, mgr, it) }
        }
    }
}
