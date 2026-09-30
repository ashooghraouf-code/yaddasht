package ir.yaddasht.app.ui.theme

import androidx.compose.ui.graphics.Color

val EditorPaperColors: List<Color> = listOf(
    Color(0xFFFFF8E1), Color(0xFFF4ECD8), Color(0xFFE8DCC4), Color(0xFFFFFBF0),
    Color(0xFFEFE6D3), Color(0xFFE0D3B8), Color(0xFFE3F2FD), Color(0xFFE8F5E9),
    Color(0xFFEDE7F6), Color(0xFFFFF3E0), Color(0xFFF9FBE7), Color(0xFFFFE0EC),
    Color(0xFFE0F7FA), Color(0xFFFFE0B2), Color(0xFFF3E5F5), Color(0xFFECEFF1)
)

private val PaperPresets: List<ThemeConfig> = listOf(
    ThemeConfig(ThemeKind.Solid, 0xFFFFF8E1.toInt()),
    ThemeConfig(ThemeKind.Solid, 0xFFF4ECD8.toInt()),
    ThemeConfig(ThemeKind.Solid, 0xFFE8DCC4.toInt()),
    ThemeConfig(ThemeKind.Pattern, 0xFFFFFBF0.toInt(), 0xFF5B665F.toInt(), PatternKind.Dots, 0.14f),
    ThemeConfig(ThemeKind.Pattern, 0xFFEFE6D3.toInt(), 0xFF8D6E63.toInt(), PatternKind.Lines, 0.14f),
    ThemeConfig(ThemeKind.Pattern, 0xFFE0D3B8.toInt(), 0xFF3E2723.toInt(), PatternKind.Linen, 0.09f),
    ThemeConfig(ThemeKind.Pattern, 0xFFE3F2FD.toInt(), 0xFF01579B.toInt(), PatternKind.Lines, 0.12f),
    ThemeConfig(ThemeKind.Pattern, 0xFFE8F5E9.toInt(), 0xFF2E7D52.toInt(), PatternKind.Dots, 0.13f),
    ThemeConfig(ThemeKind.Pattern, 0xFFEDE7F6.toInt(), 0xFF4A148C.toInt(), PatternKind.Diagonal, 0.10f),
    ThemeConfig(ThemeKind.Pattern, 0xFFFFF3E0.toInt(), 0xFFBF360C.toInt(), PatternKind.Linen, 0.08f),
    ThemeConfig(ThemeKind.Pattern, 0xFFF9FBE7.toInt(), 0xFF33691E.toInt(), PatternKind.Grid, 0.10f),
    ThemeConfig(ThemeKind.Pattern, 0xFFFFE0EC.toInt(), 0xFFAD1457.toInt(), PatternKind.Dots, 0.13f),
    ThemeConfig(ThemeKind.Pattern, 0xFFE0F7FA.toInt(), 0xFF006064.toInt(), PatternKind.Lines, 0.12f),
    ThemeConfig(ThemeKind.Pattern, 0xFFFFE0B2.toInt(), 0xFFE65100.toInt(), PatternKind.Waves, 0.12f),
    ThemeConfig(ThemeKind.Pattern, 0xFFF3E5F5.toInt(), 0xFF6A1B9A.toInt(), PatternKind.Grid, 0.10f),
    ThemeConfig(ThemeKind.Pattern, 0xFFECEFF1.toInt(), 0xFF37474F.toInt(), PatternKind.Bokeh, 0.14f)
)

fun paperConfigForIndex(index: Int): ThemeConfig = PaperPresets.getOrElse(index) { PaperPresets[0] }
