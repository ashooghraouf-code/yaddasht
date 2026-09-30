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

enum class ThemeKind { Solid, Gradient, Pattern }
enum class PatternKind { None, Dots, Lines, Grid, Diagonal, Linen, Waves, Stars, Bokeh, Diamonds }
enum class ThemeScope { App, Board, NotePaper, WidgetNote, WidgetTask }

data class ThemeConfig(
    val kind: ThemeKind = ThemeKind.Solid,
    val primary: Int = 0xFF0F3D2E.toInt(),
    val secondary: Int? = null,
    val pattern: PatternKind = PatternKind.None,
    val textureAlpha: Float = 0.10f,
    val dim: Float = 0f
)
data class ThemeSwatch(val name: String, val emoji: String, val color: Int)
data class ThemeGradient(val name: String, val emoji: String, val from: Int, val to: Int)
data class ThemePreset(val name: String, val emoji: String, val config: ThemeConfig)
data class ThemeContentColors(
    val onBackground: Color, val onSurface: Color, val muted: Color, val accent: Color
)

object ThemeKit {
    private const val PREFS = "yaddasht_theme_kit"
    private const val LEGACY_HOME_PREFS = "home_theme_prefs"
    private const val LEGACY_HOME_KEY = "home_theme_config"
    private val DefaultApp = 0xFF0F3D2E.toInt()
    private val DefaultBoard = 0xFF143D2B.toInt()
    private val DefaultPaper = 0xFFFFF8E1.toInt()
    private val DefaultWidget = 0xE60E1116.toInt()

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun scopeKey(s: ThemeScope, id: Long? = null) =
        if (id != null && id > 0L) "theme_${s.name}_$id" else "theme_${s.name}"

    fun default(s: ThemeScope): ThemeConfig = when (s) {
        ThemeScope.App -> ThemeConfig(ThemeKind.Solid, DefaultApp)
        ThemeScope.Board -> ThemeConfig(ThemeKind.Solid, DefaultBoard)
        ThemeScope.NotePaper -> ThemeConfig(ThemeKind.Solid, DefaultPaper)
        ThemeScope.WidgetNote, ThemeScope.WidgetTask -> ThemeConfig(ThemeKind.Solid, DefaultWidget)
    }

    fun get(c: Context, s: ThemeScope): ThemeConfig {
        prefs(c).getString(scopeKey(s), null)?.let { if (!it.isNullOrBlank()) decode(it)?.let { d -> return d } }
        if (s == ThemeScope.App) legacyHomeConfig(c)?.let { return it }
        return default(s)
    }
    fun set(c: Context, s: ThemeScope, cfg: ThemeConfig) {
        prefs(c).edit().putString(scopeKey(s), encode(cfg)).apply()
        if (s == ThemeScope.App) writeLegacyHomeConfig(c, cfg)
    }

    fun getBoard(c: Context, boardId: Long): ThemeConfig {
        if (boardId > 0L) prefs(c).getString(scopeKey(ThemeScope.Board, boardId), null)?.let { if (!it.isNullOrBlank()) decode(it)?.let { d -> return d } }
        return get(c, ThemeScope.Board)
    }
    fun setBoard(c: Context, boardId: Long, cfg: ThemeConfig) {
        if (boardId > 0L) prefs(c).edit().putString(scopeKey(ThemeScope.Board, boardId), encode(cfg)).apply()
        else set(c, ThemeScope.Board, cfg)
    }

    fun getNotePaper(c: Context, noteId: Long): ThemeConfig {
        if (noteId > 0L) prefs(c).getString(scopeKey(ThemeScope.NotePaper, noteId), null)?.let { if (!it.isNullOrBlank()) decode(it)?.let { d -> return d } }
        return get(c, ThemeScope.NotePaper)
    }
    fun setNotePaper(c: Context, noteId: Long, cfg: ThemeConfig) {
        if (noteId > 0L) prefs(c).edit().putString(scopeKey(ThemeScope.NotePaper, noteId), encode(cfg)).apply()
        else set(c, ThemeScope.NotePaper, cfg)
    }
    fun clearNotePaper(c: Context, noteId: Long) {
        if (noteId > 0L) prefs(c).edit().remove(scopeKey(ThemeScope.NotePaper, noteId)).apply()
    }
    /** تم یادداشت: سفارشیِ همان یادداشت، وگرنه رنگ پایه از ایندکس */
    fun getNotePaperOr(c: Context, noteId: Long, baseIndex: Int): ThemeConfig {
        if (noteId > 0L) prefs(c).getString(scopeKey(ThemeScope.NotePaper, noteId), null)?.let {
            if (!it.isNullOrBlank()) decode(it)?.let { d -> return d }
        }
        return paperConfigForIndex(baseIndex)
    }

    fun getWidgetNote(c: Context) = get(c, ThemeScope.WidgetNote)
    fun setWidgetNote(c: Context, cfg: ThemeConfig) = set(c, ThemeScope.WidgetNote, cfg)
    fun getWidgetTask(c: Context) = get(c, ThemeScope.WidgetTask)
    fun setWidgetTask(c: Context, cfg: ThemeConfig) = set(c, ThemeScope.WidgetTask, cfg)

    private fun encode(cfg: ThemeConfig): String = JSONObject().apply {
        put("kind", cfg.kind.name); put("primary", cfg.primary)
        if (cfg.secondary == null) put("secondary", JSONObject.NULL) else put("secondary", cfg.secondary)
        put("pattern", cfg.pattern.name); put("textureAlpha", cfg.textureAlpha.toDouble()); put("dim", cfg.dim.toDouble())
    }.toString()

    private fun decode(raw: String): ThemeConfig? = try {
        val o = JSONObject(raw)
        val kind = runCatching { ThemeKind.valueOf(o.optString("kind", ThemeKind.Solid.name)) }.getOrDefault(ThemeKind.Solid)
        val pattern = runCatching { PatternKind.valueOf(o.optString("pattern", PatternKind.None.name)) }.getOrDefault(PatternKind.None)
        val secondary = if (o.has("secondary") && !o.isNull("secondary")) o.optInt("secondary") else null
        ThemeConfig(kind, o.optInt("primary", DefaultApp), secondary, pattern,
            o.optDouble("textureAlpha", 0.10).toFloat(), o.optDouble("dim", 0.0).toFloat())
    } catch (_: Exception) { null }

