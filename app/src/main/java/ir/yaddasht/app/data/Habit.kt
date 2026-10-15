package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.yaddasht.app.util.FaDate
import java.util.Calendar

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,
    val emoji: String = "✅",
    val colorIndex: Int = 0,

    // BINARY = انجام شد / نشد
    // COUNT  = شمارشی، مثلا ۸ لیوان آب یا ۲۵ دقیقه مطالعه
    val kind: String = "BINARY",

    val targetPerDay: Int = 1,
    val unit: String = "بار",

    // DAILY | WEEKLY_DAYS | EVERY_N
    // اختیاری: @HH:mm برای یادآور ساعتی
    // مثال: DAILY@21:30
    val pattern: String = "DAILY",

    // برای WEEKLY_DAYS
    // بیت ۰ = شنبه، بیت ۱ = یکشنبه، ... بیت ۶ = جمعه
    val weekdaysMask: Int = 0,

    // برای EVERY_N
    val everyNDays: Int = 1,

    val cueText: String = "",
    val rewardText: String = "",

    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object HabitSchedule {

    const val DAILY = "DAILY"
    const val WEEKLY_DAYS = "WEEKLY_DAYS"
    const val EVERY_N = "EVERY_N"

    private const val DAY_MS = 86_400_000L

    fun basePattern(pattern: String): String =
        pattern.substringBefore("@").trim().uppercase()

    fun reminderMinutes(pattern: String): Int? {
        val at = pattern.indexOf("@")
        if (at < 0) return null

        val time = pattern.substring(at + 1).trim()
        val parts = time.split(":")
        if (parts.size != 2) return null

        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null

        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun makePattern(base: String, minutes: Int?): String {
        val cleanBase = base.trim().uppercase()

        if (minutes == null) return cleanBase

        val h = (minutes / 60).coerceIn(0, 23)
        val m = (minutes % 60).coerceIn(0, 59)

        return cleanBase + "@" +
            h.toString().padStart(2, '0') + ":" +
            m.toString().padStart(2, '0')
    }

    fun dateKey(millis: Long): String {
        val d = FaDate.jalali(millis)

        return buildString {
            append(d.first.toString().padStart(4, '0'))
            append("-")
            append(d.second.toString().padStart(2, '0'))
            append("-")
            append(d.third.toString().padStart(2, '0'))
        }
    }

    /**
     * شنبه = 0، یکشنبه = 1، دوشنبه = 2، سه‌شنبه = 3،
     * چهارشنبه = 4، پنجشنبه = 5، جمعه = 6
     */
    fun weekdayIndex(millis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = millis

        // Calendar.DAY_OF_WEEK: شنبه=1، یکشنبه=2، ... جمعه=7
        return (c.get(Calendar.DAY_OF_WEEK) + 6) % 7
    }

    fun isScheduledOn(habit: Habit, millis: Long): Boolean {
        return when (basePattern(habit.pattern)) {
            WEEKLY_DAYS -> {
                if (habit.weekdaysMask == 0) {
                    true
                } else {
                    val idx = weekdayIndex(millis)
                    ((habit.weekdaysMask shr idx) and 1) == 1
                }
            }

            EVERY_N -> {
                val n = habit.everyNDays.coerceAtLeast(1)
                val base = if (habit.createdAt > 0L) habit.createdAt else millis
                val diff = daysBetween(base, millis)

                diff >= 0L && diff % n == 0L
            }

            else -> true
        }
    }

    fun nextTriggerMillis(habit: Habit, from: Long): Long? {
        val minutes = reminderMinutes(habit.pattern) ?: return null

        val hour = minutes / 60
        val minute = minutes % 60

        for (offset in 0..400) {
            val c = Calendar.getInstance()
            c.timeInMillis = from
            c.add(Calendar.DAY_OF_YEAR, offset)

            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, minute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)

            val candidate = c.timeInMillis

            if (candidate > from && isScheduledOn(habit, candidate)) {
                return candidate
            }
        }

        return null
    }

    fun isDone(habit: Habit, log: HabitLog?): Boolean {
        if (log == null) return false
        if (log.rest) return true

        return if (habit.kind == "COUNT") {
            log.count >= habit.targetPerDay.coerceAtLeast(1)
        } else {
            log.count >= 1
        }
    }

    fun progressText(habit: Habit, log: HabitLog?): String {
        if (habit.kind != "COUNT") return ""

        val current = log?.count ?: 0
        val target = habit.targetPerDay.coerceAtLeast(1)

        return "$current/$target ${habit.unit}"
    }

    private fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis

        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)

        return c.timeInMillis
    }

    private fun daysBetween(from: Long, to: Long): Long {
        val diff = startOfDay(to) - startOfDay(from)
        return diff / DAY_MS
    }
}
