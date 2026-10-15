package ir.yaddasht.app.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.yaddasht.app.MainActivity
import ir.yaddasht.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        if (id < 0) return

        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val isTask = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_TASK, false)
        val leadType = intent.getIntExtra(ReminderScheduler.EXTRA_LEAD_TYPE, 0)
        val leadMinutes = intent.getIntExtra(ReminderScheduler.EXTRA_LEAD_MINUTES, 0)

        ReminderScheduler.ensureChannel(context)

        val pending = goAsync()
        val app = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val originalTime = System.currentTimeMillis() + leadMinutes.toLong() * 60_000L
                showNotification(app, id, title, isTask, leadType, leadMinutes, originalTime)
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        id: Long,
        title: String,
        isTask: Boolean,
        leadType: Int,
        leadMinutes: Int,
        originalTime: Long
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_id", id)
            putExtra("open_is_task", isTask)
        }

        val openPi = PendingIntent.getActivity(
            context,
            id.toInt() + 900_000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            putExtra(ReminderActionReceiver.EXTRA_ACTION, ReminderActionReceiver.ACTION_DONE)
            putExtra(ReminderScheduler.EXTRA_NOTE_ID, id)
            putExtra(ReminderScheduler.EXTRA_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_IS_TASK, isTask)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, id.toInt())
            putExtra(ReminderActionReceiver.EXTRA_ORIGINAL_TIME, originalTime)
        }

        val donePi = PendingIntent.getBroadcast(
            context,
            id.toInt() + 1_000_000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            putExtra(ReminderActionReceiver.EXTRA_ACTION, ReminderActionReceiver.ACTION_SNOOZE)
            putExtra(ReminderScheduler.EXTRA_NOTE_ID, id)
            putExtra(ReminderScheduler.EXTRA_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_IS_TASK, isTask)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, id.toInt())
            putExtra(ReminderActionReceiver.EXTRA_ORIGINAL_TIME, originalTime)
        }

        val snoozePi = PendingIntent.getBroadcast(
            context,
            id.toInt() + 2_000_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tomorrowIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            putExtra(ReminderActionReceiver.EXTRA_ACTION, ReminderActionReceiver.ACTION_TOMORROW)
            putExtra(ReminderScheduler.EXTRA_NOTE_ID, id)
            putExtra(ReminderScheduler.EXTRA_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_IS_TASK, isTask)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, id.toInt())
            putExtra(ReminderActionReceiver.EXTRA_ORIGINAL_TIME, originalTime)
        }

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

        val body = buildString {
            append(kindLabel)
            append(" • ")
            append(leadLabel)
        }

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
// ✅ END OF ReminderReceiver.kt
