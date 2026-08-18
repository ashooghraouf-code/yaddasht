package ir.yaddasht.app.util

import java.util.Calendar

object FaDate {
    private val MONTHS = listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")
    private val WEEKDAYS = listOf("یکشنبه","دوشنبه","سه‌شنبه","چهارشنبه","پنجشنبه","جمعه","شنبه")

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

    fun full(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val (jy, jm, jd) = toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH))
        val wd = WEEKDAYS[c.get(Calendar.DAY_OF_WEEK)-1]
        return "$wd، ${jd.fa()} ${MONTHS[jm-1]} ${jy.fa()}"
    }

    fun short(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val (_, jm, jd) = toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH))
        return "${jd.fa()} ${MONTHS[jm-1]}"
    }
}