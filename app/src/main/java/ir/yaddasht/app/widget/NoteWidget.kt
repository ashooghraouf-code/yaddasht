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
import ir.yaddasht.app.util.NoteLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.note_widget_layout)
                
                // ۱. اعمال رنگ مستقل یادداشت
                val bgColor = WidgetPreferences.getNoteColor(context)
                try { views.setInt(R.id.note_widget_root, "setBackgroundColor", bgColor) } catch (_: Exception) {}

                // ۲. خواندن از دیتابیس
                val notes = AppDatabase.get(context).dao().getRecentNotes()
                
                if (notes.isNotEmpty()) {
                    val n1 = notes.getOrNull(0)
                    views.setTextViewText(R.id.note_title_1, n1?.title ?: "بدون عنوان")
                    views.setTextViewText(R.id.note_body_1, if (NoteLock.isLocked(n1?.body ?: "")) "🔒 قفل شده" else (n1?.body ?: ""))

                    val n2 = notes.getOrNull(1)
                    if (n2 != null) {
                        views.setTextViewText(R.id.note_title_2, n2.title)
                        views.setTextViewText(R.id.note_body_2, if (NoteLock.isLocked(n2.body)) "🔒 قفل شده" else n2.body)
                        views.setViewVisibility(R.id.note_item_2, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.note_item_2, View.GONE)
                    }

                    val n3 = notes.getOrNull(2)
                    if (n3 != null) {
                        views.setTextViewText(R.id.note_title_3, n3.title)
                        views.setTextViewText(R.id.note_body_3, if (NoteLock.isLocked(n3.body)) "🔒 قفل شده" else n3.body)
                        views.setViewVisibility(R.id.note_item_3, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.note_item_3, View.GONE)
                    }
                }

                // ۳. تنظیم کلیک‌ها
                val intentMain = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
                val pendingMain = PendingIntent.getActivity(context, 0, intentMain, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                
                val intentAdd = Intent(context, MainActivity::class.java).apply { 
                    putExtra("open_new_note", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK 
                }
                val pendingAdd = PendingIntent.getActivity(context, 1, intentAdd, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                try {
                    views.setOnClickPendingIntent(R.id.note_widget_root, pendingMain)
                    views.setOnClickPendingIntent(R.id.note_refresh, pendingMain)
                    views.setOnClickPendingIntent(R.id.note_add, pendingAdd)
                } catch (_: Exception) {}

                // ۴. آپدیت در رشته اصلی
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        fun forceUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, NoteWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
