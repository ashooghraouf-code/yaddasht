package ir.yaddasht.app.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
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

private fun xmlEsc(s: String): String = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
private fun buildDocumentXml(title: String, body: String): String {
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
    sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>")
    sb.append("<w:p><w:pPr><w:jc w:val=\"right\"/><w:rtl/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"44\"/><w:rtl/></w:rPr><w:t xml:space=\"preserve\">").append(xmlEsc(title)).append("</w:t></w:r></w:p>")
    body.split("\n").forEach { line -> sb.append("<w:p><w:pPr><w:jc w:val=\"right\"/><w:rtl/></w:pPr><w:r><w:rPr><w:rtl/></w:rPr><w:t xml:space=\"preserve\">").append(xmlEsc(line)).append("</w:t></w:r></w:p>") }
    sb.append("<w:sectPr/></w:body></w:document>"); return sb.toString()
}
private fun safeName(s: String) = s.take(24).replace(Regex("[^\\p{L}\\p{N}_-]"),"_").ifBlank{"note"}
private fun editorLaunchToolbox(context: Context) {
    for (name in listOf("ir.yaddasht.app.tools.ToolboxActivity","ir.yaddasht.app.ui.screen.ToolboxActivity")) {
        try { context.startActivity(Intent().setClassName(context,name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch(_:Exception){}
    }
    Toast.makeText(context,"جعبه‌ابزار نصب/ثبت نشده است 🧰",Toast.LENGTH_SHORT).show()
}
private fun readableOn(c: Color): Color = if (c.red*0.299f+c.green*0.587f+c.blue*0.114f > 0.6f) Color.Black else Color.White

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
    var showPaperStudio by remember { mutableStateOf(false) }
    var paperVersion by remember { mutableIntStateOf(0) }
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

    // ===== کاغذ: سفارشیِ این یادداشت، وگرنه رنگ پایه (sync با Home) =====
    val baseIndex = note?.color ?: 0
    val paperConfig = remember(realId, baseIndex, paperVersion) { ThemeKit.getNotePaperOr(context, realId, baseIndex) }
    val baseConfig = remember(baseIndex) { paperConfigForIndex(baseIndex) }
    val paperTone = ThemeKit.contentColors(paperConfig)
    val paperInk = paperTone.onSurface; val paperInkSoft = paperTone.muted; val paperAccent = paperTone.accent

    val dictation = remember { ContinuousDictation(context.applicationContext) }
    var dictating by remember { mutableStateOf(false) }
    var pendingDictation by remember { mutableStateOf(false) }
    val committed by dictation.committed.collectAsState()
    val partial by dictation.partial.collectAsState()
    val dictationState by dictation.state.collectAsState()
    val rootView = LocalView.current
    DisposableEffect(dictating){ rootView.keepScreenOn=dictating; onDispose{ rootView.keepScreenOn=false } }
    DisposableEffect(Unit){ onDispose{ dictation.release() } }
    fun appendDictationText(t: String){ if(t.isBlank())return; val c=note?.body.orEmpty(); note=(note?:Note(id=realId)).copy(body=if(c.isBlank())t else "$c\n$t") }
    val dictationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ g-> if(g&&pendingDictation){pendingDictation=false;dictating=true;dictation.start()} else if(!g){pendingDictation=false;Toast.makeText(context,"بدون دسترسی میکروفون دیکته ممکن نیست",Toast.LENGTH_SHORT).show()} }
    LaunchedEffect(dictationState){ val st=dictationState; if(st is DictationState.Error && dictating){ appendDictationText(dictation.stop()); dictating=false; Toast.makeText(context,st.message,Toast.LENGTH_SHORT).show() } }
    fun toggleDictation(){ if(isLocked||pendingDictation)return; if(dictating){ appendDictationText(dictation.stop()); dictating=false } else { if(!dictation.isAvailable){ Toast.makeText(context,"این دستگاه دیکته ندارد",Toast.LENGTH_SHORT).show(); return }; if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ pendingDictation=true; dictationPermission.launch(Manifest.permission.RECORD_AUDIO) } else { dictating=true; dictation.start() } } }

    LaunchedEffect(ready, realId){ if(!ready)return@LaunchedEffect; dao.observeNote(realId).collect{ n-> note=n; if(lastSaved==null) lastSaved=n } }
    LaunchedEffect(ready, realId){ if(!ready)return@LaunchedEffect; dao.observeAttachments(realId).collect{ attachments=it } }
    LaunchedEffect(ready){ if(!ready)return@LaunchedEffect; snapshotFlow{ note }.debounce(350).collect{ n-> val s=lastSaved; if(n!=null&&s!=null&&n.copy(updatedAt=s.updatedAt)!=s){ val st=n.copy(updatedAt=System.currentTimeMillis()); lastSaved=st; withContext(Dispatchers.IO){ dao.update(st) } } } }
    LaunchedEffect(recorder!=null){ if(recorder!=null) while(true){ delay(1000); recordSeconds++ } }

    val exit: () -> Unit = { scope.launch { if(dictating){ appendDictationText(dictation.stop()); dictating=false }; val n=note; if(n!=null){ val c=withContext(Dispatchers.IO){ dao.attachmentCount(n.id) }; withContext(Dispatchers.IO){ if(n.title.isBlank()&&n.body.isBlank()&&c==0) dao.deleteById(n.id) else dao.update(n.copy(updatedAt=System.currentTimeMillis())) } }; onBack() } }
    BackHandler(onBack=exit)

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ ok-> val f=pendingCameraFile; if(ok&&f!=null&&f.length()>0){ scope.launch(Dispatchers.IO){ dao.insertAttachment(Attachment(noteId=realId, fileName=f.name, filePath=f.absolutePath, mimeType="image/jpeg", isImage=true)) } } else f?.delete(); pendingCameraFile=null }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()){ importUris(context,scope,dao,realId,it) }
    val pickDocs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){ importUris(context,scope,dao,realId,it) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}

    fun startRecording(){ val f=AttachmentStore.createAudioFile(context); val r=if(Build.VERSION.SDK_INT>=31) MediaRecorder(context) else MediaRecorder(); try{ r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setOutputFile(f.absolutePath); r.prepare(); r.start(); recorder=r; recordingFile=f; recordSeconds=0 }catch(_:Exception){ r.release(); Toast.makeText(context,"ضبط شروع نشد",Toast.LENGTH_SHORT).show() } }
    fun stopRecording(){ try{ recorder?.stop() }catch(_:Exception){}; recorder?.release(); recorder=null; val f=recordingFile; recordingFile=null; if(f!=null&&f.length()>2000){ scope.launch(Dispatchers.IO){ dao.insertAttachment(Attachment(noteId=realId, fileName=f.name, filePath=f.absolutePath, mimeType="audio/mp4", isImage=false)) } } else { f?.delete(); Toast.makeText(context,"ضبط خیلی کوتاه بود",Toast.LENGTH_SHORT).show() } }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ g-> if(g) startRecording() else Toast.makeText(context,"بدون دسترسی میکروفون ضبط ممکن نیست",Toast.LENGTH_SHORT).show() }
    fun micClick(){ if(recorder!=null) stopRecording() else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) startRecording() else audioPermission.launch(Manifest.permission.RECORD_AUDIO) }

    fun scheduleReminder(ts: Long, ls: Set<LeadTime>){ val n=note?:return; note=n.copy(reminderAt=ts); ReminderScheduler.scheduleMulti(context,realId,n.title.ifBlank{"یادداشت"},ts,false,ls); if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS); Toast.makeText(context,"یادآور با ${ls.size.fa()} هشدار تنظیم شد ⏰",Toast.LENGTH_SHORT).show() }

    fun exportPdf(){ val n=note?:return; val pn=if(isLocked) n.copy(body="🔒 این یادداشت قفل است") else n; scope.launch(Dispatchers.IO){ val f=PdfExporter.exportNote(context,pn); withContext(Dispatchers.Main){ if(f!=null) sharePdf(context,f,n.title) else Toast.makeText(context,"ساخت PDF ناموفق بود",Toast.LENGTH_SHORT).show() } } }
    fun exportWord(){ val n=note?:return; scope.launch(Dispatchers.IO){ try{ val dir=File(context.filesDir,"exports"); if(!dir.exists()) dir.mkdirs(); val f=File(dir,"yaddasht-${System.currentTimeMillis()}.docx"); ZipOutputStream(FileOutputStream(f)).use{ z-> z.putNextEntry(ZipEntry("[Content_Types].xml")); z.write(DOCX_CONTENT_TYPES.toByteArray(Charsets.UTF_8)); z.closeEntry(); z.putNextEntry(ZipEntry("_rels/.rels")); z.write(DOCX_RELS.toByteArray(Charsets.UTF_8)); z.closeEntry(); z.putNextEntry(ZipEntry("word/document.xml")); z.write(buildDocumentXml(n.title.ifBlank{"بدون عنوان"},n.body).toByteArray(Charsets.UTF_8)); z.closeEntry() }; if(f.length()==0L){ withContext(Dispatchers.Main){ Toast.makeText(context,"خطا: فایل خالی ساخته شد!",Toast.LENGTH_LONG).show() }; return@launch }; val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",f); val it=Intent(Intent.ACTION_SEND).apply{ type="application/vnd.openxmlformats-officedocument.wordprocessingml.document"; putExtra(Intent.EXTRA_STREAM,uri); putExtra(Intent.EXTRA_SUBJECT,n.title.ifBlank{"یادداشت"}); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; withContext(Dispatchers.Main){ context.startActivity(Intent.createChooser(it,"اشتراک‌گذاری فایل Word")) } }catch(e:Exception){ withContext(Dispatchers.Main){ Toast.makeText(context,"خطا در ساخت Word: ${e.message}",Toast.LENGTH_LONG).show() } } } }
    fun exportJsonNote(){ val n=note?:return; scope.launch(Dispatchers.IO){ val atts=dao.attachmentsByNote(n.id); val j=JSONObject().apply{ put("title",n.title); put("body",n.body); put("color",n.color); put("pinned",n.pinned); put("reminderAt",n.reminderAt); put("createdAt",n.createdAt); put("updatedAt",n.updatedAt); put("attachments",JSONArray().apply{ atts.forEach{ a-> put(JSONObject().apply{ put("fileName",a.fileName); put("mimeType",a.mimeType); put("isImage",a.isImage) }) } }) }; val dir=File(context.filesDir,"exports").apply{ if(!exists()) mkdirs() }; val f=File(dir,"${safeName(n.title)}.json"); FileOutputStream(f).use{ it.write(j.toString(2).toByteArray(Charsets.UTF_8)) }; withContext(Dispatchers.Main){ shareBackupFile(context,f) } } }
    fun exportImage(){ val n=note?:return; scope.launch(Dispatchers.IO){ try{ val dir=File(context.filesDir,"exports"); if(!dir.exists()) dir.mkdirs(); val f=File(dir,"${safeName(n.title)}.png"); val w=1080; val lh=60; val pd=60; val lines=if(isLocked) listOf("🔒 این یادداشت قفل است") else n.body.split("\n"); val tl=lines.size.coerceAtMost(30); val h=(pd+100+40+tl*lh+80+pd).coerceAtLeast(800); val bm=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); val cv=Canvas(bm); cv.drawColor(EditorPaperColors.getOrElse(n.color){EditorPaperColors[0]}.toArgb()); val tp=Paint().apply{ color=0xFF2D2D2D.toInt(); textSize=64f; isFakeBoldText=true; textAlign=Paint.Align.RIGHT; typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD) }; cv.drawText(n.title.ifBlank{"بدون عنوان"},(w-pd).toFloat(),(pd+64f),tp); val lp=Paint().apply{ color=0xFFFFB74D.toInt(); strokeWidth=4f }; cv.drawLine(pd.toFloat(),(pd+94f),(w-pd).toFloat(),(pd+94f),lp); val bp=Paint().apply{ color=0xFF3D3D3D.toInt(); textSize=42f; textAlign=Paint.Align.RIGHT }; var y=pd+144f; for(l in lines.take(30)){ cv.drawText(l.ifEmpty{" "},(w-pd).toFloat(),y,bp); y+=lh }; val dp=Paint().apply{ color=0xFF888888.toInt(); textSize=32f; textAlign=Paint.Align.LEFT }; cv.drawText(SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.US).format(Date(n.updatedAt)),pd.toFloat(),(h-pd).toFloat(),dp); f.outputStream().use{ bm.compress(Bitmap.CompressFormat.PNG,100,it) }; bm.recycle(); withContext(Dispatchers.Main){ val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",f); val it=Intent(Intent.ACTION_SEND).apply{ type="image/png"; putExtra(Intent.EXTRA_STREAM,uri); putExtra(Intent.EXTRA_SUBJECT,n.title.ifBlank{"یادداشت"}); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(Intent.createChooser(it,"اشتراک‌گذاری عکس یادداشت")) } }catch(e:Exception){ withContext(Dispatchers.Main){ Toast.makeText(context,"خطا در ساخت عکس: ${e.message}",Toast.LENGTH_LONG).show() } } } }

    LaunchedEffect(Unit){ if(noteId==NEW_NOTE_ID){ realId=withContext(Dispatchers.IO){ dao.insert(Note()) }; val a=context as? Activity; if(a?.intent?.getBooleanExtra("auto_camera",false)==true){ val f=AttachmentStore.createCameraFile(context); pendingCameraFile=f; takePicture.launch(FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",f)); a.intent.removeExtra("auto_camera") }; if(a?.intent?.getBooleanExtra("auto_dictation",false)==true){ toggleDictation(); a.intent.removeExtra("auto_dictation") } }; ready=true; delay(600); if(note==null){ val n=withContext(Dispatchers.IO){ dao.allNotesSync().firstOrNull{ it.id==realId } }; if(n!=null){ note=n; if(lastSaved==null) lastSaved=n } } }

    Scaffold(containerColor=DeepGreen, contentWindowInsets=WindowInsets.ime){ padding->
        Column(Modifier.fillMaxSize().padding(padding)){
            Row(Modifier.fillMaxWidth().padding(horizontal=6.dp,vertical=4.dp), verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=exit){ Icon(Icons.AutoMirrored.Filled.ArrowBack,"بازگشت",tint=PaperWhite) }
                Text(if(isLocked)"🔒 یادداشت محرمانه" else "یادداشت", fontFamily=LalezarFont, fontSize=20.sp, color=PaperWhite, modifier=Modifier.weight(1f).padding(start=4.dp))
                IconButton(onClick={ note=(note?:Note(id=realId)).copy(pinned=!(note?.pinned?:false)) }){ Icon(Icons.Filled.PushPin,"سنجاق",tint=if(note?.pinned==true) Saffron else Color(0xFF5E8077)) }
                IconButton(onClick={ showExport=true }){ Icon(Icons.Filled.Share,"ارسال و خروجی",tint=PaperWhite) }
                IconButton(onClick={ confirmDelete=true }){ Icon(Icons.Filled.Delete,"حذف",tint=Brick) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=2.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)){
                ToolChip("🔒", if(isLocked)"باز کن" else "قفل"){ lockError=""; pass1=""; pass2=""; lockMode=if(isLocked) LockMode.Unlock else LockMode.Set }
                ToolChip("⏰","یادآور"){ showDatePicker=true }
                ToolChip("✅", if(isChecklist)"خروج از چک‌لیست" else "چک‌لیست"){ if(!isLocked) note?.let{ n-> note=n.copy(body=if(isChecklist) Checklist.fromChecklist(n.body) else Checklist.toChecklist(n.body)) } }
                ToolChip(if(dictating)"🛑" else "🗣️", if(dictating)"توقف دیکته" else "دیکته"){ toggleDictation() }
                ToolChip("🧰","جعبه‌ابزار"){ editorLaunchToolbox(context) }
                ToolChip("🎨","کاغذ"){ showPaperStudio=true }
                ToolChip("✒️","تمرکز"){ if(!isLocked&&!isChecklist) showFocus=true }
                ToolChip("🤖","هوش مصنوعی"){ showAi=true }
            }
            Column(Modifier.weight(1f).padding(horizontal=16.dp)){
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp))){
                    ThemeBackground(config=paperConfig, modifier=Modifier.matchParentSize())
                    Column(Modifier.fillMaxSize().padding(16.dp)){
                        val rem=note?.reminderAt?:0
                        if(rem>System.currentTimeMillis()){ Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(paperAccent.copy(alpha=.28f)).padding(horizontal=10.dp,vertical=7.dp), verticalAlignment=Alignment.CenterVertically){ Text("⏰ یادآور: "+FaDate.full(rem)+" – "+SimpleDateFormat("HH:mm",Locale.US).format(Date(rem)), fontSize=12.sp, color=paperInk, modifier=Modifier.weight(1f)); Icon(Icons.Filled.Close,"لغو یادآور",tint=Brick, modifier=Modifier.size(20.dp).clickable{ note=note?.copy(reminderAt=0); ReminderScheduler.cancelAll(context,realId,false) }) }; Spacer(Modifier.height(10.dp)) }
                        TextField(value=note?.title.orEmpty(), onValueChange={ note=(note?:Note(id=realId)).copy(title=it) }, placeholder={ Text("عنوان یادداشت", fontFamily=LalezarFont, color=paperInkSoft, fontSize=22.sp) }, textStyle=TextStyle(fontFamily=LalezarFont, fontSize=26.sp, color=paperInk, textDirection=TextDirection.Rtl), colors=transparentFieldColors(), singleLine=true, modifier=Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        if(!isLocked&&attachments.isNotEmpty()){ AttachmentsSection(attachments,paperInk,paperInkSoft,paperAccent,{viewerImage=it},{shareAttachment(context,it)},{att-> scope.launch(Dispatchers.IO){ dao.deleteAttachment(att); File(att.filePath).delete() }}); Spacer(Modifier.height(10.dp)) }
                        if(dictating){ val live=when{ partial.isNotBlank()&&committed.isNotBlank()->"$committed\n$partial"; partial.isNotBlank()->partial; else->committed }; val st=when(val s=dictationState){ is DictationState.Hearing->"دارم می‌شنوم…"; is DictationState.Restarting->"در حال اتصال مجدد…"; is DictationState.Listening->"آماده‌ی شنیدن…"; is DictationState.Error->s.message; is DictationState.Idle->"…" }; Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(paperAccent.copy(alpha=.22f)).padding(horizontal=10.dp,vertical=8.dp), verticalAlignment=Alignment.CenterVertically){ Text("🎙️",fontSize=16.sp); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)){ Text(st,fontSize=11.sp,color=paperInkSoft); Text(live.ifBlank{"هر وقت خواستی 🛑 بزن؛ خودش وصل می‌ماند."},fontSize=13.sp,color=paperInk,maxLines=3,overflow=TextOverflow.Ellipsis) } }; Spacer(Modifier.height(8.dp)) }
                        when{
                            isLocked -> LockedBox(paperInk,paperInkSoft){ lockError=""; pass1=""; lockMode=LockMode.Unlock }
                            isChecklist&&note!=null -> ChecklistEditor(note!!,paperInk,paperInkSoft,paperAccent){ note=it }
                            else -> TextField(value=note?.body.orEmpty(), onValueChange={ note=(note?:Note(id=realId)).copy(body=it) }, placeholder={ Text("اینجا بنویس یا از «دیکته» و «چک‌لیست» استفاده کن", color=paperInkSoft, fontSize=15.sp) }, textStyle=TextStyle(fontFamily=VazirFont, fontSize=15.sp, color=paperInk, lineHeight=28.sp, letterSpacing=0.2.sp, textAlign=TextAlign.Start, textDirection=TextDirection.Rtl), colors=transparentFieldColors(), modifier=Modifier.fillMaxWidth().weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.fillMaxWidth().background(DeepGreen)){
                AnimatedVisibility(showPalette){ Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=6.dp), horizontalArrangement=Arrangement.spacedBy(10.dp)){ EditorPaperColors.forEachIndexed{ i,c-> Box(Modifier.size(32.dp).clip(CircleShape).background(c).border(if((note?.color?:0)==i)3.dp else 1.dp, if((note?.color?:0)==i) Saffron else Color.Black.copy(alpha=.2f), CircleShape).clickable{ note=(note?:Note(id=realId)).copy(color=i); if(realId>0L) ThemeKit.clearNotePaper(context,realId); paperVersion++ }) } } }
                Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp), verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically){
                        if(recorder!=null) RecordStopPill(recordSeconds,::stopRecording) else AttachButton("صدا",Icons.Filled.Mic,Modifier.weight(1f),::micClick)
                        AttachButton("دوربین",Icons.Filled.CameraAlt,Modifier.weight(1f)){ val f=AttachmentStore.createCameraFile(context); pendingCameraFile=f; takePicture.launch(FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",f)) }
                        AttachButton("گالری",Icons.Filled.Image,Modifier.weight(1f)){ pickImages.launch("image/*") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically){
                        AttachButton("فایل",Icons.Filled.AttachFile,Modifier.weight(1f)){ pickDocs.launch(arrayOf("*/*")) }
                        AttachButton("نقاشی",Icons.Filled.Brush,Modifier.weight(1f)){ onOpenDraw(realId) }
                        IconButton(onClick={ showPalette=!showPalette }){ Icon(Icons.Filled.Palette,"رنگ",tint=if(showPalette) Saffron else Color(0xFF5E8077)) }
                    }
                }
            }
        }
    }

    if(showExport&&note!=null){ val n=note!!; AlertDialog(onDismissRequest={showExport=false}, title={Text("📤 ارسال / خروجی",fontFamily=LalezarFont,fontSize=22.sp,color=Saffron)}, text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ ExportItem("📤 اشتراک متن یادداشت"){ shareNoteText(context,n.title,if(isLocked)"🔒 (محتوا قفل است)" else n.body); showExport=false }; ExportItem("📄 خروجی PDF"){ if(!isLocked) exportPdf(); showExport=false }; ExportItem("📝 خروجی Word"){ if(!isLocked) exportWord(); showExport=false }; ExportItem("🧾 خروجی JSON"){ exportJsonNote(); showExport=false }; ExportItem("🖼️ خروجی عکس"){ if(!isLocked) exportImage(); showExport=false } }}, confirmButton={TextButton(onClick={showExport=false}){Text("بستن",color=Saffron,fontWeight=FontWeight.Bold)}}) }
    if(confirmDelete){ AlertDialog(onDismissRequest={confirmDelete=false}, title={Text("حذف یادداشت؟",fontFamily=LalezarFont,fontSize=20.sp)}, text={Text("«${note?.title?.ifBlank{"بدون عنوان"}?:"بدون عنوان"}» همراه با ضمیمه‌هایش برای همیشه حذف می‌شود.")}, confirmButton={TextButton(onClick={ confirmDelete=false; val n=note; scope.launch{ if(n!=null) withContext(Dispatchers.IO){ dao.attachmentsByNote(n.id).forEach{ File(it.filePath).delete() }; dao.deleteById(n.id) }; onBack() } }){Text("حذف",color=Brick,fontWeight=FontWeight.Bold)}}, dismissButton={TextButton(onClick={confirmDelete=false}){Text("انصراف")}}) }
    lockMode?.let{ mode-> AlertDialog(onDismissRequest={lockMode=null}, title={Text(if(mode==LockMode.Set)"🔒 گذاشتن رمز" else "🔓 باز کردن قفل",fontFamily=LalezarFont,fontSize=20.sp)}, text={Column{ OutlinedTextField(pass1,{pass1=it},label={Text(if(mode==LockMode.Unlock)"رمز یا کد بازیابی" else "رمز عبور")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),modifier=Modifier.fillMaxWidth()); if(mode==LockMode.Set){ Spacer(Modifier.height(8.dp)); OutlinedTextField(pass2,{pass2=it},label={Text("تکرار رمز")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),modifier=Modifier.fillMaxWidth()) }; if(lockError.isNotBlank()){ Spacer(Modifier.height(8.dp)); Text(lockError,color=Brick,fontSize=12.sp) } }}, confirmButton={TextButton(onClick={ when(mode){ LockMode.Set-> when{ pass1.length<4->lockError="رمز حداقل ۴ کاراکتر باشد"; pass1!=pass2->lockError="تکرار رمز یکسان نیست"; note?.body.isNullOrBlank()->lockError="یادداشت خالی است!"; else->{ val o=note!!.body; val code=Recovery.genCode(); note=note?.copy(body=NoteLock.lock(o,pass1)); Recovery.saveBackup(context,realId,code,o); lockMode=null; showRecoveryCode=code; Toast.makeText(context,"یادداشت قفل شد 🔒",Toast.LENGTH_SHORT).show() } } LockMode.Unlock->{ val u=note?.body?.let{ NoteLock.unlock(it,pass1) }; if(u!=null){ note=note?.copy(body=u); lockMode=null } else { val rc=Recovery.tryRecover(context,realId,pass1); if(rc!=null){ note=note?.copy(body=rc); lockMode=null; Toast.makeText(context,"با کد بازیابی باز شد 🔑",Toast.LENGTH_SHORT).show() } else lockError="رمز یا کد بازیابی اشتباه است! ❌" } } } }){Text("تأیید",color=Saffron,fontWeight=FontWeight.Bold)}}, dismissButton={TextButton(onClick={lockMode=null}){Text("انصراف")}}) }
    showRecoveryCode?.let{ code-> AlertDialog(onDismissRequest={showRecoveryCode=null}, title={Text("🔑 کد بازیابی",fontFamily=LalezarFont,fontSize=20.sp)}, text={Column{ Text("این کد را جای امن بنویس! اگه روزی رمزت را فراموش کردی، با همین کد می‌تونی یادداشت را باز کنی:"); Spacer(Modifier.height(12.dp)); Text(code,fontFamily=LalezarFont,fontSize=30.sp,color=Saffron,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center) }}, confirmButton={TextButton(onClick={showRecoveryCode=null}){Text("ذخیره کردم ✅",color=Saffron,fontWeight=FontWeight.Bold)}}) }
    if(showDatePicker) YadavarDatePickerDialog(onConfirm={ pickedDate=it; showDatePicker=false; showLeads=true }, onDismiss={ showDatePicker=false })
    if(showLeads) AlertDialog(onDismissRequest={showLeads=false}, title={Text("🔔 هشدارهای یادآور",fontFamily=LalezarFont,fontSize=18.sp)}, text={Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)){ LeadTime.values().forEach{ l-> LeadToggleChip(l.label, leads.contains(l)){ leads=if(leads.contains(l)) leads-l else leads+l } } }}, confirmButton={TextButton(onClick={ scheduleReminder(pickedDate,leads); showLeads=false }){Text("تنظیم ⏰",color=Saffron,fontWeight=FontWeight.Bold)}}, dismissButton={TextButton(onClick={showLeads=false}){Text("انصراف")}})
    viewerImage?.let{ att-> Dialog(onDismissRequest={viewerImage=null}){ Box(Modifier.fillMaxSize().background(Color(0xFF06100E))){ AsyncImage(model=File(att.filePath),contentDescription=att.fileName,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize()); IconButton(onClick={viewerImage=null},modifier=Modifier.align(Alignment.TopStart).padding(10.dp)){Icon(Icons.Filled.Close,"بستن",tint=Color.White)}; IconButton(onClick={shareAttachment(context,att)},modifier=Modifier.align(Alignment.TopEnd).padding(10.dp)){Icon(Icons.Filled.Share,"ارسال",tint=Saffron)} } } }
    if(showFocus&&note!=null&&!isLocked) FocusModeOverlay(note!!.body,{ note=(note?:Note(id=realId)).copy(body=it) },{ showFocus=false })
    if(showAi&&note!=null) AiAnalysisDialog(note?.title?:"", note?.body?:"", isLocked){ showAi=false }

    if(showPaperStudio){ ThemeStudioDialog(scope=ThemeScope.NotePaper, initial=paperConfig, titleOverride="🎨 کاغذ این یادداشت", defaultConfig=baseConfig, onDismiss={ showPaperStudio=false }, onApply={ cfg-> if(realId>0L) ThemeKit.setNotePaper(context,realId,cfg); paperVersion++; showPaperStudio=false }) }
}

