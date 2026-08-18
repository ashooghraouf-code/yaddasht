package ir.yaddasht.app.widget

import android.content.Context

object WidgetPrefs {
    private const val PREFS = "yaddasht_widget_prefs"
    private const val KEY_NOTE = "note_"
    private const val KEY_COLOR = "color_"

    fun setNote(context: Context, widgetId: Int, noteId: Long) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_NOTE + widgetId, noteId).apply()

    fun setColor(context: Context, widgetId: Int, color: Int) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_COLOR + widgetId, color).apply()

    fun getNote(context: Context, widgetId: Int): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_NOTE + widgetId, -1L)

    fun getColor(context: Context, widgetId: Int): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_COLOR + widgetId, 0)

    fun clear(context: Context, widgetId: Int) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_NOTE + widgetId).remove(KEY_COLOR + widgetId).apply()
}