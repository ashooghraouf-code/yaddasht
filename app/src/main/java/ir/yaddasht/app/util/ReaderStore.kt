package ir.yaddasht.app.util

import android.content.Context
import java.io.File

data class ReaderProgress(val pageOrScroll: Int, val timestamp: Long)

data class ReaderSettings(
    val themeIndex: Int = 0,
    val fontSize: Int = 16,
    val columns: Int = 1
)

object ReaderStore {
    private const val PREFS = "reader_store"
    private const val KEY_PROGRESS_POS = "progress:pos:"
    private const val KEY_PROGRESS_TS = "progress:ts:"
    private const val KEY_SETTINGS_THEME = "settings:theme"
    private const val KEY_SETTINGS_FONT = "settings:font"
    private const val KEY_SETTINGS_COLS = "settings:cols"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun fileKey(path: String): String {
        val name = File(path).nameWithoutExtension
        val hash = Integer.toHexString(path.hashCode())
        return "${name}_$hash"
    }

    fun saveProgress(c: Context, path: String, pageOrScroll: Int) {
        val k = fileKey(path)
        prefs(c).edit()
            .putInt(KEY_PROGRESS_POS + k, pageOrScroll)
            .putLong(KEY_PROGRESS_TS + k, System.currentTimeMillis())
            .apply()
    }

    fun getProgress(c: Context, path: String): ReaderProgress? {
        val k = fileKey(path)
        val p = prefs(c)
        if (!p.contains(KEY_PROGRESS_POS + k)) return null
        return ReaderProgress(
            p.getInt(KEY_PROGRESS_POS + k, 0),
            p.getLong(KEY_PROGRESS_TS + k, 0)
        )
    }

    fun saveSettings(c: Context, s: ReaderSettings) {
        prefs(c).edit()
            .putInt(KEY_SETTINGS_THEME, s.themeIndex)
            .putInt(KEY_SETTINGS_FONT, s.fontSize)
            .putInt(KEY_SETTINGS_COLS, s.columns)
            .apply()
    }

    fun getSettings(c: Context): ReaderSettings {
        val p = prefs(c)
        return ReaderSettings(
            themeIndex = p.getInt(KEY_SETTINGS_THEME, 0),
            fontSize = p.getInt(KEY_SETTINGS_FONT, 16),
            columns = p.getInt(KEY_SETTINGS_COLS, 1)
        )
    }
}
