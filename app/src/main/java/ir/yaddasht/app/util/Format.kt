package ir.yaddasht.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.faDigits(): String = map { c -> if (c in '0'..'9') ('۰' + (c - '0')) else c }.joinToString("")
fun Int.fa(): String = toString().faDigits()

fun relativeTimeFa(ts: Long): String {
    val minutes = (System.currentTimeMillis() - ts) / 60_000
    return when {
        minutes < 1 -> "همین حالا"
        minutes < 60 -> "${minutes.toInt().fa()} دقیقه پیش"
        minutes < 24 * 60 -> "${(minutes / 60).toInt().fa()} ساعت پیش"
        minutes < 7 * 24 * 60 -> "${(minutes / (24 * 60)).toInt().fa()} روز پیش"
        else -> SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("fa")).format(Date(ts))
    }
}
