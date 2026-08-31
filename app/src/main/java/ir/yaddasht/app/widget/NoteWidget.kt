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
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, mgr, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> updateAll(context)
            ACTION_ADD_NOTE -> {
                context.startActivity(Intent(context, MainActivity::class.java).apply {
                    putExtra("note_id", -1L)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "ir.yaddasht.app.REFRESH_NOTES"
        const val ACTION_ADD_NOTE = "ir.yaddasht.app.ADD_NOTE"

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, NoteWidget::class.java))
            ids.forEach { updateAppWidget(context, mgr, it) }
        }

        fun updateAppWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.note_widget_layout)

            CoroutineScope(Dispatchers.IO).launch {
                val notes = try {
                    AppDatabase.get(context.applicationContext).dao().allNotesSync()
                        .filter { !NoteLock.isLocked(it.body) }
                        .sortedByDescending { it.updatedAt }
                        .take(3)
                } catch (e: Exception) { emptyList() }

                val slots = listOf(
                    Triple(R.id.note_item_1, R.id.note_title_1, R.id.note_body_1),
                    Triple(R.id.note_item_2, R.id.note_title_2, R.id.note_body_2),
                    Triple(R.id.note_item_3, R.id.note_title_3, R.id.note_body_3)
                )

                slots.forEachIndexed { i, (rootId, titleId, bodyId) ->
                    if (i < notes.size) {
                        val n = notes[i]
                        val prefix = if (n.pinned) "📌 " else ""
                        views.setTextViewText(titleId, prefix + n.title.ifBlank { "بدون عنوان" })
                        views.setTextViewText(bodyId, n.body.take(80).replace("\n", " ").ifBlank { "متنی نیست" })
                        val click = Intent(context, MainActivity::class.java).apply {
                            putExtra("note_id", n.id)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        views.setOnClickPendingIntent(rootId, PendingIntent.getActivity(
                            context, (n.id + 1000).toInt(), click,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    } else {
                        views.setTextViewText(titleId, if (i == 0 && notes.isEmpty()) "یادداشتی نیست ✍️" else "")
                        views.setTextViewText(bodyId, "")
                        views.setOnClickPendingIntent(rootId, null)
                    }
                }

                views.setOnClickPendingIntent(R.id.note_add, PendingIntent.getBroadcast(
                    context, 9001, Intent(context, NoteWidget::class.java).apply { action = ACTION_ADD_NOTE },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

                views.setOnClickPendingIntent(R.id.note_refresh, PendingIntent.getBroadcast(
                    context, 9002, Intent(context, NoteWidget::class.java).apply { action = ACTION_REFRESH },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

                withContext(Dispatchers.Main) { mgr.updateAppWidget(id, views) }
            }
        }

        fun updateSingle(context: Context, widgetId: Int) {
            updateAppWidget(context, AppWidgetManager.getInstance(context), widgetId)
        }

        fun forceUpdate(context: Context) = updateAll(context)
    }
}
