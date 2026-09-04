package ir.yaddasht.app.util

import android.content.Context
import androidx.compose.ui.graphics.Color

object AppThemePreferences {
    private const val PREFS = "app_theme_prefs"
    private const val KEY_BG_COLOR = "app_bg_color"

    data class ThemeColor(val name: String, val emoji: String, val colorValue: Long)

    val themeColors = listOf(
        // ═══ رنگ‌های کلاسیک ═══
        ThemeColor("سبز جنگلی", "🌲", 0xFF06100E),
        ThemeColor("ارغوانی شاهانه", "🔮", 0xFF1B0A3C),
        ThemeColor("نیلی اقیانوس", "🌊", 0xFF0A1628),
        ThemeColor("شرابی لوکس", "🍷", 0xFF2C0A1A),
        ThemeColor("قهوه‌ای چرم", "🟤", 0xFF1C1008),
        ThemeColor("خاکستری فولاد", "⚙️", 0xFF121820),
        ThemeColor("سبز زیتونی", "🫒", 0xFF121C08),
        ThemeColor("بنفش کهکشانی", "🪐", 0xFF1A0830),
        ThemeColor("آبی نیمه‌شب", "🌙", 0xFF081428),
        ThemeColor("مشکی مخملی", "🖤", 0xFF0C0C0C),

        // ═══ ✨ رنگ‌های شاد فانتزی ✨ ═══
        ThemeColor("صورتی جادویی", "💖", 0xFFAD1457),
        ThemeColor("پسته‌ای رویایی", "🌿", 0xFF558B2F),
        ThemeColor("زرد طلایی", "✨", 0xFFF57F17),
        ThemeColor("آبی آسمانی", "🦋", 0xFF0277BD),
        ThemeColor("بنفش یاسی", "💜", 0xFF7B1FA2),
        ThemeColor("نارنجی پرتقالی", "🍊", 0xFFD84315),
    )

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBgColorValue(c: Context): Long {
        return prefs(c).getLong(KEY_BG_COLOR, 0xFF06100E)
    }

    fun getBgColor(c: Context): Color {
        return Color(getBgColorValue(c))
    }

    fun setBgColor(c: Context, colorValue: Long) {
        prefs(c).edit().putLong(KEY_BG_COLOR, colorValue).commit()
    }
}
