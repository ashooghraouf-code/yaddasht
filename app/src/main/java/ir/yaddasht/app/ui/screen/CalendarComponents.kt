package ir.yaddasht.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.ui.theme.*
import ir.yaddasht.app.util.FaDate
import ir.yaddasht.app.util.fa
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

val WeekDaysFa = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

// ✅ ساخت میلی‌ثانیه از تاریخ شمسی — بدون باگ (ماه میلادی از ۰)
fun dayMillis(jy: Int, jm: Int, jd: Int): Long {
    val (gy, gm, gd) = FaDate.toGregorian(jy, jm, jd)
    val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
    c.clear()
    c.set(gy, gm - 1, gd, 0, 0, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

// ✅ روز هفتهٔ اول ماه (شنبه = ۰)
fun firstDowIndex(jy: Int, jm: Int): Int {
    val (gy, gm, gd) = FaDate.toGregorian(jy, jm, 1)
    val c = Calendar.getInstance()
    c.clear()
    c.set(gy, gm - 1, gd)
    return c.get(Calendar.DAY_OF_WEEK) % 7
}

// ✅ تاریخ قمری به فارسی (تقویم دیواری شمسی-قمری)
fun hijriFa(millis: Long): String = try {
    val fmt = android.icu.text.SimpleDateFormat("d MMMM y", android.icu.util.ULocale("fa@calendar=islamic"))
    fmt.format(Date(millis))
} catch (e: Exception) { "" }

fun gregorianFa(millis: Long): String {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    return "${c.get(Calendar.YEAR)}/${(c.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}/${c.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}"
}

// 🗓️ تقویم دیواری برای انتخاب تاریخ
@Composable
fun ShamsiCalendarPickerDialog(onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val (jy0, jm0, jd0) = FaDate.jalali(System.currentTimeMillis())
    var jy by remember { mutableIntStateOf(jy0) }
    var jm by remember { mutableIntStateOf(jm0) }
    var selDay by remember { mutableIntStateOf(0) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("📅 تقویم یادآور", fontFamily = LalezarFont, fontSize = 20.sp) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (jm > 1) jm-- else { jm = 12; if (jy > jy0 - 1) jy-- } }) {
                        Icon(Icons.Filled.ChevronRight, "قبل", tint = Saffron)
                    }
                    Text("${FaDate.monthName(jm)} ${jy.fa()}", fontFamily = LalezarFont, fontSize = 18.sp,
                        color = MaterialThemeColorsOnSurface(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    IconButton(onClick = { if (jm < 12) jm++ else { jm = 1; if (jy < jy0 + 3) jy++ } }) {
                        Icon(Icons.Filled.ChevronLeft, "بعد", tint = Saffron)
                    }
                }
                val infoMillis = dayMillis(jy, jm, if (selDay > 0) selDay else 1)
                val hijri = hijriFa(infoMillis)
                if (hijri.isNotBlank()) Text("🌙 $hijri", fontSize = 11.sp,
                    color = MaterialThemeColorsOnSurface().copy(alpha = .7f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("میلادی: ${gregorianFa(infoMillis)}", fontSize = 10.sp,
                    color = MaterialThemeColorsOnSurface().copy(alpha = .5f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    WeekDaysFa.forEach { w ->
                        Text(w, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Saffron,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(4.dp))
                val leading = firstDowIndex(jy, jm)
                val len = FaDate.monthLength(jy, jm)
                val cells = List(leading) { 0 } + (1..len).toList()
                cells.chunked(7).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { d ->
                            if (d == 0) Box(Modifier.weight(1f))
                            else {
                                val isToday = jy == jy0 && jm == jm0 && d == jd0
                                val isSel = selDay == d
                                Box(Modifier.weight(1f).aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Saffron else Color.Transparent)
                                    .border(if (isToday && !isSel) 1.5.dp else 0.dp, Saffron, RoundedCornerShape(10.dp))
                                    .clickable { selDay = d },
                                    contentAlignment = Alignment.Center) {
                                    Text(d.fa(), fontSize = 13.sp,
                                        color = if (isSel) Ink else MaterialThemeColorsOnSurface(),
                                        fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        repeat(7 - row.size) { Box(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selDay > 0, onClick = { onConfirm(dayMillis(jy, jm, selDay)) }) {
                Text("ادامه ⏰", color = if (selDay > 0) Saffron else Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun MaterialThemeColorsOnSurface() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
