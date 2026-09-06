package ir.yaddasht.app.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.util.Annotation
import ir.yaddasht.app.util.AnnotationPalette
import ir.yaddasht.app.util.AnnotationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfSession(private val renderer: PdfRenderer, private val pfd: ParcelFileDescriptor) {
    val pageCount: Int get() = try { renderer.pageCount } catch (_: Exception) { 0 }

    fun renderPage(index: Int, targetWidth: Int): Bitmap? {
        return try {
            synchronized(renderer) {
                renderer.openPage(index).use { page ->
                    val scale = targetWidth.toFloat() / page.width
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        } catch (_: Exception) { null }
    }

    fun close() {
        try { renderer.close() } catch (_: Exception) {}
        try { pfd.close() } catch (_: Exception) {}
    }
}

fun openPdfSession(path: String): PdfSession? {
    return try {
        val pfd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        PdfSession(PdfRenderer(pfd), pfd)
    } catch (_: Exception) { null }
}

@Composable
fun PdfViewer(
    session: PdfSession,
    initialPage: Int,
    zoom: Float,
    twoPage: Boolean,
    themeIndex: Int,
    annotations: List<Annotation>,
    onPageChanged: (Int) -> Unit,
    onPageTap: (pageIndex: Int, relX: Float, relY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = session.pageCount.coerceAtLeast(1)

    key(twoPage) {
        val spreadCount = if (twoPage) (total + 1) / 2 else total
        val initial = (if (twoPage) initialPage / 2 else initialPage).coerceIn(0, spreadCount - 1)
        val pagerState = rememberPagerState(initialPage = initial) { spreadCount }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { s ->
                onPageChanged(if (twoPage) (s * 2).coerceAtMost(total - 1) else s)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1
        ) { spread ->
            val firstIdx = spread * (if (twoPage) 2 else 1)
            val secondIdx = if (twoPage) spread * 2 + 1 else -1

            androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    PdfPageImage(
                        session = session,
                        index = firstIdx,
                        zoom = zoom,
                        themeIndex = themeIndex,
                        pageAnnotations = annotations.filter { it.pageIndex == firstIdx },
                        onPageTap = { rx, ry -> onPageTap(firstIdx, rx, ry) }
                    )
                }
                if (twoPage && secondIdx < total) {
                    Box(Modifier.weight(1f)) {
                        PdfPageImage(
                            session = session,
                            index = secondIdx,
                            zoom = zoom,
                            themeIndex = themeIndex,
                            pageAnnotations = annotations.filter { it.pageIndex == secondIdx },
                            onPageTap = { rx, ry -> onPageTap(secondIdx, rx, ry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageImage(
    session: PdfSession,
    index: Int,
    zoom: Float,
    themeIndex: Int,
    pageAnnotations: List<Annotation>,
    onPageTap: (Float, Float) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthPx = remember { context.resources.displayMetrics.widthPixels }
    val screenWidthDp = with(density) { screenWidthPx.toDp() }
    val targetWidth = (screenWidthPx * zoom).toInt().coerceAtLeast(1)

    var bitmap by remember(index, targetWidth) { mutableStateOf<Bitmap?>(null) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(index, targetWidth) {
        bitmap = withContext(Dispatchers.Default) { session.renderPage(index, targetWidth) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3A3A3A)),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            val bmpW = bmp.width
            val bmpH = bmp.height
            val aspect = bmpH.toFloat() / bmpW.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(index, imageSize) {
                        detectTapGestures { offset ->
                            if (imageSize.width > 0 && imageSize.height > 0) {
                                onPageTap(offset.x / imageSize.width, offset.y / imageSize.height)
                            }
                        }
                    }
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "صفحه ${index + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            androidx.compose.ui.layout.onSizeChanged { imageSize = it }.let { Modifier }
                        )
                )

                // Annotation overlay
                Box(Modifier.matchParentSize()) {
                    pageAnnotations.forEach { ann ->
                        val ac = AnnotationPalette.find(ann.colorKey)
                        when (ann.type) {
                            AnnotationType.HIGHLIGHT, AnnotationType.UNDERLINE -> {
                                Box(
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .offset(
                                            x = screenWidthDp * ann.relX,
                                            y = screenWidthDp * ann.relY * aspect
                                        )
                                        .size(
                                            width = (screenWidthDp * ann.relW).coerceAtLeast(8.dp),
                                            height = (screenWidthDp * ann.relH * aspect).coerceAtLeast(8.dp)
                                        )
                                        .background(ac.colorForTheme(themeIndex))
                                )
                            }
                            AnnotationType.NOTE -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(
                                            x = screenWidthDp * ann.relX - 14.dp,
                                            y = screenWidthDp * ann.relY * aspect - 14.dp
                                        )
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(ac.color),
                                    contentAlignment = Alignment.Center
                                ) { Text("📝", fontSize = 14.sp) }
                            }
                            AnnotationType.BOOKMARK -> {
                                Text(
                                    "🔖",
                                    fontSize = 20.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(
                                            x = screenWidthDp * ann.relX,
                                            y = screenWidthDp * ann.relY * aspect
                                        )
                                )
                            }
                            AnnotationType.UNDERLINE -> Unit
                        }
                    }
                }
            }
        } ?: run {
            // Loading indicator
            androidx.compose.material3.CircularProgressIndicator(color = Color.White)
        }
    }
}
