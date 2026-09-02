package ir.yaddasht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.R
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.util.FaDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class TaskWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.task_widget_layout)
                
                val bgColor = WidgetPreferences.getTaskColor(context)
                try { views.setInt(R.id.task_widget_root, "setBackgroundColor", bgColor) } catch (_: Exception) {}

                // Fetch today's tasks (dueDate = today or earlier, and not completed)
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
                
                val tasks = AppDatabase.get(context).taskDao().getRecentTasks()
                    .filter { it.dueDate <= today && !it.isCompleted }
                    .take(3)
                
                if (tasks.isNotEmpty()) {
                    val t1 = tasks.getOrNull(0)
                    if (t1 != null) {
                        views.setTextViewText(R.id.task_title_1, t1.title)
                        views.setTextViewText(R.id.task_check_1, if (t1.isCompleted) "☑" else "☐")
                        
                        // PendingIntent for toggling completion
                        val toggleIntent = Intent(context, TaskWidgetReceiver::class.java).apply {
                            action = "TOGGLE_TASK"
                            putExtra("task_id", t1.id)
                            putExtra("is_completed", !t1.isCompleted)
                        }
                        val togglePending = PendingIntent.getBroadcast(context, t1.id.toInt(), toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.task_check_1, togglePending)
                        
                        // PendingIntent to open the task in the app
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            putExtra("open_task", t1.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val openPending = PendingIntent.getActivity(context, t1.id.toInt() + 1000, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.task_item_1, openPending)
                        
                        views.setViewVisibility(R.id.task_item_1, View.VISIBLE)
                    }

                    val t2 = tasks.getOrNull(1)
                    if (t2 != null) {
                        views.setTextViewText(R.id.task_title_2, t2.title)
                        views.setTextViewText(R.id.task_check_2, if (t2.isCompleted) "☑" else "☐")
                        
                        val toggleIntent = Intent(context, TaskWidgetReceiver::class.java).apply {
                            action = "TOGGLE_TASK"
                            putExtra("task_id", t2.id)
                            putExtra("is_completed", !t2.isCompleted)
                        }
                        val togglePending = PendingIntent.getBroadcast(context, t2.id.toInt(), toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.task_check_2, togglePending)
                        
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            putExtra("open_task", t2.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val openPending = PendingIntent.getActivity(context, t2.id.toInt() + 1000, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.task_item_2, openPending)
                        
                        views.setViewVisibility(R.id.task_item_2, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.task_item_2, View.GONE)
                    }

                    val t3 = tasks.getOrNull(2)
                    if (t3 != null) {
                        views.setTextViewText(R.id.task_title_3, t3.title)
                        views.setTextViewText(R.id.task_check_3, if (t3.isCompleted) "☑" else "☐")
                        
                        val toggleIntent = Intent(context, TaskWidgetReceiver::class.java).apply {
                            action = "TOGGLE_TASK"
                            putExtra("task_id", t3.id)
                            putExtra("is_completed", !t3.isCompleted)
                        }
                        val togglePending = PendingIntent.getBroadcast(context, t3.id.toInt(), toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.task_check_3, togglePending)
                        
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            putExtra("open_task", t3.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val openPending = PendingIntent.getActivity(context, t3.id.toInt() + 1000, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.task_item_3, openPending)
                        
                        views.setViewVisibility(R.id.task_item_3, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.task_item_3, View.GONE)
                    }
                } else {
                    views.setTextViewText(R.id.task_title_1, "No tasks for today")
                    views.setViewVisibility(R.id.task_check_1, View.GONE)
                    views.setViewVisibility(R.id.task_item_2, View.GONE)
                    views.setViewVisibility(R.id.task_item_3, View.GONE)
                }

                val intentMain = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingMain = PendingIntent.getActivity(context, 0, intentMain, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                
                val intentAdd = Intent(context, MainActivity::class.java).apply {
                    putExtra("open_new_task", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingAdd = PendingIntent.getActivity(context, 1, intentAdd, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                try {
                    views.setOnClickPendingIntent(R.id.task_widget_root, pendingMain)
                    views.setOnClickPendingIntent(R.id.task_refresh, pendingMain)
                    views.setOnClickPendingIntent(R.id.task_add, pendingAdd)
                } catch (_: Exception) {}

                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        fun forceUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TaskWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
