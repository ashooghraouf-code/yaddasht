package ir.yaddasht.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
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
            val bgColor = WidgetPreferences.getNoteColor(context)
            try { views.setInt(R.id.widget_root, "setBackgroundColor", bgColor) } catch (_: Exception) {}
            try { views.setTextViewText(R.id.widget_text, "📝 یادداشت‌ها") } catch (_: Exception) {}
            appWidgetManager.updateAppWidget(appWidgetId, views)
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
