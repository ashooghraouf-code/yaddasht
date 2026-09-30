package ir.yaddasht.app.ui.screen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.R
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.data.Priority
import ir.yaddasht.app.data.Task
import ir.yaddasht.app.data.TaskDao
import ir.yaddasht.app.reminder.LeadTime
import ir.yaddasht.app.reminder.ReminderScheduler
import ir.yaddasht.app.ui.theme.Brick
import ir.yaddasht.app.ui.theme.DeepGreen
import ir.yaddasht.app.ui.theme.DeepGreenSoft
import ir.yaddasht.app.ui.theme.Ink
import ir.yaddasht.app.ui.theme.InkSoft
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.LineGreen
import ir.yaddasht.app.ui.theme.MutedGreenText
import ir.yaddasht.app.ui.theme.PaperWhite
import ir.yaddasht.app.ui.theme.Saffron
import ir.yaddasht.app.ui.theme.VazirFont
import ir.yaddasht.app.ui.theme.ThemeBackground
import ir.yaddasht.app.ui.theme.ThemeKit
import ir.yaddasht.app.util.Checklist
import ir.yaddasht.app.util.FaDate
import ir.yaddasht.app.util.FullBackup
import ir.yaddasht.app.util.NoteLock
import ir.yaddasht.app.util.fa
import ir.yaddasht.app.util.faDigits
import ir.yaddasht.app.util.relativeTimeFa
import ir.yaddasht.app.util.shareBackupFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val HOLIDAY_RED = Color(0xFFE5484D)
private val WEEK_FA = listOf("ش","ی","د","س","چ","پ","ج")
private const val DAY_MS = 86_400_000L
private const val HOME_THEME_PREFS = "home_theme_prefs"
private const val HOME_THEME_KEY = "home_theme_config"
private const val HOME_DEFAULT_PRIMARY = 0xFF0F3D2E.toInt()

private enum class HomeThemeKind { Solid, Gradient, Pattern }
private enum class HomePatternKind { None, Dots, Lines, Grid, Diagonal, Linen, Waves, Stars, Bokeh, Diamonds }
private data class HomeThemeConfig(val kind: HomeThemeKind=HomeThemeKind.Solid, val primary:Int=HOME_DEFAULT_PRIMARY, val secondary:Int?=null, val pattern:HomePatternKind=HomePatternKind.None, val textureAlpha:Float=0.10f, val dim:Float=0f)
private data class HomeSwatch(val name:String, val emoji:String, val color:Int)
private data class HomeGradient(val name:String, val emoji:String, val from:Int, val to:Int)

private val HomeSolidPresets = listOf(
    HomeSwatch("کاغذ کرم","📜",0xFFFFF8E1.toInt()),HomeSwatch("سپیا","🧻",0xFFF4ECD8.toInt()),HomeSwatch("چای نبات","🍵",0xFFE8DCC4.toInt()),HomeSwatch("ابریشم","🤍",0xFFFFFBF0.toInt()),HomeSwatch("کاغذ پوستی","📃",0xFFEFE6D3.toInt()),HomeSwatch("مومیایی","🏺",0xFFE0D3B8.toInt()),
    HomeSwatch("شب","🌙",0xFF121212.toInt()),HomeSwatch("نفتی","🛢️",0xFF0E1116.toInt()),HomeSwatch("جنگلان","🌲",0xFF102018.toInt()),HomeSwatch("زغالی","🪨",0xFF1A1A1A.toInt()),HomeSwatch("نیمه‌شب","🌌",0xFF0B1026.toInt()),HomeSwatch("دود","🌫️",0xFF20232A.toInt()),
    HomeSwatch("سبز چراغ","🏮",0xFF0F3D2E.toInt()),HomeSwatch("باغ","🌿",0xFF1B5E20.toInt()),HomeSwatch("زیتون","🫒",0xFF33691E.toInt()),HomeSwatch("کاج","🌲",0xFF0B3D2C.toInt()),HomeSwatch("یشم","💎",0xFF2E7D52.toInt()),HomeSwatch("جنگل بارانی","🌧️",0xFF143D2B.toInt()),
    HomeSwatch("آسمان","☁️",0xFFE3F2FD.toInt()),HomeSwatch("دریا","🌊",0xFF01579B.toInt()),HomeSwatch("فیروزه","🧿",0xFF00838F.toInt()),HomeSwatch("نیلی","🔵",0xFF1A237E.toInt()),HomeSwatch("اقیانوس","🐋",0xFF023E5C.toInt()),HomeSwatch("مه","🌫️",0xFFCFD8DC.toInt()),
    HomeSwatch("آفتاب","☀️",0xFFFFF3E0.toInt()),HomeSwatch("نارنج","🍊",0xFFE65100.toInt()),HomeSwatch("گل‌گون","🌸",0xFFF8BBD0.toInt()),HomeSwatch("عنابی","🍇",0xFF880E4F.toInt()),HomeSwatch("زعفران","🌼",0xFFFFB74D.toInt()),HomeSwatch("آجر","🧱",0xFFB71C1C.toInt()),
    HomeSwatch("فیروزه ایرانی","🕌",0xFF00A6A6.toInt()),HomeSwatch("لاجورد","🔷",0xFF283593.toInt()),HomeSwatch("زرشک","🍒",0xFF8E1B3A.toInt()),HomeSwatch("کویر","🏜️",0xFF8D6E63.toInt()),HomeSwatch("مس","🥉",0xFFB87333.toInt()),HomeSwatch("زیتون ایرانی","🌿",0xFF6B8E23.toInt()),
    HomeSwatch("ارغوانی","🪻",0xFF6A1B9A.toInt()),HomeSwatch("فیروزهٔ روشن","🩵",0xFF4DD0E1.toInt()),HomeSwatch("لیمویی تیره","🍈",0xFF9E9D24.toInt()),HomeSwatch("صورتی چرخی","🍥",0xFFEC407A.toInt()),HomeSwatch("آبی بادامی","🫧",0xFF4FC3F7.toInt()),HomeSwatch("قهوه شیری","🥛",0xFFA1887F.toInt())
)
private val HomeGradientPresets = listOf(
    HomeGradient("سپیده‌دم","🌅",0xFF0F2027.toInt(),0xFF2C5364.toInt()),HomeGradient("جنگل مه‌آلود","🌫️",0xFF134E5E.toInt(),0xFF0F2027.toInt()),HomeGradient("شب تار","🌑",0xFF090909.toInt(),0xFF1F1F1F.toInt()),HomeGradient("باغ سبز","🌿",0xFF0F3D2E.toInt(),0xFF1B5E20.toInt()),HomeGradient("آسمان نیلی","🔵",0xFF1A237E.toInt(),0xFF0D47A1.toInt()),HomeGradient("غروب کویر","🏜️",0xFF8D6E63.toInt(),0xFFBF360C.toInt()),HomeGradient("زعفران","🌼",0xFFFFB74D.toInt(),0xFFE65100.toInt()),HomeGradient("یاقوت","❤️‍🔥",0xFF880E4F.toInt(),0xFF4A148C.toInt()),HomeGradient("فیروزه","🧿",0xFF00838F.toInt(),0xFF006064.toInt()),HomeGradient("کاغذ و چای","🍵",0xFFFFF8E1.toInt(),0xFFE0D3B8.toInt()),HomeGradient("ارغوان","🪻",0xFF6A1B9A.toInt(),0xFFAD1457.toInt()),HomeGradient("اقیانوس","🐋",0xFF023E5C.toInt(),0xFF006064.toInt())
)
private val HomePatternLabels = listOf(HomePatternKind.None to "بدون طرح",HomePatternKind.Dots to "نقاط",HomePatternKind.Lines to "خط‌دار",HomePatternKind.Grid to "شطرنجی",HomePatternKind.Diagonal to "مورب",HomePatternKind.Linen to "کتان",HomePatternKind.Waves to "موج",HomePatternKind.Stars to "ستاره",HomePatternKind.Bokeh to "بوکه",HomePatternKind.Diamonds to "الماس")

