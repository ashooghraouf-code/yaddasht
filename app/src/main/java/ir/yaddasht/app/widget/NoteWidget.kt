package ir.yaddasht.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import ir.yaddasht.app.R

class NoteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.note_widget_layout)
            // تغییر متن برای اثبات اینکه کد اجرا شده است
            views.setTextViewText(R.id.widget_text, "ویجت با موفقیت ساخته شد! ✅")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
