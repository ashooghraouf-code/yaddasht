package ir.yaddasht.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ir.yaddasht.app.R
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.util.relativeTimeFa
import kotlinx.coroutines.runBlocking

class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetFactory(applicationContext)
    }
}

private class NoteWidgetFactory(
    private val context: android.content.Context
) : RemoteViewsService.RemoteViewsFactory {

    private var notes: List<Note> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            notes = try {
                val all = AppDatabase.get(context.applicationContext).dao().allNotesSync()
                all.sortedByDescending { it.updatedAt }.take(20)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun onDestroy() { notes = emptyList() }
    override fun getCount(): Int = notes.size
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = if (position < notes.size) notes[position].id else position.toLong()
    override fun hasStableIds(): Boolean = true

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.note_widget_item)
        if (position < 0 || position >= notes.size) return rv
        val note = notes[position]

        val title = if (note.pinned) "📌 ${note.title.ifBlank { "بدون عنوان" }}" else note.title.ifBlank { "بدون عنوان" }
        rv.setTextViewText(R.id.note_item_title, title)

        val snippet = note.body.take(100).replace("\n", " ")
        rv.setTextViewText(R.id.note_item_snippet, snippet.ifBlank { "متنی نیست" })
        rv.setTextViewText(R.id.note_item_time, relativeTimeFa(note.updatedAt))

        val fillIn = Intent().apply {
            putExtra(NoteWidget.EXTRA_NOTE_ID, note.id)
        }
        rv.setOnClickFillInIntent(R.id.note_item_root, fillIn)
        return rv
    }
}
