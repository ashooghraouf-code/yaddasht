package ir.yaddasht.app.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import ir.yaddasht.app.NEW_NOTE_ID
import ir.yaddasht.app.data.Attachment
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.reminder.ReminderScheduler
import ir.yaddasht.app.ui.theme.*
import ir.yaddasht.app.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LockMode { Set, Unlock }

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
private fun safeName(s: String) = s.take(24).replace(Regex("[^\\p{L}\\p{N}_-]"), "_").ifBlank { "note" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(dao: NoteDao, noteId: Long, onBack: () -> Unit, onOpenDraw: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var realId by remember { mutableLongStateOf(noteId) }
    var ready by remember { mutableStateOf(noteId != NEW_NOTE_ID) }
    var note by remember { mutableStateOf<Note?>(null) }
    var lastSaved by remember { mutableStateOf<Note?>(null) }
    var attachments by remember { mutableStateOf(emptyList<Attachment>()) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var showPalette by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var viewerImage by remember { mutableStateOf<Attachment?>(null) }
    var showFocus by remember { mutableStateOf(false) }
    var showAi by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var lockMode by remember { mutableStateOf<LockMode?>(null) }
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var lockError by remember { mutableStateOf("") }
    var showRecoveryCode by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableLongStateOf(0L) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    val isLocked = note?.body?.let(NoteLock::isLocked) == true
    val isChecklist = note?.body?.let(Checklist::isChecklist) == true

    LaunchedEffect(Unit) {
        if (noteId == NEW_NOTE_ID) realId = withContext(Dispatchers.IO) { dao.insert(Note()) }
        ready = true
    }
    LaunchedEffect(ready, realId) { if (!ready) return@LaunchedEffect; dao.observeNote(realId).collect { n -> note = n; if (lastSaved == null) lastSaved = n } }
    LaunchedEffect(ready, realId) { if (!ready) return@LaunchedEffect; dao.observeAttachments(realId).collect { attachments = it } }
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        snapshotFlow { note }.debounce(350).collect { n ->
            val saved = lastSaved
            if (n != null && saved != null && n.copy(updatedAt = saved.updatedAt) != saved) {
                val stamped = n.copy(updatedAt = System.currentTimeMillis()); lastSaved = stamped
                withContext(Dispatchers.IO) { dao.update(stamped) }
            }
        }
    }
    LaunchedEffect(recorder != null) { if (recorder != null) while (true) { delay(1000); recordSeconds++ } }

    val exit: () -> Unit = {
        scope.launch {
            val n = note
            if (n != null) {
                val count = withContext(Dispatchers.IO) { dao.attachmentCount(n.id) }
                withContext(Dispatchers.IO) {
                    if (n.title.isBlank() && n.body.isBlank() && count == 0) dao.deleteById(n.id)
                    else dao.update(n.copy(updatedAt = System.currentTimeMillis()))
                }
            }
            onBack()
        }
    }
    BackHandler(onBack = exit)

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingCameraFile
        if (ok && file != null && file.length() > 0) {
            scope.launch(Dispatchers.IO) { dao.insertAttachment(Attachment(noteId = realId, fileName = file.name, filePath = file.absolutePath, mimeType = "image/jpeg", isImage = true)) }
        } else file?.delete()
        pendingCameraFile = null
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { importUris(context, scope, dao, realId, it) }
    val pickDocs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { importUris(context, scope, dao, realId, it) }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) { val current = note?.body.orEmpty(); note = note?.copy(body = if (current.isBlank()) text else "$current\n$text") }
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun startRecording() {
        val file = AttachmentStore.createAudioFile(context)
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setOutputFile(file.absolutePath)
            r.prepare(); r.start(); recorder = r; recordingFile = file; recordSeconds = 0
        } catch (e: Exception) { r.release(); Toast.makeText(context, "ضبط شروع نشد", Toast.LENGTH_SHORT).show() }
    }
    fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release(); recorder = null
        val f = recordingFile; recordingFile = null
        if (f != null && f.length() > 2000) {
            scope.launch(Dispatchers.IO) { dao.insertAttachment(Attachment(noteId = realId, fileName = f.name, filePath = f.absolutePath, mimeType = "audio/mp4", isImage = false)) }
        } else { f?.delete(); Toast.makeText(context, "ضبط خیلی کوتاه بود", Toast.LENGTH_SHORT).show() }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(context, "بدون دسترسی میکروفون ضبط ممکن نیست", Toast.LENGTH_SHORT).show()
    }
    fun micClick() {
        if (recorder != null) stopRecording()
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
        else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    fun launchSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR"); putExtra(RecognizerIntent.EXTRA_PROMPT, "حرف بزن… 🎙️")
        }
        try { speechLauncher.launch(intent) } catch (e: Exception) { Toast.makeText(context, "این دستگاه دیکته ندارد", Toast.LENGTH_SHORT).show() }
    }
    fun scheduleReminder(ts: Long) {
        val n = note ?: return
        note = n.copy(reminderAt = ts)
        ReminderScheduler.schedule(context, realId, n.title, ts)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        Toast.makeText(context, "یادآور تنظیم شد ⏰", Toast.LENGTH_SHORT).show()
    }
    fun exportPdf() {
        val n = note ?: return
        val pdfNote = if (isLocked) n.copy(body = "🔒 این یادداشت قفل است") else n
        scope.launch(Dispatchers.IO) {
            val file = PdfExporter.exportNote(context, pdfNote)
            withContext(Dispatchers.Main) { if (file != null) sharePdf(context, file, n.title) else Toast.makeText(context, "ساخت PDF ناموفق بود", Toast.LENGTH_SHORT).show() }
        }
    }
    fun exportWord() {
        val n = note ?: return
        scope.launch(Dispatchers.IO) {
            val html = "<html dir='rtl'><head><meta charset='utf-8'></head><body><h1>${esc(n.title)}</h1><p>${esc(n.body).replace("\n", "<br/>")}</p></body></html>"
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "${safeName(n.title)}.doc")
            file.writeText(html, Charsets.UTF_8)
            withContext(Dispatchers.Main) { shareBackupFile(context, file) }
        }
    }
    fun exportJsonNote() {
        val n = note ?: return
        scope.launch(Dispatchers.IO) {
            val atts = dao.attachmentsByNote(n.id)
            val json = JSONObject().apply {
                put("title", n.title); put("body", n.body); put("color", n.color); put("pinned", n.pinned)
                put("reminderAt", n.reminderAt); put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
                put("attachments", JSONArray().apply { atts.forEach { a -> put(JSONObject().apply {
                    put("fileName", a.fileName); put("mimeType", a.mimeType); put("isImage", a.isImage)
                }) } })
            }
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "${safeName(n.title)}.json")
            file.writeText(json.toString(2), Charsets.UTF_8)
            withContext(Dispatchers.Main) { shareBackupFile(context, file) }
        }
