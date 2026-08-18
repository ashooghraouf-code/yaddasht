package ir.yaddasht.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.yaddasht.app.R

val DeepGreen = Color(0xFF0D2B26)
val DeepGreenSoft = Color(0xFF153B34)
val LineGreen = Color(0xFF2C5148)
val Saffron = Color(0xFFF2A93B)
val Brick = Color(0xFFC64B2C)
val Ink = Color(0xFF22302B)
val InkSoft = Color(0xFF5C6B63)
val PaperWhite = Color(0xFFFBF7EC)
val MutedGreenText = Color(0xFF9DBBB0)

val PaperColors = listOf(
    Color(0xFFFBF7EC), Color(0xFFFDE9B8), Color(0xFFD9EDDC),
    Color(0xFFD8EAF2), Color(0xFFF7DBE2), Color(0xFFEDE4F3)
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