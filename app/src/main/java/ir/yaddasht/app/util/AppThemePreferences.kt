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
        ThemeColor("نیلی اقیانوس", "🌊", 0xFF004F7A),
        ThemeColor("قهوه‌ای چرم", "🟤", 0xFF1C1008),
        ThemeColor("سبز زیتونی", "🫒", 0xFF121C08),
        ThemeColor("بنفش کهکشانی", "🪐", 0xFF1A0830),
        ThemeColor("مشکی مخملی", "🖤", 0xFF0C0C0C),
        ThemeColor("آبی آسمانی", "🦋", 0xFF0277BD),
        ThemeColor(" نقره یاسمین زهرا", "🤍", 0xFF9B8EBF),
    
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
