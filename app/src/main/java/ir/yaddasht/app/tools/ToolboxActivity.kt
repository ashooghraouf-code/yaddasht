package ir.yaddasht.app.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

// ---------------- پالت ----------------
private object Tb {
    val bg = Color(0xFF0E1116)
    val surface = Color(0xFF161B22)
    val card = Color(0xFF1C2330)
    val border = Color(0xFF2A3342)
    val gold = Color(0xFFFFB74D)
    val teal = Color(0xFF4DD0E1)
    val green = Color(0xFF46A758)
    val red = Color(0xFFE5484D)
    val text = Color(0xFFE6EDF3)
    val muted = Color(0xFF8B949E)
}

private enum class Cat(val fa: String) { CALC("محاسبه"), CONVERT("تبدیل"), ISLAMIC("اسلامی"), TEXT("متن"), LIFE("زندگی") }

private data class Tool(val id: String, val title: String, val subtitle: String, val emoji: String, val cat: Cat, val accent: Color)

private val ALL_TOOLS = listOf(
    Tool("calc", "ماشین‌حساب", "عملیات کامل + تاریخچه", "🧮", Cat.CALC, Tb.gold),
    Tool("percent", "درصد و تخفیف", "سود، مالیات، تخفیف", "💯", Cat.CALC, Tb.teal),
    Tool("split", "تقسیم صورت‌حساب", "با انعام و نفرات", "🍽️", Cat.CALC, Tb.green),
    Tool("unit", "تبدیل واحد", "طول، وزن، دما، سرعت", "📏", Cat.CONVERT, Tb.teal),
    Tool("date", "فاصلهٔ تاریخ", "سن و روزهای باقی‌مانده", "📅", Cat.CONVERT, Tb.gold),
    Tool("qibla", "قبله‌نما", "جهت کعبه با قطب‌نما", "🕋", Cat.ISLAMIC, Tb.green),
    Tool("tasbih", "تسبیح دیجیتال", "شمارنده با لرزش", "📿", Cat.ISLAMIC, Tb.gold),
    Tool("text", "ابزار متن", "شمارش و عدد به حروف", "🔤", Cat.TEXT, Tb.teal),
    Tool("water", "شمارگر آب", "هدف روزانهٔ نوشیدن", "💧", Cat.LIFE, Tb.teal),
    Tool("habit", "عادت روزانه", "زنجیرهٔ انجام کار", "✅", Cat.LIFE, Tb.green)
)

class ToolboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ToolboxRoot() }
    }
}

@Composable
private fun ToolboxRoot() {
    var openTool by remember { mutableStateOf<Tool?>(null) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Tb.bg, Color(0xFF0A0D12))))) {
        val t = openTool
        if (t == null) ToolHome(onOpen = { openTool = it }) else ToolScreen(tool = t, onBack = { openTool = null })
    }
}

