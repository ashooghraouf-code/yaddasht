package ir.yaddasht.app.data

import ir.yaddasht.app.util.FaDate
import java.util.Calendar
import kotlin.math.max

/**
 * موتور محاسبهٔ زمان‌بندی عادت‌ها.
 *
 * الگوهای پشتیبانی‌شده (ذخیره‌شده در فیلد `Habit.pattern`):
 *  - "DAILY@21:00"          → هر روز ساعت ۲۱:۰۰
 *  - "WEEKLY_DAYS@08:30"    → روزهای منتخب هفته (با weekdaysMask) ساعت ۰۸:۳۰
 *  - "EVERY_N@10:00"        → هر N روز (everyNDays) ساعت ۱۰:۰۰
 *  - "INTERVAL_HOURS@3"     → هر ۳ ساعت یک‌بار (یادآور دوره‌ای قرص/آب/...)    ← جدی‌د
 *
 * نکتهٔ کلیدی دربارهٔ «INTERVAL_HOURS»:
 *   این الگو به زمانِ روز وابسته نیست؛ از لحظهٔ ساخت عادت (createdAt) شروع شده و
 *   هر `N` ساعت یک‌بار یادآور می‌اندازد. پس از هر اجرای یادآور، HabitReminderReceiver
 *   دوباره `schedule(context, habit, now)` را صدا می‌زند و nextTriggerMillis با همان
 *   `now` مقدار «now + N ساعت» را برمی‌گرداند؛ یعنی حلقهٔ تکرار خودکار است و نیازی
 *   به زمان‌بندی دستی روزانه ندارد.
 */
object HabitSchedule {

    const val DAILY = "DAILY"
    const val WEEKLY_DAYS = "WEEKLY_DAYS"
    const val EVERY_N = "EVERY_N"

    /** هر چند ساعت یک‌بار (یادآور دوره‌ای داخل روز). */
    const val INTERVAL_HOURS = "INTERVAL_HOURS"

    private const val DAY_MS = 86_400_000L
    private const val HOUR_MS = 3_600_000L

    /** حداکثر بازهٔ مجاز برای INTERVAL_HOURS (۲۴ ساعت = معادل روزانه). */
    const val MAX_INTERVAL_HOURS = 24

    /** بخش پایهٔ الگو (قبل از «@»)، با حروف بزرگ. */
    fun basePattern(pattern: String): String =
        pattern.substringBefore('@').trim().uppercase()

    /** برای الگوهای روزانه: دقیقهٔ ساعتِ روز (۰..۱۴۳۹) یا null. */
    fun reminderMinutes(pattern: String): Int? {
        val idx = pattern.indexOf('@')
        if (idx < 0) return null
        val parts = pattern.substring(idx + 1).trim().split(':')
        if (parts.size == 2) {
            val h = parts[0].trim().toIntOrNull() ?: return null
            val m = parts[1].trim().toIntOrNull() ?: return null
            if (h in 0..23 && m in 0..59) return h * 60 + m
        }
        return null
    }

    /** برای الگوی دوره‌ای: تعداد ساعت بین دو یادآور (۱..۲۴) یا ۰ اگر نامعتبر باشد. */
    fun intervalHours(pattern: String): Int {
        val idx = pattern.indexOf('@')
        if (idx < 0) return 0
        val raw = pattern.substring(idx + 1).trim()
        // هر دو فرمت "3" و "3:00" پشتیبانی می‌شوند؛ فقط بخش ساعت لحاظ می‌شود.
        val h = raw.substringBefore(':').trim().toIntOrNull() ?: return 0
        return h.coerceIn(1, MAX_INTERVAL_HOURS)
    }

