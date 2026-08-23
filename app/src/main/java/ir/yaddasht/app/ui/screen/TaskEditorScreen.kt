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
import ir.yaddasht.app.data.*
import ir.yaddasht.app.reminder.ReminderScheduler
import ir.yaddasht.app.ui.theme.*
import ir.yaddasht.app.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class TLockMode { Set, Unlock }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(taskDao: TaskDao, taskId: Long, onBack: () -> Unit, onOpenDraw: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var task by remember { mutableStateOf<Task?>(null) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var hasReminder by remember { mutableStateOf(false) }
    var reminderAt by remember { mutableLongStateOf(0L) }
    var loaded by remember { mutableStateOf(false) }
    var attachments by remember { mutableStateOf(emptyList<TaskAttachment>()) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var viewerImage by remember { mutableStateOf<TaskAttachment?>(null) }
    var showFocus by remember { mutableStateOf(false) }
    var lockMode by remember { mutableStateOf<TLockMode?>(null) }
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var lockError by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableLongStateOf(0L) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    val isLocked = NoteLock.isLocked(desc)
    val isChecklist = Checklist.isChecklist(desc)

    LaunchedEffect(taskId) {
        val t = withContext(Dispatchers.IO) { taskDao.getTaskById(taskId) }
        if (t != null) {
            task = t; title = t.title; desc = t.description
            hasReminder = t.hasReminder; reminderAt = t.reminderTime
        }
        loaded = true
    }
    LaunchedEffect(loaded, taskId) {
        if (!loaded) return@LaunchedEffect
        taskDao.observeTaskAttachments(taskId).collect { attachments = it }
    }
    LaunchedEffect(recorder != null) {
        if (recorder != null) while (true) { delay(1000); recordSeconds++ }
    }

    fun saveTask() {
        val t = task ?: return
        scope.launch(Dispatchers.IO) {
            taskDao.update(t.copy(title = title, description = desc,
                hasReminder = hasReminder, reminderTime = reminderAt,
                updatedAt = System.currentTimeMillis()))
        }
    }

    val exit: () -> Unit = { saveTask(); onBack() }
    BackHandler(onBack = exit)

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingCameraFile
        if (ok && file != null && file.length() > 0) {
            scope.launch(Dispatchers.IO) {
                taskDao.insertTaskAttachment(TaskAttachment(taskId = taskId, fileName = file.name, filePath = file.absolutePath, mimeType = "image/jpeg", isImage = true))
            }
        } else file?.delete()
        pendingCameraFile = null
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        importTaskUris(context, scope, taskDao, taskId, uris)
    }
    val pickDocs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        importTaskUris(context, scope, taskDao, taskId, uris)
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) desc = if (desc.isBlank()) text else "$desc\n$text"
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun startRecording() {
        val file = AttachmentStore.createAudioFile(context)
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(file.absolutePath)
            r.prepare(); r.start()
            recorder = r; recordingFile = file; recordSeconds = 0
        } catch (e: Exception) { r.release(); Toast.makeText(context, "ضبط شروع نشد", Toast.LENGTH_SHORT).show() }
    }
    fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release(); recorder = null
        val f = recordingFile; recordingFile = null
        if (f != null && f.length() > 2000) {
            scope.launch(Dispatchers.IO) {
                taskDao.insertTaskAttachment(TaskAttachment(taskId = taskId, fileName = f.name, filePath = f.absolutePath, mimeType = "audio/mp4", isImage = false))
            }
        } else { f?.delete(); Toast.makeText(context, "ضبط خیلی کوتاه بود", Toast.LENGTH_SHORT).show() }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(context, "بدون میکروفون ضبط ممکن نیست", Toast.LENGTH_SHORT).show()
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "حرف بزن… 🎙️")
        }
        try { speechLauncher.launch(intent) } catch (e: Exception) { Toast.makeText(context, "دیکته در دسترس نیست", Toast.LENGTH_SHORT).show() }
    }
    fun scheduleReminder(ts: Long) {
        hasReminder = true; reminderAt = ts
        ReminderScheduler.schedule(context, taskId, title.ifBlank { "وظیفه" }, ts, isTask = true)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        saveTask()
        Toast.makeText(context, "یادآور وظیفه تنظیم شد ⏰", Toast.LENGTH_SHORT).show()
    }
    fun exportPdf() {
        scope.launch(Dispatchers.IO) {
            val file = PdfExporter.exportNote(context, Note(title = title.ifBlank { "وظیفه" }, body = if (isLocked) "🔒 قفل است" else desc))
            withContext(Dispatchers.Main) {
                if (file != null) sharePdf(context, file, title)
                else Toast.makeText(context, "ساخت PDF ناموفق بود", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(containerColor = DeepGreen) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = exit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = PaperWhite) }
                Text("✅ ویرایش وظیفه", fontFamily = LalezarFont, fontSize = 20.sp, color = PaperWhite,
                    modifier = Modifier.weight(1f).padding(start = 4.dp))
                IconButton(onClick = { shareNoteText(context, title, if (isLocked) "🔒 (قفل)" else desc) }) {
                    Icon(Icons.Filled.Share, "اشتراک", tint = PaperWhite)
                }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "حذف", tint = Brick) }
            }

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TToolChip("🔒", if (isLocked) "باز کن" else "قفل") { lockError = ""; pass1 = ""; pass2 = ""; lockMode = if (isLocked) TLockMode.Unlock else TLockMode.Set }
                TToolChip("⏰", "یادآور") { showDatePicker = true }
                TToolChip("✅", if (isChecklist) "خروج از چک‌لیست" else "چک‌لیست") {
                    desc = if (isChecklist) Checklist.fromChecklist(desc) else Checklist.toChecklist(desc)
                }
                TToolChip("🗣️", "دیکته") { if (!isLocked) launchSpeech() }
                TToolChip("📄", "PDF") { if (!isLocked) exportPdf() }
                TToolChip("✒️", "تمرکز") { if (!isLocked && !isChecklist) showFocus = true }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(PaperWhite).padding(16.dp)) {
                    if (hasReminder && reminderAt > System.currentTimeMillis()) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Saffron.copy(alpha = .28f))
                            .padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰ یادآور: " + FaDate.full(reminderAt) + " – " + SimpleDateFormat("HH:mm", Locale.US).format(Date(reminderAt)),
                                fontSize = 12.sp, color = Ink, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.Close, "لغو", tint = Brick,
                                modifier = Modifier.size(20.dp).clickable {
                                    hasReminder = false; reminderAt = 0
                                    ReminderScheduler.cancel(context, taskId, isTask = true); saveTask()
                                })
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    TextField(value = title, onValueChange = { title = it },
                        placeholder = { Text("عنوان وظیفه…", fontFamily = LalezarFont, color = InkSoft, fontSize = 22.sp) },
                        textStyle = TextStyle(fontFamily = LalezarFont, fontSize = 26.sp, color = Ink),
                        colors = tTransparent(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    if (!isLocked && attachments.isNotEmpty()) {
                        TaskAttachmentsSection(attachments,
                            onImageClick = { viewerImage = it },
                            onShare = { shareAttachment(context, Attachment(noteId = it.taskId, fileName = it.fileName, filePath = it.filePath, mimeType = it.mimeType, isImage = it.isImage)) },
                            onDelete = { att -> scope.launch(Dispatchers.IO) { taskDao.deleteTaskAttachment(att); File(att.filePath).delete() } })
                        Spacer(Modifier.height(10.dp))
                    }
                    when {
                        isLocked -> TLockedBox { lockError = ""; pass1 = ""; lockMode = TLockMode.Unlock }
                        isChecklist -> TChecklistEditor(desc) { desc = it }
                        else -> TextField(value = desc, onValueChange = { desc = it },
                            placeholder = { Text("توضیح وظیفه…", color = InkSoft, fontSize = 15.sp) },
                            textStyle = TextStyle(fontFamily = VazirFont, fontSize = 15.sp, color = Ink, lineHeight = 26.sp),
                            colors = tTransparent(), modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Column(Modifier.fillMaxWidth().background(DeepGreen)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (recorder != null) TRecordPill(recordSeconds, ::stopRecording)
                        else TAttachButton("صدا", Icons.Filled.Mic, Modifier.weight(1f), ::micClick)
                        TAttachButton("دوربین", Icons.Filled.CameraAlt, Modifier.weight(1f)) {
                            val file = AttachmentStore.createCameraFile(context)
                            pendingCameraFile = file
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            takePicture.launch(uri)
                        }
                        TAttachButton("گالری", Icons.Filled.Image, Modifier.weight(1f)) { pickImages.launch("image/*") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TAttachButton("فایل", Icons.Filled.AttachFile, Modifier.weight(1f)) { pickDocs.launch(arrayOf("*/*")) }
                        TAttachButton("نقاشی", Icons.Filled.Brush, Modifier.weight(1f)) { onOpenDraw(taskId) }
                    }
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("حذف وظیفه؟", fontFamily = LalezarFont, fontSize = 20.sp) },
        text = { Text("«${title.ifBlank { "بدون عنوان" }}» همراه ضمیمه‌ها حذف می‌شود.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.launch(Dispatchers.IO) {
                    taskDao.taskAttachmentsByTask(taskId).forEach { File(it.filePath).delete() }
                    ReminderScheduler.cancel(context, taskId, isTask = true)
                    taskDao.deleteById(taskId)
                }
                onBack()
            }) { Text("حذف", color = Brick, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } })

    lockMode?.let { mode ->
        AlertDialog(onDismissRequest = { lockMode = null },
            title = { Text(if (mode == TLockMode.Set) "🔒 گذاشتن رمز" else "🔓 باز کردن قفل", fontFamily = LalezarFont, fontSize = 20.sp) },
            text = { Column {
                OutlinedTextField(pass1, { pass1 = it }, label = { Text("رمز عبور") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                if (mode == TLockMode.Set) { Spacer(Modifier.height(8.dp))
                    OutlinedTextField(pass2, { pass2 = it }, label = { Text("تکرار رمز") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()) }
                if (lockError.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(lockError, color = Brick, fontSize = 12.sp) }
            } },
            confirmButton = {
                TextButton(onClick = {
                    when (mode) {
                        TLockMode.Set -> when {
                            pass1.length < 4 -> lockError = "رمز حداقل ۴ کاراکتر"
                            pass1 != pass2 -> lockError = "تکرار رمز یکسان نیست"
                            desc.isBlank() -> lockError = "متن خالی است!"
                            else -> { desc = NoteLock.lock(desc, pass1); lockMode = null; Toast.makeText(context, "وظیفه قفل شد 🔒", Toast.LENGTH_SHORT).show() }
                        }
                        TLockMode.Unlock -> {
                            val u = NoteLock.unlock(desc, pass1)
                            if (u != null) { desc = u; lockMode = null } else lockError = "رمز اشتباه است! ❌"
                        }
                    }
                }) { Text("تأیید", color = Saffron, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { lockMode = null }) { Text("انصراف") } })
    }

    if (showDatePicker) TShamsiDateDialog(
        onConfirm = { pickedDate = it; showDatePicker = false; showTimePicker = true },
        onDismiss = { showDatePicker = false })

    if (showTimePicker) {
        val st = rememberTimePickerState()
        AlertDialog(onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
                        timeInMillis = pickedDate
                        set(Calendar.HOUR_OF_DAY, st.hour); set(Calendar.MINUTE, st.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    if (cal.timeInMillis > System.currentTimeMillis()) scheduleReminder(cal.timeInMillis)
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
                AsyncImage(model = File(att.filePath), contentDescription = att.fileName,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                IconButton(onClick = { viewerImage = null }, modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                    Icon(Icons.Filled.Close, "بستن", tint = Color.White)
                }
            }
        }
    }

    if (showFocus) TFocusOverlay(desc, { desc = it }, { showFocus = false })
}

@Composable
private fun TShamsiDateDialog(onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val (jy0, jm0, jd0) = FaDate.jalali(System.currentTimeMillis())
    var jy by remember { mutableIntStateOf(jy0) }
    var jm by remember { mutableIntStateOf(jm0) }
    var jd by remember { mutableIntStateOf(jd0) }
    if (jd > FaDate.monthLength(jy, jm)) jd = FaDate.monthLength(jy, jm)
    val (gy, gm, gd) = FaDate.toGregorian(jy, jm, jd)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("📅 تاریخ یادآور وظیفه", fontFamily = LalezarFont, fontSize = 20.sp) },
        text = { Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TDateStep(FaDate.monthName(jm), { if (jm < 12) jm++ else { jm = 1; if (jy < jy0 + 3) jy++ } }, { if (jm > 1) jm-- else { jm = 12; if (jy > jy0 - 1) jy-- } })
                TDateStep(jd.fa(), { if (jd < FaDate.monthLength(jy, jm)) jd++ }, { if (jd > 1) jd-- })
                TDateStep(jy.fa(), { if (jy < jy0 + 3) jy++ }, { if (jy > jy0 - 1) jy-- })
            }
            Spacer(Modifier.height(12.dp))
            Text("معادل میلادی: $gy/${gm.toString().padStart(2, '0')}/${gd.toString().padStart(2, '0')}", fontSize = 12.sp)
        } },
        confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
                cal.clear(); cal.set(gy, gm - 1, gd, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                onConfirm(cal.timeInMillis)
            }) { Text("ادامه", color = Saffron, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable private fun TDateStep(v: String, plus: () -> Unit, minus: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = plus) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        Text(v, fontFamily = LalezarFont, fontSize = 18.sp)
        TextButton(onClick = minus) { Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun tTransparent() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent)
@Composable private fun TToolChip(emoji: String, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = DeepGreenSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineGreen)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 14.sp); Spacer(Modifier.width(5.dp)); Text(label, fontSize = 12.sp, color = PaperWhite)
        }
    }
}
@Composable private fun TLockedBox(onUnlock: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔒", fontSize = 42.sp); Spacer(Modifier.height(10.dp))
        Text("این وظیفه قفل است", fontFamily = LalezarFont, fontSize = 19.sp, color = Ink)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onUnlock, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = PaperWhite)) {
            Text("باز کردن با رمز", fontFamily = VazirFont)
        }
    }
}
@Composable private fun TChecklistEditor(desc: String, onChange: (String) -> Unit) {
    val lines = desc.lines()
    val (done, total) = Checklist.progress(desc)
    if (total > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { done.toFloat() / total.coerceAtLeast(1) },
                modifier = Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)),
                color = if (done == total) Color(0xFF3E9B4F) else Saffron, trackColor = Color.Black.copy(alpha = .08f))
            Spacer(Modifier.width(10.dp))
            Text("${done.fa()}/${total.fa()}", fontFamily = LalezarFont, fontSize = 16.sp, color = Ink)
        }
        Spacer(Modifier.height(10.dp))
    }
    lines.forEachIndexed { i, line ->
        val checked = line.startsWith("☑ ")
        val text = line.removePrefix("☐ ").removePrefix("☑ ")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onChange(Checklist.toggleLine(desc, i)) },
                colors = CheckboxDefaults.colors(checkedColor = Saffron, checkmarkColor = Ink))
            TextField(value = text, onValueChange = { v ->
                val mark = if (checked) "☑ " else "☐ "
                val list = lines.toMutableList(); list[i] = mark + v
                onChange(list.joinToString("\n"))
            }, textStyle = TextStyle(fontFamily = VazirFont, fontSize = 15.sp, color = Ink,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None),
                colors = tTransparent(), modifier = Modifier.weight(1f))
        }
    }
    TextButton(onClick = { onChange(desc.trimEnd('\n') + "\n☐ ") }) { Text("+ مورد جدید", color = Saffron, fontWeight = FontWeight.Bold) }
}
@Composable private fun TRecordPill(seconds: Int, onStop: () -> Unit) {
    val tr = rememberInfiniteTransition(label = "blink")
    val a by tr.animateFloat(1f, .25f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "dot")
    Surface(onClick = onStop, shape = RoundedCornerShape(14.dp), color = Brick) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White.copy(alpha = a)))
            Spacer(Modifier.width(7.dp))
            Text("توقف ضبط • ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}".faDigits(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable private fun TAttachButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = DeepGreenSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineGreen)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Saffron, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text(text, color = PaperWhite, fontSize = 13.sp, fontFamily = VazirFont, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable private fun TaskAttachmentsSection(attachments: List<TaskAttachment>,
    onImageClick: (TaskAttachment) -> Unit, onShare: (TaskAttachment) -> Unit, onDelete: (TaskAttachment) -> Unit) {
    val images = attachments.filter { it.isImage }
    val audios = attachments.filter { !it.isImage && it.mimeType.startsWith("audio/") }
    val docs = attachments.filter { !it.isImage && !it.mimeType.startsWith("audio/") }
    if (images.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            images.forEach { att ->
                Box(Modifier.padding(end = 10.dp).size(96.dp).clip(RoundedCornerShape(14.dp))) {
                    AsyncImage(model = File(att.filePath), contentDescription = att.fileName,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clickable { onImageClick(att) })
                    IconButton(onClick = { onDelete(att) }, modifier = Modifier.align(Alignment.TopStart).size(26.dp)
                        .clip(CircleShape).background(Color.Black.copy(alpha = .45f))) {
                        Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    audios.forEach { att ->
        var player by remember { mutableStateOf<MediaPlayer?>(null) }
        var playing by remember { mutableStateOf(false) }
        DisposableEffect(att.id) { onDispose { player?.release() } }
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp))
            .background(Saffron.copy(alpha = .18f)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("🎤", fontSize = 18.sp); Spacer(Modifier.width(8.dp))
            Text("پیام صوتی", fontSize = 12.sp, color = Ink, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                if (playing) { player?.stop(); player?.release(); player = null; playing = false }
                else { player = MediaPlayer().apply { setDataSource(att.filePath)
                    setOnCompletionListener { mp -> playing = false; mp.release() }; prepare(); start() }; playing = true }
            }, modifier = Modifier.size(32.dp)) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, "پخش", tint = Ink) }
            IconButton(onClick = { onDelete(att) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.Close, "حذف", tint = Brick, modifier = Modifier.size(16.dp)) }
        }
    }
    docs.forEach { att ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = .05f)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("📄", fontSize = 18.sp); Spacer(Modifier.width(8.dp))
            Text(att.fileName, fontSize = 12.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { onShare(att) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Share, "ارسال", tint = InkSoft, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = { onDelete(att) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Close, "حذف", tint = Brick, modifier = Modifier.size(16.dp)) }
        }
    }
}
@Composable private fun TFocusOverlay(body: String, onChange: (String) -> Unit, onExit: () -> Unit) {
    val words = remember(body) { body.split(Regex("\\s+")).count { it.isNotBlank() } }
    Dialog(onDismissRequest = onExit, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(PaperWhite)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "خروج", tint = InkSoft) }
                    Spacer(Modifier.weight(1f))
                    Text("${words.fa()} کلمه", fontFamily = LalezarFont, fontSize = 15.sp, color = InkSoft)
                    Spacer(Modifier.weight(1f)); Text("✒️", fontSize = 20.sp)
                }
                TextField(value = body, onValueChange = onChange,
                    textStyle = TextStyle(fontFamily = VazirFont, fontSize = 19.sp, color = Ink, lineHeight = 36.sp),
                    colors = tTransparent(), modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 26.dp, vertical = 10.dp))
            }
        }
    }
}
private fun importTaskUris(context: Context, scope: CoroutineScope, taskDao: TaskDao, taskId: Long, uris: List<Uri>) {
    if (uris.isEmpty()) return
    scope.launch(Dispatchers.IO) {
        uris.forEach { uri ->
            val file = AttachmentStore.copyToPrivate(context, uri) ?: return@forEach
            val mime = context.contentResolver.getType(uri) ?: guessMimeType(file.name)
            taskDao.insertTaskAttachment(TaskAttachment(taskId = taskId, fileName = file.name, filePath = file.absolutePath, mimeType = mime, isImage = mime.startsWith("image/")))
        }
        withContext(Dispatchers.Main) { Toast.makeText(context, "ضمیمه اضافه شد ✔", Toast.LENGTH_SHORT).show() }
    }
}
