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
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import ir.yaddasht.app.util.shareNoteText
import ir.yaddasht.app.util.sharePdf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LockMode { Set, Unlock }

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
    LaunchedEffect(ready, realId) {
        if (!ready) return@LaunchedEffect
        dao.observeNote(realId).collect { n -> note = n; if (lastSaved == null) lastSaved = n }
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
        if (recorder != null) while (true) { delay(1000); recordSeconds++ }
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
            scope.launch(Dispatchers.IO) {
                dao.insertAttachment(Attachment(noteId = realId, fileName = f.name, filePath = f.absolutePath, mimeType = "audio/mp4", isImage = false))
            }
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
            withContext(Dispatchers.Main) {
                if (file != null) sharePdf(context, file, n.title) else Toast.makeText(context, "ساخت PDF ناموفق بود", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(containerColor = DeepGreen) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = exit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = PaperWhite) }
                Text(if (isLocked) "🔒 یادداشت محرمانه" else "یادداشت", fontFamily = LalezarFont, fontSize = 20.sp, color = PaperWhite,
                    modifier = Modifier.weight(1f).padding(start = 4.dp))
                IconButton(onClick = { note = note?.copy(pinned = !(note?.pinned ?: false)) }) {
                    Icon(Icons.Filled.PushPin, "سنجاق", tint = if (note?.pinned == true) Saffron else Color(0xFF5E8077))
                }
                IconButton(onClick = { note?.let { shareNoteText(context, it.title, if (isLocked) "🔒 (محتوا قفل است)" else it.body) } }) {
                    Icon(Icons.Filled.Share, "اشتراک‌گذاری", tint = PaperWhite)
                }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "حذف", tint = Brick) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolChip("🔒", if (isLocked) "باز کن" else "قفل") { lockError = ""; pass1 = ""; pass2 = ""; lockMode = if (isLocked) LockMode.Unlock else LockMode.Set }
                ToolChip("⏰", "یادآور") { showDatePicker = true }
                ToolChip("✅", if (isChecklist) "خروج از چک‌لیست" else "چک‌لیست") {
                    if (!isLocked) note?.let { n -> note = n.copy(body = if (isChecklist) Checklist.fromChecklist(n.body) else Checklist.toChecklist(n.body)) }
                }
                ToolChip("🗣️", "دیکته") { if (!isLocked) launchSpeech() }
                ToolChip("📄", "PDF") { if (!isLocked) exportPdf() }
                ToolChip("✒️", "تمرکز") { if (!isLocked && !isChecklist) showFocus = true }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(paperColor(note?.color ?: 0)).padding(16.dp)) {
                    val rem = note?.reminderAt ?: 0
                    if (rem > System.currentTimeMillis()) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Saffron.copy(alpha = .28f))
                            .padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰ یادآور: " + FaDate.full(rem) + " – " + SimpleDateFormat("HH:mm", Locale.US).format(Date(rem)),
                                fontSize = 12.sp, color = Ink, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.Close, "لغو یادآور", tint = Brick,
                                modifier = Modifier.size(20.dp).clickable {
                                    note = note?.copy(reminderAt = 0)
                                    ReminderScheduler.cancel(context, realId)
                                })
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    TextField(value = note?.title.orEmpty(), onValueChange = { note = note?.copy(title = it) },
                        placeholder = { Text("عنوان یادداشت…", fontFamily = LalezarFont, color = InkSoft, fontSize = 22.sp) },
                        textStyle = TextStyle(fontFamily = LalezarFont, fontSize = 26.sp, color = Ink),
                        colors = transparentFieldColors(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    if (!isLocked && attachments.isNotEmpty()) {
                        AttachmentsSection(attachments, { viewerImage = it }, { shareAttachment(context, it) },
                            { att -> scope.launch(Dispatchers.IO) { dao.deleteAttachment(att); File(att.filePath).delete() } })
                        Spacer(Modifier.height(10.dp))
                    }
                    when {
                        isLocked -> LockedBox { lockError = ""; pass1 = ""; lockMode = LockMode.Unlock }
                        isChecklist -> ChecklistEditor(note!!) { note = it }
                        else -> TextField(value = note?.body.orEmpty(), onValueChange = { note = note?.copy(body = it) },
                            placeholder = { Text("اینجا بنویس… یا از «دیکته» و «چک‌لیست» استفاده کن", color = InkSoft, fontSize = 15.sp) },
                            textStyle = TextStyle(fontFamily = VazirFont, fontSize = 15.sp, color = Ink, lineHeight = 26.sp),
                            colors = transparentFieldColors(), modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.fillMaxWidth().background(DeepGreen)) {
                AnimatedVisibility(showPalette) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PaperColors.forEachIndexed { i, c ->
                            Box(Modifier.size(32.dp).clip(CircleShape).background(c)
                                .border(if ((note?.color ?: 0) == i) 3.dp else 1.dp, if ((note?.color ?: 0) == i) Saffron else Color.Black.copy(alpha = .2f), CircleShape)
                                .clickable { note = note?.copy(color = i) })
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (recorder != null) RecordStopPill(recordSeconds, ::stopRecording)
                        else AttachButton("صدا", Icons.Filled.Mic, Modifier.weight(1f), ::micClick)
                        AttachButton("دوربین", Icons.Filled.CameraAlt, Modifier.weight(1f)) {
                            val file = AttachmentStore.createCameraFile(context); pendingCameraFile = file
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file); takePicture.launch(uri)
                        }
                        AttachButton("گالری", Icons.Filled.Image, Modifier.weight(1f)) { pickImages.launch("image/*") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AttachButton("فایل", Icons.Filled.AttachFile, Modifier.weight(1f)) { pickDocs.launch(arrayOf("*/*")) }
                        AttachButton("نقاشی", Icons.Filled.Brush, Modifier.weight(1f)) { onOpenDraw(realId) }
                        IconButton(onClick = { showPalette = !showPalette }) {
                            Icon(Icons.Filled.Palette, "رنگ", tint = if (showPalette) Saffron else Color(0xFF5E8077))
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text("حذف یادداشت؟", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = { Text("این یادداشت همراه با همهٔ ضمیمه‌هایش برای همیشه حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val n = note
                    scope.launch {
                        if (n != null) withContext(Dispatchers.IO) {
                            dao.attachmentsByNote(n.id).forEach { File(it.filePath).delete() }
                            dao.deleteById(n.id)
                        }
                        onBack()
                    }
                }) { Text("حذف", color = Brick, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } })
    }

    lockMode?.let { mode ->
        AlertDialog(onDismissRequest = { lockMode = null },
            title = { Text(if (mode == LockMode.Set) "🔒 گذاشتن رمز" else "🔓 باز کردن قفل", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                Column {
                    OutlinedTextField(pass1, { pass1 = it }, label = { Text(if (mode == LockMode.Unlock) "رمز یا کد بازیابی" else "رمز عبور") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    if (mode == LockMode.Set) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(pass2, { pass2 = it }, label = { Text("تکرار رمز") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    }
                    if (lockError.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(lockError, color = Brick, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when (mode) {
                        LockMode.Set -> when {
                            pass1.length < 4 -> lockError = "رمز حداقل ۴ کاراکتر باشد"
                            pass1 != pass2 -> lockError = "تکرار رمز یکسان نیست"
                            note?.body.isNullOrBlank() -> lockError = "یادداشت خالی است!"
                            else -> {
                                val original = note!!.body
                                val code = Recovery.genCode()
                                note = note?.copy(body = NoteLock.lock(original, pass1))
                                Recovery.saveBackup(context, realId, code, original)
                                lockMode = null; showRecoveryCode = code
                                Toast.makeText(context, "یادداشت قفل شد 🔒", Toast.LENGTH_SHORT).show()
                            }
                        }
                        LockMode.Unlock -> {
                            val unlocked = note?.body?.let { NoteLock.unlock(it, pass1) }
                            if (unlocked != null) { note = note?.copy(body = unlocked); lockMode = null }
                            else {
                                val rec = Recovery.tryRecover(context, realId, pass1)
                                if (rec != null) { note = note?.copy(body = rec); lockMode = null; Toast.makeText(context, "با کد بازیابی باز شد 🔑", Toast.LENGTH_SHORT).show() }
                                else lockError = "رمز یا کد بازیابی اشتباه است! ❌"
                            }
                        }
                    }
                }) { Text("تأیید", color = Saffron, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { lockMode = null }) { Text("انصراف") } })
    }

    showRecoveryCode?.let { code ->
        AlertDialog(onDismissRequest = { showRecoveryCode = null },
            title = { Text("🔑 کد بازیابی", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = {
                Column {
                    Text("این کد را جای امن بنویس! اگه روزی رمزت را فراموش کردی، با همین کد می‌تونی یادداشت را باز کنی:")
                    Spacer(Modifier.height(12.dp))
                    Text(code, fontFamily = LalezarFont, fontSize = 30.sp, color = Saffron, modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            },
            confirmButton = { TextButton(onClick = { showRecoveryCode = null }) { Text("ذخیره کردم ✅", color = Saffron, fontWeight = FontWeight.Bold) } })
    }

    if (showDatePicker) {
        ShamsiCalendarPickerDialog(
            onConfirm = { pickedDate = it; showDatePicker = false; showTimePicker = true },
            onDismiss = { showDatePicker = false })
    }

    if (showTimePicker) {
        val st = rememberTimePickerState()
        AlertDialog(onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val finalTime = pickedDate + st.hour.toLong() * 3600_000 + st.minute.toLong() * 60_000
                    if (finalTime > System.currentTimeMillis()) scheduleReminder(finalTime)
                    else Toast.makeText(context, "این زمان گذشته است!", Toast.LENGTH_SHORT).show()
                    showTimePicker = false
                }) { Text("تنظیم ⏰", color = Saffron, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("انصراف") } },
            text = { TimePicker(st) })
    }

    viewerImage?.let { att ->
        Dialog(onDismissRequest = { viewerImage = null }) {
            Box(Modifier.fillMaxSize().background(Color(0xFF06100E))) {
                AsyncImage(model = File(att.filePath), contentDescription = att.fileName, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                IconButton(onClick = { viewerImage = null }, modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) { Icon(Icons.Filled.Close, "بستن", tint = Color.White) }
                IconButton(onClick = { shareAttachment(context, att) }, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) { Icon(Icons.Filled.Share, "ارسال", tint = Saffron) }
            }
        }
    }

    if (showFocus && note != null && !isLocked) {
        FocusModeOverlay(body = note!!.body, onChange = { note = note?.copy(body = it) }, onExit = { showFocus = false })
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent)

@Composable
private fun ToolChip(emoji: String, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = DeepGreenSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineGreen)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 14.sp); Spacer(Modifier.width(5.dp)); Text(label, fontSize = 12.sp, color = PaperWhite)
        }
    }
}

@Composable
private fun LockedBox(onUnlock: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔒", fontSize = 42.sp); Spacer(Modifier.height(10.dp))
        Text("این یادداشت قفل است", fontFamily = LalezarFont, fontSize = 19.sp, color = Ink)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onUnlock, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = PaperWhite)) { Text("باز کردن با رمز", fontFamily = VazirFont) }
    }
}

@Composable
private fun ChecklistEditor(note: Note, onChange: (Note) -> Unit) {
    val lines = note.body.lines()
    val (done, total) = Checklist.progress(note.body)
    if (total > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { done.toFloat() / total.coerceAtLeast(1) },
                modifier = Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)),
                color = if (done == total) Color(0xFF3E9B4F) else Saffron, trackColor = Color.Black.copy(alpha = .08f))
            Spacer(Modifier.width(10.dp)); Text("${done.fa()}/${total.fa()}", fontFamily = LalezarFont, fontSize = 16.sp, color = Ink)
        }
        if (done == total) { Spacer(Modifier.height(8.dp)); Text("🎉 آفرین! همهٔ کارها انجام شد", color = Color(0xFF2E7D52), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Spacer(Modifier.height(10.dp))
    }
    lines.forEachIndexed { i, line ->
        val checked = line.startsWith("☑ ")
        val text = line.removePrefix("☐ ").removePrefix("☑ ")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onChange(note.copy(body = Checklist.toggleLine(note.body, i))) },
                colors = CheckboxDefaults.colors(checkedColor = Saffron, checkmarkColor = Ink))
            TextField(value = text, onValueChange = { v ->
                val mark = if (checked) "☑ " else "☐ "
                val list = lines.toMutableList(); list[i] = mark + v
                onChange(note.copy(body = list.joinToString("\n")))
            }, textStyle = TextStyle(fontFamily = VazirFont, fontSize = 15.sp, color = Ink,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None),
                colors = transparentFieldColors(), modifier = Modifier.weight(1f))
        }
    }
    TextButton(onClick = { onChange(note.copy(body = note.body.trimEnd('\n') + "\n☐ ")) }) { Text("+ مورد جدید", color = Saffron, fontWeight = FontWeight.Bold) }
}

@Composable
private fun RecordStopPill(seconds: Int, onStop: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "blink")
    val dotAlpha by transition.animateFloat(initialValue = 1f, targetValue = .25f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "dot")
    val timeText = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}".faDigits()
    Surface(onClick = onStop, shape = RoundedCornerShape(14.dp), color = Brick) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White.copy(alpha = dotAlpha)))
            Spacer(Modifier.width(7.dp))
            Text("توقف ضبط • $timeText", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AttachButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = DeepGreenSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineGreen)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Saffron, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text(text, color = PaperWhite, fontSize = 13.sp, fontFamily = VazirFont, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AttachmentsSection(attachments: List<Attachment>,
    onImageClick: (Attachment) -> Unit, onShare: (Attachment) -> Unit, onDelete: (Attachment) -> Unit) {
    val images = attachments.filter { it.isImage }
    val audios = attachments.filter { !it.isImage && it.mimeType.startsWith("audio/") }
    val docs = attachments.filter { !it.isImage && !it.mimeType.startsWith("audio/") }
    if (images.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            images.forEach { att ->
                Box(Modifier.padding(end = 10.dp).size(96.dp).clip(RoundedCornerShape(14.dp))) {
                    AsyncImage(model = File(att.filePath), contentDescription = att.fileName, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clickable { onImageClick(att) })
                    IconButton(onClick = { onDelete(att) }, modifier = Modifier.align(Alignment.TopStart).size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f))) {
                        Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
                    IconButton(onClick = { onShare(att) }, modifier = Modifier.align(Alignment.TopEnd).size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f))) {
                        Icon(Icons.Filled.Share, null, tint = Saffron, modifier = Modifier.size(15.dp)) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    audios.forEach { att -> AudioAttachmentRow(att, { onShare(att) }, { onDelete(att) }) }
    docs.forEach { att ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .05f)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("📄", fontSize = 18.sp); Spacer(Modifier.width(8.dp))
            Text(att.fileName, fontSize = 12.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { onShare(att) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Share, "ارسال", tint = InkSoft, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = { onDelete(att) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Close, "حذف", tint = Brick, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun AudioAttachmentRow(att: Attachment, onShare: () -> Unit, onDelete: () -> Unit) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(att.id) { onDispose { player?.release() } }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(Saffron.copy(alpha = .18f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("🎤", fontSize = 18.sp); Spacer(Modifier.width(8.dp))
        Text("پیام صوتی", fontSize = 12.sp, color = Ink, modifier = Modifier.weight(1f))
        IconButton(onClick = {
            if (playing) { player?.stop(); player?.release(); player = null; playing = false }
            else { player = MediaPlayer().apply { setDataSource(att.filePath); setOnCompletionListener { mp -> playing = false; mp.release() }; prepare(); start() }; playing = true }
        }, modifier = Modifier.size(32.dp)) { Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, if (playing) "توقف" else "پخش", tint = Ink) }
        IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Share, "ارسال", tint = InkSoft, modifier = Modifier.size(16.dp)) }
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Close, "حذف", tint = Brick, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun FocusModeOverlay(body: String, onChange: (String) -> Unit, onExit: () -> Unit) {
    val words = remember(body) { body.split(Regex("\\s+")).count { it.isNotBlank() } }
    Dialog(onDismissRequest = onExit, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(PaperWhite)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "خروج", tint = InkSoft) }
                    Spacer(Modifier.weight(1f))
                    Text("${words.fa()} کلمه • ${body.length.fa()} حرف", fontFamily = LalezarFont, fontSize = 15.sp, color = InkSoft)
                    Spacer(Modifier.weight(1f)); Text("✒️", fontSize = 20.sp)
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(Saffron.copy(alpha = .5f)))
                TextField(value = body, onValueChange = onChange,
                    textStyle = TextStyle(fontFamily = VazirFont, fontSize = 19.sp, color = Ink, lineHeight = 36.sp),
                    colors = transparentFieldColors(),
                    placeholder = { Text("فقط بنویس…", color = InkSoft.copy(alpha = .6f), fontSize = 18.sp) },
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 26.dp, vertical = 10.dp))
            }
        }
    }
}

private fun importUris(context: Context, scope: CoroutineScope, dao: NoteDao, noteId: Long, uris: List<Uri>) {
    if (uris.isEmpty()) return
    scope.launch(Dispatchers.IO) {
        uris.forEach { uri ->
            val file = AttachmentStore.copyToPrivate(context, uri) ?: return@forEach
            val mime = context.contentResolver.getType(uri) ?: guessMimeType(file.name)
            dao.insertAttachment(Attachment(noteId = noteId, fileName = file.name, filePath = file.absolutePath, mimeType = mime, isImage = mime.startsWith("image/")))
        }
        withContext(Dispatchers.Main) { Toast.makeText(context, "ضمیمه اضافه شد ✔", Toast.LENGTH_SHORT).show() }
    }
}