@Composable
private fun ToolHome(onOpen: (Tool) -> Unit) {
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<Cat?>(null) }
    val shown = remember(query, cat) {
        ALL_TOOLS.filter {
            (cat == null || it.cat == cat) &&
                (query.isBlank() || it.title.contains(query, true) || it.subtitle.contains(query, true))
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🧰", fontSize = 26.sp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("جعبه‌ابزار", color = Tb.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("ابزارهای کاربردی آفلاین", color = Tb.muted, fontSize = 12.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)).background(Tb.surface)
                .border(1.dp, Tb.border, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(color = Tb.text, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                decorationBox = { inner -> Box { if (query.isEmpty()) Text("🔍 جستجوی ابزار…", color = Tb.muted, fontSize = 14.sp); inner() } }
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatChip("همه", cat == null) { cat = null }
            Cat.entries.forEach { c -> CatChip(c.fa, cat == c) { cat = if (cat == c) null else c } }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) { items(shown, key = { it.id }) { tool -> ToolCard(tool, onOpen) } }
    }
}

@Composable
private fun CatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).background(if (selected) Tb.gold else Tb.surface)
            .border(1.dp, if (selected) Tb.gold else Tb.border, CircleShape).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) { Text(label, color = if (selected) Tb.bg else Tb.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ToolCard(tool: Tool, onOpen: (Tool) -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Tb.card).border(1.dp, Tb.border, RoundedCornerShape(18.dp))
            .clickable { onOpen(tool) }.padding(14.dp)
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(tool.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Text(tool.emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(tool.title, color = Tb.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(tool.subtitle, color = Tb.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ToolScreen(tool: Tool, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = Tb.text) }
            Text(tool.emoji, fontSize = 20.sp); Spacer(Modifier.width(8.dp))
            Text(tool.title, color = Tb.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            when (tool.id) {
                "calc" -> CalcTool()
                "percent" -> PercentTool()
                "split" -> SplitTool()
                "unit" -> UnitTool()
                "date" -> DateTool()
                "qibla" -> QiblaTool()
                "tasbih" -> TasbihTool()
                "text" -> TextTool()
                "water" -> WaterTool()
                "habit" -> HabitTool()
            }
        }
    }
}

// ================= ۱) ماشین‌حساب =================
@Composable
private fun CalcTool() {
    val context = LocalContext.current
    var expr by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }
    fun eval() {
        if (expr.isBlank()) return
        try {
            val v = calcEvaluate(expr)
            if (v.isNaN() || v.isInfinite()) { result = "خطا"; return }
            val s = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            result = s; history.add(0, "$expr = $s"); if (history.size > 30) history.removeAt(history.size - 1)
        } catch (_: Exception) { result = "خطا" }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tb.surface).border(1.dp, Tb.border, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text(expr.ifBlank { "۰" }, color = Tb.muted, fontSize = 16.sp, maxLines = 2, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(result.ifBlank { " " }, color = Tb.gold, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        val rows = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "(", ")")
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    val isOp = k in listOf("÷", "×", "−", "+", "%", "C", "⌫", "(", ")")
                    KeyBtn(k, Modifier.weight(1f), if (isOp) Tb.card else Tb.surface, if (isOp) Tb.teal else Tb.text) {
                        when (k) {
                            "C" -> { expr = ""; result = "" }
                            "⌫" -> expr = expr.dropLast(1)
                            else -> { expr += k.replace("×", "*").replace("÷", "/").replace("−", "-"); result = "" }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyBtn("=", Modifier.weight(3f), Tb.gold, Tb.bg) { eval() }
            KeyBtn("📋", Modifier.weight(1f), Tb.surface, Tb.muted) {
                if (result.isNotBlank()) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("calc", result))
                }
            }
        }
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(12.dp)); Text("تاریخچه", color = Tb.muted, fontSize = 12.sp)
            Column(Modifier.fillMaxWidth().height(110.dp).verticalScroll(rememberScrollState())) {
                history.forEach { h -> Text(h, color = Tb.text, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { expr = h.substringBefore(" ="); result = h.substringAfter(" =") }.padding(vertical = 4.dp)) }
            }
        }
    }
}

