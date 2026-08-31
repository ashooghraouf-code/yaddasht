package ir.yaddasht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.R

class NoteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, mgr, it) }
    }
    override fun onAppWidgetOptionsChanged(context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle?) {
        updateAppWidget(context, mgr, id)
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> forceUpdate(context)
            ACTION_OPEN_NOTE -> {
                val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
                if (noteId < 0) return
                context.startActivity(Intent(context, MainActivity::class.java).apply {
                    putExtra("note_id", noteId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            }
        }
    }
    companion object {
        const val ACTION_REFRESH = "ir.yaddasht.app.REFRESH_NOTES"
        const val ACTION_OPEN_NOTE = "ir.yaddasht.app.OPEN_NOTE"
        const val EXTRA_NOTE_ID = "note_id"
        fun updateAppWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.note_widget_layout)
            val svc = Intent(context, NoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.note_list, svc)
            views.setEmptyView(R.id.note_list, R.id.note_empty)
            views.setPendingIntentTemplate(R.id.note_list, PendingIntent.getBroadcast(context, 0,
                Intent(context, Note
