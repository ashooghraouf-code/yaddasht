package ir.yaddasht.app.widget

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.Attachment
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.util.AttachmentStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class JournalistActionActivity : Activity() {

    companion object {
        const val ACTION_CAMERA = "JOURNALIST_CAMERA"
        const val ACTION_MIC = "JOURNALIST_MIC"
        private const val REQ_VIDEO = 100
        private const val REQ_SPEECH = 101
        private const val REQ_AUDIO_PERM = 102
    }

    private var noteId: Long = 0
    private var pendingFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch {
            noteId = AppDatabase.get(applicationContext).dao().insert(Note())
            withContext(Dispatchers.Main) {
                when (intent?.action) {
                    ACTION_CAMERA -> launchVideoCapture()
                    ACTION_MIC -> checkMicPermissionAndLaunch()
                    else -> finish()
                }
            }
        }
    }

    // ✅ فیلمبرداری به جای عکس
    private fun launchVideoCapture() {
        try {
            val file = AttachmentStore.createCameraFile(this)
            // تغییر پسوند به mp4
            val videoFile = File(file.parent, file.nameWithoutExtension + ".mp4")
            pendingFile = videoFile
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", videoFile)
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) // کیفیت بالا
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 300) // حداکثر ۵ دقیقه
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQ_VIDEO)
        } catch (e: Exception) {
            Toast.makeText(this, "دوربین باز نشد", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkMicPermissionAndLaunch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecognition()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO_PERM)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUDIO_PERM) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchSpeechRecognition()
            } else {
                Toast.makeText(this, "بدون دسترسی میکروفون ممکن نیست", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun launchSpeechRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "حرف بزن…")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            startActivityForResult(intent, REQ_SPEECH)
        } catch (e: Exception) {
            Toast.makeText(this, "دیکته در این دستگاه موجود نیست", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_VIDEO -> {
                val file = pendingFile
                if (resultCode == RESULT_OK && file != null && file.length() > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.get(applicationContext).dao().insertAttachment(
                            Attachment(
                                noteId = noteId,
                                fileName = file.name,
                                filePath = file.absolutePath,
                                mimeType = "video/mp4",
                                isImage = false
                            )
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@JournalistActionActivity, "🎬 فیلم ذخیره شد", Toast.LENGTH_SHORT).show()
                            openNoteInApp()
                        }
                    }
                } else {
                    file?.delete()
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.get(applicationContext).dao().deleteById(noteId)
                        withContext(Dispatchers.Main) { finish() }
                    }
                }
            }

            REQ_SPEECH -> {
                if (resultCode == RESULT_OK && data != null) {
                    val text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = AppDatabase.get(applicationContext).dao()
                            val note = dao.observeNote(noteId).first()
                            dao.update((note ?: Note()).copy(body = text, updatedAt = System.currentTimeMillis()))
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@JournalistActionActivity, "🎙️ متن ذخیره شد", Toast.LENGTH_SHORT).show()
                                openNoteInApp()
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            AppDatabase.get(applicationContext).dao().deleteById(noteId)
                            withContext(Dispatchers.Main) { finish() }
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.get(applicationContext).dao().deleteById(noteId)
                        withContext(Dispatchers.Main) { finish() }
                    }
                }
            }
        }
    }

    private fun openNoteInApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("note_id", noteId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
