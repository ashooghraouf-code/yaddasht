package ir.yaddasht.app.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class ThemeKind {
    Solid,
    Gradient,
    Pattern
}

enum class PatternKind {
    None,
    Dots,
    Lines,
    Grid,
    Diagonal,
    Linen,
    Waves,
    Stars,
    Bokeh,
    Diamonds
}

enum class ThemeScope {
    App,
    Board,
    NotePaper,
    WidgetNote,
    WidgetTask
}

data class ThemeConfig(
    val kind: ThemeKind = ThemeKind.Solid,
    val primary: Int = 0xFF0F3D2E.toInt(),
    val secondary: Int? = null,
    val pattern: PatternKind = PatternKind.None,
    val textureAlpha: Float = 0.10f,
    val dim: Float = 0f
)

data class ThemeSwatch(
    val name: String,
    val emoji: String,
    val color: Int
)

data class ThemeGradient(
    val name: String,
    val emoji: String,
    val from: Int,
    val to: Int
)

data class ThemePreset(
    val name: String,
    val emoji: String,
    val config: ThemeConfig
)

data class ThemeContentColors(
    val onBackground: Color,
    val onSurface: Color,
    val muted: Color,
    val accent: Color
)

object ThemeKit {
    private const val PREFS = "yaddasht_theme_kit"
    private const val LEGACY_HOME_PREFS = "home_theme_prefs"
    private const val LEGACY_HOME_KEY = "home_theme_config"

    private val DefaultApp = 0xFF0F3D2E.toInt()
    private val DefaultBoard = 0xFF143D2B.toInt()
    private val DefaultPaper = 0xFFFFF8E1.toInt()
    private val DefaultWidget = 0xE60E1116.toInt()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun scopeKey(scope: ThemeScope, id: Long? = null): String =
        if (id != null && id > 0L) "theme_${scope.name}_$id" else "theme_${scope.name}"

    fun default(scope: ThemeScope): ThemeConfig = when (scope) {
        ThemeScope.App -> ThemeConfig(ThemeKind.Solid, DefaultApp)
        ThemeScope.Board -> ThemeConfig(ThemeKind.Solid, DefaultBoard)
        ThemeScope.NotePaper -> ThemeConfig(ThemeKind.Solid, DefaultPaper)
        ThemeScope.WidgetNote, ThemeScope.WidgetTask -> ThemeConfig(ThemeKind.Solid, DefaultWidget)
    }

    fun get(context: Context, scope: ThemeScope): ThemeConfig {
        val stored = prefs(context).getString(scopeKey(scope), null)
        if (!stored.isNullOrBlank()) {
            val decoded = decode(stored)
            if (decoded != null) return decoded
        }

        if (scope == ThemeScope.App) {
            val legacy = legacyHomeConfig(context)
            if (legacy != null) return legacy
        }

        return default(scope)
    }

    fun set(context: Context, scope: ThemeScope, config: ThemeConfig) {
        prefs(context).edit()
            .putString(scopeKey(scope), encode(config))
            .apply()

        if (scope == ThemeScope.App) {
            writeLegacyHomeConfig(context, config)
        }
    }

    fun getBoard(context: Context, boardId: Long): ThemeConfig {
        if (boardId > 0L) {
            val stored = prefs(context).getString(scopeKey(ThemeScope.Board, boardId), null)
            if (!stored.isNullOrBlank()) {
                val decoded = decode(stored)
                if (decoded != null) return decoded
            }
        }

        return get(context, ThemeScope.Board)
    }

    fun setBoard(context: Context, boardId: Long, config: ThemeConfig) {
        if (boardId > 0L) {
            prefs(context).edit()
                .putString(scopeKey(ThemeScope.Board, boardId), encode(config))
                .apply()
        } else {
            set(context, ThemeScope.Board, config)
        }
    }

    fun getNotePaper(context: Context, noteId: Long): ThemeConfig {
        if (noteId > 0L) {
            val stored = prefs(context).getString(scopeKey(ThemeScope.NotePaper, noteId), null)
            if (!stored.isNullOrBlank()) {
                val decoded = decode(stored)
                if (decoded != null) return decoded
            }
        }

        return get(context, ThemeScope.NotePaper)
    }

    fun setNotePaper(context: Context, noteId: Long, config: ThemeConfig) {
        if (noteId > 0L) {
            prefs(context).edit()
                .putString(scopeKey(ThemeScope.NotePaper, noteId), encode(config))
                .apply()
        } else {
            set(context, ThemeScope.NotePaper, config)
        }
    }

    fun clearNotePaper(context: Context, noteId: Long) {
        if (noteId > 0L) {
            prefs(context).edit()
                .remove(scopeKey(ThemeScope.NotePaper, noteId))
                .apply()
        }
    }

    fun getNotePaperOr(context: Context, noteId: Long, baseIndex: Int): ThemeConfig {
        if (noteId > 0L) {
            val stored = prefs(context).getString(scopeKey(ThemeScope.NotePaper, noteId), null)
            if (!stored.isNullOrBlank()) {
                val decoded = decode(stored)
                if (decoded != null) return decoded
            }
        }

        return paperConfigForIndex(baseIndex)
    }

    fun getWidgetNote(context: Context): ThemeConfig = get(context, ThemeScope.WidgetNote)
    fun setWidgetNote(context: Context, config: ThemeConfig) = set(context, ThemeScope.WidgetNote, config)

    fun getWidgetTask(context: Context): ThemeConfig = get(context, ThemeScope.WidgetTask)
    fun setWidgetTask(context: Context, config: ThemeConfig) = set(context, ThemeScope.WidgetTask, config)

    private fun encode(config: ThemeConfig): String =
        JSONObject().apply {
            put("kind", config.kind.name)
            put("primary", config.primary)

            if (config.secondary == null) {
                put("secondary", JSONObject.NULL)
            } else {
                put("secondary", config.secondary)
            }

            put("pattern", config.pattern.name)
            put("textureAlpha", config.textureAlpha.toDouble())
            put("dim", config.dim.toDouble())
        }.toString()