@Composable
private fun KeyBtn(label: String, modifier: Modifier, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(modifier.height(54.dp).clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, Tb.border, RoundedCornerShape(12.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// ✅ اصلاح: کلاس به‌جای توابع داخلی (حل forward reference)
private class CalcParser(private val s: String) {
    private var pos = 0
    fun parse(): Double = expr()
    private fun peek(): Char? = if (pos < s.length) s[pos] else null
    private fun skipWs() { while (pos < s.length && s[pos] == ' ') pos++ }
    private fun expr(): Double {
        var v = term()
        while (true) {
            skipWs()
            when (peek()) {
                '+' -> { pos++; v += term() }
                '-' -> { pos++; v -= term() }
                else -> return v
            }
        }
    }
    private fun term(): Double {
        var v = factor()
        while (true) {
            skipWs()
            when (peek()) {
                '*' -> { pos++; v *= factor() }
                '/' -> { pos++; val d = factor(); v = if (d == 0.0) Double.NaN else v / d }
                else -> return v
            }
        }
    }
    private fun factor(): Double {
        skipWs()
        val c = peek() ?: throw IllegalArgumentException()
        if (c == '(') { pos++; val v = expr(); skipWs(); if (peek() == ')') pos++; return v }
        if (c == '-') { pos++; return -factor() }
        if (c == '+') { pos++; return factor() }
        val start = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
        if (start == pos) throw IllegalArgumentException()
        var v = s.substring(start, pos).toDouble()
        skipWs()
        if (peek() == '%') { pos++; v /= 100.0 }
        return v
    }
}
private fun calcEvaluate(input: String): Double {
    val s = input.replace("×", "*").replace("÷", "/").replace("−", "-").replace("٫", ".").replace(",", ".")
    return CalcParser(s).parse()
}

// ================= ۲) درصد =================
@Composable
private fun PercentTool() {
    var amount by remember { mutableStateOf("") }
    var percent by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) }
    val a = amount.toDoubleOrNull(); val p = percent.toDoubleOrNull()
    val out = when {
        a == null || p == null -> "—"
        mode == 0 -> fmt(a - a * p / 100.0)
        mode == 1 -> fmt(a + a * p / 100.0)
        else -> fmt(a * p / 100.0)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegBtn("تخفیف", mode == 0) { mode = 0 }; SegBtn("افزودن", mode == 1) { mode = 1 }; SegBtn("درصدِ عدد", mode == 2) { mode = 2 }
        }
        Spacer(Modifier.height(14.dp)); NumField("مبلغ اصلی", amount) { amount = it }
        Spacer(Modifier.height(10.dp)); NumField("درصد %", percent) { percent = it }
        Spacer(Modifier.height(16.dp)); ResultBox("نتیجه", out, Tb.gold)
    }
}

// ================= ۳) تقسیم =================
@Composable
private fun SplitTool() {
    var total by remember { mutableStateOf("") }
    var people by remember { mutableStateOf("") }
    var tip by remember { mutableStateOf("") }
    val t = total.toDoubleOrNull() ?: 0.0
    val n = (people.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)
    val tp = (tip.toDoubleOrNull() ?: 0.0) / 100.0
    val grand = t * (1 + tp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NumField("مجموع صورتحساب", total) { total = it }
        Spacer(Modifier.height(10.dp)); NumField("تعداد نفرات", people) { people = it }
        Spacer(Modifier.height(10.dp)); NumField("انعام %", tip) { tip = it }
        Spacer(Modifier.height(16.dp)); ResultBox("سهم هر نفر", fmt(grand / n), Tb.teal)
        Spacer(Modifier.height(8.dp)); ResultBox("مجموع با انعام", fmt(grand), Tb.muted)
    }
}

