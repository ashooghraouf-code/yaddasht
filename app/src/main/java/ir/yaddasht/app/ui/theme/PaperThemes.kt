package ir.yaddasht.app.ui.theme

import androidx.compose.ui.graphics.Color

val EditorPaperColors: List<Color> = listOf(
    Color(0xFFFFF8E1), // 0 کاغذ کرم
    Color(0xFFF4ECD8), // 1 سپیا
    Color(0xFFE8DCC4), // 2 چای نبات
    Color(0xFFFFFBF0), // 3 ابریشم
    Color(0xFFEFE6D3), // 4 کاغذ پوستی
    Color(0xFFE0D3B8), // 5 کاهی
    Color(0xFFE3F2FD), // 6 آسمانی
    Color(0xFFE8F5E9), // 7 نعناعی
    Color(0xFFEDE7F6), // 8 اسطوخودوس
    Color(0xFFFFF3E0), // 9 هلویی
    Color(0xFFF9FBE7), // 10 لیمویی
    Color(0xFFFFE0EC)  // 11 صورتی
)

private val PaperPresets: List<ThemeConfig> = listOf(
    ThemeConfig(ThemeKind.Solid, 0xFFFFF8E1.toInt()),
    ThemeConfig(ThemeKind.Solid, 0xFFF4ECD8.toInt()),
    ThemeConfig(ThemeKind.Solid, 0xFFE8DCC4.toInt()),
    ThemeConfig(
        kind = ThemeKind.Pattern, primary = 0xFFFFFBF0.toInt(),
        secondary = 0xFF5B665F.toInt(), pattern = PatternKind.Dots, textureAlpha = 0.10f
    ),
    ThemeConfig(
        kind = ThemeKind.Pattern, primary = 0xFFEFE6D3.toInt(),
        secondary = 0xFF8D6E63.toInt(), pattern = PatternKind.Lines, textureAlpha = 0.10f
    ),
    ThemeConfig(
        kind = ThemeKind.Pattern, primary = 0xFFE0D3B8.toInt(),
        secondary = 0xFF3E2723.toInt(), pattern = PatternKind.Linen, textureAlpha = 0.07f
    ),
    ThemeConfig(
        kind = ThemeKind.Pattern, primary = 0xFFE3F2FD.toInt(),
        secondary = 0xFF01579B.toInt(), pattern = PatternKind.Lines, textureAlpha = 0.10f
    ),
    ThemeConfig(
        kind = ThemeKind.Pattern, primary = 0xFFE8F5E9.toInt(),
        secondary = 0xFF2E7D52.toInt(), pattern = PatternKind.Dots, textureAlpha = 0.11f
    ),
    ThemeConfig(
        kind = ThemeKind.Pattern, primary = 0xFFEDE7F6.toInt(),
        secondary = 0xFF4A148C.toInt(), pattern = PatternKind.Diagonal, textureAlpha = 0.08f
    ),
    ThemeConfig(
        kind = ThemeKind.Pattern, primary = 0xFFFFF3E0.toInt(),
        secondary = 0xFFBF360C.toInt(), pattern = PatternKind.Linen, textureAlpha = 0.06f
    ),
    ThemeConfig(
        kind = ThemeKind.Pattern, primary = 0xFFF9FBE7.toInt(),
        secondary = 0xFF33691E.toInt(), pattern = PatternKind.Grid, textureAlpha = 0.08f
    ),
    ThemeConfig(ThemeKind.Solid, 0xFFFFE0EC.toInt())
)

fun paperConfigForIndex(index: Int): ThemeConfig =
    PaperPresets.getOrElse(index) { PaperPresets[0] }