    private fun decode(raw: String): ThemeConfig? {
        return try {
            val obj = JSONObject(raw)

            val kind = runCatching {
                ThemeKind.valueOf(obj.optString("kind", ThemeKind.Solid.name))
            }.getOrDefault(ThemeKind.Solid)

            val pattern = runCatching {
                PatternKind.valueOf(obj.optString("pattern", PatternKind.None.name))
            }.getOrDefault(PatternKind.None)

            val secondary =
                if (obj.has("secondary") && !obj.isNull("secondary")) obj.optInt("secondary") else null

            ThemeConfig(
                kind = kind,
                primary = obj.optInt("primary", DefaultApp),
                secondary = secondary,
                pattern = pattern,
                textureAlpha = obj.optDouble("textureAlpha", 0.10).toFloat(),
                dim = obj.optDouble("dim", 0.0).toFloat()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun legacyHomeConfig(context: Context): ThemeConfig? {
        val raw = context.getSharedPreferences(LEGACY_HOME_PREFS, Context.MODE_PRIVATE)
            .getString(LEGACY_HOME_KEY, null) ?: return null

        val parts = raw.split("|")
        if (parts.size < 6) return null

        val kind = when (parts[0]) {
            "Gradient" -> ThemeKind.Gradient
            "Pattern" -> ThemeKind.Pattern
            else -> ThemeKind.Solid
        }

        val pattern = runCatching {
            PatternKind.valueOf(parts[3])
        }.getOrDefault(PatternKind.None)

        return ThemeConfig(
            kind = kind,
            primary = parts[1].toIntOrNull() ?: DefaultApp,
            secondary = if (parts[2] == "n") null else parts[2].toIntOrNull(),
            pattern = pattern,
            textureAlpha = parts[4].toFloatOrNull() ?: 0.10f,
            dim = parts[5].toFloatOrNull() ?: 0f
        )
    }

    private fun writeLegacyHomeConfig(context: Context, config: ThemeConfig) {
        val raw = buildString {
            append(config.kind.name).append('|')
            append(config.primary).append('|')
            append(config.secondary?.toString() ?: "n").append('|')
            append(config.pattern.name).append('|')
            append(config.textureAlpha).append('|')
            append(config.dim)
        }

        context.getSharedPreferences(LEGACY_HOME_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(LEGACY_HOME_KEY, raw)
            .apply()
    }

    val patternLabels = listOf(
        PatternKind.None to "بدون طرح",
        PatternKind.Dots to "نقاط",
        PatternKind.Lines to "خط‌دار",
        PatternKind.Grid to "شطرنجی",
        PatternKind.Diagonal to "مورب",
        PatternKind.Linen to "کتان",
        PatternKind.Waves to "موج",
        PatternKind.Stars to "ستاره",
        PatternKind.Bokeh to "بوکه",
        PatternKind.Diamonds to "الماس"
    )

    fun patternLabel(kind: PatternKind): String =
        patternLabels.firstOrNull { it.first == kind }?.second ?: kind.name

    val appSwatches = listOf(
        ThemeSwatch("کاغذ کرم", "📜", 0xFFFFF8E1.toInt()),
        ThemeSwatch("سپیا", "🧻", 0xFFF4ECD8.toInt()),
        ThemeSwatch("چای نبات", "🍵", 0xFFE8DCC4.toInt()),
        ThemeSwatch("ابریشم", "🤍", 0xFFFFFBF0.toInt()),
        ThemeSwatch("کاغذ پوستی", "📃", 0xFFEFE6D3.toInt()),
        ThemeSwatch("مومیایی", "🏺", 0xFFE0D3B8.toInt()),

        ThemeSwatch("شب", "🌙", 0xFF121212.toInt()),
        ThemeSwatch("نفتی", "🛢️", 0xFF0E1116.toInt()),
        ThemeSwatch("جنگلان", "🌲", 0xFF102018.toInt()),
        ThemeSwatch("زغالی", "🪨", 0xFF1A1A1A.toInt()),
        ThemeSwatch("نیمه‌شب", "🌌", 0xFF0B1026.toInt()),
        ThemeSwatch("دود", "🌫️", 0xFF20232A.toInt()),

        ThemeSwatch("سبز چراغ", "🏮", 0xFF0F3D2E.toInt()),
        ThemeSwatch("باغ", "🌿", 0xFF1B5E20.toInt()),
        ThemeSwatch("زیتون", "🫒", 0xFF33691E.toInt()),
        ThemeSwatch("کاج", "🌲", 0xFF0B3D2C.toInt()),
        ThemeSwatch("یشم", "💎", 0xFF2E7D52.toInt()),
        ThemeSwatch("جنگل بارانی", "🌧️", 0xFF143D2B.toInt()),

        ThemeSwatch("آسمان", "☁️", 0xFFE3F2FD.toInt()),
        ThemeSwatch("دریا", "🌊", 0xFF01579B.toInt()),
        ThemeSwatch("فیروزه", "🧿", 0xFF00838F.toInt()),
        ThemeSwatch("نیلی", "🔵", 0xFF1A237E.toInt()),
        ThemeSwatch("اقیانوس", "🐋", 0xFF023E5C.toInt()),
        ThemeSwatch("مه", "🌫️", 0xFFCFD8DC.toInt()),

        ThemeSwatch("آفتاب", "☀️", 0xFFFFF3E0.toInt()),
        ThemeSwatch("نارنج", "🍊", 0xFFE65100.toInt()),
        ThemeSwatch("گل‌گون", "🌸", 0xFFF8BBD0.toInt()),
        ThemeSwatch("عنابی", "🍇", 0xFF880E4F.toInt()),
        ThemeSwatch("زعفران", "🌼", 0xFFFFB74D.toInt()),
        ThemeSwatch("آجر", "🧱", 0xFFB71C1C.toInt()),

        ThemeSwatch("فیروزه ایرانی", "🕌", 0xFF00A6A6.toInt()),
        ThemeSwatch("لاجورد", "🔷", 0xFF283593.toInt()),
        ThemeSwatch("زرشک", "🍒", 0xFF8E1B3A.toInt()),
        ThemeSwatch("کویر", "🏜️", 0xFF8D6E63.toInt()),
        ThemeSwatch("مس", "🥉", 0xFFB87333.toInt()),
        ThemeSwatch("زیتون ایرانی", "🌿", 0xFF6B8E23.toInt()),

        ThemeSwatch("ارغوانی", "🪻", 0xFF6A1B9A.toInt()),
        ThemeSwatch("فیروزهٔ روشن", "🩵", 0xFF4DD0E1.toInt()),
        ThemeSwatch("لیمویی تیره", "🍈", 0xFF9E9D24.toInt()),
        ThemeSwatch("صورتی چرخی", "🍥", 0xFFEC407A.toInt()),
        ThemeSwatch("آبی بادامی", "🫧", 0xFF4FC3F7.toInt()),
        ThemeSwatch("قهوه شیری", "🥛", 0xFFA1887F.toInt())
    )

    val boardSwatches = listOf(
        ThemeSwatch("تابلوی سبز", "🟩", 0xFF143D2B.toInt()),
        ThemeSwatch("گچ‌تخته", "🧑‍🏫", 0xFF0B3D2C.toInt()),
        ThemeSwatch("تخته سیاه", "⬛", 0xFF111111.toInt()),
        ThemeSwatch("چوب گرم", "🪵", 0xFF6B4A2B.toInt()),
        ThemeSwatch("کاغذ کاهی", "📜", 0xFFE0D3B8.toInt()),
        ThemeSwatch("سنگ slate", "🪨", 0xFF2B3137.toInt()),
        ThemeSwatch("نیلوفر تیره", "🌌", 0xFF18244A.toInt()),
        ThemeSwatch("فیروزه تیره", "🧿", 0xFF074B52.toInt()),
        ThemeSwatch("قهوه تلخ", "☕", 0xFF3E2723.toInt()),
        ThemeSwatch("کرم روشن", "🥛", 0xFFFFF3D6.toInt()),
        ThemeSwatch("مه سرد", "🌫️", 0xFFD7DEE8.toInt()),
        ThemeSwatch("زغالی", "🌑", 0xFF1A1A1A.toInt()),
        ThemeSwatch("زرشکی تیره", "🍷", 0xFF4A0E2A.toInt()),
        ThemeSwatch("نیلی سلطنتی", "🔷", 0xFF1A237E.toInt()),
        ThemeSwatch("زیتونی", "🫒", 0xFF33691E.toInt()),
        ThemeSwatch("مسی", "🥉", 0xFF7A4F2A.toInt())
    )

    val paperSwatches = listOf(
        ThemeSwatch("کاغذ کرم", "📜", 0xFFFFF8E1.toInt()),
        ThemeSwatch("سپیا", "🧻", 0xFFF4ECD8.toInt()),
        ThemeSwatch("چای نبات", "🍵", 0xFFE8DCC4.toInt()),
        ThemeSwatch("ابریشم", "🤍", 0xFFFFFBF0.toInt()),
        ThemeSwatch("کاغذ پوستی", "📃", 0xFFEFE6D3.toInt()),
        ThemeSwatch("سفید دفتری", "📄", 0xFFFDFDFD.toInt()),
        ThemeSwatch("آسمانی", "☁️", 0xFFE3F2FD.toInt()),
        ThemeSwatch("نعنایی", "🌿", 0xFFE8F5E9.toInt()),
        ThemeSwatch("اسطوخودوس", "💜", 0xFFEDE7F6.toInt()),
        ThemeSwatch("هلویی", "🍑", 0xFFFFF3E0.toInt()),
        ThemeSwatch("لیمویی", "🍋", 0xFFF9FBE7.toInt()),
        ThemeSwatch("دودی", "🌫️", 0xFFECEFF1.toInt()),
        ThemeSwatch("صورتی ملایم", "🌸", 0xFFFFE0EC.toInt()),
        ThemeSwatch("فیروزه‌ای", "🩵", 0xFFE0F7FA.toInt()),
        ThemeSwatch("کهربایی", "🟠", 0xFFFFE0B2.toInt()),
        ThemeSwatch("یاسی", "🪻", 0xFFF3E5F5.toInt())
    )

    val widgetSwatches = listOf(
        ThemeSwatch("شیشهٔ تیره", "🪟", 0xE60E1116.toInt()),
        ThemeSwatch("سبز چراغ", "🏮", 0xF20F3D2E.toInt()),
        ThemeSwatch("نیمه‌شب", "🌌", 0xF20B1026.toInt()),
        ThemeSwatch("فیروزه", "🧿", 0xF200838F.toInt()),
        ThemeSwatch("زعفران", "🌼", 0xF2FFB74D.toInt()),
        ThemeSwatch("کاغذ کرم", "📜", 0xF2FFF8E1.toInt()),
        ThemeSwatch("مه", "🌫️", 0xF2CFD8DC.toInt()),
        ThemeSwatch("زغالی", "🌑", 0xF21A1A1A.toInt()),
        ThemeSwatch("نیلی", "🔵", 0xF21A237E.toInt()),
        ThemeSwatch("عنابی", "🍇", 0xF2880E4F.toInt()),
        ThemeSwatch("جنگل", "🌲", 0xF2102018.toInt()),
        ThemeSwatch("دریا", "🌊", 0xF201579B.toInt()),
        ThemeSwatch("ارغوانی", "🪻", 0xF26A1B9A.toInt()),
        ThemeSwatch("مسی", "🥉", 0xF2B87333.toInt())
    )

    val gradients = listOf(
        ThemeGradient("سپیده‌دم", "🌅", 0xFF0F2027.toInt(), 0xFF2C5364.toInt()),
        ThemeGradient("جنگل مه‌آلود", "🌫️", 0xFF134E5E.toInt(), 0xFF0F2027.toInt()),
        ThemeGradient("شب تار", "🌑", 0xFF090909.toInt(), 0xFF1F1F1F.toInt()),
        ThemeGradient("باغ سبز", "🌿", 0xFF0F3D2E.toInt(), 0xFF1B5E20.toInt()),
        ThemeGradient("آسمان نیلی", "🔵", 0xFF1A237E.toInt(), 0xFF0D47A1.toInt()),
        ThemeGradient("غروب کویر", "🏜️", 0xFF8D6E63.toInt(), 0xFFBF360C.toInt()),
        ThemeGradient("زعفران", "🌼", 0xFFFFB74D.toInt(), 0xFFE65100.toInt()),
        ThemeGradient("یاقوت", "❤️‍🔥", 0xFF880E4F.toInt(), 0xFF4A148C.toInt()),
        ThemeGradient("فیروزه", "🧿", 0xFF00838F.toInt(), 0xFF006064.toInt()),
        ThemeGradient("کاغذ و چای", "🍵", 0xFFFFF8E1.toInt(), 0xFFE0D3B8.toInt()),
        ThemeGradient("ارغوان", "🪻", 0xFF6A1B9A.toInt(), 0xFFAD1457.toInt()),
        ThemeGradient("اقیانوس", "🐋", 0xFF023E5C.toInt(), 0xFF006064.toInt())
    )

    val appPresets = listOf(
        ThemePreset("سبز چراغ", "🏮", ThemeConfig(ThemeKind.Solid, 0xFF0F3D2E.toInt())),
        ThemePreset("شب نقطه‌ای", "🌌", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF0B1026.toInt(),
            secondary = 0xFFFFFFFF.toInt(),
            pattern = PatternKind.Stars,
            textureAlpha = 0.14f
        )),
        ThemePreset("کاغذ خط‌دار", "📜", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFFFF8E1.toInt(),
            secondary = 0xFF1A237E.toInt(),
            pattern = PatternKind.Lines,
            textureAlpha = 0.12f
        )),
        ThemePreset("جنگل مه‌آلود", "🌫️", ThemeConfig(ThemeKind.Gradient, 0xFF134E5E.toInt(), 0xFF0F2027.toInt())),
        ThemePreset("کتان گرم", "🧵", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFE8DCC4.toInt(),
            secondary = 0xFF1A1A1A.toInt(),
            pattern = PatternKind.Linen,
            textureAlpha = 0.09f
        )),
        ThemePreset("موج آرام", "🌊", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFE3F2FD.toInt(),
            secondary = 0xFF01579B.toInt(),
            pattern = PatternKind.Waves,
            textureAlpha = 0.12f
        )),
        ThemePreset("بوکهٔ شب", "✨", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF121212.toInt(),
            secondary = 0xFFFFB74D.toInt(),
            pattern = PatternKind.Bokeh,
            textureAlpha = 0.18f
        )),
        ThemePreset("الماس فیروزه", "🔷", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF00838F.toInt(),
            secondary = 0xFFFFFFFF.toInt(),
            pattern = PatternKind.Diamonds,
            textureAlpha = 0.10f
        )),
        ThemePreset("ارغوان ستاره", "🪻", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF2A0E3F.toInt(),
            secondary = 0xFFE1BEE7.toInt(),
            pattern = PatternKind.Stars,
            textureAlpha = 0.16f
        )),
        ThemePreset("کویر مورب", "🏜️", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF8D6E63.toInt(),
            secondary = 0xFFFFE0B2.toInt(),
            pattern = PatternKind.Diagonal,
            textureAlpha = 0.12f
        ))
    )

    val boardPresets = listOf(
        ThemePreset("تابلوی سبز", "🟩", ThemeConfig(ThemeKind.Solid, 0xFF143D2B.toInt())),
        ThemePreset("گچ‌تخته", "🧑🏫", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF0B3D2C.toInt(),
            secondary = 0xFFFFFFFF.toInt(),
            pattern = PatternKind.Grid,
            textureAlpha = 0.12f
        )),
        ThemePreset("چای نبات", "🍵", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFE8DCC4.toInt(),
            secondary = 0xFF1A1A1A.toInt(),
            pattern = PatternKind.Linen,
            textureAlpha = 0.09f
        )),
        ThemePreset("نیمه‌شب ستاره", "🌌", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF0B1026.toInt(),
            secondary = 0xFFFFFFFF.toInt(),
            pattern = PatternKind.Stars,
            textureAlpha = 0.15f
        )),
        ThemePreset("شطرنجی مهندسی", "📐", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFE0D3B8.toInt(),
            secondary = 0xFF1A237E.toInt(),
            pattern = PatternKind.Grid,
            textureAlpha = 0.12f
        )),
        ThemePreset("موج دریایی", "🌊", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF023E5C.toInt(),
            secondary = 0xFFFFFFFF.toInt(),
            pattern = PatternKind.Waves,
            textureAlpha = 0.12f
        )),
        ThemePreset("الماس تاریک", "♦️", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF111111.toInt(),
            secondary = 0xFFFFB74D.toInt(),
            pattern = PatternKind.Diamonds,
            textureAlpha = 0.10f
        )),
        ThemePreset("بوکهٔ تابلو", "✨", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF1A1A1A.toInt(),
            secondary = 0xFF2E7D52.toInt(),
            pattern = PatternKind.Bokeh,
            textureAlpha = 0.18f
        )),
        ThemePreset("چوب گرم مورب", "🪵", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF6B4A2B.toInt(),
            secondary = 0xFFFFE0B2.toInt(),
            pattern = PatternKind.Diagonal,
            textureAlpha = 0.12f
        )),
        ThemePreset("نیلی نقاط", "🔷", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFF18244A.toInt(),
            secondary = 0xFF9FA8DA.toInt(),
            pattern = PatternKind.Dots,
            textureAlpha = 0.14f
        ))
    )

    val paperPresets = listOf(
        ThemePreset("کاغذ کرم", "📜", ThemeConfig(ThemeKind.Solid, 0xFFFFF8E1.toInt())),
        ThemePreset("خط‌دار کلاسیک", "📘", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFFFFDF5.toInt(),
            secondary = 0xFF1A237E.toInt(),
            pattern = PatternKind.Lines,
            textureAlpha = 0.14f
        )),
        ThemePreset("نقطه‌ای بولت", "🔘", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFFFFBF0.toInt(),
            secondary = 0xFF5B665F.toInt(),
            pattern = PatternKind.Dots,
            textureAlpha = 0.14f
        )),
        ThemePreset("شطرنجی کاغذ", "🗂️", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFF4ECD8.toInt(),
            secondary = 0xFF8D6E63.toInt(),
            pattern = PatternKind.Grid,
            textureAlpha = 0.12f
        )),
        ThemePreset("کاغذ کاهی", "🧻", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFE0D3B8.toInt(),
            secondary = 0xFF3E2723.toInt(),
            pattern = PatternKind.Linen,
            textureAlpha = 0.09f
        )),
        ThemePreset("آسمان خط‌دار", "☁️", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFE3F2FD.toInt(),
            secondary = 0xFF01579B.toInt(),
            pattern = PatternKind.Lines,
            textureAlpha = 0.12f
        )),
        ThemePreset("نعنایی نقطه‌ای", "🌿", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFE8F5E9.toInt(),
            secondary = 0xFF2E7D52.toInt(),
            pattern = PatternKind.Dots,
            textureAlpha = 0.13f
        )),
        ThemePreset("اسطوخودوس مورب", "💜", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFEDE7F6.toInt(),
            secondary = 0xFF4A148C.toInt(),
            pattern = PatternKind.Diagonal,
            textureAlpha = 0.10f
        )),
        ThemePreset("هلویی کتان", "🍑", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFFFF3E0.toInt(),
            secondary = 0xFFBF360C.toInt(),
            pattern = PatternKind.Linen,
            textureAlpha = 0.08f
        )),
        ThemePreset("سپیا", "🏺", ThemeConfig(ThemeKind.Solid, 0xFFF4ECD8.toInt())),
        ThemePreset("صورتی نقاط", "🌸", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFFFE0EC.toInt(),
            secondary = 0xFFAD1457.toInt(),
            pattern = PatternKind.Dots,
            textureAlpha = 0.13f
        )),
        ThemePreset("فیروزه‌ای خط‌دار", "🩵", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFE0F7FA.toInt(),
            secondary = 0xFF006064.toInt(),
            pattern = PatternKind.Lines,
            textureAlpha = 0.12f
        )),
        ThemePreset("کهربایی موج", "🟠", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFFFE0B2.toInt(),
            secondary = 0xFFE65100.toInt(),
            pattern = PatternKind.Waves,
            textureAlpha = 0.12f
        )),
        ThemePreset("یاسی شطرنجی", "🪻", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFF3E5F5.toInt(),
            secondary = 0xFF6A1B9A.toInt(),
            pattern = PatternKind.Grid,
            textureAlpha = 0.10f
        )),
        ThemePreset("لیمویی مورب", "🍋", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFF9FBE7.toInt(),
            secondary = 0xFF33691E.toInt(),
            pattern = PatternKind.Diagonal,
            textureAlpha = 0.10f
        )),
        ThemePreset("دودی بوکه", "🌫️", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xFFECEFF1.toInt(),
            secondary = 0xFF37474F.toInt(),
            pattern = PatternKind.Bokeh,
            textureAlpha = 0.14f
        ))
    )

    val widgetPresets = listOf(
        ThemePreset("شیشهٔ تیره", "🪟", ThemeConfig(ThemeKind.Solid, 0xE60E1116.toInt())),
        ThemePreset("سبز چراغ", "🏮", ThemeConfig(ThemeKind.Solid, 0xF20F3D2E.toInt())),
        ThemePreset("نیمه‌شب نقطه‌ای", "🌌", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xF20B1026.toInt(),
            secondary = 0xFFFFFFFF.toInt(),
            pattern = PatternKind.Dots,
            textureAlpha = 0.12f
        )),
        ThemePreset("کاغذ کرم", "📜", ThemeConfig(ThemeKind.Solid, 0xF2FFF8E1.toInt())),
        ThemePreset("فیروزه", "🧿", ThemeConfig(ThemeKind.Gradient, 0xF200838F.toInt(), 0xF2006064.toInt())),
        ThemePreset("زعفران", "🌼", ThemeConfig(ThemeKind.Solid, 0xF2FFB74D.toInt())),
        ThemePreset("دود خط‌دار", "🌫️", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xF220232A.toInt(),
            secondary = 0xFFFFFFFF.toInt(),
            pattern = PatternKind.Lines,
            textureAlpha = 0.10f
        )),
        ThemePreset("جنگل موج", "🌲", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xF2102018.toInt(),
            secondary = 0xFF46A758.toInt(),
            pattern = PatternKind.Waves,
            textureAlpha = 0.12f
        )),
        ThemePreset("ارغوان ستاره", "🪻", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xF22A0E3F.toInt(),
            secondary = 0xFFE1BEE7.toInt(),
            pattern = PatternKind.Stars,
            textureAlpha = 0.14f
        )),
        ThemePreset("مسی الماس", "🥉", ThemeConfig(
            kind = ThemeKind.Pattern,
            primary = 0xF27A4F2A.toInt(),
            secondary = 0xFFFFE0B2.toInt(),
            pattern = PatternKind.Diamonds,
            textureAlpha = 0.10f
        ))
    )

    fun swatchesFor(scope: ThemeScope): List<ThemeSwatch> = when (scope) {
        ThemeScope.App -> appSwatches
        ThemeScope.Board -> boardSwatches
        ThemeScope.NotePaper -> paperSwatches
        ThemeScope.WidgetNote, ThemeScope.WidgetTask -> widgetSwatches
    }

    fun presetsFor(scope: ThemeScope): List<ThemePreset> = when (scope) {
        ThemeScope.App -> appPresets
        ThemeScope.Board -> boardPresets
        ThemeScope.NotePaper -> paperPresets
        ThemeScope.WidgetNote, ThemeScope.WidgetTask -> widgetPresets
    }

    fun titleFor(scope: ThemeScope): String = when (scope) {
        ThemeScope.App -> "🎨 استودیوی تم اپ"
        ThemeScope.Board -> "🎨 استودیوی تابلو"
        ThemeScope.NotePaper -> "🎨 استودیوی کاغذ یادداشت"
        ThemeScope.WidgetNote -> "🎨 استودیوی ویجت یادداشت"
        ThemeScope.WidgetTask -> "🎨 استودیوی ویجت وظیفه"
    }

    fun previewLabel(scope: ThemeScope): String = when (scope) {
        ThemeScope.App -> "چراغ راه 🏮"
        ThemeScope.Board -> "📌 تابلو"
        ThemeScope.NotePaper -> "🗒️ کاغذ یادداشت"
        ThemeScope.WidgetNote -> "📝 ویجت یادداشت"
        ThemeScope.WidgetTask -> "✅ ویجت وظیفه"
    }

    fun previewHeight(scope: ThemeScope): Int = when (scope) {
        ThemeScope.App -> 130
        ThemeScope.Board -> 150
        ThemeScope.NotePaper -> 110
        ThemeScope.WidgetNote, ThemeScope.WidgetTask -> 90
    }

    fun isLight(config: ThemeConfig): Boolean {
        val lum = Color(config.primary).luminance() * (1f - config.dim.coerceIn(0f, 0.85f))
        return lum > 0.52f
    }

    fun contentColors(config: ThemeConfig): ThemeContentColors =
        if (isLight(config)) {
            ThemeContentColors(
                onBackground = Color(0xFF17201B),
                onSurface = Color(0xFF202820),
                muted = Color(0xFF5B665F),
                accent = Color(0xFF8A5A00)
            )
        } else {
            ThemeContentColors(
                onBackground = Color(0xFFF7FBF8),
                onSurface = Color(0xFFE6EDF3),
                muted = Color(0xFF8FA596),
                accent = Color(0xFFFFB74D)
            )
        }
}

