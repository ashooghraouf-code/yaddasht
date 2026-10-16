package ir.yaddasht.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.HabitSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HabitReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(EXTRA_ACTION) ?: return
        val habitId = intent.getLongExtra(HabitReminderScheduler.EXTRA_HABIT_ID, -1L)
        val title = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_TITLE).orEmpty()

        if (habitId < 0) return

        try {
            NotificationManagerCompat.from(context).cancel(notificationIdFor(habitId))
        } catch (_: SecurityException) {
        }

        val app = context.applicationContext
        val pending = goAsync()

        when (action) {
            ACTION_DONE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.get(app)
                        val habit = db.habitDao().getHabitByIdSync(habitId)

                        if (habit != null) {
                            db.habitDao().markDone(
                                habitId,
                                HabitSchedule.dateKey(System.currentTimeMillis()),
                                habit.targetPerDay,
                                habit.kind == "COUNT"
                            )
                        }
                    } catch (_: Exception) {
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(app, "✅ «$title» انجام شد", Toast.LENGTH_SHORT).show()
                        pending.finish()
                    }
                }
            }

            ACTION_SNOOZE -> {
                HabitReminderScheduler.scheduleOneShot(
                    app,
                    habitId,
                    title,
                    intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_EMOJI).orEmpty(),
                    System.currentTimeMillis() + 10 * 60_000L
                )

                Toast.makeText(app, "⏰ ۱۰ دقیقهٔ دیگه یادآوری می‌شه", Toast.LENGTH_SHORT).show()
                pending.finish()
            }

            else -> pending.finish()
        }
    }

    companion object {
        const val EXTRA_ACTION = "habit_action"
        const val ACTION_DONE = "habit_done"
        const val ACTION_SNOOZE = "habit_snooze"

        fun notificationIdFor(habitId: Long): Int = 500_000_000 + habitId.toInt()
    }
}
