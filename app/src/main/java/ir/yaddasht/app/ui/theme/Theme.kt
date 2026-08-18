package ir.yaddasht.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.yaddasht.app.R

val DeepGreen = Color(0xFF123B33)
val DeepGreenSoft = Color(0xFF1B4A40)
val LineGreen = Color(0xFF35685C)
val Saffron = Color(0xFFFFB020)
val Brick = Color(0xFFFF6B4A)
val Ink = Color(0xFF243229)
val InkSoft = Color(0xFF5F7168)
val PaperWhite = Color(0xFFFFFBEE)
val MutedGreenText = Color(0xFFA9C6BB)

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