    /** ساخت الگوی روزانه: "BASE@HH:MM". */
    fun makePattern(base: String, minutes: Int? = null): String {
        val b = base.trim().uppercase()
        if (minutes == null) return b
        val h = (minutes / 60).coerceIn(0, 23)
        val m = (minutes % 60).coerceIn(0, 59)
        return "$b@${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    /** ساخت الگوی دوره‌ای: "INTERVAL_HOURS@N". */
    fun makeIntervalPattern(hours: Int): String =
        "$INTERVAL_HOURS@${hours.coerceIn(1, MAX_INTERVAL_HOURS)}"

    /** آیا این الگو از نوع دوره‌ای (هر چند ساعت) است؟ */
    fun isInterval(pattern: String): Boolean = basePattern(pattern) == INTERVAL_HOURS

    fun dateKey(millis: Long): String {
        val (y, m, d) = FaDate.jalali(millis)
        return "${y.toString().padStart(4, '0')}-" +
            "${m.toString().padStart(2, '0')}-" +
            "${d.toString().padStart(2, '0')}"
    }

    fun weekdayIndex(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return get(Calendar.DAY_OF_WEEK) % 7
    }

    /**
     * آیا در این لحظه باید اجرا شود؟
     * برای INTERVAL_HOURS همیشه true است (زمان‌بندی مبتنی‌بر بازه است، نه روز).
     */
    fun isScheduledOn(habit: Habit, millis: Long): Boolean {
        val base = basePattern(habit.pattern)
        return when (base) {
            WEEKLY_DAYS -> habit.weekdaysMask == 0 ||
                ((habit.weekdaysMask shr weekdayIndex(millis)) and 1) == 1
            EVERY_N -> {
                val n = max(habit.everyNDays, 1)
                val origin = if (habit.createdAt > 0) habit.createdAt else millis
                val days = daysBetween(origin, millis)
                days >= 0 && days % n == 0L
            }
            INTERVAL_HOURS -> true
            else -> true // DAILY و سایر
        }
    }

    /**
     * نزدیک‌ترین لحظهٔ اجرا بعد از `from`.
     * - برای INTERVAL_HOURS: نزدیک‌ترین نقطهٔ شبکهٔ «origin + k*interval» که > from باشد.
     * - برای بقیه: نزدیک‌ترین روز آینده که در ساعت ثابتِ یادآور و isScheduledOn برقرار باشد.
     */
    fun nextTriggerMillis(habit: Habit, from: Long): Long? {
        val base = basePattern(habit.pattern)

        // ── الگوی دوره‌ای: هر چند ساعت یک‌بار ─────────────────────────────
        if (base == INTERVAL_HOURS) {
            val hours = intervalHours(habit.pattern)
            if (hours <= 0) return null
            val intervalMs = hours * HOUR_MS
            val origin = if (habit.createdAt > 0) habit.createdAt else from
            var next = origin + intervalMs
            // اگر چند بازه جا مانده باشد، تا نخستین لحظهٔ آینده جلو برو
            // (یادآورهای از‌دست‌رفته را پشت سر هم نمی‌اندازد).
            while (next <= from) next += intervalMs
            return next
        }

        // ── الگوهای روزانه با ساعت ثابت ────────────────────────────────────
        val minutes = reminderMinutes(habit.pattern) ?: return null
        val h = minutes / 60
        val m = minutes % 60
        for (i in 0..400) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = from
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val t = cal.timeInMillis
            if (t > from && isScheduledOn(habit, t)) return t
        }
        return null
    }

    fun isDone(habit: Habit, log: HabitLog?): Boolean {
        if (log == null) return false
        if (log.rest) return true
        return if (habit.kind == "COUNT") {
            log.count >= max(habit.targetPerDay, 1)
        } else {
            log.count >= 1
        }
    }

    fun progressText(habit: Habit, log: HabitLog?): String {
        if (habit.kind == "COUNT") {
            val count = log?.count ?: 0
            return "$count/${max(habit.targetPerDay, 1)} ${habit.unit}"
        }
        return ""
    }

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun daysBetween(from: Long, to: Long): Long =
        (startOfDay(to) - startOfDay(from)) / DAY_MS
}