@Composable private fun ExportItem(label:String, onClick:()->Unit){ Surface(onClick=onClick, shape=RoundedCornerShape(12.dp), color=PaperWhite, border=BorderStroke(1.dp,LineGreen)){ Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp), verticalAlignment=Alignment.CenterVertically){ Text(label,fontSize=15.sp,color=Ink,fontWeight=FontWeight.Bold,fontFamily=VazirFont); Spacer(Modifier.weight(1f)); Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint=Saffron,modifier=Modifier.size(18.dp)) } } }
@Composable private fun transparentFieldColors()=TextFieldDefaults.colors(focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,disabledContainerColor=Color.Transparent,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent,disabledIndicatorColor=Color.Transparent)
@Composable private fun ToolChip(emoji:String,label:String,onClick:()->Unit){ Surface(onClick=onClick, shape=RoundedCornerShape(12.dp), color=DeepGreenSoft, border=BorderStroke(1.dp,LineGreen)){ Row(Modifier.padding(horizontal=10.dp,vertical=7.dp), verticalAlignment=Alignment.CenterVertically){ Text(emoji,fontSize=14.sp); Spacer(Modifier.width(5.dp)); Text(label,fontSize=12.sp,color=PaperWhite) } } }
@Composable private fun LockedBox(ink:Color,inkSoft:Color,onUnlock:()->Unit){ Column(Modifier.fillMaxWidth().padding(vertical=28.dp), horizontalAlignment=Alignment.CenterHorizontally){ Text("🔒",fontSize=42.sp); Spacer(Modifier.height(10.dp)); Text("این یادداشت قفل است",fontFamily=LalezarFont,fontSize=19.sp,color=ink); Spacer(Modifier.height(14.dp)); Button(onClick=onUnlock, colors=ButtonDefaults.buttonColors(containerColor=ink.copy(alpha=.92f),contentColor=readableOn(ink))){ Text("باز کردن با رمز",fontFamily=VazirFont) } } }
@Composable private fun ChecklistEditor(note:Note,ink:Color,inkSoft:Color,accent:Color,onChange:(Note)->Unit){ val lines=note.body.lines(); val (done,total)=Checklist.progress(note.body); if(total>0){ Row(verticalAlignment=Alignment.CenterVertically){ LinearProgressIndicator(progress={done.toFloat()/total.coerceAtLeast(1)},modifier=Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)),color=if(done==total) Color(0xFF3E9B4F) else accent,trackColor=ink.copy(alpha=.08f)); Spacer(Modifier.width(10.dp)); Text("${done.fa()}/${total.fa()}",fontFamily=LalezarFont,fontSize=16.sp,color=ink) }; if(done==total){ Spacer(Modifier.height(8.dp)); Text("🎉 آفرین! همهٔ کارها انجام شد",color=accent,fontWeight=FontWeight.Bold,fontSize=13.sp) }; Spacer(Modifier.height(10.dp)) } Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ lines.forEachIndexed{ i,line-> val checked=line.startsWith("☑ "); val text=line.removePrefix("☐ ").removePrefix("☑ "); Row(verticalAlignment=Alignment.CenterVertically){ Checkbox(checked,{onChange(note.copy(body=Checklist.toggleLine(note.body,i)))},colors=CheckboxDefaults.colors(checkedColor=accent,checkmarkColor=readableOn(accent))); TextField(text,{v-> val m=if(checked)"☑ " else "☐ "; val l=lines.toMutableList(); l[i]=m+v; onChange(note.copy(body=l.joinToString("\n")))},textStyle=TextStyle(fontFamily=VazirFont,fontSize=15.sp,color=ink,lineHeight=26.sp,letterSpacing=0.2.sp,textDecoration=if(checked)TextDecoration.LineThrough else TextDecoration.None,textAlign=TextAlign.Start,textDirection=TextDirection.Rtl),colors=transparentFieldColors(),modifier=Modifier.weight(1f)) } } }; TextButton(onClick={onChange(note.copy(body=note.body.trimEnd('\n')+"\n☐ "))}){ Text("+ مورد جدید",color=accent,fontWeight=FontWeight.Bold) } }
@Composable private fun RecordStopPill(seconds:Int,onStop:()->Unit){ val tr=rememberInfiniteTransition(label="blink"); val da by tr.animateFloat(1f,.25f,infiniteRepeatable(tween(550),androidx.compose.animation.core.RepeatMode.Reverse),label="dot"); val tt="${seconds/60}:${(seconds%60).toString().padStart(2,'0')}".faDigits(); Surface(onClick=onStop,shape=RoundedCornerShape(14.dp),color=Brick){ Row(Modifier.padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){ Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White.copy(alpha=da))); Spacer(Modifier.width(7.dp)); Text("توقف ضبط • $tt",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold) } } }
@Composable private fun AttachButton(text:String,icon:ImageVector,modifier:Modifier=Modifier,onClick:()->Unit){ Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(14.dp),color=DeepGreenSoft,border=BorderStroke(1.dp,LineGreen)){ Row(Modifier.padding(horizontal=13.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){ Icon(icon,null,tint=Saffron,modifier=Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(text,color=PaperWhite,fontSize=13.sp,fontFamily=VazirFont,fontWeight=FontWeight.Bold) } } }
@Composable private fun AttachmentsSection(attachments:List<Attachment>,ink:Color,inkSoft:Color,accent:Color,onImageClick:(Attachment)->Unit,onShare:(Attachment)->Unit,onDelete:(Attachment)->Unit){ val images=attachments.filter{it.isImage}; val audios=attachments.filter{!it.isImage&&it.mimeType.startsWith("audio/")}; val docs=attachments.filter{!it.isImage&&!it.mimeType.startsWith("audio/")}; if(images.isNotEmpty()){ Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){ images.forEach{att-> Box(Modifier.padding(end=10.dp).size(96.dp).clip(RoundedCornerShape(14.dp))){ AsyncImage(model=File(att.filePath),contentDescription=att.fileName,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize().clickable{onImageClick(att)}); IconButton(onClick={onDelete(att)},modifier=Modifier.align(Alignment.TopStart).size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha=.45f))){Icon(Icons.Filled.Close,null,tint=Color.White,modifier=Modifier.size(15.dp))}; IconButton(onClick={onShare(att)},modifier=Modifier.align(Alignment.TopEnd).size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha=.45f))){Icon(Icons.Filled.Share,null,tint=Saffron,modifier=Modifier.size(15.dp))} } } }; Spacer(Modifier.height(12.dp)) }; audios.forEach{att-> AudioAttachmentRow(att,ink,inkSoft,accent,{onShare(att)},{onDelete(att)}) }; docs.forEach{att-> Row(Modifier.fillMaxWidth().padding(vertical=3.dp).clip(RoundedCornerShape(12.dp)).background(ink.copy(alpha=.05f)).padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){ Text("📄",fontSize=18.sp); Spacer(Modifier.width(8.dp)); Text(att.fileName,fontSize=12.sp,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f)); IconButton(onClick={onShare(att)},modifier=Modifier.size(30.dp)){Icon(Icons.Filled.Share,"ارسال",tint=inkSoft,modifier=Modifier.size(16.dp))}; IconButton(onClick={onDelete(att)},modifier=Modifier.size(30.dp)){Icon(Icons.Filled.Close,"حذف",tint=Brick,modifier=Modifier.size(16.dp))} } } }
@Composable private fun AudioAttachmentRow(att:Attachment,ink:Color,inkSoft:Color,accent:Color,onShare:()->Unit,onDelete:()->Unit){ var player by remember{mutableStateOf<MediaPlayer?>(null)}; var playing by remember{mutableStateOf(false)}; DisposableEffect(att.id){onDispose{player?.release()}}; Row(Modifier.fillMaxWidth().padding(vertical=3.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha=.18f)).padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){ Text("🎤",fontSize=18.sp); Spacer(Modifier.width(8.dp)); Text("پیام صوتی",fontSize=12.sp,color=ink,modifier=Modifier.weight(1f)); IconButton(onClick={ if(playing){player?.stop();player?.release();player=null;playing=false} else {player=MediaPlayer().apply{setDataSource(att.filePath);setOnCompletionListener{mp->playing=false;mp.release()};prepare();start()};playing=true} },modifier=Modifier.size(32.dp)){Icon(if(playing)Icons.Filled.Stop else Icons.Filled.PlayArrow,if(playing)"توقف" else "پخش",tint=ink)}; IconButton(onClick=onShare,modifier=Modifier.size(30.dp)){Icon(Icons.Filled.Share,"ارسال",tint=inkSoft,modifier=Modifier.size(16.dp))}; IconButton(onClick=onDelete,modifier=Modifier.size(30.dp)){Icon(Icons.Filled.Close,"حذف",tint=Brick,modifier=Modifier.size(16.dp))} } }
@Composable private fun FocusModeOverlay(body:String,onChange:(String)->Unit,onExit:()->Unit){ val words=remember(body){body.split(Regex("\\s+")).count{it.isNotBlank()}}; Dialog(onDismissRequest=onExit,properties=DialogProperties(usePlatformDefaultWidth=false)){ Box(Modifier.fillMaxSize().background(PaperWhite)){ Column(Modifier.fillMaxSize()){ Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){ IconButton(onClick=onExit){Icon(Icons.AutoMirrored.Filled.ArrowBack,"خروج",tint=InkSoft)}; Spacer(Modifier.weight(1f)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ Text("${words.fa()} کلمه",fontFamily=LalezarFont,fontSize=15.sp,color=InkSoft); Text("•",fontSize=15.sp,color=InkSoft); Text("${body.length.fa()} حرف",fontFamily=LalezarFont,fontSize=15.sp,color=InkSoft) }; Spacer(Modifier.weight(1f)); Text("✒️",fontSize=20.sp) }; Box(Modifier.fillMaxWidth().height(2.dp).background(Saffron.copy(alpha=.5f))); TextField(body,onChange,textStyle=TextStyle(fontFamily=VazirFont,fontSize=19.sp,color=Ink,lineHeight=36.sp,letterSpacing=0.2.sp,textAlign=TextAlign.Start,textDirection=TextDirection.Rtl),colors=transparentFieldColors(),placeholder={Text("فقط بنویس",color=InkSoft.copy(alpha=.6f),fontSize=18.sp)},modifier=Modifier.fillMaxWidth().weight(1f).padding(horizontal=26.dp,vertical=10.dp)) } } } }
private fun importUris(context:Context,scope:CoroutineScope,dao:NoteDao,noteId:Long,uris:List<Uri>){ if(uris.isEmpty())return; scope.launch(Dispatchers.IO){ uris.forEach{uri-> val f=AttachmentStore.copyToPrivate(context,uri)?:return@forEach; val mime=context.contentResolver.getType(uri)?:guessMimeType(f.name); dao.insertAttachment(Attachment(noteId=noteId,fileName=f.name,filePath=f.absolutePath,mimeType=mime,isImage=mime.startsWith("image/"))) }; withContext(Dispatchers.Main){ Toast.makeText(context,"ضمیمه اضافه شد ✔",Toast.LENGTH_SHORT).show() } } }
