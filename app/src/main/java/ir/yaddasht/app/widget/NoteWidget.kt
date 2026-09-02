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
            
            // دریافت رنگ ذخیره‌شده
            val bgColor = WidgetPreferences.getNoteColor(context)
            views.setInt(R.id.note_widget_root, "setBackgroundColor", bgColor)

            // تنظیم کلیک برای باز کردن اپ
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("open_widget_settings", true)
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.note_widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.note_refresh, pendingIntent)
            views.setOnClickPendingIntent(R.id.note_add, pendingIntent)

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
