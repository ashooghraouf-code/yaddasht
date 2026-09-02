package ir.yaddasht.app.widget

import android.content.Context
import androidx.core.content.edit

object WidgetPreferences {
    private const val PREFS = "widget_prefs_v2"
    
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getNoteColor(c: Context): Int = prefs(c).getInt("note_bg_color", 0xFFFFE082.toInt())
    fun setNoteColor(c: Context, color: Int) = prefs(c).edit { putInt("note_bg_color", color) }

    fun getTaskColor(c: Context): Int = prefs(c).getInt("task_bg_color", 0xFF80DEEA.toInt())
    fun setTaskColor(c: Context, color: Int) = prefs(c).edit { putInt("task_bg_color", color) }
}