// ================= ۴) تبدیل واحد =================
private data class U(val name: String, val factor: Double)
private val UNIT_GROUPS: Map<String, List<U>> = mapOf(
    "طول" to listOf(U("متر", 1.0), U("کیلومتر", 1000.0), U("سانتی‌متر", 0.01), U("میلی‌متر", 0.001), U("مایل", 1609.344), U("فوت", 0.3048), U("اینچ", 0.0254)),
    "وزن" to listOf(U("کیلوگرم", 1.0), U("گرم", 0.001), U("تن", 1000.0), U("پوند", 0.45359237), U("اونس", 0.028349523)),
    "دما" to listOf(U("سلسیوس", 0.0), U("فارنهایت", 0.0), U("کلوین", 0.0)),
    "سرعت" to listOf(U("متر/ثانیه", 1.0), U("کیلومتر/ساعت", 0.277778), U("مایل/ساعت", 0.44704)),
    "مساحت" to listOf(U("متر مربع", 1.0), U("کیلومتر مربع", 1_000_000.0), U("هکتار", 10_000.0), U("فوت مربع", 0.092903))
)
@Composable
private fun UnitTool() {
    var group by remember { mutableStateOf("طول") }
    val units = UNIT_GROUPS[group]!!
    var from by remember { mutableStateOf(units[0].name) }
    var to by remember { mutableStateOf(units[1].name) }
    var value by remember { mutableStateOf("1") }
    LaunchedEffect(group) { from = units[0].name; to = units[1].name }
    val v = value.toDoubleOrNull() ?: 0.0
    val res = if (group == "دما") convertTemp(v, from, to) else fmt(v * units.first { it.name == from }.factor / units.first { it.name == to }.factor)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // ✅ اصلاح: horizontalScroll + import
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UNIT_GROUPS.keys.forEach { g -> SegBtn(g, group == g) { group = g } }
        }
        Spacer(Modifier.height(14.dp)); NumField("مقدار", value) { value = it }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // ✅ اصلاح: modifier به‌عنوان پارامتر
            Picker("از", from, units.map { it.name }, Modifier.weight(1f)) { from = it }
            Picker("به", to, units.map { it.name }, Modifier.weight(1f)) { to = it }
        }
        Spacer(Modifier.height(16.dp)); ResultBox("نتیجه", res, Tb.gold)
    }
}
private fun convertTemp(v: Double, from: String, to: String): String {
    val c = when (from) { "فارنهایت" -> (v - 32) * 5 / 9; "کلوین" -> v - 273.15; else -> v }
    val out = when (to) { "فارنهایت" -> c * 9 / 5 + 32; "کلوین" -> c + 273.15; else -> c }
    return fmt(out)
}
@Composable
private fun Picker(label: String, sel: String, opts: List<String>, modifier: Modifier = Modifier, onSel: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButtonRow(label, sel) { open = true }
        if (open) AlertDialog(onDismissRequest = { open = false },
            title = { Text(label, color = Tb.text) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                opts.forEach { o -> Text(o, color = if (o == sel) Tb.gold else Tb.text, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { onSel(o); open = false }.padding(vertical = 10.dp)) }
            } },
            confirmButton = { TextButton(onClick = { open = false }) { Text("بستن", color = Tb.gold) } },
            containerColor = Tb.surface)
    }
}
@Composable
private fun OutlinedButtonRow(label: String, sel: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tb.surface).border(1.dp, Tb.border, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp)) {
        Text(label, color = Tb.muted, fontSize = 11.sp); Text(sel, color = Tb.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ================= ۵) تاریخ =================
@Composable
private fun DateTool() {
    var d1 by remember { mutableStateOf(todayStr()) }
    var d2 by remember { mutableStateOf(todayStr()) }
    val days = remember(d1, d2) { daysBetween(d1, d2) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NumField("تاریخ ۱ (YYYY-MM-DD)", d1) { d1 = it }
        Spacer(Modifier.height(10.dp)); NumField("تاریخ ۲ (YYYY-MM-DD)", d2) { d2 = it }
        Spacer(Modifier.height(16.dp)); ResultBox("تفاضل روز", if (days == null) "—" else "${abs(days)} روز", Tb.teal)
        if (days != null) { Spacer(Modifier.height(8.dp)); ResultBox("تقریبی", "${abs(days) / 7} هفته · ${abs(days) / 30} ماه", Tb.muted) }
    }
}
private fun todayStr(): String { val c = Calendar.getInstance(); return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)) }
private fun daysBetween(a: String, b: String): Long? = try {
    val pa = a.split("-").map { it.toLong() }; val pb = b.split("-").map { it.toLong() }
    val ca = Calendar.getInstance(); ca.clear(); ca.set(pa[0].toInt(), pa[1].toInt() - 1, pa[2].toInt())
    val cb = Calendar.getInstance(); cb.clear(); cb.set(pb[0].toInt(), pb[1].toInt() - 1, pb[2].toInt())
    (cb.timeInMillis - ca.timeInMillis) / 86_400_000L
} catch (_: Exception) { null }

