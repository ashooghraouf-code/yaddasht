package ir.yaddasht.app.util

import android.util.Log
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object TextExtractor {
    private const val TAG = "TextExtractor"
    private const val MAX_FILE_SIZE = 5L * 1024 * 1024 // 5MB
    private const val MAX_TEXT_LENGTH = 500_000 // 500K کاراکتر

    var lastError: String? = null
        private set

    fun extract(path: String): String {
        lastError = null
        val file = File(path)

        if (!file.exists()) {
            lastError = "فایل وجود ندارد"
            return ""
        }

        if (file.length() > MAX_FILE_SIZE) {
            lastError = "فایل بزرگ‌تر از ۵ مگابایت است (${file.length() / 1024 / 1024}MB). لطفاً فایل کوچک‌تر استفاده کنید."
            return ""
        }

        if (file.length() == 0L) {
            lastError = "فایل خالی است"
            return ""
        }

        val lower = path.lowercase()
        return try {
            when {
                lower.endsWith(".docx") -> fromDocx(file)
                lower.endsWith(".doc") -> {
                    lastError = "فرمت .doc قدیمی پشتیبانی نمی‌شود. فایل را به .docx تبدیل کنید."
                    ""
                }
                lower.endsWith(".txt") -> readFile(file)
                else -> readFile(file)
            }
        } catch (e: OutOfMemoryError) {
            lastError = "حافظه کافی نیست (فایل خیلی بزرگ است)"
            ""
        } catch (e: Exception) {
            lastError = "خطا: ${e.javaClass.simpleName} - ${e.message}"
            Log.e(TAG, "extract", e)
            ""
        }
    }

    private fun readFile(file: File): String {
        return try {
            val text = file.readText(Charsets.UTF_8)
            if (text.length > MAX_TEXT_LENGTH) text.take(MAX_TEXT_LENGTH) + "\n\n[... ادامه حذف شد - فایل خیلی بزرگ است]"
            else text
        } catch (e: Exception) {
            lastError = "خطا: ${e.message}"
            ""
        }
    }

    private fun fromDocx(file: File): String {
        val paragraphs = mutableListOf<String>()
        var found = false
        var bytesRead = 0L

        try {
            file.inputStream().use { fis ->
                ZipInputStream(fis).use { z ->
                    var entry = z.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            found = true
                            val bytes = z.readBytes()
                            bytesRead = bytes.size.toLong()
                            if (bytesRead > MAX_FILE_SIZE) {
                                lastError = "محتوای Word بیش از حد بزرگ است"
                                return ""
                            }
                            parse(bytes.toString(Charsets.UTF_8), paragraphs)
                            break
                        }
                        entry = z.nextEntry
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            lastError = "فایل Word خیلی بزرگ است"
            return ""
        } catch (e: Exception) {
            lastError = "خطا در Word: ${e.javaClass.simpleName}"
            return ""
        }

        if (!found) { lastError = "فایل Word معتبر نیست"; return "" }
        if (paragraphs.isEmpty()) { lastError = "فایل Word خالی است"; return "" }

        val result = paragraphs.joinToString("\n\n")
        return if (result.length > MAX_TEXT_LENGTH) result.take(MAX_TEXT_LENGTH) + "\n\n[... ادامه حذف شد]"
        else result
    }

    private fun parse(content: String, out: MutableList<String>) {
        val paraRegex = Regex("<w:p[ >].*?</w:p>", RegexOption.DOT_MATCHES_ALL)
        val textRegex = Regex("<w:t[^>]*>([^<]*)</w:t>")
        for (para in paraRegex.findAll(content)) {
            val t = textRegex.findAll(para.value).map { it.groupValues[1] }.joinToString("")
            if (t.isNotBlank()) out.add(t)
        }
    }
}
