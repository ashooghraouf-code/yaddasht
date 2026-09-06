package ir.yaddasht.app.util

import android.util.Log
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object TextExtractor {
    var lastError: String? = null
        private set

    fun extract(path: String): String {
        lastError = null
        val lower = path.lowercase()
        return try {
            when {
                lower.endsWith(".docx") -> fromDocx(File(path).inputStream())
                lower.endsWith(".doc") -> { lastError = "فرمت .doc پشتیبانی نمی‌شود؛ به .docx تبدیل کنید."; "" }
                else -> try { File(path).readText(Charsets.UTF_8) } catch (e: Exception) { lastError = "خطا: ${e.message}"; "" }
            }
        } catch (e: Exception) { lastError = "خطا: ${e.message}"; Log.e("TextExtractor", "err", e); "" }
    }

    fun fromDocx(input: InputStream): String {
        val paragraphs = mutableListOf<String>(); var found = false
        try {
            ZipInputStream(input).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    if (e.name == "word/document.xml") { found = true; parse(z.readBytes().toString(Charsets.UTF_8), paragraphs); break }
                    e = z.nextEntry
                }
            }
        } catch (e: Exception) { lastError = "خطا DOCX: ${e.message}"; return "" }
        if (!found) { lastError = "فایل Word معتبر نیست"; return "" }
        if (paragraphs.isEmpty()) { lastError = "فایل Word خالی است"; return "" }
        return paragraphs.joinToString("\n\n")
    }

    private fun parse(content: String, out: MutableList<String>) {
        val paraRegex = Regex("<w:p[ >].*?</w:p>", RegexOption.DOT_MATCHES_ALL)
        val textRegex = Regex("<w:t[^>]*>([^<]*)</w:t>")
        for (para in paraRegex.findAll(content)) {
            val t = textRegex.findAll(para.value).map { it.groupValues[1] }.joinToString("")
            out.add(t)
        }
    }
}
