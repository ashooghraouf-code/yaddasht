package ir.yaddasht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.R
import ir.yaddasht.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

                // ✅ رنگ پس‌زمینه از SharedPreferences
                val bgColor = WidgetPreferences.getTaskColor(context)
                try { views.setInt(R.id.task_widget_root, "setBackgroundColor", bgColor) } catch (_: Exception) {}

                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                // ✅ همهٔ وظایف امروز (انجام‌شده + نشده) برای دیدن بیلان کار
                val allTasks = AppDatabase.get(context).taskDao().getRecentTasks()
                    .filter { it.dueDate <= today }
                    .take(3)

                val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

                if (allTasks.isNotEmpty()) {

                    // ─── آیتم ۱ ───
                    val t1 = allTasks.getOrNull(0)
                    if (t1 != null) {
                        // ✅ خط‌خوردگی برای انجام‌شده
                        val title1 = if (t1.isCompleted) {
                            SpannableString(t1.title).also { it.setSpan(StrikethroughSpan(), 0, it.length, 0) }
                        } else {
                            SpannableString(t1.title)
                        }
                        views.setTextViewText(R.id.task_title_1, title1)
                        views.setTextViewText(R.id.task_check_1, if (t1.isCompleted) "☑" else "☐")

                        // ✅ رنگ متن: خاکستری تیره برای انجام‌شده، مشکی برای نشده
                        val titleColor1 = if (t1.isCompleted) 0xFF546E7A.toInt() else 0xFF002226.toInt()
                        views.setTextColor(R.id.task_title_1, titleColor1)

                        // ✅ ساعت وظیفه
                        val time1 = if (t1.dueDate > 0) "⏰ " + timeFormat.format(Date(t1.dueDate)) else ""
                        views.setTextViewText(R.id.task_time_1, time1)
                        val timeColor1 = if (t1.isCompleted) 0xFF90A4AE.toInt() else 0xFF005549.toInt()
                        views.setTextColor(R.id.task_time_1, timeColor1)

                        // ✅ نقطه رنگی اهمیت
                        val priorityColor1 = when {
                            t1.isCompleted -> 0xFFBDBDBD.toInt()
                            t1.dueDate > 0 && t1.dueDate - System.currentTimeMillis() < 3600000 -> 0xFFFF1744.toInt()
                            t1.dueDate > 0 && t1.dueDate - System.currentTimeMillis() < 86400000 -> 0xFFFF9100.toInt()
                            t1.dueDate > 0 -> 0xFF00C853.toInt()
                            else -> 0xFFBDBDBD.toInt()
                        }
                        views.setTextColor(R.id.task_priority_1, priorityColor1)

                        // PendingIntent تیک زدن
                        val toggleIntent1 = Intent(context, TaskWidgetReceiver::class.java).apply {
                            action = "TOGGLE_TASK"
                            putExtra("task_id", t1.id)
                        }
                        val togglePending1 = PendingIntent.getBroadcast(
                            context, t1.id.toInt(), toggleIntent1,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.task_check_1, togglePending1)

                        // PendingIntent باز کردن در اپ
                        val openIntent1 = Intent(context, MainActivity::class.java).apply {
                            putExtra("open_task", t1.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val openPending1 = PendingIntent.getActivity(
                            context, t1.id.toInt() + 1000, openIntent1,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.task_item_1, openPending1)

                        views.setViewVisibility(R.id.task_item_1, View.VISIBLE)
                    }

                    // ─── آیتم ۲ ───
                    val t2 = allTasks.getOrNull(1)
                    if (t2 != null) {
                        val title2 = if (t2.isCompleted) {
                            SpannableString(t2.title).also { it.setSpan(StrikethroughSpan(), 0, it.length, 0) }
                        } else {
                            SpannableString(t2.title)
                        }
                        views.setTextViewText(R.id.task_title_2, title2)
                        views.setTextViewText(R.id.task_check_2, if (t2.isCompleted) "☑" else "☐")

                        val titleColor2 = if (t2.isCompleted) 0xFF546E7A.toInt() else 0xFF002226.toInt()
                        views.setTextColor(R.id.task_title_2, titleColor2)

                        val time2 = if (t2.dueDate > 0) "⏰ " + timeFormat.format(Date(t2.dueDate)) else ""
                        views.setTextViewText(R.id.task_time_2, time2)
                        val timeColor2 = if (t2.isCompleted) 0xFF90A4AE.toInt() else 0xFF005549.toInt()
                        views.setTextColor(R.id.task_time_2, timeColor2)

                        val priorityColor2 = when {
                            t2.isCompleted -> 0xFFBDBDBD.toInt()
                            t2.dueDate > 0 && t2.dueDate - System.currentTimeMillis() < 3600000 -> 0xFFFF1744.toInt()
                            t2.dueDate > 0 && t2.dueDate - System.currentTimeMillis() < 86400000 -> 0xFFFF9100.toInt()
                            t2.dueDate > 0 -> 0xFF00C853.toInt()
                            else -> 0xFFBDBDBD.toInt()
                        }
                        views.setTextColor(R.id.task_priority_2, priorityColor2)

                        val toggleIntent2 = Intent(context, TaskWidgetReceiver::class.java).apply {
                            action = "TOGGLE_TASK"
                            putExtra("task_id", t2.id)
                        }
                        val togglePending2 = PendingIntent.getBroadcast(
                            context, t2.id.toInt(), toggleIntent2,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.task_check_2, togglePending2)

                        val openIntent2 = Intent(context, MainActivity::class.java).apply {
                            putExtra("open_task", t2.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val openPending2 = PendingIntent.getActivity(
                            context, t2.id.toInt() + 1000, openIntent2,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.task_item_2, openPending2)

                        views.setViewVisibility(R.id.task_item_2, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.task_item_2, View.GONE)
                    }

                    // ─── آیتم ۳ ───
                    val t3 = allTasks.getOrNull(2)
                    if (t3 != null) {
                        val title3 = if (t3.isCompleted) {
                            SpannableString(t3.title).also { it.setSpan(StrikethroughSpan(), 0, it.length, 0) }
                        } else {
                            SpannableString(t3.title)
                        }
                        views.setTextViewText(R.id.task_title_3, title3)
                        views.setTextViewText(R.id.task_check_3, if (t3.isCompleted) "☑" else "☐")

                        val titleColor3 = if (t3.isCompleted) 0xFF546E7A.toInt() else 0xFF002226.toInt()
                        views.setTextColor(R.id.task_title_3, titleColor3)

                        val time3 = if (t3.dueDate > 0) "⏰ " + timeFormat.format(Date(t3.dueDate)) else ""
                        views.setTextViewText(R.id.task_time_3, time3)
                        val timeColor3 = if (t3.isCompleted) 0xFF90A4AE.toInt() else 0xFF005549.toInt()
                        views.setTextColor(R.id.task_time_3, timeColor3)

                        val priorityColor3 = when {
                            t3.isCompleted -> 0xFFBDBDBD.toInt()
                            t3.dueDate > 0 && t3.dueDate - System.currentTimeMillis() < 3600000 -> 0xFFFF1744.toInt()
                            t3.dueDate > 0 && t3.dueDate - System.currentTimeMillis() < 86400000 -> 0xFFFF9100.toInt()
                            t3.dueDate > 0 -> 0xFF00C853.toInt()
                            else -> 0xFFBDBDBD.toInt()
                        }
                        views.setTextColor(R.id.task_priority_3, priorityColor3)

                        val toggleIntent3 = Intent(context, TaskWidgetReceiver::class.java).apply {
                            action = "TOGGLE_TASK"
                            putExtra("task_id", t3.id)
                        }
                        val togglePending3 = PendingIntent.getBroadcast(
                            context, t3.id.toInt(), toggleIntent3,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.task_check_3, togglePending3)

                        val openIntent3 = Intent(context, MainActivity::class.java).apply {
                            putExtra("open_task", t3.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val openPending3 = PendingIntent.getActivity(
                            context, t3.id.toInt() + 1000, openIntent3,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.task_item_3, openPending3)

                        views.setViewVisibility(R.id.task_item_3, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.task_item_3, View.GONE)
                    }

                    // ✅ شمارندهٔ بیلان کار
                    val doneCount = allTasks.count { it.isCompleted }
                    val totalCount = allTasks.size
                    views.setTextViewText(R.id.task_widget_title, "✅ وظایف امروز ($doneCount/$totalCount)")

                } else {
                    // هیچ وظیفه‌ای نیست
                    views.setTextViewText(R.id.task_widget_title, "✅ وظیفه‌ای برای امروز نیست")
                    views.setViewVisibility(R.id.task_check_1, View.GONE)
                    views.setViewVisibility(R.id.task_time_1, View.GONE)
                    views.setViewVisibility(R.id.task_priority_1, View.GONE)
                    views.setViewVisibility(R.id.task_item_2, View.GONE)
                    views.setViewVisibility(R.id.task_item_3, View.GONE)
                }

                // PendingIntent اصلی (کلیک روی کل ویجت)
                val intentMain = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingMain = PendingIntent.getActivity(
                    context, 0, intentMain,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.task_widget_root, pendingMain)
                views.setOnClickPendingIntent(R.id.task_refresh, pendingMain)

                // PendingIntent افزودن وظیفه جدید
                val intentAdd = Intent(context, MainActivity::class.java).apply {
                    putExtra("open_new_task", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingAdd = PendingIntent.getActivity(
                    context, 1, intentAdd,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.task_add, pendingAdd)

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
