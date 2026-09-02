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
                
                // ۱. اعمال رنگ مستقل وظایف
                val bgColor = WidgetPreferences.getTaskColor(context)
                try { views.setInt(R.id.task_widget_root, "setBackgroundColor", bgColor) } catch (_: Exception) {}

                // ۲. خواندن از دیتابیس
                val tasks = AppDatabase.get(context).taskDao().getRecentTasks()
                
                if (tasks.isNotEmpty()) {
                    val t1 = tasks.getOrNull(0)
                    views.setTextViewText(R.id.task_title_1, t1?.title ?: "بدون عنوان")
                    val date1 = if (t1?.dueDate ?: 0L > 0) FaDate.jalali(t1.dueDate).let { "${it.third}/${it.second}/${it.first}" } else "بدون سررسید"
                    views.setTextViewText(R.id.task_due_1, "📅 $date1")
                    views.setTextViewText(R.id.task_check_1, if (t1?.isCompleted == true) "☑" else "☐")

                    val t2 = tasks.getOrNull(1)
                    if (t2 != null) {
                        views.setTextViewText(R.id.task_title_2, t2.title)
                        val date2 = if (t2.dueDate > 0) FaDate.jalali(t2.dueDate).let { "${it.third}/${it.second}/${it.first}" } else "بدون سررسید"
                        views.setTextViewText(R.id.task_due_2, "📅 $date2")
                        views.setTextViewText(R.id.task_check_2, if (t2.isCompleted) "☑" else "☐")
                        views.setViewVisibility(R.id.task_item_2, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.task_item_2, View.GONE)
                    }

                    val t3 = tasks.getOrNull(2)
                    if (t3 != null) {
                        views.setTextViewText(R.id.task_title_3, t3.title)
                        val date3 = if (t3.dueDate > 0) FaDate.jalali(t3.dueDate).let { "${it.third}/${it.second}/${it.first}" } else "بدون سررسید"
                        views.setTextViewText(R.id.task_due_3, "📅 $date3")
                        views.setTextViewText(R.id.task_check_3, if (t3.isCompleted) "☑" else "☐")
                        views.setViewVisibility(R.id.task_item_3, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.task_item_3, View.GONE)
                    }
                }

                // ۳. تنظیم کلیک‌ها
                val intentMain = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
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
