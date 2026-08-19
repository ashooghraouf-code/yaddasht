package ir.yaddasht.app.util

import java.util.Calendar

object FaDate {
    private val MONTHS = listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")
    private val WEEKDAYS = listOf("یکشنبه","دوشنبه","سه‌شنبه","چهارشنبه","پنجشنبه","جمعه","شنبه")

    fun monthName(jm: Int): String = MONTHS.getOrElse(jm - 1) { "" }

    fun jalali(ts: Long): Triple<Int, Int, Int> {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    private fun isLeap(jy: Int): Boolean {
        val r = ((jy + 1595) % 33 + 33) % 33
        return r in listOf(1, 5, 9, 13, 17, 22, 26, 30)
    }

    private fun toJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gdm = intArrayOf(0,31,59,90,120,151,181,212,243,273,304,334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666L + 365L*gy + ((gy2+3)/4) - ((gy2+99)/100) + ((gy2+399)/400) + gd + gdm[gm-1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) { jy += ((days-1)/365).toInt(); days = (days-1)%365 }
        val jm: Int; val jd: Int
        if (days < 186) { jm = 1 + (days/31).toInt(); jd = 1 + (days%31).toInt() }
        else { jm = 7 + ((days-186)/30).toInt(); jd = 1 + ((days-186)%30).toInt() }
        return Triple(jy.toInt(), jm, jd)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy2 = jy + 1595
        var days = -355668L + 365L*jy2 + (jy2/33)*8 + ((jy2%33)+3)/4 + jd + (if (jm < 7) (jm-1)*31 else (jm-7)*30 + 186)
        var gy = (400 * (days / 146097)).toInt()
        days %= 146097
        if (days > 36524) { gy += (100 * (--days / 36524)).toInt(); days %= 36524; if (days >= 365) days++ }
        gy += (4 * (days / 1461)).toInt()
        days %= 1461
        if (days > 365) { gy += ((days-1)/365).toInt(); days = (days-1)%365 }
        var gd = (days + 1).toInt()
        val sal = intArrayOf(0,31, if ((gy%4==0 && gy%100!=0) || gy%400==0) 29 else 28,31,30,31,30,31,31,30,31,30,31)
        var gm = 0
        while (gm < 12 && gd > sal[gm]) { gd -= sal[gm]; gm++ }
        return Triple(gy, gm + 1, gd)
    }

    fun epoch(jy: Int, jm: Int, jd: Int): Long {
        val (gy, gm, gd) = toGregorian(jy, jm, jd)
        return Calendar.getInstance().apply {
            set(gy, gm - 1, gd, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun full(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val (jy, jm, jd) = jalali(ts)
        val wd = WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1]
        return "$wd، ${jd.fa()} ${MONTHS[jm-1]} ${jy.fa()}"
    }

    fun short(ts: Long): String {
        val (_, jm, jd) = jalali(ts)
        return "${jd.fa()} ${MONTHS[jm-1]}"
    }
}