    private fun legacyHomeConfig(c: Context): ThemeConfig? = try {
        val raw = c.getSharedPreferences(LEGACY_HOME_PREFS, Context.MODE_PRIVATE).getString(LEGACY_HOME_KEY, null) ?: return null
        val p = raw.split("|"); if (p.size < 6) return null
        val kind = when (p[0]) { "Gradient" -> ThemeKind.Gradient; "Pattern" -> ThemeKind.Pattern; else -> ThemeKind.Solid }
        val pattern = runCatching { PatternKind.valueOf(p[3]) }.getOrDefault(PatternKind.None)
        ThemeConfig(kind, p[1].toIntOrNull() ?: DefaultApp, if (p[2] == "n") null else p[2].toIntOrNull(), pattern,
            p[4].toFloatOrNull() ?: 0.10f, p[5].toFloatOrNull() ?: 0f)
    } catch (_: Exception) { null }

    private fun writeLegacyHomeConfig(c: Context, cfg: ThemeConfig) {
        val raw = buildString {
            append(cfg.kind.name).append('|').append(cfg.primary).append('|')
            append(cfg.secondary?.toString() ?: "n").append('|').append(cfg.pattern.name).append('|')
            append(cfg.textureAlpha).append('|').append(cfg.dim)
        }
        c.getSharedPreferences(LEGACY_HOME_PREFS, Context.MODE_PRIVATE).edit().putString(LEGACY_HOME_KEY, raw).apply()
    }

    val patternLabels = listOf(
        PatternKind.None to "بدون طرح", PatternKind.Dots to "نقاط", PatternKind.Lines to "خط‌دار",
        PatternKind.Grid to "شطرنجی", PatternKind.Diagonal to "مورب", PatternKind.Linen to "کتان",
        PatternKind.Waves to "موج", PatternKind.Stars to "ستاره", PatternKind.Bokeh to "بوکه", PatternKind.Diamonds to "الماس"
    )
    fun patternLabel(k: PatternKind) = patternLabels.firstOrNull { it.first == k }?.second ?: k.name

