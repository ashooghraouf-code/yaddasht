package ir.yaddasht.app.ui.screen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import java.io.File

class FilePickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword",
                    "text/plain"
                )
            )
        }

        try {
            startActivityForResult(intent, PICK_FILE_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(this, "فایل‌پیکر در دسترس نیست", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                try {
                    // ✅ ذخیره دائمی در filesDir (نه cacheDir) تا پیشرفت و هایلایت‌ها پایدار بمانند
                    val readerDir = File(filesDir, "reader").apply { mkdirs() }
                    val fileName = getFileName(uri) ?: "file"
                    val targetFile = File(readerDir, fileName)

                    contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }

                    val isPdf = fileName.endsWith(".pdf", ignoreCase = true)
                    val readerIntent = Intent(this, ReaderActivity::class.java).apply {
                        putExtra("file_path", targetFile.absolutePath)
                        putExtra("is_pdf", isPdf)
                    }
                    startActivity(readerIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "خطا در باز کردن فایل: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        finish()
    }

    private fun getFileName(uri: android.net.Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    companion object {
        private const val PICK_FILE_REQUEST = 1001
    }
}
