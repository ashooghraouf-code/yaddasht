package ir.yaddasht.app.util

import android.content.Context
import java.io.File

data class ReaderProgress(val pageOrScroll: Int, val timestamp: Long)
data class ReaderSettings(val themeIndex: Int = 0, val fontSize: Int = 16, val columns: Int = 1)

object ReaderStore {
    private const val PREFS = "reader_store"
    private const val KEY_POS = "progress:pos:"
    private const val KEY_TS = "progress:ts:"
    private const val KEY_THEME = "settings:theme"
    private const val KEY_FONT = "settings:font"
    private const val KEY_COLS = "settings:cols"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun fileKey(path: String) = "${File(path).nameWithoutExtension}_${Integer.toHexString(path.hashCode())}"

    fun saveProgress(c: Context, path: String, v: Int) {
        val k = fileKey(path)
        prefs(c).edit().putInt(KEY_POS + k, v).putLong(KEY_TS + k, System.currentTimeMillis()).apply()
    }
    fun getProgress(c: Context, path: String): ReaderProgress? {
        val k = fileKey(path); val p = prefs(c)
        if (!p.contains(KEY_POS + k)) return null
        return ReaderProgress(p.getInt(KEY_POS + k, 0), p.getLong(KEY_TS + k, 0))
    }
    fun saveSettings(c: Context, s: ReaderSettings) {
        prefs(c).edit().putInt(KEY_THEME, s.themeIndex).putInt(KEY_FONT, s.fontSize).putInt(KEY_COLS, s.columns).apply()
    }
    fun getSettings(c: Context): ReaderSettings {
        val p = prefs(c)
        return ReaderSettings(p.getInt(KEY_THEME, 0), p.getInt(KEY_FONT, 16), p.getInt(KEY_COLS, 1))
    }
}
