package ir.yaddasht.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AttachmentStore {

    fun copyToPrivate(context: Context, uri: Uri): File? {
        return try {
            val name = displayName(context, uri) ?: "file_${System.currentTimeMillis()}"
            val target = File(attachmentsDir(context), uniqueName(name))
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { i -> FileOutputStream(target).use { o -> i.copyTo(o) } }
            target
        } catch (e: Exception) { null }
    }

    fun createCameraFile(context: Context): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(cameraDir(context), "IMG_$stamp.jpg")
    }

    fun createAudioFile(context: Context): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(attachmentsDir(context), "AUD_$stamp.m4a")
    }

    fun attachmentsDir(context: Context) =
        File(context.filesDir, "attachments").apply { mkdirs() }

    private fun cameraDir(context: Context) =
        File(context.filesDir, "camera").apply { mkdirs() }

    private fun displayName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) name = c.getString(i)
            }
        } catch (_: Exception) {}
        return name ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun uniqueName(name: String): String {
        val clean = name.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_")
        return "${System.currentTimeMillis()}_$clean"
    }
}

fun guessMimeType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"; "png" -> "image/png"; "gif" -> "image/gif"
    "webp" -> "image/webp"; "pdf" -> "application/pdf"; "txt" -> "text/plain"
    "mp3" -> "audio/mpeg"; "mp4" -> "video/mp4"; "m4a" -> "audio/mp4"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "xls" -> "application/vnd.ms-excel"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    else -> "application/octet-stream"
}