    val appSwatches = listOf(
        ThemeSwatch("کاغذ کرم","📜",0xFFFFF8E1.toInt()), ThemeSwatch("سپیا","🧻",0xFFF4ECD8.toInt()),
        ThemeSwatch("چای نبات","🍵",0xFFE8DCC4.toInt()), ThemeSwatch("ابریشم","🤍",0xFFFFFBF0.toInt()),
        ThemeSwatch("کاغذ پوستی","📃",0xFFEFE6D3.toInt()), ThemeSwatch("مومیایی","🏺",0xFFE0D3B8.toInt()),
        ThemeSwatch("شب","🌙",0xFF121212.toInt()), ThemeSwatch("نفتی","🛢️",0xFF0E1116.toInt()),
        ThemeSwatch("جنگلان","🌲",0xFF102018.toInt()), ThemeSwatch("زغالی","🪨",0xFF1A1A1A.toInt()),
        ThemeSwatch("نیمه‌شب","🌌",0xFF0B1026.toInt()), ThemeSwatch("دود","🌫️",0xFF20232A.toInt()),
        ThemeSwatch("سبز چراغ","🏮",0xFF0F3D2E.toInt()), ThemeSwatch("باغ","🌿",0xFF1B5E20.toInt()),
        ThemeSwatch("زیتون","🫒",0xFF33691E.toInt()), ThemeSwatch("کاج","🌲",0xFF0B3D2C.toInt()),
        ThemeSwatch("یشم","💎",0xFF2E7D52.toInt()), ThemeSwatch("جنگل بارانی","🌧️",0xFF143D2B.toInt()),
        ThemeSwatch("آسمان","☁️",0xFFE3F2FD.toInt()), ThemeSwatch("دریا","🌊",0xFF01579B.toInt()),
        ThemeSwatch("فیروزه","🧿",0xFF00838F.toInt()), ThemeSwatch("نیلی","🔵",0xFF1A237E.toInt()),
        ThemeSwatch("اقیانوس","🐋",0xFF023E5C.toInt()), ThemeSwatch("مه","🌫️",0xFFCFD8DC.toInt()),
        ThemeSwatch("آفتاب","☀️",0xFFFFF3E0.toInt()), ThemeSwatch("نارنج","🍊",0xFFE65100.toInt()),
        ThemeSwatch("گل‌گون","🌸",0xFFF8BBD0.toInt()), ThemeSwatch("عنابی","🍇",0xFF880E4F.toInt()),
        ThemeSwatch("زعفران","🌼",0xFFFFB74D.toInt()), ThemeSwatch("آجر","🧱",0xFFB71C1C.toInt()),
        ThemeSwatch("فیروزه ایرانی","🕌",0xFF00A6A6.toInt()), ThemeSwatch("لاجورد","🔷",0xFF283593.toInt()),
        ThemeSwatch("زرشک","🍒",0xFF8E1B3A.toInt()), ThemeSwatch("کویر","🏜️",0xFF8D6E63.toInt()),
        ThemeSwatch("مس","🥉",0xFFB87333.toInt()), ThemeSwatch("زیتون ایرانی","🌿",0xFF6B8E23.toInt()),
        ThemeSwatch("ارغوانی","🪻",0xFF6A1B9A.toInt()), ThemeSwatch("فیروزهٔ روشن","",0xFF4DD0E1.toInt()),
        ThemeSwatch("لیمویی تیره","🍈",0xFF9E9D24.toInt()), ThemeSwatch("صورتی چرخی","🍥",0xFFEC407A.toInt()),
        ThemeSwatch("آبی بادامی","🫧",0xFF4FC3F7.toInt()), ThemeSwatch("قهوه شیری","🥛",0xFFA1887F.toInt())
    )
    val boardSwatches = listOf(
        ThemeSwatch("تابلوی سبز","🟩",0xFF143D2B.toInt()), ThemeSwatch("گچ‌تخته","🧑‍🏫",0xFF0B3D2C.toInt()),
        ThemeSwatch("تخته سیاه","⬛",0xFF111111.toInt()), ThemeSwatch("چوب گرم","🪵",0xFF6B4A2B.toInt()),
        ThemeSwatch("کاغذ کاهی","📜",0xFFE0D3B8.toInt()), ThemeSwatch("سنگ slate","🪨",0xFF2B3137.toInt()),
        ThemeSwatch("نیلوفر تیره","🌌",0xFF18244A.toInt()), ThemeSwatch("فیروزه تیره","🧿",0xFF074B52.toInt()),
        ThemeSwatch("قهوه تلخ","☕",0xFF3E2723.toInt()), ThemeSwatch("کرم روشن","🥛",0xFFFFF3D6.toInt()),
        ThemeSwatch("مه سرد","🌫️",0xFFD7DEE8.toInt()), ThemeSwatch("زغالی","🌑",0xFF1A1A1A.toInt()),
        ThemeSwatch("زرشکی تیره","🍷",0xFF4A0E2A.toInt()), ThemeSwatch("نیلی سلطنتی","🔷",0xFF1A237E.toInt()),
        ThemeSwatch("زیتونی","🫒",0xFF33691E.toInt()), ThemeSwatch("مسی","🥉",0xFF7A4F2A.toInt())
    )
    val paperSwatches = listOf(
        ThemeSwatch("کاغذ کرم","📜",0xFFFFF8E1.toInt()), ThemeSwatch("سپیا","🧻",0xFFF4ECD8.toInt()),
        ThemeSwatch("چای نبات","🍵",0xFFE8DCC4.toInt()), ThemeSwatch("ابریشم","🤍",0xFFFFFBF0.toInt()),
        ThemeSwatch("کاغذ پوستی","📃",0xFFEFE6D3.toInt()), ThemeSwatch("سفید دفتری","📄",0xFFFDFDFD.toInt()),
        ThemeSwatch("آسمانی","☁️",0xFFE3F2FD.toInt()), ThemeSwatch("نعنایی","🌿",0xFFE8F5E9.toInt()),
        ThemeSwatch("اسطوخودوس","💜",0xFFEDE7F6.toInt()), ThemeSwatch("هلویی","🍑",0xFFFFF3E0.toInt()),
        ThemeSwatch("لیمویی","🍋",0xFFF9FBE7.toInt()), ThemeSwatch("دودی","🌫️",0xFFECEFF1.toInt()),
        ThemeSwatch("صورتی ملایم","🌸",0xFFFFE0EC.toInt()), ThemeSwatch("فیروزه‌ای","🩵",0xFFE0F7FA.toInt()),
        ThemeSwatch("کهربایی","🟠",0xFFFFE0B2.toInt()), ThemeSwatch("یاسی","🪻",0xFFF3E5F5.toInt())
    )
    val widgetSwatches = listOf(
        ThemeSwatch("شیشهٔ تیره","🪟",0xE60E1116.toInt()), ThemeSwatch("سبز چراغ","🏮",0xF20F3D2E.toInt()),
        ThemeSwatch("نیمه‌شب","🌌",0xF20B1026.toInt()), ThemeSwatch("فیروزه","🧿",0xF200838F.toInt()),
        ThemeSwatch("زعفران","🌼",0xF2FFB74D.toInt()), ThemeSwatch("کاغذ کرم","📜",0xF2FFF8E1.toInt()),
        ThemeSwatch("مه","🌫️",0xF2CFD8DC.toInt()), ThemeSwatch("زغالی","🌑",0xF21A1A1A.toInt()),
        ThemeSwatch("نیلی","🔵",0xF21A237E.toInt()), ThemeSwatch("عنابی","🍇",0xF2880E4F.toInt()),
        ThemeSwatch("جنگل","🌲",0xF2102018.toInt()), ThemeSwatch("دریا","🌊",0xF201579B.toInt()),
        ThemeSwatch("ارغوانی","🪻",0xF26A1B9A.toInt()), ThemeSwatch("مسی","🥉",0xF2B87333.toInt())
    )
    val gradients = listOf(
        ThemeGradient("سپیده‌دم","🌅",0xFF0F2027.toInt(),0xFF2C5364.toInt()),
        ThemeGradient("جنگل مه‌آلود","🌫️",0xFF134E5E.toInt(),0xFF0F2027.toInt()),
        ThemeGradient("شب تار","🌑",0xFF090909.toInt(),0xFF1F1F1F.toInt()),
        ThemeGradient("باغ سبز","🌿",0xFF0F3D2E.toInt(),0xFF1B5E20.toInt()),
        ThemeGradient("آسمان نیلی","🔵",0xFF1A237E.toInt(),0xFF0D47A1.toInt()),
        ThemeGradient("غروب کویر","🏜️",0xFF8D6E63.toInt(),0xFFBF360C.toInt()),
        ThemeGradient("زعفران","🌼",0xFFFFB74D.toInt(),0xFFE65100.toInt()),
        ThemeGradient("یاقوت","❤️‍🔥",0xFF880E4F.toInt(),0xFF4A148C.toInt()),
        ThemeGradient("فیروزه","🧿",0xFF00838F.toInt(),0xFF006064.toInt()),
        ThemeGradient("کاغذ و چای","🍵",0xFFFFF8E1.toInt(),0xFFE0D3B8.toInt()),
        ThemeGradient("ارغوان","🪻",0xFF6A1B9A.toInt(),0xFFAD1457.toInt()),
        ThemeGradient("اقیانوس","🐋",0xFF023E5C.toInt(),0xFF006064.toInt())
    )

