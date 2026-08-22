package ir.yaddasht.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.yaddasht.app.R
import ir.yaddasht.app.util.FaDate

private val seasonMonth = FaDate.jalali(System.currentTimeMillis()).second

// 🌸 بهار | ☀️ تابستان | 🍂 پاییز | ❄️ زمستان
val DeepGreen = when (seasonMonth) {
    in 1..3 -> Color(0xFF0D3A2C)
    in 4..6 -> Color(0xFF0E3C33)
    in 7..9 -> Color(0xFF2C2116)
    else -> Color(0xFF12283C)
}
val DeepGreenSoft = when (seasonMonth) {
    in 1..3 -> Color(0xFF15483A)
    in 4..6 -> Color(0xFF165046)
    in 7..9 -> Color(0xFF3E2F1F)
    else -> Color(0xFF1C3550)
}
val LineGreen = when (seasonMonth) {
    in 1..3 -> Color(0xFF2F6B52)
    in 4..6 -> Color(0xFF2F6B5C)
    in 7..9 -> Color(0xFF6B5236)
    else -> Color(0xFF3A5A78)
}
val Saffron = when (seasonMonth) {
    in 1..3 -> Color(0xFFFF8FA3)
    in 4..6 -> Color(0xFFFFB325)
    in 7..9 -> Color(0xFFFF8352)
    else -> Color(0xFF8CC0FF)
}
val Brick = Color(0xFFFF6B4A)
val Ink = Color(0xFF253329)
val InkSoft = Color(0xFF5E7268)
val PaperWhite = Color(0xFFFFFBF0)
val MutedGreenText = when (seasonMonth) {
    in 1..3 -> Color(0xFFB4D2C0)
    in 4..6 -> Color(0xFFB2D2C6)
    in 7..9 -> Color(0xFFD2BFA9)
    else -> Color(0xFFB4C6D8)
}

// 🎨 کاغذهای پاستلی هماهنگ
val PaperColors = listOf(
    Color(0xFFFFF7E6), Color(0xFFFFE9A6), Color(0xFFD8F1E1),
    Color(0xFFDBEAF9), Color(0xFFFAE0E8), Color(0xFFE9E2F6)
)
fun paperColor(index: Int): Color = PaperColors.getOrElse(index) { PaperColors[0] }

val LalezarFont = FontFamily(Font(R.font.lalezar))
val VazirFont = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val Colors = darkColorScheme(
    primary = Saffron, onPrimary = Ink,
    background = DeepGreen, surface = DeepGreenSoft,
    onBackground = PaperWhite, onSurface = PaperWhite, error = Brick
)

@Composable
fun YaddashtTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
