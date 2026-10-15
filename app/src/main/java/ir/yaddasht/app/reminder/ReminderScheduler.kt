package ir.yaddasht.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ir.yaddasht.app.data.Habit
import ir.yaddasht.app.data.HabitSchedule

object HabitReminderScheduler {

    const val EXTRAHABITID = "habit_id"
    const val EXTRAHABITTITLE = "habit_title"
    const val EXTRAHABITEMOJI = "habit_emoji"
    const val EXTRATRIGGERTIME = "trigger_time"

    fun schedule(context: Context, habit: Habit, from: Long = System.currentTimeMillis()) {
        val trigger = HabitSchedule.nextTriggerMillis(habit, from)

        if (trigger == null) {
            cancel(context, habit.id)
            return
        }

        setAlarm(context, habit.id, habit.title, habit.emoji, trigger, dailyCode(habit.id))
    }

    fun scheduleOneShot(
        context: Context,
        habitId: Long,
        title: String,
        emoji: String,
        trigger: Long
    ) {
        setAlarm(context, habitId, title, emoji, trigger, oneShotCode(habitId))
    }

    fun cancel(context: Context, habitId: Long) {
        cancelCode(context, dailyCode(habitId))
        cancelCode(context, oneShotCode(habitId))
    }

    private fun setAlarm(
        context: Context,
        habitId: Long,
        title: String,
        emoji: String,
        trigger: Long,
        code: Int
    ) {
        val intent = Intent(context, HabitReminderReceiver::class.java)
            .putExtra(EXTRAHABITID, habitId)
            .putExtra(EXTRAHABITTITLE, title)
            .putExtra(EXTRAHABITEMOJI, emoji)
            .putExtra(EXTRATRIGGERTIME, trigger)

        val pi = PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAGUPDATECURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(AlarmManager::class.java)

        if (canExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } else {
            am.setWindow(AlarmManager.RTCWAKEUP, trigger, 10 * 60000L, pi)
        }
    }

    private fun cancelCode(context: Context, code: Int) {
        val pi = PendingIntent.getBroadcast(
            context,
            code,
            Intent(context, HabitReminderReceiver::class.java),
            PendingIntent.FLAGNOCREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pi != null) {
            val am = context.getSystemService(AlarmManager::class.java)
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun dailyCode(habitId: Long): Int = 1000000_000 + habitId.toInt()
    private fun oneShotCode(habitId: Long): Int = 1200000_000 + habitId.toInt()

    private fun canExact(am: AlarmManager): Boolean =
        if (Build.VERSION.SDKINT >= Build.VERSIONCODES.S) am.canScheduleExactAlarms() else true
}
// ✅ END OF HabitReminderScheduler.kt