    val appPresets = listOf(
        ThemePreset("سبز چراغ","🏮",ThemeConfig(ThemeKind.Solid,0xFF0F3D2E.toInt())),
        ThemePreset("شب نقطه‌ای","🌌",ThemeConfig(ThemeKind.Pattern,0xFF0B1026.toInt(),0xFFFFFFFF.toInt(),PatternKind.Stars,0.14f)),
        ThemePreset("کاغذ خط‌دار","📜",ThemeConfig(ThemeKind.Pattern,0xFFFFF8E1.toInt(),0xFF1A237E.toInt(),PatternKind.Lines,0.12f)),
        ThemePreset("جنگل مه‌آلود","🌫️",ThemeConfig(ThemeKind.Gradient,0xFF134E5E.toInt(),0xFF0F2027.toInt())),
        ThemePreset("کتان گرم","🧵",ThemeConfig(ThemeKind.Pattern,0xFFE8DCC4.toInt(),0xFF1A1A1A.toInt(),PatternKind.Linen,0.09f)),
        ThemePreset("موج آرام","🌊",ThemeConfig(ThemeKind.Pattern,0xFFE3F2FD.toInt(),0xFF01579B.toInt(),PatternKind.Waves,0.12f)),
        ThemePreset("بوکهٔ شب","✨",ThemeConfig(ThemeKind.Pattern,0xFF121212.toInt(),0xFFFFB74D.toInt(),PatternKind.Bokeh,0.18f)),
        ThemePreset("الماس فیروزه","🔷",ThemeConfig(ThemeKind.Pattern,0xFF00838F.toInt(),0xFFFFFFFF.toInt(),PatternKind.Diamonds,0.10f)),
        ThemePreset("ارغوان ستاره","🪻",ThemeConfig(ThemeKind.Pattern,0xFF2A0E3F.toInt(),0xFFE1BEE7.toInt(),PatternKind.Stars,0.16f)),
        ThemePreset("کویر مورب","🏜️",ThemeConfig(ThemeKind.Pattern,0xFF8D6E63.toInt(),0xFFFFE0B2.toInt(),PatternKind.Diagonal,0.12f))
    )
    val boardPresets = listOf(
        ThemePreset("تابلوی سبز","🟩",ThemeConfig(ThemeKind.Solid,0xFF143D2B.toInt())),
        ThemePreset("گچ‌تخته","🧑🏫",ThemeConfig(ThemeKind.Pattern,0xFF0B3D2C.toInt(),0xFFFFFFFF.toInt(),PatternKind.Grid,0.12f)),
        ThemePreset("چای نبات","🍵",ThemeConfig(ThemeKind.Pattern,0xFFE8DCC4.toInt(),0xFF1A1A1A.toInt(),PatternKind.Linen,0.09f)),
        ThemePreset("نیمه‌شب ستاره","🌌",ThemeConfig(ThemeKind.Pattern,0xFF0B1026.toInt(),0xFFFFFFFF.toInt(),PatternKind.Stars,0.15f)),
        ThemePreset("شطرنجی مهندسی","📐",ThemeConfig(ThemeKind.Pattern,0xFFE0D3B8.toInt(),0xFF1A237E.toInt(),PatternKind.Grid,0.12f)),
        ThemePreset("موج دریایی","🌊",ThemeConfig(ThemeKind.Pattern,0xFF023E5C.toInt(),0xFFFFFFFF.toInt(),PatternKind.Waves,0.12f)),
        ThemePreset("الماس تاریک","♦️",ThemeConfig(ThemeKind.Pattern,0xFF111111.toInt(),0xFFFFB74D.toInt(),PatternKind.Diamonds,0.10f)),
        ThemePreset("بوکهٔ تابلو","✨",ThemeConfig(ThemeKind.Pattern,0xFF1A1A1A.toInt(),0xFF2E7D52.toInt(),PatternKind.Bokeh,0.18f)),
        ThemePreset("چوب گرم مورب","🪵",ThemeConfig(ThemeKind.Pattern,0xFF6B4A2B.toInt(),0xFFFFE0B2.toInt(),PatternKind.Diagonal,0.12f)),
        ThemePreset("نیلی نقاط","🔷",ThemeConfig(ThemeKind.Pattern,0xFF18244A.toInt(),0xFF9FA8DA.toInt(),PatternKind.Dots,0.14f))
    )
    val paperPresets = listOf(
        ThemePreset("کاغذ کرم","📜",ThemeConfig(ThemeKind.Solid,0xFFFFF8E1.toInt())),
        ThemePreset("خط‌دار کلاسیک","📘",ThemeConfig(ThemeKind.Pattern,0xFFFFFDF5.toInt(),0xFF1A237E.toInt(),PatternKind.Lines,0.14f)),
        ThemePreset("نقطه‌ای بولت","🔘",ThemeConfig(ThemeKind.Pattern,0xFFFFFBF0.toInt(),0xFF5B665F.toInt(),PatternKind.Dots,0.14f)),
        ThemePreset("شطرنجی کاغذ","🗂️",ThemeConfig(ThemeKind.Pattern,0xFFF4ECD8.toInt(),0xFF8D6E63.toInt(),PatternKind.Grid,0.12f)),
        ThemePreset("کاغذ کاهی","🧻",ThemeConfig(ThemeKind.Pattern,0xFFE0D3B8.toInt(),0xFF3E2723.toInt(),PatternKind.Linen,0.09f)),
        ThemePreset("آسمان خط‌دار","☁️",ThemeConfig(ThemeKind.Pattern,0xFFE3F2FD.toInt(),0xFF01579B.toInt(),PatternKind.Lines,0.12f)),
        ThemePreset("نعنایی نقطه‌ای","🌿",ThemeConfig(ThemeKind.Pattern,0xFFE8F5E9.toInt(),0xFF2E7D52.toInt(),PatternKind.Dots,0.13f)),
        ThemePreset("اسطوخودوس مورب","💜",ThemeConfig(ThemeKind.Pattern,0xFFEDE7F6.toInt(),0xFF4A148C.toInt(),PatternKind.Diagonal,0.10f)),
        ThemePreset("هلویی کتان","🍑",ThemeConfig(ThemeKind.Pattern,0xFFFFF3E0.toInt(),0xFFBF360C.toInt(),PatternKind.Linen,0.08f)),
        ThemePreset("سپیا","🏺",ThemeConfig(ThemeKind.Solid,0xFFF4ECD8.toInt())),
        ThemePreset("صورتی نقاط","🌸",ThemeConfig(ThemeKind.Pattern,0xFFFFE0EC.toInt(),0xFFAD1457.toInt(),PatternKind.Dots,0.13f)),
        ThemePreset("فیروزه‌ای خط‌دار","🩵",ThemeConfig(ThemeKind.Pattern,0xFFE0F7FA.toInt(),0xFF006064.toInt(),PatternKind.Lines,0.12f)),
        ThemePreset("کهربایی موج","🟠",ThemeConfig(ThemeKind.Pattern,0xFFFFE0B2.toInt(),0xFFE65100.toInt(),PatternKind.Waves,0.12f)),
        ThemePreset("یاسی شطرنجی","🪻",ThemeConfig(ThemeKind.Pattern,0xFFF3E5F5.toInt(),0xFF6A1B9A.toInt(),PatternKind.Grid,0.10f)),
        ThemePreset("لیمویی مورب","🍋",ThemeConfig(ThemeKind.Pattern,0xFFF9FBE7.toInt(),0xFF33691E.toInt(),PatternKind.Diagonal,0.10f)),
        ThemePreset("دودی بوکه","🌫️",ThemeConfig(ThemeKind.Pattern,0xFFECEFF1.toInt(),0xFF37474F.toInt(),PatternKind.Bokeh,0.14f))
    )
    val widgetPresets = listOf(
        ThemePreset("شیشهٔ تیره","🪟",ThemeConfig(ThemeKind.Solid,0xE60E1116.toInt())),
        ThemePreset("سبز چراغ","🏮",ThemeConfig(ThemeKind.Solid,0xF20F3D2E.toInt())),
        ThemePreset("نیمه‌شب نقطه‌ای","🌌",ThemeConfig(ThemeKind.Pattern,0xF20B1026.toInt(),0xFFFFFFFF.toInt(),PatternKind.Dots,0.12f)),
        ThemePreset("کاغذ کرم","📜",ThemeConfig(ThemeKind.Solid,0xF2FFF8E1.toInt())),
        ThemePreset("فیروزه","🧿",ThemeConfig(ThemeKind.Gradient,0xF200838F.toInt(),0xF2006064.toInt())),
        ThemePreset("زعفران","🌼",ThemeConfig(ThemeKind.Solid,0xF2FFB74D.toInt())),
        ThemePreset("دود خط‌دار","🌫️",ThemeConfig(ThemeKind.Pattern,0xF220232A.toInt(),0xFFFFFFFF.toInt(),PatternKind.Lines,0.10f)),
        ThemePreset("جنگل موج","🌲",ThemeConfig(ThemeKind.Pattern,0xF2102018.toInt(),0xFF46A758.toInt(),PatternKind.Waves,0.12f)),
        ThemePreset("ارغوان ستاره","🪻",ThemeConfig(ThemeKind.Pattern,0xF22A0E3F.toInt(),0xFFE1BEE7.toInt(),PatternKind.Stars,0.14f)),
        ThemePreset("مسی الماس","🥉",ThemeConfig(ThemeKind.Pattern,0xF27A4F2A.toInt(),0xFFFFE0B2.toInt(),PatternKind.Diamonds,0.10f))
    )

