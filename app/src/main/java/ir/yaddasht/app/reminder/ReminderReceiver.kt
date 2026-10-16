package ir.yaddasht.app.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.yaddasht.app.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_RECEIVER_CLASS = "ir.yaddasht.app.reminder.ReminderActionReceiver"
        private const val ACTION_EXTRA = "action"
        private const val NOTIF_ID_EXTRA = "notif_id"
        private const val ORIG_TIME_EXTRA = "orig_time"

        private const val ACTION_DONE = "done"
        private const val ACTION_SNOOZE = "snooze"
        private const val ACTION_TOMORROW = "tomorrow"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        if (id < 0) return

        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val isTask = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_TASK, false)
        val leadType = intent.getIntExtra(ReminderScheduler.EXTRA_LEAD_TYPE, 0)
        val leadMinutes = intent.getIntExtra(ReminderScheduler.EXTRA_LEAD_MINUTES, 0)

        ReminderScheduler.ensureChannel(context)

        val pending = goAsync()

        try {
            val originalTime = System.currentTimeMillis() + leadMinutes.toLong() * 60_000L
            showNotification(context, id, title, isTask, leadMinutes, originalTime)
        } catch (_: Exception) {
        } finally {
            pending.finish()
        }
    }

    private fun showNotification(
        context: Context,
        id: Long,
        title: String,
        isTask: Boolean,
        leadMinutes: Int,
        originalTime: Long
    ) {
        val openIntent = Intent(context, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        openIntent.putExtra("open_id", id)
        openIntent.putExtra("open_is_task", isTask)

        val openPi = PendingIntent.getActivity(
            context,
            id.toInt() + 900_000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent()
        doneIntent.setClassName(context, ACTION_RECEIVER_CLASS)
        doneIntent.putExtra(ACTION_EXTRA, ACTION_DONE)
        doneIntent.putExtra(ReminderScheduler.EXTRA_NOTE_ID, id)
        doneIntent.putExtra(ReminderScheduler.EXTRA_TITLE, title)
        doneIntent.putExtra(ReminderScheduler.EXTRA_IS_TASK, isTask)
        doneIntent.putExtra(NOTIF_ID_EXTRA, id.toInt())
        doneIntent.putExtra(ORIG_TIME_EXTRA, originalTime)

        val donePi = PendingIntent.getBroadcast(
            context,
            id.toInt() + 1_000_000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent()
        snoozeIntent.setClassName(context, ACTION_RECEIVER_CLASS)
        snoozeIntent.putExtra(ACTION_EXTRA, ACTION_SNOOZE)
        snoozeIntent.putExtra(ReminderScheduler.EXTRA_NOTE_ID, id)
        snoozeIntent.putExtra(ReminderScheduler.EXTRA_TITLE, title)
        snoozeIntent.putExtra(ReminderScheduler.EXTRA_IS_TASK, isTask)
        snoozeIntent.putExtra(NOTIF_ID_EXTRA, id.toInt())
        snoozeIntent.putExtra(ORIG_TIME_EXTRA, originalTime)

        val snoozePi = PendingIntent.getBroadcast(
            context,
            id.toInt() + 2_000_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tomorrowIntent = Intent()
        tomorrowIntent.setClassName(context, ACTION_RECEIVER_CLASS)
        tomorrowIntent.putExtra(ACTION_EXTRA, ACTION_TOMORROW)
        tomorrowIntent.putExtra(ReminderScheduler.EXTRA_NOTE_ID, id)
        tomorrowIntent.putExtra(ReminderScheduler.EXTRA_TITLE, title)
        tomorrowIntent.putExtra(ReminderScheduler.EXTRA_IS_TASK, isTask)
        tomorrowIntent.putExtra(NOTIF_ID_EXTRA, id.toInt())
        tomorrowIntent.putExtra(ORIG_TIME_EXTRA, originalTime)

        val tomorrowPi = PendingIntent.getBroadcast(
            context,
            id.toInt() + 3_000_000,
            tomorrowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val kindLabel = if (isTask) "وظیفه" else "یادداشت"
        val leadLabel = when {
            leadMinutes == 0 -> "سر وقت"
            leadMinutes < 60 -> "$leadMinutes دقیقه قبل"
            leadMinutes < 1440 -> "${leadMinutes / 60} ساعت قبل"
            else -> "${leadMinutes / 1440} روز قبل"
        }

        val body = "$kindLabel • $leadLabel"

        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title.ifBlank { kindLabel })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_menu_save, "✅ انجام شد", donePi)
            .addAction(android.R.drawable.ic_menu_edit, "⏰ ۵ دقیقه دیگر", snoozePi)
            .addAction(android.R.drawable.ic_menu_today, "📅 فردا", tomorrowPi)

        try {
            NotificationManagerCompat.from(context)
                .notify(id.toInt(), builder.build())
        } catch (_: SecurityException) {
        }
    }
}