private fun Color.luminance(): Float =
    0.2126f * red.coerceIn(0f, 1f) +
        0.7152f * green.coerceIn(0f, 1f) +
        0.0722f * blue.coerceIn(0f, 1f)

private fun luminanceInt(color: Int): Float =
    0.2126f * (AndroidColor.red(color) / 255f) +
        0.7152f * (AndroidColor.green(color) / 255f) +
        0.0722f * (AndroidColor.blue(color) / 255f)

private fun autoAccentInt(base: Int): Int =
    if (luminanceInt(base) > 0.55f) 0xFF1A1A1A.toInt() else 0xFFFFFFFF.toInt()

private fun hash01(i: Int, salt: Int): Float =
    abs(sin(i * 12.9898f + salt * 78.233f) * 43758.5453f) % 1f

@Composable
fun ThemeBackground(
    config: ThemeConfig,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        when (config.kind) {
            ThemeKind.Solid -> {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color(config.primary))
                )
            }

            ThemeKind.Gradient -> {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(config.primary),
                                    Color(config.secondary ?: config.primary)
                                )
                            )
                        )
                )
            }

            ThemeKind.Pattern -> {
                if (config.pattern == PatternKind.None) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color(config.primary))
                    )
                } else {
                    ThemePatternCanvas(
                        base = Color(config.primary),
                        accent = Color(config.secondary ?: autoAccentInt(config.primary)),
                        alpha = config.textureAlpha,
                        kind = config.pattern,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }

        if (config.dim > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = config.dim.coerceIn(0f, 0.85f)))
            )
        }
    }
}