// ================= ۶) قبله‌نما =================
private const val KAABA_LAT = 21.4225
private const val KAABA_LNG = 39.8262
@Composable
private fun QiblaTool() {
    val context = LocalContext.current
    var lat by remember { mutableStateOf(35.6892) }
    var lng by remember { mutableStateOf(51.3890) }
    var located by remember { mutableStateOf(false) }
    var azimuth by remember { mutableStateOf(0f) }
    var hasSensor by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchOnce(context) { la, lo -> lat = la; lng = lo; located = true }
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fetchOnce(context) { la, lo -> lat = la; lng = lo; located = true }
    }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                hasSensor = true
                val rot = FloatArray(9); val rem = FloatArray(9); val ori = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.remapCoordinateSystem(rot, SensorManager.AXIS_X, SensorManager.AXIS_Y, rem)
                SensorManager.getOrientation(rem, ori)
                azimuth = Math.toDegrees(ori[0].toDouble()).toFloat()
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        // ✅ اصلاح: متغیر صریح + بررسی null
        if (sm != null && sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }
    val qibla = qiblaBearing(lat, lng)
    // ✅ اصلاح: relative به Float
    val relative = (((qibla - azimuth) % 360.0 + 360.0) % 360.0).toFloat()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!located) {
            Text("موقعیت پیش‌فرض: تهران", color = Tb.muted, fontSize = 12.sp); Spacer(Modifier.height(8.dp))
            OutlinedButtonRow("دریافت موقعیت دقیق", "لمس کنید") { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        } else Text("موقعیت: ${fmt(lat)} , ${fmt(lng)}", color = Tb.muted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(240.dp).clip(CircleShape).background(Tb.surface).border(2.dp, Tb.border, CircleShape)) {
            Text("🕋", fontSize = 40.sp, modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationZ = -relative })
            Text("N", color = Tb.red, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).graphicsLayer { rotationZ = -azimuth })
        }
        Spacer(Modifier.height(14.dp)); ResultBox("زاویهٔ قبله از شمال", "${fmt(qibla)}°", Tb.gold)
        Spacer(Modifier.height(8.dp)); ResultBox("جهت نسبت به گوشی", "${fmt(relative.toDouble())}° ${if (hasSensor) "(زنده)" else "(بدون سنسور)"}", Tb.teal)
        if (!hasSensor) Text("این دستگاه سنسور قطب‌نما ندارد؛ فقط زاویهٔ ثابت نمایش داده می‌شود.", color = Tb.muted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
// ✅ اصلاح: حذف @Composable
private fun fetchOnce(context: Context, onResult: (Double, Double) -> Unit) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val best: Location? = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { p -> try { lm.getLastKnownLocation(p) } catch (_: Exception) { null } }
            .maxByOrNull { it.accuracy }
        best?.let { onResult(it.latitude, it.longitude) }
    } catch (_: Exception) {}
}
private fun qiblaBearing(lat: Double, lng: Double): Double {
    val la1 = Math.toRadians(lat); val lo1 = Math.toRadians(lng)
    val la2 = Math.toRadians(KAABA_LAT); val lo2 = Math.toRadians(KAABA_LNG)
    val dLo = lo2 - lo1
    val y = sin(dLo); val x = cos(la1) * tan(la2) - sin(la1) * cos(dLo)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

// ================= ۷) تسبیح =================
@Composable
private fun TasbihTool() {
    val context = LocalContext.current
    var count by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(33) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(33, 99, 100).forEach { t -> SegBtn(t.toString(), target == t) { target = t; count = 0 } }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(200.dp).clip(CircleShape).background(Tb.card).border(3.dp, Tb.gold, CircleShape).clickable {
            count++; if (count >= target) count = 0; vibrate(context)
        }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.faNum(), color = Tb.gold, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                Text("/ ${target.faNum()}", color = Tb.muted, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(20.dp)); OutlinedButtonRow("صفر کردن", "شروع دوباره") { count = 0 }
    }
}
private fun vibrate(context: Context) {
    try {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(35)
    } catch (_: Exception) {}
}

// ================= ۸) متن =================
@Composable
private fun TextTool() {
    var text by remember { mutableStateOf("") }
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val nums = text.trim().toDoubleOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("متن یا عدد", color = Tb.muted) },
            minLines = 4, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tb.text, unfocusedTextColor = Tb.text, focusedBorderColor = Tb.gold, unfocusedBorderColor = Tb.border, cursorColor = Tb.gold))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultBox("کلمه", words.faNum(), Tb.teal, Modifier.weight(1f))
            ResultBox("کاراکتر", text.length.faNum(), Tb.muted, Modifier.weight(1f))
        }
        if (nums != null) { Spacer(Modifier.height(12.dp)); ResultBox("به حروف", numToFa(nums.toLong()), Tb.gold) }
        Spacer(Modifier.height(12.dp)); OutlinedButtonRow("پاک‌سازی فاصله", "یکسان‌سازی") { text = text.replace(Regex("\\s+"), " ").trim() }
    }
}

