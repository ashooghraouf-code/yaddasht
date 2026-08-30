package ir.yaddasht.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.ui.theme.DeepGreenSoft
import ir.yaddasht.app.ui.theme.Ink
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.LineGreen
import ir.yaddasht.app.ui.theme.MutedGreenText
import ir.yaddasht.app.ui.theme.PaperWhite
import ir.yaddasht.app.ui.theme.Saffron
import ir.yaddasht.app.util.FaDate
import ir.yaddasht.app.util.fa
import java.util.Calendar

private const val PD_MS = 86_400_000L
private val PD_WEEK = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
private val PD_HIJRI = arrayOf("محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه")
private val PD_GREG = arrayOf("ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن", "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر")

private fun pdJalaliMillis(jy: Int, jm: Int, jd: Int, hour: Int): Long {
    var est = 1617220800000L + (jy - 1400).toLong() * 365L * PD_MS + (jm - 1).toLong() * 30L * PD_MS + (jd - 1).toLong() * PD_MS
    for (i in 0 until 700) {
        val (y, m, d) = FaDate.jalali(est)
        if (y == jy && m == jm && d == jd) {
            val c = Calendar.getInstance()
            c.timeInMillis = est
            c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
        val dir = when { y < jy -> 1; y > jy -> -1; m < jm -> 1; m > jm -> -1; d < jd -> 1; else -> -1 }
        est += dir * PD_MS
    }
    return est
}

private fun pdLeading(jy: Int, jm: Int): Int {
    val c = Calendar.getInstance(); c.timeInMillis = pdJalaliMillis(jy, jm, 1, 12)
    return when (c.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> 0; Calendar.SUNDAY -> 1; Calendar.MONDAY -> 2; Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4; Calendar.THURSDAY -> 5; else -> 6
    }
}

private fun pdMonthLen(jy: Int, jm: Int): Int {
    val (ny, nm) = if (jm == 12) jy + 1 to 1 else jy to jm + 1
    return ((pdJalaliMillis(ny, nm, 1, 12) - pdJalaliMillis(jy, jm, 1, 12)) / PD_MS).toInt()
}

private fun pdHijri(millis: Long): Triple<Int, Int, Int> {
    val cal = android.icu.util.IslamicCalendar(); cal.timeInMillis = millis + PD_MS
    return Triple(cal.get(android.icu.util.Calendar.MONTH) + 1, cal.get(android.icu.util.Calendar.DAY_OF_MONTH), cal.get(android.icu.util.Calendar.YEAR))
}

private fun pdGreg(millis: Long): Triple<Int, Int, Int> {
    val c = Calendar.getInstance(); c.timeInMillis = millis
    return Triple(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.YEAR))
}

@Composable
fun LeadToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp),
        color = if (selected) Saffron else DeepGreenSoft,
        border = BorderStroke(1.dp, if (selected) Saffron else LineGreen)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) Text("✓ ", fontSize = 11.sp, color = Ink, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = if (selected) Ink else PaperWhite)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YadavarDatePickerDialog(onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val (tjy, tjm, tjd) = FaDate.jalali(System.currentTimeMillis())
    var calJy by remember { mutableIntStateOf(tjy) }
    var calJm by remember { mutableIntStateOf(tjm) }
    var calDay by remember { mutableIntStateOf(tjd) }
    var showTime by remember { mutableStateOf(false) }

    val selMillis = pdJal
