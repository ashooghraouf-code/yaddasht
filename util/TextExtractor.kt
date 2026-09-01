package ir.yaddasht.app.util

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object TextExtractor {

    fun extract(path: String): String {
        val lower = path.lowercase()
        return try {
            when {
                lower.endsWith(".docx") || lower.endsWith(".doc") -> fromDocx(File(path).inputStream())
                else -> File(path).readText(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun fromDocx(input: InputStream): String {
        var xml: String? = null
        try {
            ZipInputStream(input).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    if (e.name == "word/document.xml") {
                        xml = z.readBytes().toString(Charsets.UTF_8)
                        break
                    }
                    e = z.nextEntry
                }
            }
        } catch (_: Exception) {
        }
        val raw = xml ?: return ""
        val regex = Regex("<w:t[^>]*>([^<]*)</w:t>")
        return raw.split