    fun swatchesFor(s: ThemeScope) = when (s) {
        ThemeScope.App -> appSwatches; ThemeScope.Board -> boardSwatches
        ThemeScope.NotePaper -> paperSwatches; ThemeScope.WidgetNote, ThemeScope.WidgetTask -> widgetSwatches
    }
    fun presetsFor(s: ThemeScope) = when (s) {
        ThemeScope.App -> appPresets; ThemeScope.Board -> boardPresets
        ThemeScope.NotePaper -> paperPresets; ThemeScope.WidgetNote, ThemeScope.WidgetTask -> widgetPresets
    }
    fun titleFor(s: ThemeScope) = when (s) {
        ThemeScope.App -> "🎨 استودیوی تم اپ"; ThemeScope.Board -> "🎨 استودیوی تابلو"
        ThemeScope.NotePaper -> "🎨 استودیوی کاغذ یادداشت"; ThemeScope.WidgetNote -> "🎨 استودیوی ویجت یادداشت"
        ThemeScope.WidgetTask -> "🎨 استودیوی ویجت وظیفه"
    }
    fun previewLabel(s: ThemeScope) = when (s) {
        ThemeScope.App -> "چراغ راه 🏮"; ThemeScope.Board -> "📌 تابلو"
        ThemeScope.NotePaper -> "🗒️ کاغذ یادداشت"; ThemeScope.WidgetNote -> "📝 ویجت یادداشت"
        ThemeScope.WidgetTask -> "✅ ویجت وظیفه"
    }
    fun previewHeight(s: ThemeScope) = when (s) {
        ThemeScope.App -> 130; ThemeScope.Board -> 150; ThemeScope.NotePaper -> 110
        ThemeScope.WidgetNote, ThemeScope.WidgetTask -> 90
    }
    fun isLight(cfg: ThemeConfig): Boolean {
        val lum = Color(cfg.primary).luminance() * (1f - cfg.dim.coerceIn(0f, 0.85f))
        return lum > 0.52f
    }
    fun contentColors(cfg: ThemeConfig) = if (isLight(cfg))
        ThemeContentColors(Color(0xFF17201B), Color(0xFF202820), Color(0xFF5B665F), Color(0xFF8A5A00))
    else ThemeContentColors(Color(0xFFF7FBF8), Color(0xFFE6EDF3), Color(0xFF8FA596), Color(0xFFFFB74D))
}

