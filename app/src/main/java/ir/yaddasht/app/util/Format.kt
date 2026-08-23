package ir.yaddasht.app.util

import ir.yaddasht.app.util.FaDate

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
        else -> {
            val (jy, jm, jd) = FaDate.jalali(millis)
            "${jd.fa()} ${FaDate.monthName(jm)}"
        }
    }
}