// ================= ۹) آب =================
@Composable
private fun WaterTool() {
    var glasses by remember { mutableIntStateOf(0) }
    val goal = 8
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("💧", fontSize = 48.sp); Spacer(Modifier.height(10.dp))
        Text("$glasses / $goal لیوان", color = Tb.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(Tb.surface)) {
            Box(Modifier.fillMaxWidth((glasses.toFloat() / goal).coerceIn(0f, 1f)).height(14.dp).clip(CircleShape).background(Tb.teal))
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KeyBtn("−", Modifier.weight(1f), Tb.surface, Tb.text) { if (glasses > 0) glasses-- }
            KeyBtn("+", Modifier.weight(1f), Tb.gold, Tb.bg) { if (glasses < goal) glasses++ }
        }
        if (glasses >= goal) { Spacer(Modifier.height(16.dp)); Text("آفرین! هدف امروز تکمیل شد ✅", color = Tb.green, fontSize = 14.sp) }
    }
}

// ================= ۱۰) عادت =================
@Composable
private fun HabitTool() {
    var name by remember { mutableStateOf("عادت جدید") }
    var streak by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام عادت", color = Tb.muted) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tb.text, unfocusedTextColor = Tb.text, focusedBorderColor = Tb.gold, unfocusedBorderColor = Tb.border, cursorColor = Tb.gold))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tb.card).border(1.dp, Tb.border, RoundedCornerShape(16.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🔥", fontSize = 40.sp); Text("$streak روز پشت‌سرهم", color = Tb.gold, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KeyBtn("انجام شد ✅", Modifier.weight(1f), Tb.green, Color.White) { streak++ }
            KeyBtn("ریست", Modifier.weight(1f), Tb.surface, Tb.muted) { streak = 0 }
        }
    }
}

// ================= مشترک =================
@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label, color = Tb.muted) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tb.text, unfocusedTextColor = Tb.text, focusedBorderColor = Tb.gold, unfocusedBorderColor = Tb.border, cursorColor = Tb.gold))
}
// ✅ اصلاح: modifier به‌عنوان پارامتر (حل weight خارج از scope)
@Composable
private fun ResultBox(label: String, value: String, color: Color, modifier: Modifier = Modifier.fillMaxWidth()) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Tb.surface).border(1.dp, Tb.border, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text(label, color = Tb.muted, fontSize = 12.sp); Spacer(Modifier.height(4.dp)); Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
private fun SegBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) Tb.gold else Tb.surface).border(1.dp, if (selected) Tb.gold else Tb.border, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(label, color = if (selected) Tb.bg else Tb.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
private fun fmt(d: Double): String { if (d.isNaN() || d.isInfinite()) return "—"; return if (d == d.toLong().toDouble()) d.toLong().toString() else String.format("%.2f", d) }
private fun Int.faNum(): String { val map = "۰۱۲۳۴۵۶۷۸۹"; return this.toString().map { if (it.isDigit()) map[it - '0'] else it }.joinToString("") }
private fun numToFa(n: Long): String {
    if (n == 0L) return "صفر"
    val neg = n < 0; val x = abs(n)
    val ones = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    val tens = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    val hund = arrayOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    fun three(v: Int): String {
        val parts = mutableListOf<String>()
        if (v / 100 > 0) parts.add(hund[v / 100])
        val r = v % 100
        when { r < 20 -> if (ones[r].isNotEmpty()) parts.add(ones[r]); else -> { if (r / 10 > 0) parts.add(tens[r / 10]); if (r % 10 > 0) parts.add(ones[r % 10]) } }
        return parts.joinToString(" و ")
    }
    val groups = mutableListOf<String>(); var rest = x; var idx = 0
    val scales = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون")
    while (rest > 0) { val g = (rest % 1000).toInt(); if (g > 0) groups.add(0, three(g) + (if (scales[idx].isNotEmpty()) " " + scales[idx] else "")); rest /= 1000; idx++ }
    return (if (neg) "منفی " else "") + groups.joinToString(" و ")
}
