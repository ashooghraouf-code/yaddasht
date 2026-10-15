package ir.yaddasht.app.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.yaddasht.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderScheduler.EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return

        val title = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_TITLE).orEmpty()
        val emoji = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_EMOJI).orEmpty()

        ReminderScheduler.ensureChannel(context)

        val app = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(app)
                val habit = db.habitDao().getHabitByIdSync(habitId)

                showNotification(app, habitId, title, emoji)

                if (habit != null && !habit.archived) {
                    HabitReminderScheduler.schedule(app, habit, System.currentTimeMillis())
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, habitId: Long, title: String, emoji: String) {
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        val openPi = if (openIntent != null) {
            PendingIntent.getActivity(
                context,
                habitId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        val doneIntent = Intent(context, HabitReminderActionReceiver::class.java)
            .putExtra(HabitReminderActionReceiver.EXTRA_ACTION, HabitReminderActionReceiver.ACTION_DONE)
            .putExtra(HabitReminderScheduler.EXTRA_HABIT_ID, habitId)
            .putExtra(HabitReminderScheduler.EXTRA_HABIT_TITLE, title)
            .putExtra(HabitReminderScheduler.EXTRA_HABIT_EMOJI, emoji)

        val donePi = PendingIntent.getBroadcast(
            context,
            1_100_000_000 + habitId.toInt() * 3 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, HabitReminderActionReceiver::class.java)
            .putExtra(HabitReminderActionReceiver.EXTRA_ACTION, HabitReminderActionReceiver.ACTION_SNOOZE)
            .putExtra(HabitReminderScheduler.EXTRA_HABIT_ID, habitId)
            .putExtra(HabitReminderScheduler.EXTRA_HABIT_TITLE, title)
            .putExtra(HabitReminderScheduler.EXTRA_HABIT_EMOJI, emoji)

        val snoozePi = PendingIntent.getBroadcast(
            context,
            1_100_000_000 + habitId.toInt() * 3 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = "برنامه‌ریز چراغ راه منتظر تیک توست 🌱"

        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$emoji زمان عادت: $title")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_save, "✅ انجام شد", donePi)
            .addAction(android.R.drawable.ic_menu_edit, "⏰ ۱۰ دقیقه دیگر", snoozePi)

        if (openPi != null) {
            builder.setContentIntent(openPi)
        }

        try {
            NotificationManagerCompat.from(context)
                .notify(HabitReminderActionReceiver.notificationIdFor(habitId), builder.build())
        } catch (_: SecurityException) {
        }
    }
}
// ✅ END OF HabitReminderReceiver.kt
