package ir.yaddasht.app.util

import android.util.Log
import android.util.Xml
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser

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
            if (text.length > MAX_TEXT_LENGTH) {
                text.take(MAX_TEXT_LENGTH) + "\n\n[... ادامه حذف شد - فایل خیلی بزرگ است]"
            } else {
                text
            }
        } catch (e: Exception) {
            lastError = "خطا در خواندن فایل: ${e.message}"
            ""
        }
    }

    private fun fromDocx(file: File): String {
        val paragraphs = mutableListOf<String>()
        var found = false

        try {
            file.inputStream().use { fis ->
                ZipInputStream(fis).use { z ->
                    var entry = z.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            found = true
                            parseDocxXml(z, paragraphs)
                            break
                        }
                        entry = z.nextEntry
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            lastError = "فایل Word خیلی بزرگ است و حافظه کافی نیست"
            return ""
        } catch (e: Exception) {
            lastError = "خطا در پردازش Word: ${e.javaClass.simpleName}"
            Log.e(TAG, "fromDocx", e)
            return ""
        }

        if (!found) {
            lastError = "فایل Word معتبر نیست یا ساختار آن خراب است"
            return ""
        }
        if (paragraphs.isEmpty()) {
            lastError = "فایل Word خالی است یا متنی برای استخراج ندارد"
            return ""
        }

        val result = paragraphs.joinToString("\n\n")
        return if (result.length > MAX_TEXT_LENGTH) {
            result.take(MAX_TEXT_LENGTH) + "\n\n[... ادامه حذف شد]"
        } else {
            result
        }
    }

    private fun parseDocxXml(input: InputStream, out: MutableList<String>) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, Charsets.UTF_8.name())

        var eventType = parser.eventType
        val currentParagraph = StringBuilder()
        var inTextTag = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name ?: ""
                    if (name == "p") {
                        currentParagraph.setLength(0)
                    } else if (name == "t") {
                        inTextTag = true
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inTextTag) {
                        parser.text?.let { currentParagraph.append(it) }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val name = parser.name ?: ""
                    if (name == "t") {
                        inTextTag = false
                    } else if (name == "p") {
                        val paragraphText = currentParagraph.toString().trim()
                        if (paragraphText.isNotEmpty()) {
                            out.add(paragraphText)
                        }
                        currentParagraph.setLength(0)
                    }
                }
            }
            eventType = parser.next()
        }
    }
}
