package ir.yaddasht.app.util

import android.content.Context
import androidx.compose.ui.graphics.Color

object AppThemePreferences {
    private const val PREFS = "app_theme_prefs"
    private const val KEY_BG_COLOR = "app_bg_color"

    data class ThemeColor(val name: String, val emoji: String, val colorValue: Long)

    // ✅ پالت رنگ‌های جذاب تیره
    val themeColors = listOf(
        ThemeColor("سبز جنگلی", "🌲", 0xFF06100E),       // پیش‌فرض فعلی
        ThemeColor("ارغوانی شب", "🔮", 0xFF1A0A2E),
        ThemeColor("نیلی عمیق", "🌌", 0xFF0A0E2A),
        ThemeColor("شرابی تیره", "🍷", 0xFF2A0A14),
        ThemeColor("قهوه‌ای چرم", "🟤", 0xFF1E1208),
        ThemeColor("خاکستری فولاد", "⚙️", 0xFF141820),
        ThemeColor("سبز زیتونی", "🫒", 0xFF141E0A),
        ThemeColor("آبی اقیانوس", "🌊", 0xFF0A1A2E),
        ThemeColor("بنفش کهکشانی", "🪐", 0xFF1E0A30),
        ThemeColor("مشکی مخملی", "🖤", 0xFF0E0E0E),
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
