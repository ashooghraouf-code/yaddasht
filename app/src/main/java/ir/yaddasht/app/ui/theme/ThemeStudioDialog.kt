package ir.yaddasht.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.util.PatternKind
import ir.yaddasht.app.util.ThemeConfig
import ir.yaddasht.app.util.ThemeKind
import ir.yaddasht.app.util.ThemePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioDialog(
    config: ThemeConfig,
    onDismiss: () -> Unit,
    onApply: (ThemeConfig) -> Unit
) {
    val gold = Color(0xFFFFB74D)
    val muted = Color(0xFF8B949E)
    val text = Color(0xFFE6EDF3)

    var draft by remember(config) { mutableStateOf(config) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151A20),
        title = {
            Text(
                "🎨 استودیوی تم",
                color = gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PreviewCard(
                    config = draft,
                    dark = Color(0xFF1A1A1A),
                    light = Color(0xFFF7F7F7)
                )

                StyleChips(
                    selected = draft.kind,
                    gold = gold,
                    text = text,
                    muted = muted
                ) { kind ->
                    draft = when (kind) {
                        ThemeKind.Solid -> draft.copy(
                            kind = ThemeKind.Solid,
                            pattern = PatternKind.None
                        )

                        ThemeKind.Gradient -> {
                            if (draft.secondary == null) {
                                val g = ThemePresets.gradients.first()
                                draft.copy(
                                    kind = ThemeKind.Gradient,
                                    primary = g.from,
                                    secondary = g.to
                                )
                            } else {
                                draft.copy(kind = ThemeKind.Gradient)
                            }
                        }

                        ThemeKind.Pattern -> draft.copy(
                            kind = ThemeKind.Pattern,
                            pattern = if (draft.pattern == PatternKind.None) {
                                PatternKind.Dots
                            } else {
                                draft.pattern
                            }
                        )
                    }
                }

                if (draft.kind == ThemeKind.Pattern) {
                    SectionLabel("طرح زمینه", muted)
                    PatternChips(
                        selected = draft.pattern,
                        gold = gold,
                        text = text,
                        muted = muted
                    ) { pattern ->
                        draft = draft.copy(pattern = pattern)
                    }
                }

                SectionLabel("رنگ پایه", muted)
                SwatchGrid(
                    presets = ThemePresets.solids,
                    selected = draft.primary,
                    gold = gold,
                    muted = muted
                ) { color ->
                    draft = draft.copy(primary = color)
                }

                if (draft.kind == ThemeKind.Gradient) {
                    SectionLabel("گرادیان آماده", muted)
                    GradientStrip(
                        presets = ThemePresets.gradients,
                        selectedFrom = draft.primary,
                        selectedTo = draft.secondary ?: 0,
                        muted = muted
                    ) { g ->
                        draft = draft.copy(
                            kind = ThemeKind.Gradient,
                            primary = g.from,
                            secondary = g.to
                        )
                    }
                }

                SectionLabel("شدت و تیرگی", muted)

                SliderRow(
                    label = "شدت طرح",
                    value = draft.textureAlpha,
                    min = 0.03f,
                    max = 0.30f,
                    text = text,
                    muted = muted
                ) { v ->
                    draft = draft.copy(textureAlpha = v)
                }

                SliderRow(
                    label = "تیرگی",
                    value = draft.dim,
                    min = 0f,
                    max = 0.55f,
                    text = text,
                    muted = muted
                ) { v ->
                    draft = draft.copy(dim = v)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft) }) {
                Text(
                    "اعمال",
                    color = gold,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "انصراف",
                    color = muted
                )
            }
        }
    )
}

@Composable
private fun PreviewCard(
    config: ThemeConfig,
    dark: Color,
    light: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(18.dp))
    ) {
        AppBackground(
            config = config,
            modifier = Modifier.fillMaxSize()
        )

        val useDark = Color(config.primary).luminance() > 0.55f && config.dim < 0.25f
        val fg = if (useDark) dark else light

        Column(Modifier.padding(14.dp)) {
            Text(
                "پیش‌نمایش",
                color = fg,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "چراغ راه 🏮",
                color = fg.copy(alpha = .75f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StyleChips(
    selected: ThemeKind,
    gold: Color,
    text: Color,
    muted: Color,
    onSelect: (ThemeKind) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeKind.values().forEach { kind ->
            val label = when (kind) {
                ThemeKind.Solid -> "ساده"
                ThemeKind.Gradient -> "گرادیان"
                ThemeKind.Pattern -> "طرح"
            }

            Chip(
                label = label,
                selected = selected == kind,
                gold = gold,
                text = text,
                muted = muted,
                onClick = { onSelect(kind) }
            )
        }
    }
}

@Composable
private fun PatternChips(
    selected: PatternKind,
    gold: Color,
    text: Color,
    muted: Color,
    onSelect: (PatternKind) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PatternKind.values().forEach { kind ->
            Chip(
                label = patternLabel(kind),
                selected = selected == kind,
                gold = gold,
                text = text,
                muted = muted,
                onClick = { onSelect(kind) }
            )
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    gold: Color,
    text: Color,
    muted: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) gold else Color.White.copy(alpha = .08f))
            .border(
                1.dp,
                if (selected) gold else Color.White.copy(alpha = .14f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF111111) else text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SwatchGrid(
    presets: List<ThemePresets.Swatch>,
    selected: Int,
    gold: Color,
    muted: Color,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { swatch ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(swatch.color))
                            .border(
                                if (selected == swatch.color) 2.dp else 1.dp,
                                if (selected == swatch.color) gold else Color.White.copy(alpha = .18f),
                                CircleShape
                            )
                            .clickable { onSelect(swatch.color) }
                    )
                }
            }
        }

        val selectedName = presets.firstOrNull { it.color == selected }?.name ?: "دلخواه"
        Text(
            selectedName,
            color = muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun GradientStrip(
    presets: List<ThemePresets.Gradient>,
    selectedFrom: Int,
    selectedTo: Int,
    muted: Color,
    onSelect: (ThemePresets.Gradient) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { g ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(92.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(g.from), Color(g.to))))
                        .border(
                            if (selectedFrom == g.from && selectedTo == g.to) 2.dp else 1.dp,
                            if (selectedFrom == g.from && selectedTo == g.to) {
                                Color(0xFFFFB74D)
                            } else {
                                Color.White.copy(alpha = .16f)
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(g) }
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    g.name,
                    color = muted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    text: Color,
    muted: Color,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = text,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                "${(value * 100).toInt()}٪",
                color = muted,
                fontSize = 11.sp
            )
        }

        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun patternLabel(kind: PatternKind): String {
    return ThemePresets.patternLabels
        .firstOrNull { it.first == kind }
        ?.second
        ?: kind.name
}