private fun Color.luminance(): Float = 0.2126f * red.coerceIn(0f,1f) + 0.7152f * green.coerceIn(0f,1f) + 0.0722f * blue.coerceIn(0f,1f)
private fun luminanceInt(c: Int): Float = 0.2126f*(AndroidColor.red(c)/255f)+0.7152f*(AndroidColor.green(c)/255f)+0.0722f*(AndroidColor.blue(c)/255f)
private fun autoAccentInt(base: Int) = if (luminanceInt(base) > 0.55f) 0xFF1A1A1A.toInt() else 0xFFFFFFFF.toInt()
private fun hash01(i: Int, salt: Int): Float = abs(sin(i*12.9898f + salt*78.233f)*43758.5453f) % 1f

@Composable
fun ThemeBackground(config: ThemeConfig, modifier: Modifier = Modifier) {
    Box(modifier) {
        when (config.kind) {
            ThemeKind.Solid -> Box(Modifier.matchParentSize().background(Color(config.primary)))
            ThemeKind.Gradient -> Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(config.primary), Color(config.secondary ?: config.primary)))))
            ThemeKind.Pattern -> if (config.pattern == PatternKind.None) Box(Modifier.matchParentSize().background(Color(config.primary)))
            else ThemePatternCanvas(Color(config.primary), Color(config.secondary ?: autoAccentInt(config.primary)), config.textureAlpha, config.pattern, Modifier.matchParentSize())
        }
        if (config.dim > 0f) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = config.dim.coerceIn(0f,0.85f))))
    }
}

@Composable
private fun ThemePatternCanvas(base: Color, accent: Color, alpha: Float, kind: PatternKind, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(base)
        val col = accent.copy(alpha = alpha.coerceIn(0.02f, 0.45f))
        when (kind) {
            PatternKind.None -> Unit
            PatternKind.Dots -> { val step=28.dp.toPx(); val r=1.1.dp.toPx(); var y=step/2f; var row=0
                while(y<size.height){ var x=if(row%2==0) step/2f else step; while(x<size.width){ drawCircle(col,r,Offset(x,y)); x+=step }; y+=step; row++ } }
            PatternKind.Lines -> { val sp=30.dp.toPx(); var y=sp; while(y<size.height){ drawLine(col,Offset(0f,y),Offset(size.width,y),1f); y+=sp } }
            PatternKind.Grid -> { val c=34.dp.toPx(); var x=c; while(x<size.width){ drawLine(col,Offset(x,0f),Offset(x,size.height),1f); x+=c }; var y=c; while(y<size.height){ drawLine(col,Offset(0f,y),Offset(size.width,y),1f); y+=c } }
            PatternKind.Diagonal -> { val sp=22.dp.toPx(); var x=-size.height; while(x<size.width){ drawLine(col,Offset(x,0f),Offset(x+size.height,size.height),1f); x+=sp } }
            PatternKind.Linen -> { val sp=9.dp.toPx(); val thin=col.copy(alpha=col.alpha*0.55f); var x=0f; while(x<size.width){ drawLine(thin,Offset(x,0f),Offset(x,size.height),1f); x+=sp }; var y=0f; while(y<size.height){ drawLine(thin,Offset(0f,y),Offset(size.width,y),1f); y+=sp } }
            PatternKind.Waves -> { val amp=14.dp.toPx(); val wl=150.dp.toPx(); val sy=38.dp.toPx(); var y=sy
                while(y<size.height+amp){ val p=Path(); p.moveTo(0f,y); var x=0f; while(x<=size.width){ p.lineTo(x, y+sin((x/wl)*2f*PI.toFloat())*amp); x+=8f }; drawPath(p,col,style=Stroke(1.4f)); y+=sy } }
            PatternKind.Stars -> { val br=1.2.dp.toPx(); for(i in 0 until 110){ val x=hash01(i,1)*size.width; val y=hash01(i,2)*size.height; val r=if(i%9==0) br*1.7f else br; val sa=if(i%5==0)0.9f else 0.55f; drawCircle(col.copy(alpha=col.alpha*sa),r,Offset(x,y)) } }
            PatternKind.Bokeh -> { for(i in 0 until 18){ val x=hash01(i,3)*size.width; val y=hash01(i,4)*size.height; val r=(18f+(i%5)*14f).dp.toPx(); drawCircle(col.copy(alpha=col.alpha*0.22f),r,Offset(x,y)) } }
            PatternKind.Diamonds -> { val sp=42.dp.toPx(); var x=-size.height; while(x<size.width){ drawLine(col,Offset(x,0f),Offset(x+size.height,size.height),1f); drawLine(col,Offset(x+size.height,0f),Offset(x,size.height),1f); x+=sp } }
        }
    }
}

fun renderThemeBitmap(context: Context, config: ThemeConfig, widthPx: Int, heightPx: Int): Bitmap? = try {
    val w=widthPx.coerceAtLeast(1); val h=heightPx.coerceAtLeast(1); val d=context.resources.displayMetrics.density
    val bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); val cv=AndroidCanvas(bmp)
    when(config.kind){
        ThemeKind.Solid -> cv.drawColor(config.primary)
        ThemeKind.Gradient -> { val pt=Paint(); pt.shader=LinearGradient(0f,0f,w.toFloat(),h.toFloat(),config.primary,config.secondary?:config.primary,Shader.TileMode.CLAMP); cv.drawRect(0f,0f,w.toFloat(),h.toFloat(),pt) }
        ThemeKind.Pattern -> { cv.drawColor(config.primary); if(config.pattern!=PatternKind.None) drawAndroidPattern(cv,config,w,h,d) }
    }
    if(config.dim>0f){ val sc=Paint(); sc.color=AndroidColor.argb((config.dim.coerceIn(0f,0.85f)*255f).toInt().coerceAtLeast(1),0,0,0); cv.drawRect(0f,0f,w.toFloat(),h.toFloat(),sc) }
    bmp
} catch(_:Exception){ null }

