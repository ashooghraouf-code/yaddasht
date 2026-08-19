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

val DeepGreen = when (seasonMonth) {
    in 1..3 -> Color(0xFF0F3B2C)
    in 4..6 -> Color(0xFF123B33)
    in 7..9 -> Color(0xFF33261B)
    else -> Color(0xFF14283B)
}
val DeepGreenSoft = when (seasonMonth) {
    in 1..3 -> Color(0xFF17493A)
    in 4..6 -> Color(0xFF1B4A40)
    in 7..9 -> Color(0xFF413122)
    else -> Color(0xFF1D3548)
}
val LineGreen = when (seasonMonth) {
    in 1..3 -> Color(0xFF2F6B52)
    in 4..6 -> Color(0xFF35685C)
    in 7..9 -> Color(0xFF6B5236)
    else -> Color(0xFF3A5A78)
}
val Saffron = when (seasonMonth) {
    in 1..3 -> Color(0xFFFF9EB5)
    in 4..6 -> Color(0xFFFFB020)
    in 7..9 -> Color(0xFFFF7A45)
    else -> Color(0xFF7FB7FF)
}
val Brick = Color(0xFFFF6B4A)
val Ink = Color(0xFF243229)
val InkSoft = Color(0xFF5F7168)
val PaperWhite = Color(0xFFFFFBEE)
val MutedGreenText = when (seasonMonth) {
    in 1..3 -> Color(0xFFA9C6B4)
    in 4..6 -> Color(0xFFA9C6BB)
    in 7..9 -> Color(0xFFC6B4A0)
    else -> Color(0xFFA9B8C6)
}

val PaperColors = listOf(
    Color(0xFFFFF6E0), Color(0xFFFFE08A), Color(0xFFC9F0D8),
    Color(0xFFCDE9FF), Color(0xFFFFD6E0), Color(0xFFE6DCFF)
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
