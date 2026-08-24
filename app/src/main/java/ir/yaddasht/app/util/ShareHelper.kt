package ir.yaddasht.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ir.yaddasht.app.data.Attachment
import java.io.File

fun shareBackupFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".doc")) "application/msword" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری / ذخیره"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun sharePdf(context: Context, file: File, title: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری PDF"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "خطا: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun shareNoteText(context: Context, title: String, body: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n\n$body")
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری متن"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "خطا: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

// ✅ اضافه شد: اشتراک‌گذاری ضمیمه (تصویر / صدا / فایل)
fun shareAttachment(context: Context, attachment: Attachment) {
    try {
        val file = File(attachment.filePath)
        if (!file.exists()) {
            android.widget.Toast.makeText(context, "فایل یافت نشد", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = attachment.mimeType.ifBlank { guessMimeType(file.name) }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, attachment.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری ${attachment.fileName}"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