private fun homeThemePrefs(c:Context)=c.getSharedPreferences(HOME_THEME_PREFS,Context.MODE_PRIVATE)
fun homeThemePrimary(c:Context):Int=homeThemeGet(c).primary
private fun homeThemeGet(c:Context):HomeThemeConfig{ val s=homeThemePrefs(c).getString(HOME_THEME_KEY,null); if(!s.isNullOrBlank()) homeThemeDecode(s)?.let{return it}; return HomeThemeConfig() }
private fun homeThemeSet(c:Context,cfg:HomeThemeConfig){ homeThemePrefs(c).edit().putString(HOME_THEME_KEY,homeThemeEncode(cfg)).apply() }
private fun homeThemeEncode(cfg:HomeThemeConfig)=buildString{ append(cfg.kind.name).append('|').append(cfg.primary).append('|').append(cfg.secondary?.toString()?:"n").append('|').append(cfg.pattern.name).append('|').append(cfg.textureAlpha).append('|').append(cfg.dim) }
private fun homeThemeDecode(raw:String):HomeThemeConfig?{ val p=raw.split("|"); if(p.size<6)return null; val k=runCatching{HomeThemeKind.valueOf(p[0])}.getOrDefault(HomeThemeKind.Solid); val pt=runCatching{HomePatternKind.valueOf(p[3])}.getOrDefault(HomePatternKind.None); return HomeThemeConfig(k,p[1].toIntOrNull()?:HOME_DEFAULT_PRIMARY,if(p[2]=="n")null else p[2].toIntOrNull(),pt,p[4].toFloatOrNull()?:0.10f,p[5].toFloatOrNull()?:0f) }
private fun Color.luminance():Float=0.2126f*red.coerceIn(0f,1f)+0.7152f*green.coerceIn(0f,1f)+0.0722f*blue.coerceIn(0f,1f)
private fun autoAccent(b:Color)=if(b.luminance()>0.55f)0xFF1A1A1A.toInt() else 0xFFFFFFFF.toInt()
private fun homePatternLabel(k:HomePatternKind)=HomePatternLabels.firstOrNull{it.first==k}?.second?:k.name
private fun launchHomeToolbox(c:Context){ for(n in listOf("ir.yaddasht.app.tools.ToolboxActivity","ir.yaddasht.app.ui.screen.ToolboxActivity")){ try{ c.startActivity(Intent().setClassName(c,n).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }catch(_:Exception){} }; Toast.makeText(c,"جعبه‌ابزار نصب/ثبت نشده است 🧰",Toast.LENGTH_SHORT).show() }
private fun homeHash01(i:Int,s:Int):Float=abs(sin(i*12.9898f+s*78.233f)*43758.5453f)%1f

@Composable private fun HomeBackground(config:HomeThemeConfig,modifier:Modifier=Modifier){ Box(modifier){ when(config.kind){ HomeThemeKind.Solid->Box(Modifier.matchParentSize().background(Color(config.primary))); HomeThemeKind.Gradient->Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(config.primary),Color(config.secondary?:config.primary))))); HomeThemeKind.Pattern->if(config.pattern==HomePatternKind.None) Box(Modifier.matchParentSize().background(Color(config.primary))) else HomePatternCanvas(Color(config.primary),Color(config.secondary?:autoAccent(Color(config.primary))),config.textureAlpha,config.pattern,Modifier.matchParentSize()) }; if(config.dim>0f) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=config.dim.coerceIn(0f,0.85f)))) } }
@Composable private fun HomePatternCanvas(base:Color,accent:Color,alpha:Float,kind:HomePatternKind,modifier:Modifier=Modifier){ Canvas(modifier){ drawRect(base); val col=accent.copy(alpha=alpha.coerceIn(0.02f,0.45f)); when(kind){ HomePatternKind.None->Unit; HomePatternKind.Dots->{val st=28.dp.toPx();val r=1.1.dp.toPx();var y=st/2f;var row=0;while(y<size.height){var x=if(row%2==0)st/2f else st;while(x<size.width){drawCircle(col,r,Offset(x,y));x+=st};y+=st;row++}}; HomePatternKind.Lines->{val sp=30.dp.toPx();var y=sp;while(y<size.height){drawLine(col,Offset(0f,y),Offset(size.width,y),1f);y+=sp}}; HomePatternKind.Grid->{val c=34.dp.toPx();var x=c;while(x<size.width){drawLine(col,Offset(x,0f),Offset(x,size.height),1f);x+=c};var y=c;while(y<size.height){drawLine(col,Offset(0f,y),Offset(size.width,y),1f);y+=c}}; HomePatternKind.Diagonal->{val sp=22.dp.toPx();var x=-size.height;while(x<size.width){drawLine(col,Offset(x,0f),Offset(x+size.height,size.height),1f);x+=sp}}; HomePatternKind.Linen->{val sp=9.dp.toPx();val th=col.copy(alpha=col.alpha*0.55f);var x=0f;while(x<size.width){drawLine(th,Offset(x,0f),Offset(x,size.height),1f);x+=sp};var y=0f;while(y<size.height){drawLine(th,Offset(0f,y),Offset(size.width,y),1f);y+=sp}}; HomePatternKind.Waves->{val amp=14.dp.toPx();val wl=150.dp.toPx();val sy=38.dp.toPx();var y=sy;while(y<size.height+amp){val p=Path();p.moveTo(0f,y);var x=0f;while(x<=size.width){p.lineTo(x,y+sin((x/wl)*2f*PI.toFloat())*amp);x+=8f};drawPath(p,col,style=Stroke(1.4f));y+=sy}}; HomePatternKind.Stars->{val br=1.2.dp.toPx();for(i in 0 until 110){val x=homeHash01(i,1)*size.width;val y=homeHash01(i,2)*size.height;val r=if(i%9==0)br*1.7f else br;val sa=if(i%5==0)0.9f else 0.55f;drawCircle(col.copy(alpha=col.alpha*sa),r,Offset(x,y))}}; HomePatternKind.Bokeh->{for(i in 0 until 18){val x=homeHash01(i,3)*size.width;val y=homeHash01(i,4)*size.height;val r=(18f+(i%5)*14f).dp.toPx();drawCircle(col.copy(alpha=col.alpha*0.22f),r,Offset(x,y))}}; HomePatternKind.Diamonds->{val sp=42.dp.toPx();var x=-size.height;while(x<size.width){drawLine(col,Offset(x,0f),Offset(x+size.height,size.height),1f);drawLine(col,Offset(x+size.height,0f),Offset(x,size.height),1f);x+=sp}} } } }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(dao:NoteDao, taskDao:TaskDao, onOpenNote:(Long)->Unit, onNewNote:()->Unit, onOpenTask:(Long)->Unit, onThemeChanged:()->Unit={}){
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var homeTheme by remember{mutableStateOf(homeThemeGet(context))}
    val baseColor=Color(homeTheme.primary); val isLightBackground=baseColor.luminance()>0.55f&&homeTheme.dim<0.25f
    val onBg=if(isLightBackground)Color(0xFF17201B) else PaperWhite; val onBgMuted=if(isLightBackground)Color(0xFF5B665F) else MutedGreenText; val onBgAccent=if(isLightBackground)Color(0xFF8A5A00) else Saffron
    val notes by dao.observeNotes().collectAsState(initial=emptyList()); val counts by dao.observeAttachmentCounts().collectAsState(initial=emptyList()); val countMap=counts.associate{it.noteId to it.count}
    val tasks by taskDao.getAllTasks().collectAsState(initial=emptyList())
    var tab by rememberSaveable{mutableIntStateOf(0)}; var query by rememberSaveable{mutableStateOf("")}
    var noteToDelete by remember{mutableStateOf<Note?>(null)}; var showStats by remember{mutableStateOf(false)}; var hideMemory by rememberSaveable{mutableStateOf(false)}
    var showAddTask by remember{mutableStateOf(false)}; var editTask by remember{mutableStateOf<Task?>(null)}; var newTaskOnDate by remember{mutableLongStateOf(0L)}
    var showThemePicker by remember{mutableStateOf(false)}
    val (tjy,tjm,tjd)=FaDate.jalali(System.currentTimeMillis()); var calJy by remember{mutableIntStateOf(tjy)}; var calJm by remember{mutableIntStateOf(tjm)}; var calDay by remember{mutableIntStateOf(tjd)}
    LaunchedEffect(tab){ if(tab==2){ val (jy,jm,jd)=FaDate.jalali(System.currentTimeMillis()); calJy=jy; calJm=jm; calDay=jd } }
    val restoreLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri-> if(uri!=null) scope.launch(Dispatchers.IO){ val (n,t)=FullBackup.importAll(context,dao,taskDao,uri); withContext(Dispatchers.Main){ Toast.makeText(context,if(n+t>0)"$n یادداشت و $t وظیفه بازیابی شد ✅" else "فایل پشتیبان معتبر نبود ❌",Toast.LENGTH_LONG).show() } } }
    fun doBackup(){ scope.launch(Dispatchers.IO){ val f=FullBackup.exportAll(context,dao,taskDao); withContext(Dispatchers.Main){ shareBackupFile(context,f) } } }
    val filteredNotes=notes.filter{ query.isBlank()||it.title.contains(query,true)||(!NoteLock.isLocked(it.body)&&it.body.contains(query,true)) }
    val pinned=filteredNotes.filter{it.pinned}; val others=filteredNotes.filterNot{it.pinned}; val filteredTasks=tasks.filter{ query.isBlank()||it.title.contains(query,true) }
    val memory=remember(notes){pickMemory(notes)}
    val itemsByDay=remember(tasks,notes,calJy,calJm){ val l=mutableListOf<Triple<String,Long,Boolean>>(); tasks.filter{it.dueDate>0}.forEach{ l.add(Triple("task:${it.id}",it.dueDate,it.isCompleted)) }; notes.filter{it.reminderAt>0}.forEach{ l.add(Triple("note:${it.id}",it.reminderAt,false)) }; l.groupBy{ val (a,b,c)=FaDate.jalali(it.second); Triple(a,b,c) } }

    Scaffold(floatingActionButton={ when(tab){ 0->NewNoteFab(onNewNote); 1->ExtendedFloatingActionButton(onClick={ newTaskOnDate=0L; showAddTask=true },containerColor=Saffron,contentColor=Ink){ Icon(Icons.Filled.Add,"جدید"); Spacer(Modifier.width(8.dp)); Text("وظیفه جدید",fontFamily=LalezarFont,fontSize=17.sp) } } }){ padding->
        Box(Modifier.fillMaxSize().padding(padding)){
            HomeBackground(config=homeTheme,modifier=Modifier.fillMaxSize())
            if(tab==3){
                BoardScreen(notes=notes, noteDao=dao, onOpenNote={ onOpenNote(it) }, onBack={ tab=0 })
            } else {
            Column(Modifier.fillMaxSize()){
                HomeHeader(count=notes.size,onStats={showStats=true},onBackup={doBackup()},onRestore={restoreLauncher.launch(arrayOf("*/*"))},onThemePicker={showThemePicker=true},onBg=onBg,onBgMuted=onBgMuted,onBgAccent=onBgAccent)
                TabRow(selectedTabIndex=tab,containerColor=Color.Transparent,modifier=Modifier.padding(horizontal=12.dp,vertical=2.dp)){
                    Tab(selected=tab==0,onClick={tab=0},text={Text("📝 یادداشت",fontFamily=LalezarFont,fontSize=13.sp)},selectedContentColor=onBgAccent,unselectedContentColor=onBgMuted)
                    Tab(selected=tab==1,onClick={tab=1},text={Text("✅ وظیفه",fontFamily=LalezarFont,fontSize=13.sp)},selectedContentColor=onBgAccent,unselectedContentColor=onBgMuted)
                    Tab(selected=tab==2,onClick={tab=2},text={Text("📅 تقویم",fontFamily=LalezarFont,fontSize=13.sp)},selectedContentColor=onBgAccent,unselectedContentColor=onBgMuted)
                    Tab(selected=tab==3,onClick={tab=3},text={Text("📌 تابلو",fontFamily=LalezarFont,fontSize=13.sp)},selectedContentColor=onBgAccent,unselectedContentColor=onBgMuted)
                }
                if(tab==0&&!hideMemory&&memory!=null&&query.isBlank()){ Surface(onClick={onOpenNote(memory.id)},modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=6.dp),shape=RoundedCornerShape(16.dp),color=onBgAccent.copy(alpha=.14f),border=BorderStroke(1.dp,onBgAccent.copy(alpha=.4f))){ Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){ Text("⏳",fontSize=22.sp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){ Text("خاطره‌ای از گذشته",fontFamily=LalezarFont,fontSize=14.sp,color=onBgAccent); Text(memory.title.ifBlank{"بدون عنوان"},fontSize=12.sp,color=onBg,maxLines=1,overflow=TextOverflow.Ellipsis) }; IconButton(onClick={hideMemory=true},modifier=Modifier.size(28.dp)){Icon(Icons.Filled.Close,null,tint=onBgMuted,modifier=Modifier.size(16.dp))} } } } }
                if(tab!=2) SearchBox(query,{query=it},Modifier.padding(horizontal=20.dp))
                when(tab){
                    0->when{ notes.isEmpty()->EmptyState(onBg,onBgMuted,onBgAccent); filteredNotes.isEmpty()->CenterMessage("چیزی پیدا نشد 🔍",onBgMuted); else->LazyVerticalGrid(columns=GridCells.Adaptive(168.dp),contentPadding=PaddingValues(14.dp,16.dp,14.dp,120.dp),modifier=Modifier.fillMaxSize()){ if(pinned.isNotEmpty()){ item(span={GridItemSpan(maxLineSpan)}){ SectionLabel("📌 سنجاق‌شده",onBgAccent) }; items(pinned,key={ "p${it.id}" }){ note-> NoteCard(note,countMap[note.id]?:0,{onOpenNote(note.id)},{togglePin(scope,dao,note)},{noteToDelete=note}) } }; if(others.isNotEmpty()){ if(pinned.isNotEmpty()){ item(span={GridItemSpan(maxLineSpan)}){ SectionLabel("🗒️ یادداشت‌ها",onBgAccent) } }; items(others,key={ "n${it.id}" }){ note-> NoteCard(note,countMap[note.id]?:0,{onOpenNote(note.id)},{togglePin(scope,dao,note)},{noteToDelete=note}) } } } }
                    1->when{ tasks.isEmpty()->EmptyTasksState(onBg,onBgMuted,onBgAccent); filteredTasks.isEmpty()->CenterMessage("وظیفه‌ای پیدا نشد 🔍",onBgMuted); else->LazyColumn(contentPadding=PaddingValues(16.dp,12.dp,16.dp,120.dp),modifier=Modifier.fillMaxSize()){ filteredTasks.forEach{ task-> item{ TaskCard(task,{editTask=task},{ scope.launch(Dispatchers.IO){ taskDao.update(task.copy(isCompleted=!task.isCompleted)) } },{ scope.launch(Dispatchers.IO){ ReminderScheduler.cancelAll(context,task.id,true); taskDao.deleteById(task.id) } }); Spacer(Modifier.height(10.dp)) } } } }
                    else->{ val holidayDays=remember(calJy,calJm){computeHolidayDays(calJy,calJm)}; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp)){ Spacer(Modifier.height(8.dp)); Row(verticalAlignment=Alignment.CenterVertically){ IconButton(onClick={ if(calJm>1) calJm-- else {calJm=12;calJy--} }){Icon(Icons.Filled.ChevronRight,"قبل",tint=onBgAccent)}; Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){ Text("${FaDate.monthName(calJm)} ${calJy.fa()}",fontFamily=LalezarFont,fontSize=20.sp,color=onBg); val im=jalaliMillis(calJy,calJm,calDay,12); Text("🌙 قمری: "+hijriFullFa(im),fontSize=10.sp,color=onBgMuted); Text("🌍 میلادی: "+gregorianFullFa(im),fontSize=10.sp,color=onBgMuted) }; IconButton(onClick={ if(calJm<12) calJm++ else {calJm=1;calJy++} }){Icon(Icons.Filled.ChevronLeft,"بعد",tint=onBgAccent)} }; Spacer(Modifier.height(6.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){ LegendItem(HOLIDAY_RED,"تعطیل رسمی",onBgMuted); LegendItem(Color(0xFF46A758),"وظیفه دور",onBgMuted); LegendItem(onBgAccent,"یادآور یادداشت",onBgMuted) }; Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){ WEEK_FA.forEach{ w-> Text(w,fontSize=11.sp,fontWeight=FontWeight.Bold,color=onBgAccent,modifier=Modifier.weight(1f),textAlign=TextAlign.Center) } }; Spacer(Modifier.height(4.dp)); val cells=List(leadingBlanks(calJy,calJm)){0}+(1..monthLen(calJy,calJm)).toList(); cells.chunked(7).forEach{ row-> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){ row.forEach{ d-> if(d==0) Box(Modifier.weight(1f)) else { val di=itemsByDay[Triple(calJy,calJm,d)].orEmpty(); val isToday=calJy==tjy&&calJm==tjm&&d==tjd; val isSel=calDay==d; val isH=holidayDays.contains(d); Box(Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(12.dp)).background(if(isSel) DeepGreenSoft else Color.Transparent).border(if(isH)2.dp else if(isToday)1.5.dp else 0.dp,if(isH) HOLIDAY_RED else onBgAccent,RoundedCornerShape(12.dp)).combinedClickable(onClick={ calDay=d; newTaskOnDate=jalaliMillis(calJy,calJm,d,0); showAddTask=true },onLongClick={ calDay=d; newTaskOnDate=jalaliMillis(calJy,calJm,d,0); showAddTask=true }).padding(3.dp)){ Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){ val dtc=when{ isH->HOLIDAY_RED; isSel->PaperWhite; isToday->onBgAccent; else->onBg }; Text(d.fa(),fontSize=12.sp,color=dtc,fontWeight=if(isToday||isSel||isH)FontWeight.Bold else FontWeight.Normal); val ti=di.filter{it.first.startsWith("task:")}; val ni=di.filter{it.first.startsWith("note:")}; ti.take(2).forEach{ it2-> Box(Modifier.fillMaxWidth().height(6.dp).padding(top=2.dp).clip(RoundedCornerShape(3.dp)).background(taskTint(it2.second,it2.third))) }; ni.take((2-ti.size.coerceAtMost(2)).coerceAtLeast(0)).forEach{ _-> Box(Modifier.fillMaxWidth().height(6.dp).padding(top=2.dp).clip(RoundedCornerShape(3.dp)).background(Saffron)) }; if(di.size>2) Text("+${(di.size-2).fa()}",fontSize=8.sp,color=if(isSel)PaperWhite.copy(alpha=.7f) else onBgMuted) } } } }; repeat(7-row.size){ Box(Modifier.weight(1f)) } }; Spacer(Modifier.height(3.dp)) }; Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){ Text("📍 ${calDay.fa()} ${FaDate.monthName(calJm)}",fontFamily=LalezarFont,fontSize=16.sp,color=onBgAccent,modifier=Modifier.weight(1f)); Surface(onClick={ newTaskOnDate=jalaliMillis(calJy,calJm,calDay,0); showAddTask=true },shape=RoundedCornerShape(10.dp),color=Saffron){ Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Filled.Add,null,tint=Ink,modifier=Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("وظیفه جدید",fontSize=11.sp,color=Ink,fontWeight=FontWeight.Bold) } } }; Spacer(Modifier.height(6.dp)); val si=itemsByDay[Triple(calJy,calJm,calDay)].orEmpty(); if(si.isEmpty()) Text("برای این روز یادآور یا وظیفه‌ای نیست 🌤️",fontSize=12.sp,color=onBgMuted); si.forEach{ item-> if(item.first.startsWith("note:")){ val id=item.first.removePrefix("note:").toLongOrNull()?:-1L; val nt=notes.find{it.id==id}; if(nt!=null) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DeepGreenSoft).border(1.5.dp,Saffron.copy(alpha=.75f),RoundedCornerShape(18.dp)).clickable{onOpenNote(nt.id)}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){ Text("📝",fontSize=18.sp); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)){ Text(nt.title.ifBlank{"یادداشت"},fontFamily=VazirFont,fontWeight=FontWeight.Bold,fontSize=14.sp,color=PaperWhite); Text("⏰ "+fullDateTime(nt.reminderAt),fontSize=11.sp,color=Saffron) } } } else { val id=item.first.removePrefix("task:").toLongOrNull()?:-1L; val tk=tasks.find{it.id==id}; if(tk!=null) TaskCard(tk,{editTask=tk},{ scope.launch(Dispatchers.IO){ taskDao.update(tk.copy(isCompleted=!tk.isCompleted)) } },{ scope.launch(Dispatchers.IO){ ReminderScheduler.cancelAll(context,tk.id,true); taskDao.deleteById(tk.id) } }) }; Spacer(Modifier.height(8.dp)) }; Spacer(Modifier.height(120.dp)) } }
            }
            }
        }
    }
    if(showStats) StatsDialog(notes,counts.sumOf{it.count}){ showStats=false }
    if(showAddTask) AddTaskDialog(newTaskOnDate,{ showAddTask=false; newTaskOnDate=0L }){ title,due,pr,ls-> scope.launch(Dispatchers.IO){ val id=taskDao.insert(Task(title=title,dueDate=due,priority=pr)); if(due>0) ReminderScheduler.scheduleMulti(context,id,title,due,true,ls) }; showAddTask=false; newTaskOnDate=0L; Toast.makeText(context,"وظیفه اضافه شد ✅",Toast.LENGTH_SHORT).show() }
    editTask?.let{ task-> AddTaskDialog(0L,task,{ editTask=null }){ title,due,pr,ls-> scope.launch(Dispatchers.IO){ ReminderScheduler.cancelAll(context,task.id,true); taskDao.update(task.copy(title=title,dueDate=due,priority=pr)); if(due>0) ReminderScheduler.scheduleMulti(context,task.id,title,due,true,ls) }; editTask=null; Toast.makeText(context,"وظیفه به‌روز شد ✅",Toast.LENGTH_SHORT).show() } }
    noteToDelete?.let{ note-> AlertDialog(onDismissRequest={noteToDelete=null}, title={Text("حذف یادداشت؟",fontFamily=LalezarFont,fontSize=20.sp)}, text={Text("«${note.title.ifBlank{"بدون عنوان"}}» همراه با ضمیمه‌هایش برای همیشه حذف می‌شود.")}, confirmButton={TextButton(onClick={ noteToDelete=null; scope.launch(Dispatchers.IO){ val a=dao.attachmentsByNote(note.id); dao.deleteById(note.id); a.forEach{File(it.filePath).delete()} } }){Text("حذف",color=Brick,fontWeight=FontWeight.Bold)}}, dismissButton={TextButton(onClick={noteToDelete=null}){Text("انصراف")}}) }
    if(showThemePicker) HomeThemeStudioDialog(homeTheme,{ showThemePicker=false }){ nc-> homeTheme=nc; homeThemeSet(context,nc); onThemeChanged(); showThemePicker=false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun HomeThemeStudioDialog(config:HomeThemeConfig,onDismiss:()->Unit,onApply:(HomeThemeConfig)->Unit){
    val gold=Color(0xFFFFB74D); val muted=Color(0xFF8B949E); val text=Color(0xFFE6EDF3); var draft by remember(config){mutableStateOf(config)}
    AlertDialog(onDismissRequest=onDismiss, containerColor=Color(0xFF151A20), title={Text("🎨 استودیوی تم",color=gold,fontSize=20.sp,fontWeight=FontWeight.Bold)},
        text={ Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(18.dp)).border(1.dp,Color.White.copy(alpha=.12f),RoundedCornerShape(18.dp))){ HomeBackground(draft,Modifier.fillMaxSize()); val fg=if(Color(draft.primary).luminance()>0.55f&&draft.dim<0.25f) Color(0xFF1A1A1A) else Color(0xFFF7F7F7); Column(Modifier.padding(14.dp)){ Text("پیش‌نمایش",color=fg,fontSize=16.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text("چراغ راه 🏮",color=fg.copy(alpha=.75f),fontSize=12.sp) } }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ HomeThemeKind.values().forEach{ k-> val l=when(k){HomeThemeKind.Solid->"ساده";HomeThemeKind.Gradient->"گرادیان";HomeThemeKind.Pattern->"طرح"}; HomeChip(l,draft.kind==k,gold,text){ draft=when(k){ HomeThemeKind.Solid->draft.copy(kind=HomeThemeKind.Solid,pattern=HomePatternKind.None,secondary=null); HomeThemeKind.Gradient-> if(draft.secondary==null){val g=HomeGradientPresets.first(); draft.copy(kind=HomeThemeKind.Gradient,primary=g.from,secondary=g.to,pattern=HomePatternKind.None)} else draft.copy(kind=HomeThemeKind.Gradient,pattern=HomePatternKind.None); HomeThemeKind.Pattern->draft.copy(kind=HomeThemeKind.Pattern,pattern=if(draft.pattern==HomePatternKind.None)HomePatternKind.Dots else draft.pattern,secondary=null) } } } }
            if(draft.kind==HomeThemeKind.Pattern){ Text("طرح زمینه",color=muted,fontSize=12.sp,fontWeight=FontWeight.Bold); Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){ HomePatternKind.values().forEach{ k-> HomeChip(homePatternLabel(k),draft.pattern==k,gold,text){ draft=draft.copy(pattern=k) } } }; Text("شدت طرح",color=muted,fontSize=12.sp,fontWeight=FontWeight.Bold); HomeSliderRow("شدت",draft.textureAlpha,0.03f,0.40f,text,muted){ draft=draft.copy(textureAlpha=it) } }
            Text("رنگ پایه",color=muted,fontSize=12.sp,fontWeight=FontWeight.Bold); HomeSwatchGrid(HomeSolidPresets,draft.primary,gold,muted){ draft=draft.copy(primary=it) }
            if(draft.kind==HomeThemeKind.Gradient){ Text("گرادیان آماده",color=muted,fontSize=12.sp,fontWeight=FontWeight.Bold); HomeGradientStrip(HomeGradientPresets,draft.primary,draft.secondary?:0,muted){ g-> draft=draft.copy(kind=HomeThemeKind.Gradient,primary=g.from,secondary=g.to,pattern=HomePatternKind.None) } }
            Text("تیرگی",color=muted,fontSize=12.sp,fontWeight=FontWeight.Bold); HomeSliderRow("تیرگی",draft.dim,0f,0.55f,text,muted){ draft=draft.copy(dim=it) }
        } },
        confirmButton={TextButton(onClick={onApply(draft)}){Text("اعمال",color=gold,fontWeight=FontWeight.Bold)}},
        dismissButton={Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ TextButton(onClick={draft=HomeThemeConfig()}){Text("پیش‌فرض",color=muted)}; TextButton(onClick=onDismiss){Text("انصراف",color=muted)} }})
}
@Composable private fun HomeChip(label:String,selected:Boolean,gold:Color,text:Color,onClick:()->Unit){ Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if(selected)gold else Color.White.copy(alpha=.08f)).border(1.dp,if(selected)gold else Color.White.copy(alpha=.14f),RoundedCornerShape(10.dp)).clickable(onClick=onClick).padding(horizontal=12.dp,vertical=7.dp)){ Text(label,color=if(selected)Color(0xFF111111) else text,fontSize=12.sp,fontWeight=FontWeight.Bold) } }
@Composable private fun HomeSwatchGrid(presets:List<HomeSwatch>,selected:Int,gold:Color,muted:Color,onSelect:(Int)->Unit){ Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ presets.chunked(5).forEach{ row-> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ row.forEach{ s-> Box(Modifier.size(34.dp).clip(CircleShape).background(Color(s.color)).border(if(selected==s.color)2.dp else 1.dp,if(selected==s.color)gold else Color.White.copy(alpha=.18f),CircleShape).clickable{onSelect(s.color)}) } } }; Text(presets.firstOrNull{it.color==selected}?.name?:"دلخواه",color=muted,fontSize=11.sp) } }
@Composable private fun HomeGradientStrip(presets:List<HomeGradient>,selectedFrom:Int,selectedTo:Int,muted:Color,onSelect:(HomeGradient)->Unit){ Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){ presets.forEach{ g-> Column(horizontalAlignment=Alignment.CenterHorizontally){ Box(Modifier.width(92.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(g.from),Color(g.to)))).border(if(selectedFrom==g.from&&selectedTo==g.to)2.dp else 1.dp,if(selectedFrom==g.from&&selectedTo==g.to)Color(0xFFFFB74D) else Color.White.copy(alpha=.16f),RoundedCornerShape(12.dp)).clickable{onSelect(g)}); Spacer(Modifier.height(4.dp)); Text(g.name,color=muted,fontSize=10.sp,maxLines=1) } } } }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun HomeSliderRow(label:String,value:Float,min:Float,max:Float,text:Color,muted:Color,onChange:(Float)->Unit){ Column{ Row(verticalAlignment=Alignment.CenterVertically){ Text(label,color=text,fontSize=12.sp,modifier=Modifier.weight(1f)); Text("${(value*100).toInt()}٪",color=muted,fontSize=11.sp) }; Slider(value=value,onValueChange=onChange,valueRange=min..max,modifier=Modifier.fillMaxWidth()) } }
@Composable private fun LegendItem(color:Color,label:String,textColor:Color){ Row(verticalAlignment=Alignment.CenterVertically){ Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color)); Spacer(Modifier.width(4.dp)); Text(label,fontSize=9.sp,color=textColor) } }
@Composable private fun TaskCard(task:Task,onClick:()->Unit,onToggle:()->Unit,onDelete:()->Unit){ val tint=taskTint(task.dueDate,task.isCompleted); Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DeepGreenSoft).border(1.5.dp,tint.copy(alpha=.75f),RoundedCornerShape(18.dp)).clickable{onClick()}.padding(12.dp)){ Row(verticalAlignment=Alignment.CenterVertically){ Checkbox(task.isCompleted,{onToggle()},colors=CheckboxDefaults.colors(checkedColor=tint,checkmarkColor=DeepGreen)); Spacer(Modifier.width(6.dp)); Column(Modifier.weight(1f)){ Text(task.title,fontFamily=VazirFont,fontWeight=FontWeight.Bold,fontSize=14.sp,color=if(task.isCompleted)MutedGreenText else PaperWhite,textDecoration=if(task.isCompleted)TextDecoration.LineThrough else null); if(task.dueDate>0){ Spacer(Modifier.height(2.dp)); Text("📅  "+fullDateTime(task.dueDate),fontSize=11.sp,color=tint,fontWeight=FontWeight.Bold) }; Text("✏️ برای ویرایش ضربه بزن",fontSize=9.sp,color=MutedGreenText.copy(alpha=.7f)) }; if(task.priority==Priority.HIGH&&!task.isCompleted) Text("🔴",fontSize=12.sp); IconButton(onClick=onDelete,modifier=Modifier.size(30.dp)){Icon(Icons.Filled.Close,"حذف",tint=Brick,modifier=Modifier.size(16.dp))} } } }
@Composable private fun AddTaskDialog(initialDate:Long=0L,initialTask:Task?=null,onDismiss:()->Unit,onSave:(String,Long,Priority,Set<LeadTime>)->Unit){ var title by remember{mutableStateOf(initialTask?.title?:"")}; var dueDate by remember{mutableLongStateOf(initialTask?.dueDate?:initialDate)}; var showCalendar by remember{mutableStateOf(false)}; var priority by remember{mutableStateOf(initialTask?.priority?:Priority.NORMAL)}; var leadTimes by remember{mutableStateOf(setOf(LeadTime.NONE,LeadTime.HOUR_1))}
    AlertDialog(onDismissRequest=onDismiss, title={Text(if(initialTask!=null)"✏️ ویرایش وظیفه" else "✅ وظیفه جدید",fontFamily=LalezarFont,fontSize=20.sp)}, text={ Column(Modifier.verticalScroll(rememberScrollState())){ OutlinedTextField(title,{title=it},label={Text("عنوان وظیفه")},singleLine=true,modifier=Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp)); Surface(onClick={showCalendar=true},shape=RoundedCornerShape(12.dp),color=DeepGreenSoft,border=BorderStroke(1.dp,LineGreen)){ Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){ Text("📅",fontSize=18.sp); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)){ Text("سررسید + ساعت",fontSize=10.sp,color=MutedGreenText); Text(if(dueDate>0)fullDateTime(dueDate) else "انتخاب نشده",fontSize=13.sp,color=if(dueDate>0)Saffron else PaperWhite,fontWeight=FontWeight.Bold) }; if(dueDate>0) IconButton(onClick={dueDate=0L},modifier=Modifier.size(24.dp)){Icon(Icons.Filled.Close,"حذف",tint=Brick,modifier=Modifier.size(14.dp))} } }; Spacer(Modifier.height(12.dp)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ QuickDateChip("امروز ۲۱:۰۰"){ dueDate=Calendar.getInstance().apply{ set(Calendar.HOUR_OF_DAY,21); set(Calendar.MINUTE,0); set(Calendar.SECOND,0) }.timeInMillis }; QuickDateChip("فردا ۱۲:۰۰"){ dueDate=Calendar.getInstance().apply{ add(Calendar.DAY_OF_YEAR,1); set(Calendar.HOUR_OF_DAY,12); set(Calendar.MINUTE,0) }.timeInMillis }; QuickDateChip("هفته بعد"){ dueDate=Calendar.getInstance().apply{ add(Calendar.DAY_OF_YEAR,7); set(Calendar.HOUR_OF_DAY,9); set(Calendar.MINUTE,0) }.timeInMillis } }; Spacer(Modifier.height(14.dp)); Text("🔔 هشدارها (چند تا می‌تونی تیک بزنی)",fontFamily=LalezarFont,fontSize=14.sp,color=Saffron); Spacer(Modifier.height(6.dp)); Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){ LeadTime.values().forEach{ l-> LeadToggleChip(l.label,leadTimes.contains(l)){ leadTimes=if(leadTimes.contains(l)) leadTimes-l else leadTimes+l } } }; Spacer(Modifier.height(12.dp)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ DueChip("🔴 مهم",priority==Priority.HIGH){priority=Priority.HIGH}; DueChip("🟡 عادی",priority==Priority.NORMAL){priority=Priority.NORMAL}; DueChip("🟢 کم",priority==Priority.LOW){priority=Priority.LOW} } } }, confirmButton={TextButton(enabled=title.isNotBlank(),onClick={onSave(title.trim(),dueDate,priority,leadTimes)}){Text(if(initialTask!=null)"ذخیره" else "افزودن",color=if(title.isNotBlank())Saffron else Brick,fontWeight=FontWeight.Bold)}}, dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}} )
    if(showCalendar) YadavarDatePickerDialog(onConfirm={ dueDate=it; showCalendar=false },onDismiss={ showCalendar=false })
}
@Composable private fun QuickDateChip(label:String,onClick:()->Unit){ Surface(onClick=onClick,shape=RoundedCornerShape(10.dp),color=DeepGreenSoft,border=BorderStroke(1.dp,LineGreen)){ Text(label,Modifier.padding(horizontal=10.dp,vertical=6.dp),fontSize=11.sp,color=PaperWhite) } }
@Composable private fun DueChip(label:String,selected:Boolean,onClick:()->Unit){ Surface(onClick=onClick,shape=RoundedCornerShape(10.dp),color=if(selected)Saffron else DeepGreenSoft,border=BorderStroke(1.dp,if(selected)Saffron else LineGreen)){ Text(label,Modifier.padding(horizontal=10.dp,vertical=6.dp),fontSize=11.sp,color=if(selected)Ink else PaperWhite) } }
@Composable private fun EmptyTasksState(onBg:Color,onBgMuted:Color,accent:Color){ Column(Modifier.fillMaxSize().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){ Box(Modifier.size(110.dp).clip(CircleShape).border(2.dp,accent.copy(alpha=.5f),CircleShape),contentAlignment=Alignment.Center){ Text("✅",fontSize=46.sp) }; Spacer(Modifier.height(20.dp)); Text("لیست وظایفت خالی است",fontFamily=LalezarFont,fontSize=26.sp,color=onBg); Spacer(Modifier.height(8.dp)); Text("با دکمه «وظیفه جدید» اولین کار را اضافه کن 🎯",fontSize=13.sp,color=onBgMuted,textAlign=TextAlign.Center) } }
private fun pickMemory(notes:List<Note>):Note?{ val c=notes.filterNot{NoteLock.isLocked(it.body)}; val now=Calendar.getInstance(); val sd=c.filter{ val x=Calendar.getInstance().apply{timeInMillis=it.createdAt}; x.get(Calendar.MONTH)==now.get(Calendar.MONTH)&&x.get(Calendar.DAY_OF_MONTH)==now.get(Calendar.DAY_OF_MONTH)&&x.get(Calendar.YEAR)<now.get(Calendar.YEAR) }; val od=c.filter{ System.currentTimeMillis()-it.createdAt>45L*24*3600*1000 }; return sd.ifEmpty{od}.randomOrNull() }
private fun togglePin(scope:CoroutineScope,dao:NoteDao,note:Note){ scope.launch(Dispatchers.IO){ dao.update(note.copy(pinned=!note.pinned,updatedAt=System.currentTimeMillis())) } }
@Composable private fun HomeHeader(count:Int,onStats:()->Unit,onBackup:()->Unit,onRestore:()->Unit,onThemePicker:()->Unit,onBg:Color,onBgMuted:Color,onBgAccent:Color){ val context=LocalContext.current; Column(Modifier.padding(start=20.dp,end=12.dp,top=12.dp,bottom=8.dp)){ Row(verticalAlignment=Alignment.CenterVertically){ Box(Modifier.size(52.dp).clip(CircleShape).background(Saffron),contentAlignment=Alignment.Center){ Icon(painter=painterResource(id=R.drawable.ic_launcher_foreground),contentDescription="لوگو چراغ راه",modifier=Modifier.size(32.dp),tint=Color.Unspecified) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f,fill=false)){ Text("چراغ راه",fontFamily=LalezarFont,fontSize=26.sp,color=onBg,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis) }; Spacer(Modifier.width(4.dp)); Row(horizontalArrangement=Arrangement.spacedBy(0.dp)){ IconButton(onClick=onThemePicker,modifier=Modifier.size(36.dp)){Text("🎨",fontSize=18.sp)}; IconButton(onClick={launchHomeToolbox(context)},modifier=Modifier.size(36.dp)){Text("🧰",fontSize=18.sp)}; IconButton(onClick=onBackup,modifier=Modifier.size(36.dp)){Icon(Icons.Filled.FileUpload,"پشتیبان",tint=onBgMuted,modifier=Modifier.size(20.dp))}; IconButton(onClick=onRestore,modifier=Modifier.size(36.dp)){Icon(Icons.Filled.FileDownload,"بازیابی",tint=onBgMuted,modifier=Modifier.size(20.dp))}; IconButton(onClick=onStats,modifier=Modifier.size(36.dp)){Icon(Icons.Filled.BarChart,"آمار",tint=onBgMuted,modifier=Modifier.size(20.dp))} } }; Spacer(Modifier.height(6.dp)); Text(FaDate.full(System.currentTimeMillis()),fontSize=13.sp,color=onBgMuted,modifier=Modifier.padding(start=2.dp)) } }
@Composable private fun StatsDialog(notes:List<Note>,attachTotal:Int,onDismiss:()->Unit){ val ul=notes.filterNot{NoteLock.isLocked(it.body)}; val tw=ul.sumOf{it.body.split(Regex("\\s+")).count(String::isNotBlank)}; val wk=notes.count{System.currentTimeMillis()-it.updatedAt<7L*24*3600*1000}; AlertDialog(onDismissRequest=onDismiss,containerColor=Color(0xFF151A20),title={Text("📊 آمار دفترچه",fontFamily=LalezarFont,fontSize=20.sp,color=Color(0xFFFFB74D))},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){ StatRow("کل یادداشت‌ها",notes.size.fa()); StatRow("کلمه‌های نوشته‌شده",tw.fa()); StatRow("ویرایش در این هفته",wk.fa()); StatRow("ضمیمه‌ها",attachTotal.fa()); StatRow("سنجاق‌شده",notes.count{it.pinned}.fa()); StatRow("قفل‌شده 🔒",notes.count{NoteLock.isLocked(it.body)}.fa()); StatRow("چک‌لیست فعال",notes.count{Checklist.isChecklist(it.body)}.fa()) }},confirmButton={TextButton(onDismiss){Text("بستن",color=Color(0xFFFFB74D),fontWeight=FontWeight.Bold)}}) }
@Composable private fun StatRow(label:String,value:String){ Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){ Text(label,fontSize=13.sp,color=Color(0xFFBFC9C3),modifier=Modifier.weight(1f)); Text(value,fontFamily=LalezarFont,fontSize=18.sp,color=Color(0xFFFFB74D)) } }
@Composable private fun SearchBox(query:String,onQueryChange:(String)->Unit,modifier:Modifier=Modifier){ Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DeepGreenSoft).border(1.dp,LineGreen,RoundedCornerShape(16.dp)).padding(horizontal=14.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.Search,null,tint=Saffron); Spacer(Modifier.width(10.dp)); TextField(value=query,onValueChange=onQueryChange,placeholder={Text("جستجو در یادداشت‌ها و وظایف…",color=MutedGreenText)},colors=TextFieldDefaults.colors(focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent),textStyle=androidx.compose.ui.text.TextStyle(color=PaperWhite,fontSize=14.sp),singleLine=true,modifier=Modifier.fillMaxWidth()) } }
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun NoteCard(note:Note,attachCount:Int,onClick:()->Unit,onLongClick:()->Unit,onDelete:()->Unit){
    val angle=remember(note.id){((note.id%3)-1)*1.3f}; val locked=NoteLock.isLocked(note.body); val checklist=Checklist.isChecklist(note.body); val hasReminder=note.reminderAt>System.currentTimeMillis()
    val ctx=LocalContext.current
    val paperCfg=remember(note.id,note.color){ ThemeKit.getNotePaperOr(ctx,note.id,note.color) }
    val tone=ThemeKit.contentColors(paperCfg); val ink=tone.onSurface; val inkSoft=tone.muted
    Box(Modifier.padding(5.dp).fillMaxWidth().graphicsLayer{ rotationZ=angle; shadowElevation=9f; shape=RoundedCornerShape(18.dp); clip=true }){
        Box(Modifier.clip(RoundedCornerShape(18.dp)).combinedClickable(onClick=onClick,onLongClick=onLongClick)){
            ThemeBackground(config=paperCfg,modifier=Modifier.matchParentSize())
            Column(Modifier.padding(12.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){ Text((if(note.pinned)"📌  " else "")+note.title.ifBlank{"بدون عنوان"},fontFamily=LalezarFont,fontSize=17.sp,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f)); Icon(Icons.Filled.Close,"حذف",tint=inkSoft.copy(alpha=.55f),modifier=Modifier.size(26.dp).clip(CircleShape).clickable(onClick=onDelete).padding(5.dp)) }
                Spacer(Modifier.height(6.dp))
                when{ locked->Text("🔒 محتوای محرمانه",fontSize=12.sp,color=inkSoft); checklist->{ val (d,t)=Checklist.progress(note.body); Text("✅ ${d.fa()} از ${t.fa()} انجام شد",fontSize=12.sp,color=ink,fontWeight=FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(note.body.lines().take(3).joinToString("\n"){it.removePrefix("☐ ").removePrefix("☑ ")},fontSize=11.5.sp,color=inkSoft,maxLines=3,lineHeight=18.sp) }; else->if(note.body.isNotBlank()) Text(note.body,fontFamily=VazirFont,fontSize=12.5.sp,color=inkSoft,maxLines=4,overflow=TextOverflow.Ellipsis,lineHeight=20.sp) }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment=Alignment.CenterVertically){ if(attachCount>0) Text("📎 ${attachCount.fa()}",fontSize=11.sp,color=inkSoft); if(hasReminder) Text("⏰  "+fullDateTime(note.reminderAt),fontSize=10.sp,color=tone.accent,fontWeight=FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(relativeTimeFa(note.updatedAt),fontSize=10.sp,color=inkSoft.copy(alpha=.8f)) }
            }
        }
    }
}
@Composable private fun SectionLabel(text:String,color:Color){ Text(text,fontFamily=LalezarFont,fontSize=16.sp,color=color,modifier=Modifier.padding(start=6.dp,top=8.dp,bottom=2.dp)) }
@Composable private fun NewNoteFab(onNewNote:()->Unit){ ExtendedFloatingActionButton(onClick=onNewNote,containerColor=Saffron,contentColor=Ink){ Icon(Icons.Filled.Add,"جدید"); Spacer(Modifier.width(8.dp)); Text("یادداشت جدید",fontFamily=LalezarFont,fontSize=17.sp) } }
@Composable private fun EmptyState(onBg:Color,onBgMuted:Color,accent:Color){ Column(Modifier.fillMaxSize().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){ Box(Modifier.size(110.dp).clip(CircleShape).border(2.dp,accent.copy(alpha=.5f),CircleShape),contentAlignment=Alignment.Center){ Text("✍️",fontSize=46.sp) }; Spacer(Modifier.height(20.dp)); Text("دفترچه‌ات خالی است",fontFamily=LalezarFont,fontSize=26.sp,color=onBg); Spacer(Modifier.height(8.dp)); Text("یادداشت بنویس، صدا ضبط کن، نقاشی بکش، چک‌لیست بساز و رویشان قفل بگذار ✨",fontSize=13.sp,color=onBgMuted,textAlign=TextAlign.Center) } }
@Composable private fun CenterMessage(text:String,color:Color){ Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ Text(text,fontSize=15.sp,color=color) } }
private fun jalaliMillis(jy:Int,jm:Int,jd:Int,hour:Int=12):Long{ var est=1617220800000L+(jy-1400).toLong()*365L*DAY_MS+(jm-1).toLong()*30L*DAY_MS+(jd-1).toLong()*DAY_MS; for(i in 0 until 700){ val (y,m,d)=FaDate.jalali(est); if(y==jy&&m==jm&&d==jd){ val c=Calendar.getInstance(); c.timeInMillis=est; c.set(Calendar.HOUR_OF_DAY,hour); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.timeInMillis }; val dir=when{ y<jy->1; y>jy->-1; m<jm->1; m>jm->-1; d<jd->1; else->-1 }; est+=dir*DAY_MS }; return est }
private fun leadingBlanks(jy:Int,jm:Int):Int{ val c=Calendar.getInstance(); c.timeInMillis=jalaliMillis(jy,jm,1,12); return when(c.get(Calendar.DAY_OF_WEEK)){ Calendar.SATURDAY->0; Calendar.SUNDAY->1; Calendar.MONDAY->2; Calendar.TUESDAY->3; Calendar.WEDNESDAY->4; Calendar.THURSDAY->5; else->6 } }
private fun monthLen(jy:Int,jm:Int):Int{ val (ny,nm)=if(jm==12) jy+1 to 1 else jy to jm+1; return ((jalaliMillis(ny,nm,1,12)-jalaliMillis(jy,jm,1,12))/DAY_MS).toInt() }
private fun iranHijri(m:Long):Triple<Int,Int,Int>{ val c=android.icu.util.IslamicCalendar(); c.timeInMillis=m+DAY_MS; return Triple(c.get(android.icu.util.Calendar.MONTH)+1,c.get(android.icu.util.Calendar.DAY_OF_MONTH),c.get(android.icu.util.Calendar.YEAR)) }
private fun weekdayFa(m:Long):String{ val c=Calendar.getInstance(); c.timeInMillis=m; return when(c.get(Calendar.DAY_OF_WEEK)){ Calendar.SATURDAY->"شنبه"; Calendar.SUNDAY->"یکشنبه"; Calendar.MONDAY->"دوشنبه"; Calendar.TUESDAY->"سه‌شنبه"; Calendar.WEDNESDAY->"چهارشنبه"; Calendar.THURSDAY->"پنجشنبه"; else->"جمعه" } }
private fun taskTint(due:Long,completed:Boolean):Color{ if(completed)return Color(0xFF5E8077); if(due<=0L)return Color(0xFF888888); val days=(due-System.currentTimeMillis()).toFloat()/DAY_MS; val t=days.coerceIn(0f,7f)/7f; val r=Color(0xFFE5484D); val a=Color(0xFFF5A524); val g=Color(0xFF46A758); return if(t<.5f) lerp(r,a,t/.5f) else lerp(a,g,(t-.5f)/.5f) }
private fun gregorianFullFa(m:Long):String{ val c=Calendar.getInstance(); c.timeInMillis=m; val d=c.get(Calendar.DAY_OF_MONTH); val mo=c.get(Calendar.MONTH)+1; val y=c.get(Calendar.YEAR); val n=arrayOf("ژانویه","فوریه","مارس","آوریل","مه","ژوئن","ژوئیه","اوت","سپتامبر","اکتبر","نوامبر","دسامبر"); return "${weekdayFa(m)}، ${d.fa()} ${n.getOrElse(m-1){" "}} ${y.fa()}" }
private fun hijriFullFa(m:Long):String{ val (mo,d,y)=iranHijri(m); val n=arrayOf("محرم","صفر","ربیع‌الاول","ربیع‌الثانی","جمادی‌الاول","جمادی‌الثانی","رجب","شعبان","رمضان","شوال","ذی‌القعده","ذی‌الحجه"); return "${d.fa()} ${n.getOrElse(mo-1){" "}} ${y.fa()}" }
private fun fullDateTime(m:Long):String{ val (jy,jm,jd)=FaDate.jalali(m); val c=Calendar.getInstance(); c.timeInMillis=m; val h=c.get(Calendar.HOUR_OF_DAY).toString().padStart(2,'0').faDigits(); val mi=c.get(Calendar.MINUTE).toString().padStart(2,'0').faDigits(); return "${weekdayFa(m)}، ${jd.fa()} ${FaDate.monthName(jm)} ${jy.fa()}، ساعت $h:$mi" }
private fun isIranHoliday(jy:Int,jm:Int,jd:Int):Boolean{ val m=jalaliMillis(jy,jm,jd,12); val c=Calendar.getInstance(); c.timeInMillis=m; if(c.get(Calendar.DAY_OF_WEEK)==Calendar.FRIDAY)return true; when{ jm==1&&jd in 1..4->return true; jm==1&&jd==12->return true; jm==1&&jd==13->return true; jm==3&&jd==14->return true; jm==3&&jd==15->return true; jm==11&&jd==22->return true }; val (hm,hd)=iranHijri(m); if(hm==2&&hd==29&&iranHijri(m+DAY_MS).first==3)return true; return when{ hm==1&&hd==10->true; hm==2&&hd==20->true; hm==2&&hd==28->true; hm==2&&hd==30->true; hm==3&&hd==17->true; hm==7&&hd==13->true; hm==7&&hd==27->true; hm==8&&hd==15->true; hm==9&&hd==21->true; hm==10&&hd==1->true; hm==10&&hd==10->true; hm==12&&hd==18->true; else->false } }
private fun computeHolidayDays(jy:Int,jm:Int):Set<Int>{ val s=mutableSetOf<Int>(); for(d in 1..monthLen(jy,jm)) if(isIranHoliday(jy,jm,d)) s.add(d); return s }
