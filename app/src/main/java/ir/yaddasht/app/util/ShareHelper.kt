package ir.yaddasht.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import ir.yaddasht.app.data.Attachment
import java.io.File

fun shareNoteText(context: Context, title: String, body: String) {
    val text = buildString {
        if (title.isNotBlank()) append("📌 ").append(title).append("\n\n")
        append(body)
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }, "اشتراک‌گذاری یادداشت"))
}

fun shareAttachment(context: Context, attachment: Attachment) {
    val file = File(attachment.filePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = attachment.mimeType.ifBlank { "*/*" }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "ارسال ${attachment.fileName}"))
}

fun sharePdf(context: Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "ارسال PDF"))
}

fun shareBackupFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "پشتیبان یادداشت")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "ذخیره / ارسال پشتیبان"))
}