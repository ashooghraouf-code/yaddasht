package ir.yaddasht.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.unit.dp
import ir.yaddasht.app.util.PatternKind
import ir.yaddasht.app.util.ThemeConfig
import ir.yaddasht.app.util.ThemeKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

fun Color.luminance(): Float {
    val r = red.coerceIn(0f, 1f)
    val g = green.coerceIn(0f, 1f)
    val b = blue.coerceIn(0f, 1f)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

private fun autoAccent(base: Color): Color {
    return if (base.luminance() > 0.55f) {
        Color(0xFF1A1A1A)
    } else {
        Color(0xFFFFFFFF)
    }
}

@Composable
fun AppBackground(
    config: ThemeConfig,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        when (config.kind) {
            ThemeKind.Solid -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(config.primary))
                )
            }

            ThemeKind.Gradient -> {
                val c1 = Color(config.primary)
                val c2 = Color(config.secondary ?: config.primary)

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(c1, c2)))
                )
            }

            ThemeKind.Pattern -> {
                if (config.pattern == PatternKind.None) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(config.primary))
                    )
                } else {
                    PatternCanvas(
                        base = Color(config.primary),
                        accent = Color(config.secondary ?: autoAccent(Color(config.primary))),
                        alpha = config.textureAlpha,
                        kind = config.pattern
                    )
                }
            }
        }

        if (config.dim > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = config.dim.coerceIn(0f, 0.85f)))
            )
        }
    }
}

@Composable
private fun PatternCanvas(
    base: Color,
    accent: Color,
    alpha: Float,
    kind: PatternKind,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(base)

        val a = alpha.coerceIn(0.02f, 0.35f)
        val col = accent.copy(alpha = a)

        when (kind) {
            PatternKind.None -> Unit

            PatternKind.Dots -> {
                val step = 28.dp.toPx()
                val radius = 1.1.dp.toPx()
                var y = step / 2f
                var row = 0

                while (y < size.height) {
                    var x = if (row % 2 == 0) step / 2f else step

                    while (x < size.width) {
                        drawCircle(
                            color = col,
                            radius = radius,
                            center = Offset(x, y)
                        )
                        x += step
                    }

                    y += step
                    row++
                }
            }

            PatternKind.Lines -> {
                val spacing = 30.dp.toPx()
                var y = spacing

                while (y < size.height) {
                    drawLine(
                        color = col,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += spacing
                }
            }

            PatternKind.Grid -> {
                val cell = 34.dp.toPx()

                var x = cell
                while (x < size.width) {
                    drawLine(
                        color = col,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += cell
                }

                var y = cell
                while (y < size.height) {
                    drawLine(
                        color = col,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += cell
                }
            }

            PatternKind.Diagonal -> {
                val spacing = 22.dp.toPx()
                var x = -size.height

                while (x < size.width) {
                    drawLine(
                        color = col,
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1f
                    )
                    x += spacing
                }
            }

            PatternKind.Linen -> {
                val spacing = 8.dp.toPx()
                val thin = col.copy(alpha = col.alpha * 0.55f)

                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = thin,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += spacing
                }

                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = thin,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += spacing
                }
            }

            PatternKind.Waves -> {
                val amplitude = 14.dp.toPx()
                val wavelength = 150.dp.toPx()
                val stepY = 38.dp.toPx()

                var y = stepY
                while (y < size.height + amplitude) {
                    val path = Path()
                    path.moveTo(0f, y)

                    var x = 0f
                    while (x <= size.width) {
                        val yy = y + sin((x / wavelength) * 2f * PI.toFloat()) * amplitude
                        path.lineTo(x, yy)
                        x += 6f
                    }

                    drawPath(
                        path = path,
                        color = col,
                        style = Stroke(width = 1.4f)
                    )

                    y += stepY
                }
            }

            PatternKind.Stars -> {
                val count = 110
                val baseR = 1.2.dp.toPx()

                for (i in 0 until count) {
                    val fx = hash01(i, 1)
                    val fy = hash01(i, 2)

                    val x = fx * size.width
                    val y = fy * size.height
                    val r = if (i % 9 == 0) baseR * 1.7f else baseR
                    val starAlpha = if (i % 5 == 0) 0.9f else 0.55f

                    drawCircle(
                        color = col.copy(alpha = col.alpha * starAlpha),
                        radius = r,
                        center = Offset(x, y)
                    )
                }
            }

            PatternKind.Bokeh -> {
                val count = 18

                for (i in 0 until count) {
                    val fx = hash01(i, 3)
                    val fy = hash01(i, 4)

                    val x = fx * size.width
                    val y = fy * size.height
                    val r = (18f + (i % 5) * 14f).dp.toPx()

                    drawCircle(
                        color = col.copy(alpha = col.alpha * 0.22f),
                        radius = r,
                        center = Offset(x, y)
                    )
                }
            }

            PatternKind.Diamonds -> {
                val spacing = 42.dp.toPx()
                var x = -size.height

                while (x < size.width) {
                    drawLine(
                        color = col,
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1f
                    )

                    drawLine(
                        color = col,
                        start = Offset(x + size.height, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )

                    x += spacing
                }
            }
        }
    }
}

private fun hash01(i: Int, salt: Int): Float {
    val v = abs(sin(i * 12.9898f + salt * 78.233f) * 43758.5453f)
    return v % 1f
}
