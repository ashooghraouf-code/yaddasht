package ir.yaddasht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.NEW_NOTE_ID
import ir.yaddasht.app.R
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.util.NoteLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val all = AppDatabase.get(context).dao().allNotesSync()
                withContext(Dispatchers.Main) {
                    for (id in appWidgetIds) {
                        val noteId = WidgetPrefs.getNote(context, id)
                        val color = WidgetPrefs.getColor(context, id)
                        val note = if (noteId == -1L) all.maxByOrNull { it.updatedAt } else all.find { it.id == noteId }
                        updateWidget(context, manager, id, note?.title, note?.body, color)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() } finally { pending.finish() }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.clear(context, it) }
    }

    companion object {

        fun updateSingle(context: Context, widgetId: Int) {
            val manager = AppWidgetManager.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val all = AppDatabase.get(context).dao().allNotesSync()
                    val noteId = WidgetPrefs.getNote(context, widgetId)
                    val color = WidgetPrefs.getColor(context, widgetId)
                    val note = if (noteId == -1L) all.maxByOrNull { it.updatedAt } else all.find { it.id == noteId }
                    withContext(Dispatchers.Main) {
                        updateWidget(context, manager, widgetId, note?.title, note?.body, color)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        fun forceUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NoteWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(Intent(context, NoteWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int, title: String?, body: String?, color: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_note)
            views.setInt(R.id.widget_root, "setBackgroundResource", bgFor(color))
            views.setTextViewText(R.id.widget_title, title?.ifBlank { "بدون عنوان" } ?: "دفترچه خالی است")
            val snippet = when {
                body == null -> "برو اولین یادداشتت را بنویس ✍️"
                NoteLock.isLocked(body) -> "🔒 یادداشت قفل‌شده"
                else -> body
            }
            views.setTextViewText(R.id.widget_body, snippet)

            val openPi = PendingIntent.getActivity(context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, openPi)

            val newPi = PendingIntent.getActivity(context, 1,
                Intent(context, MainActivity::class.java).putExtra("note_id", NEW_NOTE_ID),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_new, newPi)

            manager.updateAppWidget(id, views)
        }

        private fun bgFor(color: Int): Int = when (color) {
            1 -> R.drawable.widget_bg_1
            2 -> R.drawable.widget_bg_2
            3 -> R.drawable.widget_bg_3
            4 -> R.drawable.widget_bg_4
            5 -> R.drawable.widget_bg_5
            else -> R.drawable.widget_bg_0
        }
    }
}
