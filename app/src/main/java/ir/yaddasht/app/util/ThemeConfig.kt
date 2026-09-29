package ir.yaddasht.app.util

import android.content.Context
import org.json.JSONObject

private val DefaultPrimary = 0xFF0F3D2E.toInt()

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

data class ThemeConfig(
    val kind: ThemeKind = ThemeKind.Solid,
    val primary: Int = DefaultPrimary,
    val secondary: Int? = null,
    val pattern: PatternKind = PatternKind.None,
    val textureAlpha: Float = 0.10f,
    val dim: Float = 0f
)

object ThemePrefs {
    private const val PREFS = "theme_prefs_v2"
    private const val KEY_CONFIG = "theme_config_v2"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context): ThemeConfig {
        val json = prefs(context).getString(KEY_CONFIG, null)
        if (!json.isNullOrBlank()) {
            try {
                return decode(json)
            } catch (_: Exception) {
            }
        }

        val old = try {
            AppThemePreferences.getBgColor(context)
        } catch (_: Exception) {
            DefaultPrimary
        }

        return ThemeConfig(
            kind = ThemeKind.Solid,
            primary = old
        )
    }

    fun set(context: Context, config: ThemeConfig) {
        prefs(context).edit()
            .putString(KEY_CONFIG, encode(config))
            .apply()

        // سازگاری با کدهای قدیمی که هنوز bgColor ساده می‌خواهند
        try {
            AppThemePreferences.setBgColor(context, config.primary)
        } catch (_: Exception) {
        }
    }

    private fun encode(config: ThemeConfig): String {
        return JSONObject().apply {
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
    }

    private fun decode(json: String): ThemeConfig {
        val o = JSONObject(json)

        val kind = runCatching {
            ThemeKind.valueOf(o.optString("kind", ThemeKind.Solid.name))
        }.getOrDefault(ThemeKind.Solid)

        val pattern = runCatching {
            PatternKind.valueOf(o.optString("pattern", PatternKind.None.name))
        }.getOrDefault(PatternKind.None)

        val secondary =
            if (o.has("secondary") && !o.isNull("secondary")) {
                o.optInt("secondary")
            } else {
                null
            }

        return ThemeConfig(
            kind = kind,
            primary = o.optInt("primary", DefaultPrimary),
            secondary = secondary,
            pattern = pattern,
            textureAlpha = o.optDouble("textureAlpha", 0.10).toFloat(),
            dim = o.optDouble("dim", 0.0).toFloat()
        )
    }
}

object ThemePresets {
    data class Swatch(
        val name: String,
        val emoji: String,
        val color: Int
    )

    data class Gradient(
        val name: String,
        val emoji: String,
        val from: Int,
        val to: Int
    )

    val solids: List<Swatch> = listOf(
        // کاغذی / روشن
        Swatch("کاغذ کرم", "📜", 0xFFFFF8E1.toInt()),
        Swatch("سپیا", "🧻", 0xFFF4ECD8.toInt()),
        Swatch("چای نبات", "🍵", 0xFFE8DCC4.toInt()),
        Swatch("ابریشم", "🤍", 0xFFFFFBF0.toInt()),
        Swatch("کاغذ پوستی", "📃", 0xFFEFE6D3.toInt()),
        Swatch("مومیایی", "🏺", 0xFFE0D3B8.toInt()),

        // تیره / متمرکز
        Swatch("شب", "🌙", 0xFF121212.toInt()),
        Swatch("نفتی", "🛢️", 0xFF0E1116.toInt()),
        Swatch("جنگلان", "🌲", 0xFF102018.toInt()),
        Swatch("زغالی", "🪨", 0xFF1A1A1A.toInt()),
        Swatch("نیمه‌شب", "🌌", 0xFF0B1026.toInt()),
        Swatch("دود", "🌫️", 0xFF20232A.toInt()),

        // سبز / چراغ راه
        Swatch("سبز چراغ", "🏮", 0xFF0F3D2E.toInt()),
        Swatch("باغ", "🌿", 0xFF1B5E20.toInt()),
        Swatch("زیتون", "🫒", 0xFF33691E.toInt()),
        Swatch("کاج", "🌲", 0xFF0B3D2C.toInt()),
        Swatch("یشم", "💎", 0xFF2E7D52.toInt()),
        Swatch("جنگل بارانی", "🌧️", 0xFF143D2B.toInt()),

        // آبی / آرامش
        Swatch("آسمان", "☁️", 0xFFE3F2FD.toInt()),
        Swatch("دریا", "🌊", 0xFF01579B.toInt()),
        Swatch("فیروزه", "🧿", 0xFF00838F.toInt()),
        Swatch("نیلی", "🔵", 0xFF1A237E.toInt()),
        Swatch("اقیانوس", "🐋", 0xFF023E5C.toInt()),
        Swatch("مه", "🌫️", 0xFFCFD8DC.toInt()),

        // گرم / انرژی
        Swatch("آفتاب", "☀️", 0xFFFFF3E0.toInt()),
        Swatch("نارنج", "🍊", 0xFFE65100.toInt()),
        Swatch("گل‌گون", "🌸", 0xFFF8BBD0.toInt()),
        Swatch("عنابی", "🍇", 0xFF880E4F.toInt()),
        Swatch("زعفران", "🌼", 0xFFFFB74D.toInt()),
        Swatch("آجر", "🧱", 0xFFB71C1C.toInt()),

        // ایرانی / سنتی
        Swatch("فیروزه ایرانی", "🕌", 0xFF00A6A6.toInt()),
        Swatch("لاجورد", "🔷", 0xFF283593.toInt()),
        Swatch("زرشک", "", 0xFF8E1B3A.toInt()),
        Swatch("کویر", "🏜️", 0xFF8D6E63.toInt()),
        Swatch("مس", "🥉", 0xFFB87333.toInt()),
        Swatch("زیتون ایرانی", "🌿", 0xFF6B8E23.toInt())
    )

    val gradients: List<Gradient> = listOf(
        Gradient("سپیده‌دم", "🌅", 0xFF0F2027.toInt(), 0xFF2C5364.toInt()),
        Gradient("جنگل مه‌آلود", "🌫️", 0xFF134E5E.toInt(), 0xFF0F2027.toInt()),
        Gradient("شب تار پریسا", "🌑", 0xFF090909.toInt(), 0xFF1F1F1F.toInt()),
        Gradient("باغ سبز", "🌿", 0xFF0F3D2E.toInt(), 0xFF1B5E20.toInt()),
        Gradient("آسمان نیلی یاسمین زهرا", "🔵", 0xFF1A237E.toInt(), 0xFF0D47A1.toInt()),
        Gradient("غروب کویر", "🏜️", 0xFF8D6E63.toInt(), 0xFFBF360C.toInt()),
        Gradient("زعفران محمد حسین", "🌼", 0xFFFFB74D.toInt(), 0xFFE65100.toInt()),
        Gradient("یاقوت", "❤️‍🔥", 0xFF880E4F.toInt(), 0xFF4A148C.toInt()),
        Gradient("فیروزه فاطمه حسنا", "🧿", 0xFF00838F.toInt(), 0xFF006064.toInt()),
        Gradient("کاغذ و چای", "🍵", 0xFFFFF8E1.toInt(), 0xFFE0D3B8.toInt())
    )

    val patternLabels: List<Pair<PatternKind, String>> = listOf(
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
}
