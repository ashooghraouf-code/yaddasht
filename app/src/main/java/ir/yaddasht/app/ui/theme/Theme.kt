package ir.yaddasht.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import ir.yaddasht.app.R
import ir.yaddasht.app.util.AppThemePreferences

// ✅ فونت‌ها (این‌ها در نسخهٔ قبلی من حذف شده بودند!)
val LalezarFont = FontFamily(Font(R.font.lalezar))
val VazirFont = FontFamily(Font(R.font.vazir))

// ✅ رنگ‌های ثابت
val DeepGreen = Color(0xFF06100E)
val DeepGreenSoft = Color(0xFF0E1F1A)
val Saffron = Color(0xFFFFB74D)
val PaperWhite = Color(0xFFFAF5E8)
val Ink = Color(0xFF2D2D2D)
val InkSoft = Color(0xFF5D5D5D)
val Brick = Color(0xFFC62828)
val LineGreen = Color(0xFF2E7D52)
val MutedGreenText = Color(0xFF8FAF9F)

// ✅ رنگ‌های کاغذ یادداشت
val PaperColors = listOf(
    Color(0xFFFFF8E1),
    Color(0xFFFFE0B2),
    Color(0xFFFFCCBC),
    Color(0xFFF8BBD0),
    Color(0xFFE1BEE7),
    Color(0xFFD1C4E9),
    Color(0xFFC8E6C9),
    Color(0xFFDCEDC8),
)

fun paperColor(index: Int): Color {
    return PaperColors.getOrElse(index) { PaperColors[0] }
}

@Composable
fun YaddashtTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    // ✅ خواندن رنگ پس‌زمینه از SharedPreferences
    val bgColorValue = AppThemePreferences.getBgColorValue(context)
    val bgColor = Color(bgColorValue)

    // ✅ ساخت ColorScheme با رنگ پس‌زمینهٔ داینامیک
    val colorScheme = darkColorScheme(
        primary = Saffron,
        onPrimary = Color.Black,
        secondary = Color(0xFF81C784),
        onSecondary = Color.Black,
        tertiary = Color(0xFFFFCC80),
        onTertiary = Color.Black,
        background = bgColor,
        onBackground = PaperWhite,
        surface = bgColor,
        onSurface = PaperWhite,
        surfaceVariant = bgColor.copy(alpha = 0.8f),
        onSurfaceVariant = PaperWhite.copy(alpha = 0.8f),
        error = Brick,
        onError = Color.White,
        outline = Color.White.copy(alpha = 0.2f),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
