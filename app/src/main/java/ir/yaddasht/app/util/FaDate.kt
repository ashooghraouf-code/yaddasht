package ir.yaddasht.app.util

import java.util.Calendar

object FaDate {

    private fun toFa(n: Int): String = n.toString().map { ('۰' + (it - '0')) }.joinToString("")

    fun monthName(m: Int): String = when (m) {
        1 -> "فروردین"; 2 -> "اردیبهشت"; 3 -> "خرداد"; 4 -> "تیر"
        5 -> "مرداد"; 6 -> "شهریور"; 7 -> "مهر"; 8 -> "آبان"
        9 -> "آذر"; 10 -> "دی"; 11 -> "بهمن"; 12 -> "اسفند"
        else -> ""
    }

    // ---------- شمسی به میلادی (الگوریتم تست‌شده: ۱۴۰۵/۶/۱ → 2026-08-23) ----------
    fun toGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy2 = jy + 1595
        var days = -355668L + 365L * jy2 + (jy2 / 33) * 8 + ((jy2 % 33) + 3) / 4 + jd +
                (if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186)
        var gy = (400 * (days / 146097)).toInt()
        days %= 146097
        if (days > 36524) {
            days--
            gy += (100 * (days / 36524)).toInt()
            days %= 36524
            if (days >= 365) days++
        }
        gy += (4 * (days / 1461)).toInt()
        days %= 1461
        if (days > 365) {
            gy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        var gd = (days + 1).toInt()
        val sal = intArrayOf(0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > sal[gm]) { gd -= sal[gm]; gm++ }
        return Triple(gy, gm, gd)
    }

    private fun gregorianMillis(gy: Int, gm: Int, gd: Int): Long {
        val c = Calendar.getInstance()
        c.clear()
        c.set(gy, gm - 1, gd, 0, 0, 0)
        return c.timeInMillis
    }

    // ---------- ✅ epoch اصلاح‌شده (دیگر یک ماه جلو نمی‌رود) ----------
    fun epoch(jy: Int, jm: Int, jd: Int): Long {
        val (gy, gm, gd) = toGregorian(jy, jm, jd)
        return gregorianMillis(gy, gm, gd)
    }

    fun monthLength(jy: Int, jm: Int): Int {
        val c1 = epoch(jy, jm, 1)
        val (ny, nm) = if (jm == 12) (jy + 1) to 1 else jy to (jm + 1)
        val c2 = epoch(ny, nm, 1)
        return ((c2 - c1 + 43200000L) / 86400000L).toInt()
    }

    private fun dayStart(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // ---------- میلادی به شمسی (تست‌شده: 2026-08-23 → ۱۴۰۵/۶/۱) ----------
    fun jalali(millis: Long): Triple<Int, Int, Int> {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        var jy = c.get(Calendar.YEAR) - 621
        val cur = dayStart(millis)
        var start = epoch(jy, 1, 1)
        if (cur < start) {
            jy--
            start = epoch(jy, 1, 1)
        } else {
            val next = epoch(jy + 1, 1, 1)
            if (cur >= next) { jy++; start = next }
        }
        var dayOfYear = ((cur - start + 43200000L) / 86400000L).toInt()
        var jm = 1
        while (jm <= 12) {
            val len = monthLength(jy, jm)
            if (dayOfYear < len) break
            dayOfYear -= len
            jm++
        }
        return Triple(jy, jm, dayOfYear + 1)
    }

    // ---------- نمایش کامل فارسی ----------
    fun full(millis: Long): String {
        val (jy, jm, jd) = jalali(millis)
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        val wd = when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنجشنبه"
            else -> "جمعه"
        }
        return "$wd، ${toFa(jd)} ${monthName(jm)} ${toFa(jy)}"
    }
}

// ---------- توابع کمکی فارسی ----------
fun Int.fa(): String = this.toString().map { ('۰' + (it - '0')) }.joinToString("")

fun String.faDigits(): String = this.map { if (it in '0'..'9') ('۰' + (it - '0')) else it }.joinToString("")

fun relativeTimeFa(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val min = diff / 60000
    return when {
        min < 1 -> "همین حالا"
        min < 60 -> "${min.toInt().fa()} دقیقه پیش"
        min < 1440 -> "${(min / 60).toInt().fa()} ساعت پیش"
        min < 43200 -> "${(min / 1440).toInt().fa()} روز پیش"
        else -> FaDate.jalali(millis).let { "${it.third.fa()} ${FaDate.monthName(it.second)}" }
    }
}
