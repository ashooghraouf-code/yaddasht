package ir.yaddasht.app.util

import android.content.Context
import androidx.core.content.edit

object WidgetPreferences {
    private const val PREFS = "widget_prefs"
    private const val KEY_NOTE_BG = "note_bg_color"
    private const val KEY_TASK_BG = "task_bg_color"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getNoteColor(c: Context): Int = prefs(c).getInt(KEY_NOTE_BG, 0xFFFFE082.toInt())
    fun setNoteColor(c: Context, color: Int) = prefs(c).edit { putInt(KEY_NOTE_BG, color) }

    fun getTaskColor(c: Context): Int = prefs(c).getInt(KEY_TASK_BG, 0xFF80DEEA.toInt())
    fun setTaskColor(c: Context, color: Int) = prefs(c).edit { putInt(KEY_TASK_BG, color) }
}
