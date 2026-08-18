package ir.yaddasht.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import ir.yaddasht.app.R
import ir.yaddasht.app.data.Note
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun exportNote(context: Context, note: Note): File? {
        return try {
            val doc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 50f
            val contentWidth = (pageWidth - margin * 2).toInt()

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 42f; color = Color.parseColor("#22302B")
                typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 17f; color = Color.parseColor("#33413B")
                typeface = ResourcesCompat.getFont(context, R.font.vazirmatn)
            }
            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 12f; color = Color.parseColor("#5C6B63")
                textAlign = Paint.Align.RIGHT
            }

            val titleText = note.title.ifBlank { "بدون عنوان" }
            val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            val bodyText = note.body.ifBlank { " " }
            val bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(7f, 1.25f)
                .build()

            var pageIndex = 0
            var currentPage: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var contentTop = margin

            fun startPage(withHeader: Boolean) {
                currentPage?.let { doc.finishPage(it) }
                val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
                val page = doc.startPage(info)
                currentPage = page
                canvas = page.canvas
                canvas!!.drawColor(Color.parseColor("#FBF7EC"))
                contentTop = margin
                if (withHeader) {
                    val c = canvas!!
                    c.save(); c.translate(margin, contentTop); titleLayout.draw(c); c.restore()
                    contentTop += titleLayout.height + 14f
                    val dateStr = SimpleDateFormat("d MMMM yyyy – HH:mm", Locale.forLanguageTag("fa")).format(Date(note.updatedAt))
                    c.drawText(dateStr, pageWidth - margin, contentTop + 12f, metaPaint)
                    contentTop += 30f
                    val sep = Paint().apply { color = Color.parseColor("#F2A93B"); strokeWidth = 3f }
                    c.drawLine(margin, contentTop, pageWidth - margin, contentTop, sep)
                    contentTop += 26f
                }
            }

            startPage(withHeader = true)
            var layoutOffset = 0f
            var available = (pageHeight - margin) - contentTop
            while (layoutOffset < bodyLayout.height) {
                val chunk = minOf(bodyLayout.height - layoutOffset, available)
                canvas!!.save()
                canvas!!.clipRect(margin, contentTop, pageWidth - margin, contentTop + chunk)
                canvas!!.translate(margin, contentTop - layoutOffset)
                bodyLayout.draw(canvas!!)
                canvas!!.restore()
                layoutOffset += chunk
                if (layoutOffset < bodyLayout.height) {
                    startPage(withHeader = false)
                    available = (pageHeight - margin) - contentTop
                }
            }
            currentPage?.let { doc.finishPage(it) }

            val dir = File(context.filesDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "yaddasht_$stamp.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }
}
