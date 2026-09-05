package ir.yaddasht.app.util

import android.util.Log
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object TextExtractor {
    private const val TAG = "TextExtractor"
    
    // ذخیرهٔ آخرین خطا برای نمایش به کاربر
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
                    // TXT و سایر فرمت‌های متنی
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
            Log.e(TAG, "extract error: ${e.message}", e)
            ""
        }
    }

    fun fromDocx(input: InputStream): String {
        val sb = StringBuilder()
        var found = false
        
        try {
            ZipInputStream(input).use { z ->
                var entry = z.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        found = true
                        val content = z.readBytes().toString(Charsets.UTF_8)
                        
                        // استخراج پاراگراف به پاراگراف
                        val paraRegex = Regex("<w:p[ >].*?</w:p>", RegexOption.DOT_MATCHES_ALL)
                        val textRegex = Regex("<w:t[^>]*>([^<]*)</w:t>")
                        
                        for (para in paraRegex.findAll(content)) {
                            val paraContent = para.value
                            val texts = textRegex.findAll(paraContent).map { it.groupValues[1] }
                            val paraText = texts.joinToString("")
                            if (paraText.isNotBlank()) {
                                sb.append(paraText).append("\n\n")
                            }
                        }
                        
                        // اگر پاراگراف پیدا نشد، fallback به روش ساده
                        if (sb.isEmpty()) {
                            val texts = textRegex.findAll(content).map { it.groupValues[1] }
                            sb.append(texts.joinToString(" "))
                        }
                        break
                    }
                    entry = z.nextEntry
                }
            }
        } catch (e: Exception) {
            lastError = "خطا در خواندن DOCX: ${e.message}"
            Log.e(TAG, "fromDocx error: ${e.message}", e)
            return ""
        }
        
        if (!found) {
            lastError = "فایل Word معتبر نیست (word/document.xml پیدا نشد)"
            return ""
        }
        
        val result = sb.toString().trim()
        if (result.isEmpty()) {
            lastError = "فایل Word خالی است"
        }
        return result
    }
}
