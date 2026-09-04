package ir.yaddasht.app.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ReaderProgress(
    val pageOrScroll: Int,
    val timestamp: Long
)

data class Highlight(
    val id: String,
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val color: String,
    val note: String,
    val timestamp: Long
)

data class ReaderSettings(
    val themeIndex: Int = 0,
    val fontSize: Int = 16,
    val columns: Int = 1
)

object ReaderStore {
    private const val PREFS = "reader_store"
    private const val KEY_PROGRESS_POS = "progress:pos:"
    private const val KEY_PROGRESS_TS = "progress:ts:"
    private const val KEY_HIGHLIGHTS = "highlights:"
    private const val KEY_SETTINGS_THEME = "settings:theme"
    private const val KEY_SETTINGS_FONT = "settings:font"
    private const val KEY_SETTINGS_COLS = "settings:cols"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Unique key: filename + path hash (prevents collisions between identically named files)
    private fun fileKey(path: String): String {
        val name = File(path).nameWithoutExtension
        val hash = Integer.toHexString(path.hashCode())
        return "${name}_$hash"
    }

    // ─── Reading Progress ───
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

    // ─── Highlights ───
    fun saveHighlight(c: Context, path: String, h: Highlight) {
        val list = getHighlights(c, path).toMutableList()
        list.removeAll { it.id == h.id }
        list.add(h)
        writeHighlights(c, path, list)
    }

    fun removeHighlight(c: Context, path: String, id: String) {
        val list = getHighlights(c, path).filterNot { it.id == id }
        writeHighlights(c, path, list)
    }

    fun getHighlights(c: Context, path: String): List<Highlight> {
        val json = prefs(c).getString(KEY_HIGHLIGHTS + fileKey(path), "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                try { jsonToHighlight(arr.getJSONObject(i)) } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun getHighlightsJson(c: Context, path: String): String {
        return prefs(c).getString(KEY_HIGHLIGHTS + fileKey(path), "[]") ?: "[]"
    }

    private fun writeHighlights(c: Context, path: String, list: List<Highlight>) {
        val arr = JSONArray()
        list.forEach { arr.put(highlightToJson(it)) }
        prefs(c).edit()
            .putString(KEY_HIGHLIGHTS + fileKey(path), arr.toString())
            .apply()
    }

    private fun highlightToJson(h: Highlight): JSONObject = JSONObject().apply {
        put("id", h.id)
        put("text", h.text)
        put("startOffset", h.startOffset)
        put("endOffset", h.endOffset)
        put("color", h.color)
        put("note", h.note)
        put("timestamp", h.timestamp)
    }

    private fun jsonToHighlight(j: JSONObject): Highlight = Highlight(
        id = j.getString("id"),
        text = j.getString("text"),
        startOffset = j.getInt("startOffset"),
        endOffset = j.getInt("endOffset"),
        color = j.getString("color"),
        note = j.optString("note", ""),
        timestamp = j.getLong("timestamp")
    )

    // ─── Settings ───
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
