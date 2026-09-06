package ir.yaddasht.app.util

import android.util.Log
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object TextExtractor {
    private const val TAG = "TextExtractor"

    var lastError: String? = null
        private set

    fun extract(path: String): String {
        lastError = null
        val lower = path.lowercase()
        return try {
            when {
                lower.endsWith(".docx") -> fromDocx(File(path).inputStream())
                lower.endsWith(".doc") -> {
                    lastError = "فرمت .doc قدیمی پشتیبانی نمی‌شود. فایل را به .docx تبدیل کنید."
                    ""
                }
                else -> {
                    try {
                        File(path).readText(Charsets.UTF_8)
                    } catch (e: Exception) {
                        lastError = "خطا در خواندن فایل: ${e.message}"
                        ""
                    }
                }
            }
        } catch (e: Exception) {
            lastError = "خطای غیرمنتظره: ${e.message}"
            Log.e(TAG, "extract error", e)
            ""
        }
    }

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("\n", "<br>")

    fun fromDocx(input: InputStream): String {
        val paragraphs = mutableListOf<String>()
        var found = false

        try {
            ZipInputStream(input).use { z ->
                var entry = z.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        found = true
                        val content = z.readBytes().toString(Charsets.UTF_8)
                        parseParagraphs(content, paragraphs)
                        break
                    }
                    entry = z.nextEntry
                }
            }
        } catch (e: Exception) {
            lastError = "خطا در خواندن DOCX: ${e.message}"
            return ""
        }

        if (!found) {
            lastError = "فایل Word معتبر نیست"
            return ""
        }
        if (paragraphs.isEmpty()) {
            lastError = "فایل Word خالی است"
            return ""
        }

        // خروجی HTML برای حفظ ساختار در WebView
        val sb = StringBuilder()
        paragraphs.forEach { p ->
            sb.append("<p>").append(escapeHtml(p)).append("</p>")
        }
        return sb.toString()
    }

    private fun parseParagraphs(content: String, out: MutableList<String>) {
        val paraRegex = Regex("<w:p[ >].*?</w:p>", RegexOption.DOT_MATCHES_ALL)
        val textRegex = Regex("<w:t[^>]*>([^<]*)</w:t>")

        for (para in paraRegex.findAll(content)) {
            val texts = textRegex.findAll(para.value).map { it.groupValues[1] }
            val paraText = texts.joinToString("")
            out.add(paraText)
        }
    }
}
