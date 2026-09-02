package ir.yaddasht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.R

class NoteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.note_widget_layout)
            
            // ۱. اعمال رنگ پس‌زمینه
            val bgColor = WidgetPreferences.getNoteColor(context)
            try { views.setInt(R.id.note_widget_root, "setBackgroundColor", bgColor) } catch (_: Exception) {}

            // ۲. تنظیم کلیک‌ها
            val intentMain = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingMain = PendingIntent.getActivity(context, 0, intentMain, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            
            val intentAdd = Intent(context, MainActivity::class.java).apply {
                putExtra("open_new_note", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingAdd = PendingIntent.getActivity(context, 1, intentAdd, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            try {
                views.setOnClickPendingIntent(R.id.note_widget_root, pendingMain)
                views.setOnClickPendingIntent(R.id.note_refresh, pendingMain) // رفرش هم فعلاً اپ را باز می‌کند
                views.setOnClickPendingIntent(R.id.note_add, pendingAdd)
                
                // کلیک روی آیتم‌ها (فعلاً اپ را باز می‌کنند، بعداً می‌توانید به یادداشت خاص لینک دهید)
                views.setOnClickPendingIntent(R.id.note_item_1, pendingMain)
                views.setOnClickPendingIntent(R.id.note_item_2, pendingMain)
                views.setOnClickPendingIntent(R.id.note_item_3, pendingMain)
            } catch (_: Exception) {}

            // ۳. آپدیت ویجت
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun forceUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = android.content.ComponentName(context, NoteWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
