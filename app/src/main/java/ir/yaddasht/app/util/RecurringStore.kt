package ir.yaddasht.app.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class RecurringTask(
    val id: Long,
    val title: String,
    val emoji: String,
    val colorIndex: Int,
    val mode: Int,
    val everyN: Int,
    val weekDays: List<Int>,
    val monthDay: Int,
    val hour: Int,
    val minute: Int,
    val createdAt: Long
) {
    companion object {
        const val MODE_DAILY = 0
        const val MODE_WEEKLY = 1
        const val MODE_EVERY_N = 2
        const val MODE_MONTHLY = 3
    }
}

object RecurringStore {
    private const val PREFS = "recurring_store"
    private const val KEY_TASKS = "tasks"
    private const val KEY_DONE = "done"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun tasks(c: Context): List<RecurringTask> {
        val json = prefs(c).getString(KEY_TASKS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                val wd = o.optJSONArray("weekDays")
                val days = if (wd == null) emptyList() else (0 until wd.length()).map { i -> wd.getInt(i) }
                RecurringTask(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    emoji = o.optString("emoji", "🔁"),
                    colorIndex = o.optInt("colorIndex", 0),
                    mode = o.optInt("mode", 0),
                    everyN = o.optInt("everyN", 2),
                    weekDays = days,
                    monthDay = o.optInt("monthDay", 1),
                    hour = o.optInt("hour", 8),
                    minute = o.optInt("minute", 0),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(c: Context, list: List<RecurringTask>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("emoji", t.emoji)
                put("colorIndex", t.colorIndex)
                put("mode", t.mode)
                put("everyN", t.everyN)
                put("weekDays", JSONArray(t.weekDays))
                put("monthDay", t.monthDay)
                put("hour", t.hour)
                put("minute", t.minute)
                put("createdAt", t.createdAt)
            })
        }
        prefs(c).edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    fun add(c: Context, t: RecurringTask) {
        save(c, tasks(c) + t)
    }

    fun update(c: Context, t: RecurringTask) {
        save(c, tasks(c).map { if (it.id == t.id) t else it })
    }

    fun remove(c: Context, id: Long) {
        save(c, tasks(c).filterNot { it.id == id })
        val done = prefs(c).getStringSet(KEY_DONE, emptySet()) ?: emptySet()
        prefs(c).edit().putStringSet(KEY_DONE, done.filterNot { it.startsWith("$id|") }).apply()
    }

    fun isDueToday(t: RecurringTask, cal: Calendar): Boolean {
        return when (t.mode) {
            RecurringTask.MODE_DAILY -> true
            RecurringTask.MODE_WEEKLY -> t.weekDays.contains(cal.get(Calendar.DAY_OF_WEEK))
            RecurringTask.MODE_EVERY_N -> {
                val n = t.everyN.coerceAtLeast(1)
                val days = ((dayStart(cal) - dayStartMs(t.createdAt)) / 86_400_000L).toInt()
                days >= 0 && days % n == 0
            }
            RecurringTask.MODE_MONTHLY -> cal.get(Calendar.DAY_OF_MONTH) == t.monthDay
            else -> true
        }
    }

    private fun dayStart(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun dayStartMs(ms: Long): Long {
        val c = Calendar.getInstance(); c.timeInMillis = ms
        return dayStart(c)
    }

    fun dayKey(cal: Calendar): String {
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    fun isDone(c: Context, t: RecurringTask, cal: Calendar): Boolean {
        val set = prefs(c).getStringSet(KEY_DONE, emptySet()) ?: emptySet()
        return set.contains("${t.id}|${dayKey(cal)}")
    }

    fun setDone(c: Context, t: RecurringTask, cal: Calendar, done: Boolean) {
        val set = (prefs(c).getStringSet(KEY_DONE, emptySet()) ?: emptySet()).toMutableSet()
        val key = "${t.id}|${dayKey(cal)}"
        if (done) set.add(key) else set.remove(key)
        prefs(c).edit().putStringSet(KEY_DONE, set).apply()
    }

    fun streak(c: Context, t: RecurringTask, cal: Calendar): Int {
        var count = 0
        val c = cal.clone() as Calendar
        if (!isDone(c, t, c)) c.add(Calendar.DAY_OF_YEAR, -1)
        while (true) {
            if (!isDueToday(t, c)) { c.add(Calendar.DAY_OF_YEAR, -1); if (count == 0 && c.timeInMillis < dayStart(cal) - 400L * 86_400_000L) break; continue }
            if (isDone(c, t, c)) { count++; c.add(Calendar.DAY_OF_YEAR, -1) } else break
            if (count > 3650) break
        }
        return count
    }

    fun modeLabel(t: RecurringTask): String {
        return when (t.mode) {
            RecurringTask.MODE_DAILY -> "هر روز"
            RecurringTask.MODE_WEEKLY -> "هفتگی"
            RecurringTask.MODE_EVERY_N -> "هر ${t.everyN} روز"
            RecurringTask.MODE_MONTHLY -> "روز ${t.monthDay} هر ماه"
            else -> ""
        }
    }
}
