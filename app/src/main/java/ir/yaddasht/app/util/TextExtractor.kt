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
    private const val MAX_TEXT_LENGTH = 500_000

    var lastError: String? = null
        private set

    fun extract(path: String): String {
        lastError = null
        val file = File(path)
        if (!file.exists()) { lastError = "فایل وجود ندارد"; return "" }
        if (file.length() > MAX_FILE_SIZE) { lastError = "فایل بزرگ‌تر از ۵ مگابایت است"; return "" }
        if (file.length() == 0L) { lastError = "فایل خالی است"; return "" }

        val lower = path.lowercase()
        return try {
            when {
                lower.endsWith(".docx") -> fromDocx(file)
                lower.endsWith(".doc") -> { lastError = "فرمت .doc قدیمی پشتیبانی نمی‌شود"; "" }
                lower.endsWith(".txt") -> readFile(file)
                else -> readFile(file)
            }
        } catch (e: Exception) {
            lastError = "خطا: ${e.message}"
            Log.e(TAG, "extract", e)
            ""
        }
    }

    private fun readFile(file: File): String {
        return try {
            val text = file.readText(Charsets.UTF_8)
            if (text.length > MAX_TEXT_LENGTH) text.take(MAX_TEXT_LENGTH) + "\n\n[... ادامه حذف شد]" else text
        } catch (e: Exception) { lastError = "خطا در خواندن فایل"; "" }
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
        } catch (e: Exception) { lastError = "خطا در پردازش Word"; return "" }

        if (!found) { lastError = "ساختار فایل Word معتبر نیست"; return "" }
        if (paragraphs.isEmpty()) { lastError = "متنی در فایل یافت نشد"; return "" }

        val result = paragraphs.joinToString("\n\n")
        return if (result.length > MAX_TEXT_LENGTH) result.take(MAX_TEXT_LENGTH) + "\n\n[... ادامه حذف شد]" else result
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
                    if (name == "p") currentParagraph.setLength(0)
                    else if (name == "t") inTextTag = true
                }
                XmlPullParser.TEXT -> {
                    if (inTextTag) parser.text?.let { currentParagraph.append(it) }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name ?: ""
                    if (name == "t") inTextTag = false
                    else if (name == "p") {
                        val text = currentParagraph.toString().trim()
                        if (text.isNotEmpty()) out.add(text)
                        currentParagraph.setLength(0)
                    }
                }
            }
            eventType = parser.next()
        }
    }
}