private fun drawAndroidPattern(cv: AndroidCanvas, config: ThemeConfig, w: Int, h: Int, d: Float) {
    val accent=config.secondary?:autoAccentInt(config.primary); val a=config.textureAlpha.coerceIn(0.02f,0.45f)
    val paint=Paint().apply{ isAntiAlias=true; strokeWidth=1f*d; color=AndroidColor.argb((a*255f).toInt().coerceAtLeast(1),AndroidColor.red(accent),AndroidColor.green(accent),AndroidColor.blue(accent)) }
    when(config.pattern){
        PatternKind.None->Unit
        PatternKind.Dots->{ val st=28f*d; val r=1.1f*d; var y=st/2f; var row=0; while(y<h){ var x=if(row%2==0) st/2f else st; while(x<w){ cv.drawCircle(x,y,r,paint); x+=st }; y+=st; row++ } }
        PatternKind.Lines->{ val sp=30f*d; var y=sp; while(y<h){ cv.drawLine(0f,y,w.toFloat(),y,paint); y+=sp } }
        PatternKind.Grid->{ val c=34f*d; var x=c; while(x<w){ cv.drawLine(x,0f,x,h.toFloat(),paint); x+=c }; var y=c; while(y<h){ cv.drawLine(0f,y,w.toFloat(),y,paint); y+=c } }
        PatternKind.Diagonal->{ val sp=22f*d; var x=-h.toFloat(); while(x<w){ cv.drawLine(x,0f,x+h,h.toFloat(),paint); x+=sp } }
        PatternKind.Linen->{ val sp=9f*d; val ta=(a*0.55f*255f).toInt().coerceAtLeast(1); val thin=Paint().apply{ isAntiAlias=true; strokeWidth=1f*d; color=AndroidColor.argb(ta,AndroidColor.red(accent),AndroidColor.green(accent),AndroidColor.blue(accent)) }; var x=0f; while(x<w){ cv.drawLine(x,0f,x,h.toFloat(),thin); x+=sp }; var y=0f; while(y<h){ cv.drawLine(0f,y,w.toFloat(),y,thin); y+=sp } }
        PatternKind.Waves->{ val amp=14f*d; val wl=150f*d; val sy=38f*d; paint.strokeWidth=1.4f*d; var y=sy; while(y<h+amp){ val p=AndroidPath(); p.moveTo(0f,y); var x=0f; while(x<=w){ p.lineTo(x,y+sin((x/wl)*2f*PI.toFloat())*amp); x+=8f*d }; cv.drawPath(p,paint); y+=sy } }
        PatternKind.Stars->{ val cell=70f*d; val cnt=((w*h)/(cell*cell)).toInt().coerceIn(18,160); val br=1.2f*d; for(i in 0 until cnt){ val x=hash01(i,1)*w; val y=hash01(i,2)*h; val r=if(i%9==0) br*1.7f else br; val sa=if(i%5==0)0.9f else 0.55f; val p=Paint(paint); p.color=AndroidColor.argb(((a*sa)*255f).toInt().coerceAtLeast(1),AndroidColor.red(accent),AndroidColor.green(accent),AndroidColor.blue(accent)); cv.drawCircle(x,y,r,p) } }
        PatternKind.Bokeh->{ val cell=160f*d; val cnt=((w*h)/(cell*cell)).toInt().coerceIn(5,24); for(i in 0 until cnt){ val x=hash01(i,3)*w; val y=hash01(i,4)*h; val r=(18f+(i%5)*14f)*d; val p=Paint().apply{ isAntiAlias=true; color=AndroidColor.argb(((a*0.22f)*255f).toInt().coerceAtLeast(1),AndroidColor.red(accent),AndroidColor.green(accent),AndroidColor.blue(accent)) }; cv.drawCircle(x,y,r,p) } }
        PatternKind.Diamonds->{ val sp=42f*d; var x=-h.toFloat(); while(x<w){ cv.drawLine(x,0f,x+h,h.toFloat(),paint); cv.drawLine(x+h,0f,x,h.toFloat(),paint); x+=sp } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioDialog(scope: ThemeScope, initial: ThemeConfig, onDismiss: () -> Unit, onApply: (ThemeConfig) -> Unit, titleOverride: String? = null, defaultConfig: ThemeConfig? = null) {
    val gold=Color(0xFFFFB74D); val muted=Color(0xFF8B949E); val text=Color(0xFFE6EDF3)
    val fallback=defaultConfig?:ThemeKit.default(scope); var draft by remember(initial){ mutableStateOf(initial) }
    AlertDialog(onDismissRequest=onDismiss, containerColor=Color(0xFF151A20),
        title={ Text(titleOverride?:ThemeKit.titleFor(scope), color=gold, fontSize=20.sp, fontWeight=FontWeight.Bold) },
        text={ Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(14.dp)) {
            ThemePreviewCard(draft, scope, Color(0xFF1A1A1A), Color(0xFFF7F7F7))
            ThemeSectionLabel("استایل آماده", muted)
            ThemePresetRow(ThemeKit.presetsFor(scope), draft){ draft=it.config }
            ThemeSectionLabel("نوع زمینه", muted)
            ThemeStyleChips(draft.kind, gold, text){ kind-> draft=when(kind){
                ThemeKind.Solid->draft.copy(kind=ThemeKind.Solid, pattern=PatternKind.None, secondary=null)
                ThemeKind.Gradient-> if(draft.secondary==null){ val g=ThemeKit.gradients.first(); draft.copy(kind=ThemeKind.Gradient, primary=g.from, secondary=g.to, pattern=PatternKind.None) } else draft.copy(kind=ThemeKind.Gradient, pattern=PatternKind.None)
                ThemeKind.Pattern->draft.copy(kind=ThemeKind.Pattern, pattern=if(draft.pattern==PatternKind.None) PatternKind.Dots else draft.pattern, secondary=draft.secondary?:autoAccentInt(draft.primary))
            } }
            if(draft.kind==ThemeKind.Pattern){
                ThemeSectionLabel("طرح", muted); ThemePatternChips(draft.pattern, gold, text){ draft=draft.copy(pattern=it) }
                ThemeSectionLabel("شدت طرح", muted); ThemeSliderRow("شدت", draft.textureAlpha, 0.03f, 0.40f, text, muted){ draft=draft.copy(textureAlpha=it) }
            }
            ThemeSectionLabel("رنگ پایه", muted); ThemeSwatchGrid(ThemeKit.swatchesFor(scope), draft.primary, gold, muted){ draft=draft.copy(primary=it) }
            if(draft.kind==ThemeKind.Gradient){ ThemeSectionLabel("گرادیان آماده", muted); ThemeGradientStrip(ThemeKit.gradients, draft.primary, draft.secondary?:0, muted){ g-> draft=draft.copy(kind=ThemeKind.Gradient, primary=g.from, secondary=g.to, pattern=PatternKind.None) } }
            ThemeSectionLabel("تیرگی", muted); ThemeSliderRow("تیرگی", draft.dim, 0f, 0.55f, text, muted){ draft=draft.copy(dim=it) }
        } },
        confirmButton={ TextButton(onClick={ onApply(draft) }){ Text("اعمال", color=gold, fontWeight=FontWeight.Bold) } },
        dismissButton={ Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ TextButton(onClick={ draft=fallback }){ Text("پیش‌فرض", color=muted) }; TextButton(onClick=onDismiss){ Text("انصراف", color=muted) } } })
}

@Composable private fun ThemePreviewCard(config: ThemeConfig, scope: ThemeScope, dark: Color, light: Color) {
    Box(Modifier.fillMaxWidth().height(ThemeKit.previewHeight(scope).dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(alpha=.12f), RoundedCornerShape(18.dp))) {
        ThemeBackground(config, Modifier.fillMaxSize())
        val fg=if(ThemeKit.isLight(config)) dark else light
        Column(Modifier.padding(14.dp)){ Text("پیش‌نمایش", color=fg, fontSize=15.sp, fontWeight=FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(ThemeKit.previewLabel(scope), color=fg.copy(alpha=.75f), fontSize=12.sp) }
    }
}
@Composable private fun ThemeSectionLabel(t: String, c: Color){ Text(t, color=c, fontSize=12.sp, fontWeight=FontWeight.Bold) }
@Composable private fun ThemePresetRow(presets: List<ThemePreset>, selected: ThemeConfig, onSelect: (ThemePreset)->Unit){
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(8.dp)){
        presets.forEach{ p-> Column(horizontalAlignment=Alignment.CenterHorizontally){
            Box(Modifier.width(92.dp).height(58.dp).clip(RoundedCornerShape(12.dp)).border(if(selected==p.config)2.dp else 1.dp, if(selected==p.config) Color(0xFFFFB74D) else Color.White.copy(alpha=.16f), RoundedCornerShape(12.dp)).clickable{ onSelect(p) }){ ThemeBackground(p.config, Modifier.fillMaxSize()) }
            Spacer(Modifier.height(4.dp)); Text(p.name, color=Color(0xFF8B949E), fontSize=10.sp, maxLines=1)
        } }
    }
}
@Composable private fun ThemeStyleChips(selected: ThemeKind, gold: Color, text: Color, onSelect:(ThemeKind)->Unit){
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ ThemeKind.values().forEach{ k-> val l=when(k){ ThemeKind.Solid->"ساده"; ThemeKind.Gradient->"گرادیان"; ThemeKind.Pattern->"طرح" }; ThemeChip(l, selected==k, gold, text){ onSelect(k) } } }
}
@Composable private fun ThemePatternChips(selected: PatternKind, gold: Color, text: Color, onSelect:(PatternKind)->Unit){
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(8.dp)){ PatternKind.values().forEach{ k-> ThemeChip(ThemeKit.patternLabel(k), selected==k, gold, text){ onSelect(k) } } }
}
@Composable private fun ThemeChip(label: String, selected: Boolean, gold: Color, text: Color, onClick:()->Unit){
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if(selected) gold else Color.White.copy(alpha=.08f)).border(1.dp, if(selected) gold else Color.White.copy(alpha=.14f), RoundedCornerShape(10.dp)).clickable(onClick=onClick).padding(horizontal=12.dp, vertical=7.dp)){ Text(label, color=if(selected) Color(0xFF111111) else text, fontSize=12.sp, fontWeight=FontWeight.Bold) }
}
@Composable private fun ThemeSwatchGrid(presets: List<ThemeSwatch>, selected: Int, gold: Color, muted: Color, onSelect:(Int)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        presets.chunked(5).forEach{ row-> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ row.forEach{ s-> Box(Modifier.size(34.dp).clip(CircleShape).background(Color(s.color)).border(if(selected==s.color)2.dp else 1.dp, if(selected==s.color) gold else Color.White.copy(alpha=.18f), CircleShape).clickable{ onSelect(s.color) }) } } }
        Text(presets.firstOrNull{ it.color==selected }?.name?:"دلخواه", color=muted, fontSize=11.sp)
    }
}
@Composable private fun ThemeGradientStrip(presets: List<ThemeGradient>, selectedFrom: Int, selectedTo: Int, muted: Color, onSelect:(ThemeGradient)->Unit){
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(8.dp)){
        presets.forEach{ g-> Column(horizontalAlignment=Alignment.CenterHorizontally){
            Box(Modifier.width(92.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(g.from),Color(g.to)))).border(if(selectedFrom==g.from&&selectedTo==g.to)2.dp else 1.dp, if(selectedFrom==g.from&&selectedTo==g.to) Color(0xFFFFB74D) else Color.White.copy(alpha=.16f), RoundedCornerShape(12.dp)).clickable{ onSelect(g) })
            Spacer(Modifier.height(4.dp)); Text(g.name, color=muted, fontSize=10.sp, maxLines=1)
        } }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ThemeSliderRow(label: String, value: Float, min: Float, max: Float, text: Color, muted: Color, onChange:(Float)->Unit){
    Column{ Row(verticalAlignment=Alignment.CenterVertically){ Text(label, color=text, fontSize=12.sp, modifier=Modifier.weight(1f)); Text("${(value*100).toInt()}٪", color=muted, fontSize=11.sp) }
        Slider(value=value, onValueChange=onChange, valueRange=min..max, modifier=Modifier.fillMaxWidth()) }
}