@Composable
private fun ThemePatternCanvas(
    base: Color,
    accent: Color,
    alpha: Float,
    kind: PatternKind,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        drawRect(base)

        val col = accent.copy(alpha = alpha.coerceIn(0.02f, 0.45f))

        when (kind) {
            PatternKind.None -> Unit

            PatternKind.Dots -> {
                val step = 28.dp.toPx()
                val radius = 1.1.dp.toPx()
                var y = step / 2f
                var row = 0

                while (y < size.height) {
                    var x = if (row % 2 == 0) step / 2f else step

                    while (x < size.width) {
                        drawCircle(color = col, radius = radius, center = Offset(x, y))
                        x += step
                    }

                    y += step
                    row++
                }
            }

            PatternKind.Lines -> {
                val spacing = 30.dp.toPx()
                var y = spacing

                while (y < size.height) {
                    drawLine(
                        color = col,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += spacing
                }
            }

            PatternKind.Grid -> {
                val cell = 34.dp.toPx()

                var x = cell
                while (x < size.width) {
                    drawLine(
                        color = col,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += cell
                }

                var y = cell
                while (y < size.height) {
                    drawLine(
                        color = col,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += cell
                }
            }

            PatternKind.Diagonal -> {
                val spacing = 22.dp.toPx()
                var x = -size.height

                while (x < size.width) {
                    drawLine(
                        color = col,
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1f
                    )
                    x += spacing
                }
            }

            PatternKind.Linen -> {
                val spacing = 9.dp.toPx()
                val thin = col.copy(alpha = col.alpha * 0.55f)

                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = thin,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += spacing
                }

                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = thin,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += spacing
                }
            }

            PatternKind.Waves -> {
                val amplitude = 14.dp.toPx()
                val wavelength = 150.dp.toPx()
                val stepY = 38.dp.toPx()

                var y = stepY
                while (y < size.height + amplitude) {
                    val path = Path()
                    path.moveTo(0f, y)

                    var x = 0f
                    while (x <= size.width) {
                        val yy = y + sin((x / wavelength) * 2f * PI.toFloat()) * amplitude
                        path.lineTo(x, yy)
                        x += 8f
                    }

                    drawPath(path = path, color = col, style = Stroke(width = 1.4f))
                    y += stepY
                }
            }

            PatternKind.Stars -> {
                val baseR = 1.2.dp.toPx()

                for (i in 0 until 110) {
                    val x = hash01(i, 1) * size.width
                    val y = hash01(i, 2) * size.height
                    val r = if (i % 9 == 0) baseR * 1.7f else baseR
                    val starAlpha = if (i % 5 == 0) 0.9f else 0.55f

                    drawCircle(
                        color = col.copy(alpha = col.alpha * starAlpha),
                        radius = r,
                        center = Offset(x, y)
                    )
                }
            }

            PatternKind.Bokeh -> {
                for (i in 0 until 18) {
                    val x = hash01(i, 3) * size.width
                    val y = hash01(i, 4) * size.height
                    val r = (18f + (i % 5) * 14f).dp.toPx()

                    drawCircle(
                        color = col.copy(alpha = col.alpha * 0.22f),
                        radius = r,
                        center = Offset(x, y)
                    )
                }
            }

            PatternKind.Diamonds -> {
                val spacing = 42.dp.toPx()
                var x = -size.height

                while (x < size.width) {
                    drawLine(
                        color = col,
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1f
                    )

                    drawLine(
                        color = col,
                        start = Offset(x + size.height, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )

                    x += spacing
                }
            }
        }
    }
}

fun renderThemeBitmap(
    context: Context,
    config: ThemeConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap? {
    return try {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val density = context.resources.displayMetrics.density

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)

        when (config.kind) {
            ThemeKind.Solid -> {
                canvas.drawColor(config.primary)
            }

            ThemeKind.Gradient -> {
                val paint = Paint()
                paint.shader = LinearGradient(
                    0f,
                    0f,
                    w.toFloat(),
                    h.toFloat(),
                    config.primary,
                    config.secondary ?: config.primary,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }

            ThemeKind.Pattern -> {
                canvas.drawColor(config.primary)

                if (config.pattern != PatternKind.None) {
                    drawAndroidPattern(canvas, config, w, h, density)
                }
            }
        }

        if (config.dim > 0f) {
            val dimAlpha = (config.dim.coerceIn(0f, 0.85f) * 255f).toInt().coerceIn(1, 255)
            val dimPaint = Paint().apply {
                color = AndroidColor.argb(dimAlpha, 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dimPaint)
        }

        bmp
    } catch (_: Exception) {
        null
    }
}

private fun drawAndroidPattern(
    canvas: AndroidCanvas,
    config: ThemeConfig,
    w: Int,
    h: Int,
    density: Float
) {
    val accent = config.secondary ?: autoAccentInt(config.primary)
    val a = config.textureAlpha.coerceIn(0.02f, 0.45f)

    val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 1f * density
        color = AndroidColor.argb(
            (a * 255f).toInt().coerceIn(1, 255),
            AndroidColor.red(accent),
            AndroidColor.green(accent),
            AndroidColor.blue(accent)
        )
    }

    when (config.pattern) {
        PatternKind.None -> Unit

        PatternKind.Dots -> {
            val step = 28f * density
            val radius = 1.1f * density
            var y = step / 2f
            var row = 0

            while (y < h) {
                var x = if (row % 2 == 0) step / 2f else step

                while (x < w) {
                    canvas.drawCircle(x, y, radius, paint)
                    x += step
                }

                y += step
                row++
            }
        }

        PatternKind.Lines -> {
            val spacing = 30f * density
            var y = spacing

            while (y < h) {
                canvas.drawLine(0f, y, w.toFloat(), y, paint)
                y += spacing
            }
        }

        PatternKind.Grid -> {
            val cell = 34f * density

            var x = cell
            while (x < w) {
                canvas.drawLine(x, 0f, x, h.toFloat(), paint)
                x += cell
            }

            var y = cell
            while (y < h) {
                canvas.drawLine(0f, y, w.toFloat(), y, paint)
                y += cell
            }
        }

        PatternKind.Diagonal -> {
            val spacing = 22f * density
            var x = -h.toFloat()

            while (x < w) {
                canvas.drawLine(x, 0f, x + h, h.toFloat(), paint)
                x += spacing
            }
        }

        PatternKind.Linen -> {
            val spacing = 9f * density
            val thinAlpha = (a * 0.55f * 255f).toInt().coerceIn(1, 255)

            val thin = Paint().apply {
                isAntiAlias = true
                strokeWidth = 1f * density
                color = AndroidColor.argb(
                    thinAlpha,
                    AndroidColor.red(accent),
                    AndroidColor.green(accent),
                    AndroidColor.blue(accent)
                )
            }

            var x = 0f
            while (x < w) {
                canvas.drawLine(x, 0f, x, h.toFloat(), thin)
                x += spacing
            }

            var y = 0f
            while (y < h) {
                canvas.drawLine(0f, y, w.toFloat(), y, thin)
                y += spacing
            }
        }

        PatternKind.Waves -> {
            val amplitude = 14f * density
            val wavelength = 150f * density
            val stepY = 38f * density
            paint.strokeWidth = 1.4f * density

            var y = stepY
            while (y < h + amplitude) {
                val path = AndroidPath()
                path.moveTo(0f, y)

                var x = 0f
                while (x <= w) {
                    path.lineTo(x, y + sin((x / wavelength) * 2f * PI.toFloat()) * amplitude)
                    x += 8f * density
                }

                canvas.drawPath(path, paint)
                y += stepY
            }
        }

        PatternKind.Stars -> {
            val cell = 70f * density
            val count = ((w * h) / (cell * cell)).toInt().coerceIn(18, 160)
            val baseR = 1.2f * density

            for (i in 0 until count) {
                val x = hash01(i, 1) * w
                val y = hash01(i, 2) * h
                val r = if (i % 9 == 0) baseR * 1.7f else baseR
                val starAlpha = if (i % 5 == 0) 0.9f else 0.55f

                val p = Paint().apply {
                    isAntiAlias = true
                    color = AndroidColor.argb(
                        (a * starAlpha * 255f).toInt().coerceIn(1, 255),
                        AndroidColor.red(accent),
                        AndroidColor.green(accent),
                        AndroidColor.blue(accent)
                    )
                }

                canvas.drawCircle(x, y, r, p)
            }
        }

        PatternKind.Bokeh -> {
            val cell = 160f * density
            val count = ((w * h) / (cell * cell)).toInt().coerceIn(5, 24)

            for (i in 0 until count) {
                val x = hash01(i, 3) * w
                val y = hash01(i, 4) * h
                val r = (18f + (i % 5) * 14f) * density

                val p = Paint().apply {
                    isAntiAlias = true
                    color = AndroidColor.argb(
                        (a * 0.22f * 255f).toInt().coerceIn(1, 255),
                        AndroidColor.red(accent),
                        AndroidColor.green(accent),
                        AndroidColor.blue(accent)
                    )
                }

                canvas.drawCircle(x, y, r, p)
            }
        }

        PatternKind.Diamonds -> {
            val spacing = 42f * density
            var x = -h.toFloat()

            while (x < w) {
                canvas.drawLine(x, 0f, x + h, h.toFloat(), paint)
                canvas.drawLine(x + h, 0f, x, h.toFloat(), paint)
                x += spacing
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioDialog(
    scope: ThemeScope,
    initial: ThemeConfig,
    onDismiss: () -> Unit,
    onApply: (ThemeConfig) -> Unit,
    titleOverride: String? = null,
    defaultConfig: ThemeConfig? = null
) {
    val gold = Color(0xFFFFB74D)
    val muted = Color(0xFF8B949E)
    val text = Color(0xFFE6EDF3)

    val fallback = defaultConfig ?: ThemeKit.default(scope)
    var draft by remember(initial) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151A20),
        title = {
            Text(
                titleOverride ?: ThemeKit.titleFor(scope),
                color = gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ThemePreviewCard(
                    config = draft,
                    scope = scope,
                    dark = Color(0xFF1A1A1A),
                    light = Color(0xFFF7F7F7)
                )

                ThemeSectionLabel("استایل آماده", muted)
                ThemePresetRow(
                    presets = ThemeKit.presetsFor(scope),
                    selected = draft,
                    onSelect = { draft = it.config }
                )

                ThemeSectionLabel("نوع زمینه", muted)
                ThemeStyleChips(
                    selected = draft.kind,
                    gold = gold,
                    text = text
                ) { kind ->
                    draft = when (kind) {
                        ThemeKind.Solid -> draft.copy(
                            kind = ThemeKind.Solid,
                            pattern = PatternKind.None,
                            secondary = null
                        )

                        ThemeKind.Gradient -> {
                            if (draft.secondary == null) {
                                val g = ThemeKit.gradients.first()
                                draft.copy(
                                    kind = ThemeKind.Gradient,
                                    primary = g.from,
                                    secondary = g.to,
                                    pattern = PatternKind.None
                                )
                            } else {
                                draft.copy(
                                    kind = ThemeKind.Gradient,
                                    pattern = PatternKind.None
                                )
                            }
                        }

                        ThemeKind.Pattern -> draft.copy(
                            kind = ThemeKind.Pattern,
                            pattern = if (draft.pattern == PatternKind.None) {
                                PatternKind.Dots
                            } else {
                                draft.pattern
                            },
                            secondary = draft.secondary ?: autoAccentInt(draft.primary)
                        )
                    }
                }

                if (draft.kind == ThemeKind.Pattern) {
                    ThemeSectionLabel("طرح", muted)
                    ThemePatternChips(
                        selected = draft.pattern,
                        gold = gold,
                        text = text
                    ) {
                        draft = draft.copy(pattern = it)
                    }

                    ThemeSectionLabel("شدت طرح", muted)
                    ThemeSliderRow(
                        label = "شدت",
                        value = draft.textureAlpha,
                        min = 0.03f,
                        max = 0.40f,
                        text = text,
                        muted = muted
                    ) {
                        draft = draft.copy(textureAlpha = it)
                    }
                }

                ThemeSectionLabel("رنگ پایه", muted)
                ThemeSwatchGrid(
                    presets = ThemeKit.swatchesFor(scope),
                    selected = draft.primary,
                    gold = gold,
                    muted = muted
                ) {
                    draft = draft.copy(primary = it)
                }

                if (draft.kind == ThemeKind.Gradient) {
                    ThemeSectionLabel("گرادیان آماده", muted)
                    ThemeGradientStrip(
                        presets = ThemeKit.gradients,
                        selectedFrom = draft.primary,
                        selectedTo = draft.secondary ?: 0,
                        muted = muted
                    ) { g ->
                        draft = draft.copy(
                            kind = ThemeKind.Gradient,
                            primary = g.from,
                            secondary = g.to,
                            pattern = PatternKind.None
                        )
                    }
                }

                ThemeSectionLabel("تیرگی", muted)
                ThemeSliderRow(
                    label = "تیرگی",
                    value = draft.dim,
                    min = 0f,
                    max = 0.55f,
                    text = text,
                    muted = muted
                ) {
                    draft = draft.copy(dim = it)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft) }) {
                Text("اعمال", color = gold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { draft = fallback }) {
                    Text("پیش‌فرض", color = muted)
                }

                TextButton(onClick = onDismiss) {
                    Text("انصراف", color = muted)
                }
            }
        }
    )
}

@Composable
private fun ThemePreviewCard(
    config: ThemeConfig,
    scope: ThemeScope,
    dark: Color,
    light: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(ThemeKit.previewHeight(scope).dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(18.dp))
    ) {
        ThemeBackground(
            config = config,
            modifier = Modifier.fillMaxSize()
        )

        val fg = if (ThemeKit.isLight(config)) dark else light

        Column(Modifier.padding(14.dp)) {
            Text(
                "پیش‌نمایش",
                color = fg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                ThemeKit.previewLabel(scope),
                color = fg.copy(alpha = .75f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ThemeSectionLabel(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ThemePresetRow(
    presets: List<ThemePreset>,
    selected: ThemeConfig,
    onSelect: (ThemePreset) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(92.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            if (selected == preset.config) 2.dp else 1.dp,
                            if (selected == preset.config) {
                                Color(0xFFFFB74D)
                            } else {
                                Color.White.copy(alpha = .16f)
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(preset) }
                ) {
                    ThemeBackground(
                        config = preset.config,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    preset.name,
                    color = Color(0xFF8B949E),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ThemeStyleChips(
    selected: ThemeKind,
    gold: Color,
    text: Color,
    onSelect: (ThemeKind) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeKind.values().forEach { kind ->
            val label = when (kind) {
                ThemeKind.Solid -> "ساده"
                ThemeKind.Gradient -> "گرادیان"
                ThemeKind.Pattern -> "طرح"
            }

            ThemeChip(
                label = label,
                selected = selected == kind,
                gold = gold,
                text = text,
                onClick = { onSelect(kind) }
            )
        }
    }
}

@Composable
private fun ThemePatternChips(
    selected: PatternKind,
    gold: Color,
    text: Color,
    onSelect: (PatternKind) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PatternKind.values().forEach { kind ->
            ThemeChip(
                label = ThemeKit.patternLabel(kind),
                selected = selected == kind,
                gold = gold,
                text = text,
                onClick = { onSelect(kind) }
            )
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    gold: Color,
    text: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) gold else Color.White.copy(alpha = .08f))
            .border(
                1.dp,
                if (selected) gold else Color.White.copy(alpha = .14f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF111111) else text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ThemeSwatchGrid(
    presets: List<ThemeSwatch>,
    selected: Int,
    gold: Color,
    muted: Color,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { swatch ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(swatch.color))
                            .border(
                                if (selected == swatch.color) 2.dp else 1.dp,
                                if (selected == swatch.color) gold else Color.White.copy(alpha = .18f),
                                CircleShape
                            )
                            .clickable { onSelect(swatch.color) }
                    )
                }
            }
        }

        Text(
            presets.firstOrNull { it.color == selected }?.name ?: "دلخواه",
            color = muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ThemeGradientStrip(
    presets: List<ThemeGradient>,
    selectedFrom: Int,
    selectedTo: Int,
    muted: Color,
    onSelect: (ThemeGradient) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { g ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(92.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(g.from), Color(g.to))))
                        .border(
                            if (selectedFrom == g.from && selectedTo == g.to) 2.dp else 1.dp,
                            if (selectedFrom == g.from && selectedTo == g.to) {
                                Color(0xFFFFB74D)
                            } else {
                                Color.White.copy(alpha = .16f)
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(g) }
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    g.name,
                    color = muted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    text: Color,
    muted: Color,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = text,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                "${(value * 100).toInt()}٪",
                color = muted,
                fontSize = 11.sp
            )
        }

        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
