package ir.yaddasht.app.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class AnnotationType(val emoji: String, val faName: String) {
    HIGHLIGHT("🖍️", "هایلایت"),
    UNDERLINE("➖", "زیرخط"),
    NOTE("📝", "یادداشت"),
    BOOKMARK("🔖", "نشانک")
}

data class Annotation(
    val id: String = UUID.randomUUID().toString(),
    val fileKey: String,
    val type: AnnotationType,
    val colorKey: String,
    val selectedText: String = "",
    val note: String = "",
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val pageIndex: Int = 0,
    val relX: Float = 0f,
    val relY: Float = 0f,
    val relW: Float = 0f,
    val relH: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("fileKey", fileKey)
        put("type", type.name)
        put("colorKey", colorKey)
        put("selectedText", selectedText)
        put("note", note)
        put("startOffset", startOffset)
        put("endOffset", endOffset)
        put("pageIndex", pageIndex)
        put("relX", relX.toDouble())
        put("relY", relY.toDouble())
        put("relW", relW.toDouble())
        put("relH", relH.toDouble())
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(j: JSONObject): Annotation = Annotation(
            id = j.getString("id"),
            fileKey = j.getString("fileKey"),
            type = AnnotationType.valueOf(j.getString("type")),
            colorKey = j.getString("colorKey"),
            selectedText = j.optString("selectedText", ""),
            note = j.optString("note", ""),
            startOffset = j.optInt("startOffset", 0),
            endOffset = j.optInt("endOffset", 0),
            pageIndex = j.optInt("pageIndex", 0),
            relX = j.optDouble("relX", 0.0).toFloat(),
            relY = j.optDouble("relY", 0.0).toFloat(),
            relW = j.optDouble("relW", 0.0).toFloat(),
            relH = j.optDouble("relH", 0.0).toFloat(),
            createdAt = j.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = j.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

data class AnnotationColor(
    val key: String,
    val name: String,
    val emoji: String,
    val hex: String,
    val lightAlpha: Float,
    val sepiaAlpha: Float,
    val nightAlpha: Float
) {
    val color: Color
        get() = Color(android.graphics.Color.parseColor(hex))

    fun alpha(themeIndex: Int): Float = when (themeIndex) {
        0 -> lightAlpha
        1 -> sepiaAlpha
        else -> nightAlpha
    }

    fun colorForTheme(themeIndex: Int): Color = color.copy(alpha = alpha(themeIndex))
}

object AnnotationPalette {
    val colors = listOf(
        AnnotationColor("gold", "طلایی", "✨", "#FFD54F", 0.50f, 0.55f, 0.40f),
        AnnotationColor("coral", "مرجانی", "🌺", "#FF7043", 0.40f, 0.45f, 0.35f),
        AnnotationColor("rose", "رُز", "🌹", "#F06292", 0.40f, 0.45f, 0.35f),
        AnnotationColor("lavender", "یاسی", "💜", "#BA68C8", 0.40f, 0.45f, 0.35f),
        AnnotationColor("sky", "آسمانی", "🌊", "#4FC3F7", 0.45f, 0.50f, 0.40f),
        AnnotationColor("mint", "نعنایی", "🌿", "#81C784", 0.45f, 0.50f, 0.40f),
        AnnotationColor("amber", "کهربایی", "🍯", "#FFB74D", 0.45f, 0.50f, 0.40f),
        AnnotationColor("teal", "فیروزه‌ای", "💎", "#4DB6AC", 0.45f, 0.50f, 0.40f),
        AnnotationColor("indigo", "نیلی", "🌌", "#7986CB", 0.40f, 0.45f, 0.35f),
        AnnotationColor("slate", "خاکستری", "📝", "#90A4AE", 0.40f, 0.45f, 0.35f)
    )

    fun find(key: String): AnnotationColor = colors.firstOrNull { it.key == key } ?: colors[0]
}

object AnnotationStore {
    private const val PREFS = "annotations_store"
    private const val KEY_PREFIX = "ann:"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun fileKey(path: String): String {
        val name = File(path).nameWithoutExtension
        val hash = Integer.toHexString(path.hashCode())
        return "${name}_$hash"
    }

    fun all(c: Context, path: String): List<Annotation> {
        val json = prefs(c).getString(KEY_PREFIX + fileKey(path), "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull {
                try { Annotation.fromJson(arr.getJSONObject(it)) } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(c: Context, path: String, a: Annotation) {
        val list = all(c, path).toMutableList()
        list.removeAll { it.id == a.id }
        list.add(a.copy(updatedAt = System.currentTimeMillis()))
        write(c, path, list)
    }

    fun remove(c: Context, path: String, id: String) {
        write(c, path, all(c, path).filterNot { it.id == id })
    }

    fun updateNote(c: Context, path: String, id: String, note: String) {
        write(c, path, all(c, path).map {
            if (it.id == id) it.copy(note = note, updatedAt = System.currentTimeMillis()) else it
        })
    }

    private fun write(c: Context, path: String, list: List<Annotation>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs(c).edit().putString(KEY_PREFIX + fileKey(path), arr.toString()).apply()
    }
}
