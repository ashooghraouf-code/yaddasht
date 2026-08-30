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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.text.style.TextAlign
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
import ir.yaddasht.app.reminder.LeadTime
import ir.yaddasht.app.reminder.ReminderScheduler
import ir.yaddasht.app.ui.theme.Brick
import ir.yaddasht.app.ui.theme.DeepGreen
import ir.yaddasht.app.ui.theme.DeepGreenSoft
import ir.yaddasht.app.ui.theme.Ink
import ir.yaddasht.app.ui.theme.InkSoft
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.LineGreen
import ir.yaddasht.app.ui.theme.PaperColors
import ir.yaddasht.app.ui.theme.PaperWhite
import ir.yaddasht.app.ui.theme.Saffron
import ir.yaddasht.app.ui.theme.VazirFont
import ir.yaddasht.app.ui.theme.paperColor
import ir.yaddasht.app.util.AiAnalysisDialog
import ir.yaddasht.app.util.AttachmentStore
import ir.yaddasht.app.util.Checklist
import ir.yaddasht.app.util.FaDate
import ir.yaddasht.app.util.NoteLock
import ir.yaddasht.app.util.PdfExporter
import ir.yaddasht.app.util.Recovery
import ir.yaddasht.app.util.fa
import ir.yaddasht.app.util.faDigits
import ir.yaddasht.app.util.guessMimeType
import ir.yaddasht.app.util.shareAttachment
import ir.yaddasht.app.util.shareBackupFile
import ir.yaddasht.app.util.shareNoteText
import ir.yaddasht.app.util.sharePdf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private enum class LockMode { Set, Unlock }

private const val DOCX_CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""

private const val DOCX_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""

private fun xmlEsc(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun buildDocumentXml(title: String, body: String): String {
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
    sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>")
    sb.append("<w:p><w:pPr><w:jc w:val=\"right\"/><w:rtl/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"44\"/><w:rtl/></w:rPr><w:t xml:space=\"preserve\">")
    sb.append(xmlEsc(title))
    sb.append("</w:t></w:r></w:p>")
    body.split("\n").forEach { line ->
        sb.append("<w:p><w:pPr><w:jc w:val=\"right\"/><w:rtl/></w:pPr><w:r><w:rPr><w:rtl/></w:rPr><w:t xml:space=\"preserve\">")
        sb.append(xmlEsc(line))
        sb.append("</w:t></w:r></w:p>")
    }
    sb.append("<w:sectPr/></w:body></w:document>")
    return sb.toString()
}

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
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
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
    var showLeads by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableLongStateOf(0L) }
    var leads by remember { mutableStateOf<Set<LeadTime>>(setOf(LeadTime.NONE, LeadTime.HOUR_1)) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    val isLocked = note?.body?.let(NoteLock::isLocked) == true
    val isChecklist = note?.body?.let(Checklist::isChecklist) == true

    LaunchedEffect(Unit) {
        if (noteId == NEW_NOTE_ID) realId = withContext(Dispatchers.IO) { dao.insert(Note()) }
        ready = true
    }
    LaunchedEffect(ready, realId) {
        if (!ready) return@LaunchedEffect
        dao.observeNote(realId).collect { n ->
            note = n
            if (lastSaved == null) lastSaved = n
        }
    }
    LaunchedEffect(ready, realId) {
        if (!ready) return@LaunchedEffect
        dao.observeAttachments(realId).collect { attachments = it }
    }
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        snapshotFlow { note }.debounce(350).collect { n ->
            val saved = lastSaved
            if (n != null && saved != null && n.copy(updatedAt = saved.updatedAt) != saved) {
                val stamped = n.copy(updatedAt = System.currentTimeMillis())
                lastSaved = stamped
                withContext(Dispatchers.IO) { dao.update(stamped) }
            }
        }
    }
    LaunchedEffect(recorder != null) {
        if (recorder != null) {
            while (true) {
                delay(1000)
                recordSeconds++
            }
        }
    }

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
            scope.launch(Dispatchers.IO) {
                dao.insertAttachment(Attachment(noteId = realId, fileName = file.name, filePath = file.absolutePath, mimeType = "image/jpeg", isImage = true))
            }
        } else file?.delete()
        pendingCameraFile = null
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { importUris(context, scope, dao, realId, it) }
    val pickDocs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { importUris(context, scope, dao, realId, it) }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) {
                val current = note?.body.orEmpty()
                note = note?.copy(body = if (current.isBlank()) text else "$current\n$text")
            }
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun startRecording() {
        val file = AttachmentStore.createAudioFile(context)
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordingFile = file
            recordSeconds = 0
        } catch (e: Exception) {
            r.release()
            Toast.makeText(context, "ضبط شروع نشد", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        val f = recordingFile
        recordingFile = null
        if (f != null && f.length() > 2000) {
            scope.launch(Dispatchers.IO) {
                dao.insertAttachment(Attachment(noteId = realId, fileName = f.name, filePath = f.absolutePath, mimeType = "audio/mp4", isImage = false))
            }
        } else {
            f?.delete()
            Toast.makeText(context, "ضبط خیلی کوتاه بود", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
        else Toast.makeText(context, "بدون دسترسی میکروفون ضبط ممکن نیست", Toast.LENGTH_SHORT).show()
    }

    fun micClick() {
        if (recorder != null) stopRecording()
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
        else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun launchSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "حرف بزن…")
        }
        try { speechLauncher.launch(intent) }
        catch (e: Exception) { Toast.makeText(context, "این دستگاه دیکته ندارد", Toast.LENGTH_SHORT).show() }
    }

    fun scheduleReminder(ts: Long, leadsSet: Set<LeadTime>) {
        val n = note ?: return
        note = n.copy(reminderAt = ts)
        ReminderScheduler.scheduleMulti(context, realId, n.title.ifBlank { "یادداشت" }, ts, false, leadsSet)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        Toast.makeText(context, "یادآور با ${leadsSet.size.fa
