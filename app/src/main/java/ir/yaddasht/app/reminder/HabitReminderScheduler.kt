package ir.yaddasht.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.Habit
import ir.yaddasht.app.data.HabitSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ✅ FINAL-SCHED-v2
object HabitReminderScheduler {

    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_HABIT_TITLE = "habit_title"
    const val EXTRA_HABIT_EMOJI = "habit_emoji"
    const val EXTRA_TRIGGER_TIME = "trigger_time"

    private const val RECEIVER_CLASS = "ir.yaddasht.app.reminder.HabitReminderReceiver"

    suspend fun rescheduleAll(context: Context): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val habits = AppDatabase.get(app).habitDao().getActiveHabitsSync()

        var scheduled = 0
        habits.forEach { habit ->
            if (HabitSchedule.nextTriggerMillis(habit, now) != null) {
                schedule(app, habit, now)
                scheduled++
            } else {
                cancel(app, habit.id)
            }
        }
        scheduled
    }

    fun schedule(context: Context, habit: Habit, from: Long = System.currentTimeMillis()) {
        val app = context.applicationContext
        val trigger = HabitSchedule.nextTriggerMillis(habit, from)

        if (trigger == null) {
            cancel(app, habit.id)
            return
        }

        setAlarm(app, habit.id, habit.title, habit.emoji, trigger, dailyCode(habit.id))
    }

    fun scheduleOneShot(
        context: Context,
        habitId: Long,
        title: String,
        emoji: String,
        trigger: Long
    ) {
        setAlarm(context.applicationContext, habitId, title, emoji, trigger, oneShotCode(habitId))
    }

    fun cancel(context: Context, habitId: Long) {
        val app = context.applicationContext
        cancelCode(app, dailyCode(habitId))
        cancelCode(app, oneShotCode(habitId))
    }

    private fun setAlarm(
        context: Context,
        habitId: Long,
        title: String,
        emoji: String,
        trigger: Long,
        code: Int
    ) {
        val intent = Intent()
        intent.setClassName(context, RECEIVER_CLASS)
        intent.putExtra(EXTRA_HABIT_ID, habitId)
        intent.putExtra(EXTRA_HABIT_TITLE, title)
        intent.putExtra(EXTRA_HABIT_EMOJI, emoji)
        intent.putExtra(EXTRA_TRIGGER_TIME, trigger)

        val pi = PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(AlarmManager::class.java)

        if (canExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, trigger, 10 * 60_000L, pi)
        }
    }

    private fun cancelCode(context: Context, code: Int) {
        val intent = Intent()
        intent.setClassName(context, RECEIVER_CLASS)

        val pi = PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pi != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pi)
            pi.cancel()
        }
    }

    private fun dailyCode(habitId: Long): Int = 1_000_000_000 + habitId.toInt()
    private fun oneShotCode(habitId: Long): Int = 1_200_000_000 + habitId.toInt()

    private fun canExact(am: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
}
